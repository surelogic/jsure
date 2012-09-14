package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.TypeUtils.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Pair;

class MethodBinder {
	private static final Logger LOG = AbstractJavaBinder.LOG;
	
    private final HashMap<Pair<IJavaType,IJavaType>,Boolean> callCompatCache = 
    	new HashMap<Pair<IJavaType,IJavaType>, Boolean>();
	
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
    	final Pair<IJavaType, IJavaType> key = Pair.getInstance(t1, t2);
    	Boolean result = callCompatCache.get(key);
    	if (result == null) {
    		result = typeEnvironment.isCallCompatible(t1, t2);
    		callCompatCache.put(key, result);
    	}
    	return result;
    }
    
    private class SearchState {
    	final TypeUtils utils = new TypeUtils(typeEnvironment);
    	final Iterable<IBinding> methods;
    	final IRNode targs;		
    	final IRNode args; 
    	final IJavaType[] argTypes;
    	final int numTypeArgs;
    	
    	BindingInfo bestMethod = null;
    	IJavaType bestClass = null; // type of containing class
    	final IJavaType[] bestArgs;
    	final IJavaType[] tmpTypes;
    	MethodState bestState;
    	
		SearchState(Iterable<IBinding> methods, IRNode targs,
				IRNode args, IJavaType[] argTypes) {
			this.methods = methods;
			this.targs = targs;
			this.args = args;
			this.argTypes = argTypes;
			numTypeArgs = AbstractJavaBinder.numChildrenOrZero(targs);
			bestArgs = new IJavaType[argTypes.length];
			tmpTypes = new IJavaType[argTypes.length];
		}

		void updateBestMethod(MethodState m) {    		
    		IRNode tdecl = JJNode.tree.getParent(JJNode.tree.getParent(m.match.method.getNode()));
    		IJavaType tmpClass = typeEnvironment.convertNodeTypeToIJavaType(tdecl);
    		// we don't detect the case that there is no best method.
    		
    		// TODO FIX
    		if (bestMethod == null || useMatch(m, tmpClass)) { 
    			// BUG: this algorithm does the wrong
    			// thing in the case of non-overridden multiple inheritance
    			// But there's no right thing to do, so...
    			System.arraycopy(tmpTypes, 0, bestArgs, 0, bestArgs.length);
    			bestMethod = m.match;
    			bestClass = tmpClass;      
    			bestState = m;
    		}
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
		private boolean useMatch(MethodState match, IJavaType tmpClass) {
			// Handle simple cases of shadowing/overriding
			if (areEqual(bestArgs,tmpTypes)) {
				return typeEnvironment.isSubType(tmpClass,bestClass);
			}
			/* 
			 * One fixed-arity member method named m is more specific than another member
			 * method of the same name and arity if all of the following conditions hold:
			 * -- The declared types of the parameters of the first member method are T1, ... , Tn.
			 * -- The declared types of the parameters of the other method are U1, ... , Un.
			 * -- If the second method is generic then let R1 ... Rp , be its formal type
			 *    parameters, let Bl be the declared bound of Rl, , let A1 ... Ap be the
			 *    actual type arguments inferred (§15.12.2.7) for this invocation under the initial
			 *    constraints Ti << Ui, and let Si = Ui[R1 = A1, ..., Rp = Ap] ; otherwise let Si = Ui .
			 *    
			 * Conditions:
			 * -- For all j from 1 to n, Tj <: Sj.
			 * -- If the second method is a generic method as described above then Al <:
			 *    Bl[R1 = A1, ..., Rp = Ap], .
			 */
		 	final IJavaType[] sArgs;
			if (bestState.numTypeFormals > 0) {
				sArgs = computeEquivalentArgs(match);
				if (sArgs == null) {
					return false;
				}
			} else {
				sArgs = bestArgs;
			}
			if (typeEnvironment.isAssignmentCompatible(sArgs,tmpTypes)) { 				
				return (isStatic(match.bind.getNode()) && isStatic(bestState.bind.getNode())) ||
				       typeEnvironment.isSubType(tmpClass,bestClass);
	    		//return true;
	    	}
	    	return false;
		}

		private boolean areEqual(IJavaType[] s, IJavaType[] t) {
			for(int i=0; i<s.length; i++) {
				if (!s[i].isEqualTo(typeEnvironment, t[i])) {
					return false;
				}
			}
			return true;
		}

		private boolean isStatic(IRNode m) {
			return JavaNode.getModifier(m, JavaNode.STATIC);
		}
		
		private IJavaType[] computeEquivalentArgs(MethodState match) {
		 	final IJavaType[] u = bestArgs;
		 	// Infer actual type arguments
		 	final Constraints constraints = 
	     		utils.getEmptyConstraints(new HashMap<IJavaType,IJavaType>(bestState.substMap), false, false); 		 	
	     	for(int i=0; i<bestArgs.length; i++) {
	     		// Ui >> Ti
	     		constraints.addConstraints(u[i], tmpTypes[i]); 
	     	}
	     	final Mapping a = constraints.computeTypeMapping();
	     	if (!a.checkIfSatisfiesBounds()) {
	     		return null;
	     	}
	     	// Compute equiv args
		 	final IJavaType[] s = new IJavaType[bestArgs.length];
		 	for(int i=0; i<bestArgs.length; i++) {
		 		s[i] = a.substitute(u[i]);
		 	}
		 	return s;
		}    	
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
    	final SearchState state = new SearchState(methods, targs, args, argTypes);
    	BindingInfo best  = findMostSpecificApplicableMethod(state, false, false);
    	if (best == null) {
    		best = findMostSpecificApplicableMethod(state, true, false);
    		if (best == null) {
    			best = findMostSpecificApplicableMethod(state, true, true);
    		}
    	}
    	return best;
    }
    
    private BindingInfo findMostSpecificApplicableMethod(final SearchState s, 
    		boolean allowBoxing, boolean allowVarargs) {            	
    	for(final IBinding mbind : s.methods) {
    		final IRNode mdecl = mbind.getNode();
    		if (debug) {
    			LOG.finer("Considering method binding: " + mdecl + " : " + DebugUnparser.toString(mdecl)
    					+ binder.getInVersionString());
    		}
    		final MethodState m = isApplicable(s, allowBoxing, allowVarargs, mbind);
    		if (m.match != null) {
    			s.updateBestMethod(m);
    		}
    	}
        return s.bestMethod;
	}
    
    private class MethodState {
    	final SearchState search;
    	final IBinding bind;
    	final IRNode formals;
    	final IRNode typeFormals;
    	final int numTypeFormals;
    	final IJavaTypeSubstitution methodTypeSubst;
    	final Map<IJavaType,IJavaType> substMap;
    	BindingInfo match;
    	
    	MethodState(SearchState s, IBinding m) {
    		search = s;
    		bind = m;
    		
    		// Setup various info about the method
        	final IRNode mdecl = m.getNode();
        	final Operator op  = JJNode.tree.getOperator(mdecl);      
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
        	numTypeFormals = AbstractJavaBinder.numChildrenOrZero(typeFormals);
        	
        	// Compute a type substitution if needed
        	IJavaTypeSubstitution subst = IJavaTypeSubstitution.NULL;
        	if (s.numTypeArgs != 0) {
        		if (s.numTypeArgs == numTypeFormals) {
        			// Use explicit type arguments
        			subst = FunctionParameterSubstitution.create(binder, m, s.targs);
        		}
        	} 
        	methodTypeSubst = subst;
        	
        	if (numTypeFormals != 0) {
        		substMap = new HashMap<IJavaType,IJavaType>();
        	} else {
        		substMap = Collections.emptyMap();
        	}
    	}

		void initSubstMap() { 	
	    	if (numTypeFormals != 0) {
	    		int i = 0;
	    		for(IRNode tf : JJNode.tree.children(typeFormals)) {
	    			IJavaTypeFormal jtf = JavaTypeFactory.getTypeFormal(tf);
	    			/*
		    		System.out.println("Mapping "+jtf+" within "+
		    				JavaNames.getFullName(VisitUtil.getEnclosingDecl(jtf.getDeclaration())));
	    			 */
	    			if (search.numTypeArgs == 0) {    		
	    				IJavaType subst = bind.convertType(jtf); 
	    				substMap.put(jtf, subst); // FIX slow lookup
	    			} else {
	    				IRNode targ = TypeActuals.getType(search.targs, i);
	    				IJavaType targT = binder.getJavaType(targ);
	    				substMap.put(jtf, targT);    			
	    			}
	    			i++;
	    		}
	    	}
		}

		IRNode getVarargsType() {
			IRNode varType;
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
	    	return varType;
		}
    }
    
    private MethodState isApplicable(final SearchState s, final boolean allowBoxing, final boolean allowVarargs, 
    		final IBinding mbind) { 
    	final MethodState m = new MethodState(s, mbind);
    	if (s.numTypeArgs != 0 && s.numTypeArgs != m.numTypeFormals) {
    		// No way for this method to be applicable
    		m.match = null;
    		return m; 
    	}    	
    	m.initSubstMap();
    	
    	m.match = matchedParameters(s, allowBoxing, allowVarargs, m);    	
    	if (m.match == null && !m.substMap.isEmpty()) {
    		// Necessary to match "raw" usages of a method
    		m.substMap.clear();
    		m.match = matchedParameters(s, allowBoxing, allowVarargs, m);    	
    	}    	
    	return m;
    }
    
	private BindingInfo matchedParameters(final SearchState s, final boolean allowBoxing,
			final boolean allowVarargs, final MethodState m) {
    	final IRNode varType = m.getVarargsType();        
    	// Check that the #params matches #args
    	final int numFormals = JJNode.tree.numChildren(m.formals);
    	if (allowVarargs && varType != null) {
    		if (s.argTypes.length < numFormals - 1) {
    			if (debug) {
    				LOG.finer("Wrong number of parameters.");
    			}
    			return null;
    		}
    	} 
    	else if (numFormals != s.argTypes.length) {
    		if (debug) {
    			LOG.finer("Wrong number of parameters.");
    		}
    		return null;
    	}    	    	    	
     	int numBoxed = 0;    	    	
    	
    	// First, capture type variables
    	// (expanding varargs to fill in what would be null)
     	final TypeUtils.Constraints constraints =  s.utils.getEmptyConstraints(m.substMap, allowBoxing, allowVarargs);    	
    	final Iterator<IRNode> fe = JJNode.tree.children(m.formals);
    	IJavaType varArgBase = null;
    	for (int i=0; i < s.argTypes.length; ++i) {
    		IJavaType fty;
    		if (!fe.hasNext()) {
    			// No more formals .. so it has to be varargs for a match
    			if (!allowVarargs) {
    				return null;
    			}
    			else if (varType == null) {
    				LOG.severe("Not enough parameters to continue");
    				return null;
    			}
    			else if (varArgBase == null) {
    				LOG.severe("No varargs type to copy");
    				return null;
    			}
    			// Expanding to match the number of arguments
    			fty = varArgBase;
    		} else {
    			IRNode ptype  = ParameterDeclaration.getType(fe.next());    		
    			fty = binder.getTypeEnvironment().convertNodeTypeToIJavaType(ptype);
    			
    			fty = m.bind.convertType(fty);
    	
    			if (allowVarargs && ptype == varType) {
    				// Check if I need to use varargs
    				// 1. Last formal (varargs) is before the last argument
    				//	  f(Object, Object...) vs f(a, b1, b2) 
    				// 2. Last formal matches up with the last argument, and is not call compatible
    				if (i < s.argTypes.length-1 || 
    					(i==s.argTypes.length-1 && !isCallCompatible(fty, s.argTypes[i]))) {
    					// was !(s.argTypes[i] instanceof IJavaArrayType))) {
    			
    					// Convert the varargs type to get the element type
    					IJavaArrayType at = (IJavaArrayType) fty;
    					varArgBase = at.getElementType();      					
    					fty = varArgBase;
    				}
    			}
    		}
    		if (allowBoxing) {
    			// Handle boxing
    			IJavaType[] newArgTypes = handleBoxing(fty, s.argTypes, i);
    			if (newArgTypes != s.argTypes) {
    				// arg[i] was (un)boxed
    				// 
    				// TODO is this right?  why is it modifying the args 
    				s.argTypes[i] = newArgTypes[i];
    				numBoxed++;
    			}
    		}
    		s.tmpTypes[i] = fty;    		
    		if (m.numTypeFormals > 0 && s.numTypeArgs == 0) {
    			// Need to infer the types
    			constraints.addConstraints(fty, s.argTypes[i]);
    		}
    	}
    	final TypeUtils.Mapping map = constraints.computeTypeMapping();
    	
    	// Then, substitute and check if compatible
    	final boolean isVarArgs = varType != null;
    	for (int i=0; i < s.argTypes.length; ++i) {       
    		IJavaType fty      = s.tmpTypes[i];
    		IJavaType captured = m.substMap == null ? binder.getTypeEnvironment().computeErasure(fty) : 
    			map.substitute(fty);          
    		if (!isCallCompatible(captured,s.argTypes[i])) {        	
    			// Check if need (un)boxing
    			if (allowBoxing && onlyNeedsBoxing(captured, s.argTypes[i])) {
    				numBoxed++;
    				continue;
    			}    			
    			if (isVarArgs && i == s.argTypes.length-1 && captured instanceof IJavaArrayType &&
    					s.argTypes[i] instanceof IJavaArrayType) {
    				// issue w/ the last/varargs parameter
    				final IJavaArrayType at = (IJavaArrayType) captured;
    				final IJavaType eltType = at.getElementType();
    				if (allowVarargs && s.args != null) {
    					final IRNode varArg = Arguments.getArg(s.args, i); 
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
    				LOG.finer("... but " + s.argTypes[i] + " !<= " + captured);
    			}
    			return null;
    		}
    	}
    	map.export(m.substMap);

    	final IJavaTypeSubstitution subst;
    	if (!m.substMap.isEmpty() && m.methodTypeSubst == IJavaTypeSubstitution.NULL) {
    		//System.out.println("Making method subst for "+JavaNames.getFullName(mbind.getNode()));
    		subst = 
    			FunctionParameterSubstitution.create(binder, m.bind.getNode(), m.substMap);
    	} else {
    		/*
    		if (mSubst != IJavaTypeSubstitution.NULL) {
    			System.out.println("Using explicit type arguments at call: "+mSubst);
    		}
    		*/
    		subst = m.methodTypeSubst;
    	}
    	if (subst != IJavaTypeSubstitution.NULL) {
    		return new BindingInfo(IBinding.Util.makeMethodBinding(m.bind, subst), numBoxed, isVarArgs);
    	}
    	return new BindingInfo(m.bind, numBoxed, isVarArgs);
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
    	if (formal instanceof IJavaPrimitiveType) {
       		// Could unbox arg?
    		if (arg instanceof IJavaDeclaredType) {        	
    			IJavaDeclaredType argD = (IJavaDeclaredType) arg;
    			IJavaType unboxed = JavaTypeFactory.getCorrespondingPrimType(argD);
    			return unboxed != null && isCallCompatible(formal, unboxed);  
    		} 
    		else if (arg instanceof IJavaReferenceType) {
    			IJavaPrimitiveType formalP = (IJavaPrimitiveType) formal;
    			IJavaType boxedEquivalent = JavaTypeFactory.getCorrespondingDeclType(typeEnvironment, formalP);
    			return boxedEquivalent != null && isCallCompatible(boxedEquivalent, arg);
    		}
    	}
    	else if (formal instanceof IJavaReferenceType && arg instanceof IJavaPrimitiveType) {
    		// Could box arg?
    		IJavaPrimitiveType argP = (IJavaPrimitiveType) arg;
    		IJavaType boxed         = JavaTypeFactory.getCorrespondingDeclType(typeEnvironment, argP);
    		return boxed != null && isCallCompatible(formal, boxed); 
    	}
    	return false;
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