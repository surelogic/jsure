package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Hashtable2;

class MethodBinder {
	private static final Logger LOG = AbstractJavaBinder.LOG;
	
    private final Hashtable2<IJavaType,IJavaType,Boolean> callCompatCache = 
    	new Hashtable2<IJavaType,IJavaType,Boolean>();
	
	private final boolean debug;
	private final AbstractJavaBinder binder;
	private final ITypeEnvironment typeEnvironment;
	
	MethodBinder(AbstractJavaBinder b, boolean debug) {
		binder = b;
		typeEnvironment = b.getTypeEnvironment();
		this.debug = debug;
	}
    
    private boolean isCallCompatible(IJavaType t1, IJavaType t2) {
    	if (t1 == null || t2 == null) {    	
    		return false;
    	}
    	Boolean result = callCompatCache.get(t1, t2);
    	if (result == null) {
    		result = typeEnvironment.isCallCompatible(t1, t2);
    		callCompatCache.put(t1, t2, result);
    	}
    	return result;
    }
	
	/**
	 * 15.12.2.5 Choosing the Most Specific Method If more than one member
	 * method is both accessible and applicable to a method invocation, it is
	 * necessary to choose one to provide the descriptor for the run-time method
	 * dispatch. The Java programming language uses the rule that the most
	 * specific method is chosen. The informal intuition is that one method is
	 * more specific than another if any invocation handled by the first method
	 * could be passed on to the other one without a compile-time type error.
	 */
    private boolean useMatch(BindingInfo best, IJavaType bestClass, IJavaType[] bestArgs, BindingInfo match, IJavaType tmpClass, IJavaType[] tmpTypes) {    
    	if (!match.usedVarArgs && best.usedVarArgs) {
    		return true;
    	}
    	if (best.numBoxed > match.numBoxed) {
    		return true;
    	}
    	if (typeEnvironment.isAssignmentCompatible(bestArgs,tmpTypes) && 
			typeEnvironment.isSubType(tmpClass,bestClass)) {
    		return best.numBoxed >= match.numBoxed;
    	}
    	return false;
	}
    
	/**
	 * The process of determining applicability begins by determining the
	 * potentially applicable methods (§15.12.2.1). The remainder of the process
	 * is split into three phases.
	 * 
	 * The first phase (§15.12.2.2) performs overload resolution without
	 * permitting boxing or unboxing conversion, or the use of variable arity
	 * method invocation. If no applicable method is found during this phase
	 * then processing continues to the second phase.
	 * 
	 * The second phase (§15.12.2.3) performs overload resolution while allowing
	 * boxing and unboxing, but still precludes the use of variable arity method
	 * invocation. If no applicable method is found during this phase then
	 * processing continues to the third phase.
	 * 
	 * The third phase (§15.12.2.4) allows overloading to be combined with
	 * variable arity methods, boxing and unboxing. Deciding whether a method is
	 * applicable will, in the case of generic methods (§8.4.4), require that
	 * actual type arguments be determined. Actual type arguments may be passed
	 * explicitly or implicitly. If they are passed implicitly, they must be
	 * inferred (§15.12.2.7) from the types of the argument expressions. If
	 * several applicable methods have been identified during one of the three
	 * phases of applicability testing, then the most specific one is chosen, as
	 * specified in section §15.12.2.5. See the following subsections for
	 * details.
	 */
    BindingInfo findBestMethod(Iterable<IBinding> methods, IRNode targs, IRNode args, IJavaType[] argTypes) {
        BindingInfo bestMethod = null;
        IJavaType bestClass = null; // type of containing class
        IJavaType[] bestArgs = new IJavaType[argTypes.length];
        IJavaType[] tmpTypes = new IJavaType[argTypes.length];
        
        final int numTypeArgs = AbstractJavaBinder.numChildrenOrZero(targs);
    	findMethod: for(final IBinding mbind : methods) {
    		final IRNode mdecl = mbind.getNode();
    		if (debug) {
    			LOG.finer("Considering method binding: " + mdecl + " : " + DebugUnparser.toString(mdecl)
    					+ binder.getInVersionString());
    		}
    		final IRNode typeFormals = SomeFunctionDeclaration.getTypes(mbind.getNode());
    		final int numTypeFormals = AbstractJavaBinder.numChildrenOrZero(typeFormals);
    		IJavaTypeSubstitution methodTypeSubst = IJavaTypeSubstitution.NULL;
    		if (numTypeArgs != 0) {
    			if (numTypeArgs != numTypeFormals) {
    				continue findMethod;
    			} else {
    				// Use explicit type arguments
    				methodTypeSubst = FunctionParameterSubstitution.create(binder, mbind, targs);
    			}
    		}    

    		final BindingInfo match = matchMethod(targs, args, argTypes, mbind, tmpTypes, methodTypeSubst);
    		if (match == null) {
    			continue findMethod;
    		}

    		IRNode tdecl = JJNode.tree.getParent(JJNode.tree.getParent(match.method.getNode()));
    		IJavaType tmpClass = typeEnvironment.convertNodeTypeToIJavaType(tdecl);
    		// we don't detect the case that there is no best method.
    		if (bestMethod == null || useMatch(bestMethod, bestClass, bestArgs, match, tmpClass, tmpTypes)) { 
    			// BUG: this algorithm does the wrong
    			// thing in the case of non-overridden multiple inheritance
    			// But there's no right thing to do, so...
    			IJavaType[] t = bestArgs;
    			bestArgs = tmpTypes;
    			tmpTypes = t;
    			bestMethod = match;
    			bestClass = tmpClass;      
    		}
    	}
        return bestMethod;
    }
    
	/**
     * @param tmpTypes The types matched against
     * @return non-null if mbind matched the arguments
     */
    private BindingInfo matchMethod(IRNode targs, IRNode args, IJavaType[] argTypes, 
                                  IBinding mbind, IJavaType[] tmpTypes,
                                  final IJavaTypeSubstitution mSubst) {
      final int numTypeArgs = AbstractJavaBinder.numChildrenOrZero(targs);
      IRNode mdecl = mbind.getNode();
      Operator op  = JJNode.tree.getOperator(mdecl);      
      IRNode formals;
      IRNode typeFormals;
      if (op instanceof MethodDeclaration) {
        formals = MethodDeclaration.getParams(mdecl);
        typeFormals = MethodDeclaration.getTypes(mdecl);
        /*
        if ("toArray".equals(MethodDeclaration.getId(mdecl))) {
        	System.out.println(DebugUnparser.toString(mdecl));
        	System.out.println();
        }
        */
      } else {
        formals = ConstructorDeclaration.getParams(mdecl);
        typeFormals = ConstructorDeclaration.getTypes(mdecl);
      }
      int numTypeFormals = JJNode.tree.numChildren(typeFormals);
      Map<IJavaType,IJavaType> map;
      if (numTypeFormals != 0) {
        map = new HashMap<IJavaType,IJavaType>();
        int i = 0;
        for(IRNode tf : JJNode.tree.children(typeFormals)) {
    		IJavaTypeFormal jtf = JavaTypeFactory.getTypeFormal(tf);
    		/*
    		System.out.println("Mapping "+jtf+" within "+
    				JavaNames.getFullName(VisitUtil.getEnclosingDecl(jtf.getDeclaration())));
			*/
    		if (numTypeArgs == 0) {    		
    			IJavaType subst = mbind.convertType(jtf); 
    			map.put(jtf, subst); // FIX slow lookup
    		} else {
    			IRNode targ = TypeActuals.getType(targs, i);
    			IJavaType targT = binder.getJavaType(targ);
    			map.put(jtf, targT);    			
    		}
    		i++;
        }
      } else {
        map = Collections.emptyMap();
      }
      BindingInfo matched = matchedParameters(targs, args, argTypes, mbind, formals, tmpTypes, 
    		                               map, mSubst);
      if (matched == null) {
    	map = Collections.emptyMap();
        matched = matchedParameters(targs, args, argTypes, mbind, formals, tmpTypes, map, mSubst);
      }
      return matched;
    }
    
    private BindingInfo matchedParameters(IRNode targs, IRNode args, IJavaType[] argTypes, 
    		IBinding mbind, IRNode formals, IJavaType[] tmpTypes, 
    		final Map<IJavaType,IJavaType> map,
    		final IJavaTypeSubstitution mSubst) {
    	// Get the last parameter 
    	final IRNode varType;
    	IRLocation lastLoc = JJNode.tree.lastChildLocation(formals);
    	if (lastLoc != null) {
    		IRNode lastParam = JJNode.tree.getChild(formals,lastLoc);
    		IRNode ptype = ParameterDeclaration.getType(lastParam);
    		if (VarArgsType.prototype.includes(ptype)) {
    			if (debug) {
    				LOG.finer("Handling variable numbers of parameters.");
    			}
    			varType = ptype;
    		} else {
    			varType = null;
    		}
    	} else {
    		varType = null;
    	}
    	final int numFormals = JJNode.tree.numChildren(formals);
    	if (varType != null) {
    		if (argTypes.length < numFormals - 1) {
    			if (debug) {
    				LOG.finer("Wrong number of parameters.");
    			}
    			return null;
    		}
    	} 
    	else if (numFormals != argTypes.length) {
    		if (debug) {
    			LOG.finer("Wrong number of parameters.");
    		}
    		return null;
    	}    	    	    	
     	int numBoxed = 0;    	    	
    	
    	// First, capture type variables
    	// (expanding varargs to fill in what would be null)
    	final Iterator<IRNode> fe = JJNode.tree.children(formals);
    	IJavaType varArgBase = null;
    	for (int i=0; i < argTypes.length; ++i) {
    		IJavaType fty;
    		if (!fe.hasNext()) {
    			if (varType == null) {
    				LOG.severe("Not enough parameters to continue");
    				return null;
    			}
    			else if (varArgBase == null) {
    				LOG.severe("No varargs type to copy");
    				return null;
    			}
    			// Expanded from 
    			fty = varArgBase;
    		} else {
    			IRNode ptype  = ParameterDeclaration.getType(fe.next());    		
    			fty = binder.getTypeEnvironment().convertNodeTypeToIJavaType(ptype);
    			//fty = JavaTypeFactory.convertNodeTypeToIJavaType(ptype,AbstractJavaBinder.this);
    			
    			fty = mbind.convertType(fty);
    			if (ptype == varType && 
    					(i < argTypes.length-1 || 
    							(i==argTypes.length-1 && !(argTypes[i] instanceof IJavaArrayType)))) {
    				// FIX what's the right way to convert if the number of args match
    				IJavaArrayType at = (IJavaArrayType) fty;
    				varArgBase = at.getElementType();     		
    				fty = varArgBase;
    			}
    		}
    		// Handle boxing
    		IJavaType[] newArgTypes = handleBoxing(fty, argTypes, i);
    		if (newArgTypes != argTypes) {
    			// arg[i] was (un)boxed
    			argTypes = newArgTypes;
    			numBoxed++;
    		}
    		tmpTypes[i] = fty;    		
    		if (map != null) {
    			capture(map, fty, argTypes[i]);
    		}
    	}
    	/*
if (argTypes.length == 1 && argTypes[0].toString().equals("java.util.List<? extends capture.B>")) {
System.out.println("matching against "+tmpTypes);
}	
    	 */
    	// Then, substitute and check if compatible
    	final boolean isVarArgs = varType != null;
    	for (int i=0; i < argTypes.length; ++i) {       
    		IJavaType fty      = tmpTypes[i];
    		IJavaType captured = map == null ? binder.getTypeEnvironment().computeErasure(fty) : substitute(map, fty);          
    		if (!isCallCompatible(captured,argTypes[i])) {        	
    			// Check if need (un)boxing
    			if (onlyNeedsBoxing(captured, argTypes[i])) {
    				numBoxed++;
    				continue;
    			}
    			if (isVarArgs && i == argTypes.length-1 && captured instanceof IJavaArrayType &&
    					argTypes[i] instanceof IJavaArrayType) {
    				// issue w/ the last/varargs parameter
    				final IJavaArrayType at = (IJavaArrayType) captured;
    				final IJavaType eltType = at.getElementType();
    				if (args != null) {
    					final IRNode varArg = Arguments.getArg(args, i); 
    					if (VarArgsExpression.prototype.includes(varArg)) {
    						inner:
    							for(IRNode arg : VarArgsExpression.getArgIterator(varArg)) {
    								final IJavaType argType = binder.getJavaType(arg);
    								if (!isCallCompatible(eltType, argType)) {        	
    									// Check if need (un)boxing
    									if (onlyNeedsBoxing(eltType, argType)) {
    										numBoxed++;
    										continue inner;
    									}
    									return null;
    								}
    							}
    					continue;
    					}
    				}
    			}
    			if (debug) {
    				LOG.finer("... but " + argTypes[i] + " !<= " + captured);
    			}
    			return null;
    		}
    	}

    	final IJavaTypeSubstitution subst;
    	if (!map.isEmpty() && mSubst == IJavaTypeSubstitution.NULL) {
    		//System.out.println("Making method subst for "+JavaNames.getFullName(mbind.getNode()));
    		subst = 
    			FunctionParameterSubstitution.create(binder, mbind.getNode(), map);
    	} else {
    		/*
    		if (mSubst != IJavaTypeSubstitution.NULL) {
    			System.out.println("Using explicit type arguments at call: "+mSubst);
    		}
    		*/
    		subst = mSubst;
    	}
    	if (subst != IJavaTypeSubstitution.NULL) {
    		return new BindingInfo(IBinding.Util.makeMethodBinding(mbind, subst), numBoxed, isVarArgs);
    	}
    	return new BindingInfo(mbind, numBoxed, isVarArgs);
    }
    
    private IJavaType[] handleBoxing(IJavaType formal, IJavaType[] argTypes, int i) {
    	final IJavaType arg = argTypes[i];
    	if (formal instanceof IJavaReferenceType && arg instanceof IJavaPrimitiveType) {
    		// Box arg to match
    		IJavaPrimitiveType argP = (IJavaPrimitiveType) arg;
    		IJavaType boxed         = JavaTypeFactory.getCorrespondingDeclType(typeEnvironment, argP);
    		IJavaType[] newArgTypes = Arrays.copyOf(argTypes, argTypes.length);
    		newArgTypes[i] = boxed;
    		return newArgTypes;
    	}
		return argTypes;
	}

	private boolean onlyNeedsBoxing(IJavaType formal, IJavaType arg) {
    	if (formal instanceof IJavaPrimitiveType && arg instanceof IJavaDeclaredType) {    
    		// Could unbox arg?
    		IJavaDeclaredType argD = (IJavaDeclaredType) arg;
    		IJavaType unboxed = JavaTypeFactory.getCorrespondingPrimType(argD);
    		return unboxed != null && isCallCompatible(formal, unboxed);  
    	}
    	else if (formal instanceof IJavaDeclaredType && arg instanceof IJavaPrimitiveType) {
    		// Could box arg?
    		IJavaPrimitiveType argP = (IJavaPrimitiveType) arg;
    		IJavaType boxed         = JavaTypeFactory.getCorrespondingDeclType(typeEnvironment, argP);
    		return boxed != null && isCallCompatible(formal, boxed);    		 
    	}
    	return false;
    }

    private IJavaType substitute(Map<IJavaType, IJavaType> map, IJavaType fty) {
    	if (fty == null) {
    		return null;
    	}
    	if (map.isEmpty()) {
    		return fty;
    	}
    	if (fty instanceof IJavaTypeFormal) {
    		IJavaType rv = map.get(fty);  
    		if (rv != null) {
    			return rv;
    		}
    	}
    	else if (fty instanceof IJavaDeclaredType) {
    		IJavaDeclaredType dt = (IJavaDeclaredType) fty;
    		// copied from captureDeclaredType
    		final int size   = dt.getTypeParameters().size(); 
    		boolean captured = false;
    		List<IJavaType> params = new ArrayList<IJavaType>(size);
    		for(int i=0; i<size; i++) {
    			IJavaType oldT = dt.getTypeParameters().get(i);            
    			IJavaType newT = substitute(map, oldT); 
    			params.add(newT);
    			if (!oldT.equals(newT)) {
    				captured = true;                
    			}
    		}
    		if (captured) {
    			return JavaTypeFactory.getDeclaredType(dt.getDeclaration(), params, 
    					dt.getOuterType());
    		}
    	}
    	else if (fty instanceof IJavaWildcardType) {
    		return substituteWildcardType(map, fty);
    	}
    	else if (fty instanceof IJavaArrayType) {
    		IJavaArrayType at = (IJavaArrayType) fty;
    		IJavaType baseT   = at.getBaseType();
    		IJavaType newBase = substitute(map, baseT);
    		if (newBase != baseT) {
    			return JavaTypeFactory.getArrayType(newBase, at.getDimensions());
    		}
    	}
    	return fty;
    }
    
    private IJavaReferenceType substituteWildcardType(
    		Map<IJavaType, IJavaType> map, IJavaType fty) {
    	IJavaWildcardType wt     = (IJavaWildcardType) fty;
    	if (wt.getUpperBound() != null) {
    		IJavaReferenceType upper = (IJavaReferenceType) substitute(map, wt.getUpperBound());
    		if (!upper.equals(wt.getUpperBound())) {
    			return JavaTypeFactory.getWildcardType(upper, null);
    		}
    	}
    	else if (wt.getLowerBound() != null) {
    		IJavaReferenceType lower = (IJavaReferenceType) substitute(map, wt.getLowerBound());
    		if (!lower.equals(wt.getLowerBound())) {
    			return JavaTypeFactory.getWildcardType(null, lower);
    		}
    	}
    	return wt;
    }
    
    /**
     * For now, try to handle the simple cases 
     * 1. T appears as the type of a parameter
     * 2. T appears as the base type of an array parameter (T[])
     * 3. T appears as a parameter of a parameterized type (Foo<T,T>)
     * 4. T appears as a bound on a wildcard (? extends T)
     * 
     * This no longer does any substitution
     */
    private void capture(Map<IJavaType, IJavaType> map,
                              IJavaType fty, IJavaType argType) {
      if (fty == null) {
        return;
      }
      if (map.isEmpty()) {
        return;
      }
      
      // Check case 1
      if (fty instanceof IJavaTypeFormal) {
    	//IJavaTypeFormal tf = (IJavaTypeFormal) fty;
    	/*
        System.out.println("Trying to capture "+fty+" within "+
        		JavaNames.getFullName(VisitUtil.getEnclosingDecl(tf.getDeclaration())));
        */
    	final IJavaType mapping = map.get(fty);        
        if (mapping instanceof IJavaTypeFormal/*== fty*/) { // no substitution yet
          // Add mapping temporarily to do substitution on extends bound
          IJavaType extendsT = mapping.getSuperclass(typeEnvironment);          
          map.put(fty, argType);
          capture(map, extendsT, argType);
          
          if (typeEnvironment.isSubType(argType, substitute(map, extendsT))) {                      
            return;
          } else {
//            System.out.println("Couldn't quite match "+fty+", "+argType);
            // restore previous mapping
            map.put(fty, mapping);
          }
        } else {
          // FIX What if it's not the identity mapping?
        }
      }
      // Check case 2
      else if (fty instanceof IJavaArrayType) {
        IJavaArrayType fat = (IJavaArrayType) fty;
        if (argType instanceof IJavaArrayType) {
          IJavaArrayType aat = (IJavaArrayType) argType;
          if (fat.getDimensions() == aat.getDimensions()) {  
        	capture(map, fat.getBaseType(), aat.getBaseType());  
          }
          else if (fat.getDimensions() < aat.getDimensions()) {          
        	final int diff = aat.getDimensions() - fat.getDimensions();
            capture(map, fat.getBaseType(), JavaTypeFactory.getArrayType(aat.getBaseType(), diff));          
          }
        }
      }
      // Check case 3
      else if (fty instanceof IJavaDeclaredType) {
        IJavaDeclaredType fdt = (IJavaDeclaredType) fty;
        final int size        = fdt.getTypeParameters().size();
        
        if (size == 0) {
          return; // No type parameters to look at   
        }
        if (argType instanceof IJavaDeclaredType) {
          IJavaDeclaredType adt = (IJavaDeclaredType) argType;
          captureDeclaredType(map, fdt, adt);
        }
      }
      // FIX This may require binding the return type first
      else if (fty instanceof IJavaWildcardType) {
        // nothing to capture
      }      
    }

    private void captureDeclaredType(final Map<IJavaType, IJavaType> map,
                                          final IJavaDeclaredType fdt, 
                                          final IJavaDeclaredType adt) {
      final int size = fdt.getTypeParameters().size();
      // Check if it's the same (parameterized) type
      if (fdt.getDeclaration().equals(adt.getDeclaration())) {
        if (size == adt.getTypeParameters().size()) {
          for(int i=0; i<size; i++) {
            IJavaType oldT = fdt.getTypeParameters().get(i);            
            capture(map, oldT, adt.getTypeParameters().get(i)); 
          }
        }
        // Looks to be a raw type of the same kind
        else if (size > 0 && adt.getTypeParameters().isEmpty()) { 
          captureMissingTypeParameters(map, fdt, adt);
        }
      }
      // Look at supertypes for type variables?
      if (ClassDeclaration.prototype.includes(fdt.getDeclaration())) {
        IJavaDeclaredType superT = adt.getSuperclass(typeEnvironment);
        if (superT != null) {
          captureDeclaredType(map, fdt, superT);
        }
      } else {
        // FIX is this right?
        for(IJavaType superT : adt.getSupertypes(typeEnvironment)) {
          capture(map, fdt, superT);
        }
      }
    }
    
    /**
     * Match up the parameters in fdt with the missing ones in adt
     */
    private void captureMissingTypeParameters(Map<IJavaType, IJavaType> map,
                                              final IJavaDeclaredType fdt, 
                                              final IJavaDeclaredType adt) {
      final Operator op = JJNode.tree.getOperator(adt.getDeclaration());            
      final IRNode formals;
      if (ClassDeclaration.prototype.includes(op)) {        
        formals = ClassDeclaration.getTypes(adt.getDeclaration());
      }
      else if (InterfaceDeclaration.prototype.includes(op)) {
        formals = InterfaceDeclaration.getTypes(adt.getDeclaration());
      }
      else {
        return; // nothing to do        
      }
      Iterator<IJavaType> fdtParams = fdt.getTypeParameters().iterator(); 
      for(IRNode tf : TypeFormals.getTypeIterator(formals)) {
        IJavaType fT = JavaTypeFactory.getTypeFormal(tf);
        capture(map, fdtParams.next(), fT.getSuperclass(typeEnvironment));
      }
    }

	IJavaType[] getFormalTypes(IJavaDeclaredType t, IRNode mdecl) {
		final IRNode formals;
		final Operator op = JJNode.tree.getOperator(mdecl);
		if (op instanceof MethodDeclaration) {
			formals = MethodDeclaration.getParams(mdecl);
			/*
	          if ("toArray".equals(MethodDeclaration.getId(mdecl))) {
	          	System.out.println(DebugUnparser.toString(mdecl));
	          	System.out.println();
	          }
			 */
		} else {
			formals = ConstructorDeclaration.getParams(mdecl);
		}
		final int numFormals = JJNode.tree.numChildren(formals);
		IJavaType[] types = new IJavaType[numFormals];
		int i=0; 
		for(IRNode n : JJNode.tree.children(formals)) {
			IRNode ptype = ParameterDeclaration.getType(n);
			types[i] = binder.getJavaType(ptype);
			i++;
		}
		return types;
	}
}

class BindingInfo {
	final IBinding method;
	final int numBoxed;
	final boolean usedVarArgs;

	BindingInfo(IBinding m, int boxed, boolean var) {
		method = m;
		numBoxed = boxed;
		usedVarArgs = var;
	}
}