package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.*;

import com.surelogic.common.Pair;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.bind.TypeUtils.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

class MethodBinder implements IMethodBinder {
	public static final boolean captureTypes = true;
	public static final boolean captureTypes2 = false;
	
	private static final Logger LOG = AbstractJavaBinder.LOG;
	
    private final HashMap<Pair<IJavaType,IJavaType>,Boolean> callCompatCache = 
    	new HashMap<Pair<IJavaType,IJavaType>, Boolean>();
    
    private final HashMap<Pair<IJavaType,IJavaType>,Boolean> callCompatCache_erasure = 
        	new HashMap<Pair<IJavaType,IJavaType>, Boolean>();
	
	private final boolean debug;
	private final AbstractJavaBinder binder;
	private final ITypeEnvironment typeEnvironment;
	
	MethodBinder(AbstractJavaBinder b, boolean debug) {
		binder = b;
		typeEnvironment = b.getTypeEnvironment();
		this.debug = debug;
	}
    
	static IJavaScope.Selector makeAccessSelector(final ITypeEnvironment typeEnvironment, final IRNode from) {
		return new IJavaScope.AbstractSelector("Ignore") {
			@Override
			public String label() {
				return "Is accessible from "+DebugUnparser.toString(from);
			}
			public boolean select(IRNode decl) {
				boolean ok = BindUtil.isAccessible(typeEnvironment, decl, from);
				return ok;
			}    	  
		};
	}
	
    private boolean isCallCompatible(IJavaType param, IJavaType arg, final boolean tryErasure) {
    	if (param == null || arg == null) {    	
    		return false;
    	}    	
    	final HashMap<Pair<IJavaType,IJavaType>,Boolean> cache = 
    	    tryErasure ? callCompatCache_erasure : callCompatCache;
    	final Pair<IJavaType, IJavaType> key = Pair.getInstance(param, arg);
    	Boolean result = cache.get(key);
    	if (result == null) {
    		result = typeEnvironment.isCallCompatible(param, arg);
    		if (!result && tryErasure) {
    		  // TODO hack
    	      IJavaType erasure = typeEnvironment.computeErasure(param);
    	      boolean oldRv = typeEnvironment.isSubType(arg, erasure);
    	      result = oldRv;
    		}
    		cache.put(key, result);
    	}
    	return result;
    }
    
    private class SearchState {
    	final TypeUtils utils = new TypeUtils(typeEnvironment);
    	final Iterable<IBinding> methods;
    	final CallState call;
    	final IJavaType[] argTypes;
    	final int numTypeArgs;
    	final boolean usesDiamondOp;
    	
    	BindingInfo bestMethod = null;
    	IJavaType bestClass = null; // type of containing class
    	final IJavaType[] bestArgs;
    	final IJavaType[] tmpTypes;
    	MethodState bestState;
    	
		SearchState(Iterable<IBinding> methods, CallState call) {
			this.methods = methods;
			this.call = call;
			this.argTypes = call.getArgTypes();
			usesDiamondOp = call.usesDiamondOp();
			numTypeArgs = call.getNumTypeArgs();
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
			// TODO hack
			if (match.match.numUsingErasure == 0 && bestMethod.numUsingErasure > 0) {				
				return true;
			}						
			if (match.match.numUsingErasure > 0 && bestMethod.numUsingErasure == 0) {				
				return false;
			}		
			
			/* 
			 * One fixed-arity member method named m is more specific than another member
			 * method of the same name and arity if all of the following conditions hold:
			 * -- The declared types of the parameters of the first member method are T1, ... , Tn.
			 * -- The declared types of the parameters of the other method are U1, ... , Un.
			 * -- If the second method is generic then let R1 ... Rp , be its formal type
			 *    parameters, let Bl be the declared bound of Rl, , let A1 ... Ap be the
			 *    actual type arguments inferred (�15.12.2.7) for this invocation under the initial
			 *    constraints Ti << Ui, and let Si = Ui[R1 = A1, ..., Rp = Ap] ; otherwise let Si = Ui .
			 *    
			 * Conditions:
			 * -- For all j from 1 to n, Tj <: Sj.
			 * -- If the second method is a generic method as described above then Al <:
			 *    Bl[R1 = A1, ..., Rp = Ap], .
			 */
		 	final IJavaType[] sArgs;
			if (bestState.isGeneric()) {
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
	     		utils.getEmptyConstraints(match.search.call, match.bind,
	     				new HashMap<IJavaType,IJavaType>(bestState.substMap), false, false); 		 	
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
	 * potentially applicable methods (�15.12.2.1). The remainder of the process
	 * is split into three phases.
	 * 
	 * The first phase (�15.12.2.2) performs overload resolution without
	 * permitting boxing or unboxing conversion, or the use of variable arity
	 * method invocation. If no applicable method is found during this phase
	 * then processing continues to the second phase.
	 * 
	 * The second phase (�15.12.2.3) performs overload resolution while allowing
	 * boxing and unboxing, but still precludes the use of variable arity method
	 * invocation. If no applicable method is found during this phase then
	 * processing continues to the third phase.
	 * 
	 * The third phase (�15.12.2.4) allows overloading to be combined with
	 * variable arity methods, boxing and unboxing. Deciding whether a method is
	 * applicable will, in the case of generic methods (�8.4.4), require that
	 * actual type arguments be determined. Actual type arguments may be passed
	 * explicitly or implicitly. If they are passed implicitly, they must be
	 * inferred (�15.12.2.7) from the types of the argument expressions. If
	 * several applicable methods have been identified during one of the three
	 * phases of applicability testing, then the most specific one is chosen, as
	 * specified in section �15.12.2.5. See the following subsections for
	 * details.
	 */
    public BindingInfo findBestMethod(final IJavaScope scope, final LookupContext context, final boolean needMethod, IRNode from, CallState call) {    	
        final IJavaScope.Selector isAccessible = makeAccessSelector(typeEnvironment, from);
        final Iterable<IBinding> methods = new Iterable<IBinding>() {
//  			@Override
  			public Iterator<IBinding> iterator() {
  				return IJavaScope.Util.lookupCallable(scope, context, isAccessible, needMethod);
  			}
        };        
        //if ("this.root(name, java.util.EnumSet.of(# . EOpt.ENDTAG))".equals(DebugUnparser.toString(call.call))) {
        /*
        if (DebugUnparser.toString(call.call).contains("versionChanges.getValue")) {
    		System.out.println("Looking at problematic call");
    	} 
    	*/   	
    	final SearchState state = new SearchState(methods, call);
    	BindingInfo best  = findMostSpecificApplicableMethod(state, false, false);
    	if (best == null && !call.needsExactInvocation()) {
    		best = findMostSpecificApplicableMethod(state, true, false);
    		if (best == null) {
    			best = findMostSpecificApplicableMethod(state, true, true);

    			// Debug code
    			if (best == null) {
    				findMostSpecificApplicableMethod(state, false, false);
    				findMostSpecificApplicableMethod(state, true, true);
    			}
    		}
    	}
    	return best;
    }
    
    private BindingInfo findMostSpecificApplicableMethod(final SearchState s, 
    		boolean allowBoxing, boolean allowVarargs) {            	
    	for(final IBinding mbind : s.methods) {
    		final IRNode mdecl = mbind.getNode();
    		if (debug) {
    			System.out.println("Considering method binding: " + mdecl + " : " + DebugUnparser.toString(mdecl)
    					+ binder.getInVersionString());
    		}
    		final MethodState m = isApplicable(s, allowBoxing, allowVarargs, mbind);
    		if (m.match != null) {
    			s.updateBestMethod(m);
    		}
    	}
        return s.bestMethod;
	}
    
    private class MethodState extends MethodBinding {
    	final SearchState search;
    	final IJavaTypeSubstitution methodTypeSubst;
    	final Map<IJavaType,IJavaType> substMap;
    	BindingInfo match;
    	
    	MethodState(SearchState s, IBinding m) {
    		super(m);
    		search = s;
        	
        	// Compute a type substitution if needed
        	IJavaTypeSubstitution subst = IJavaTypeSubstitution.NULL;
        	if (s.numTypeArgs != 0) {
        		if (s.numTypeArgs == numTypeFormals) {
        			// Use explicit type arguments
        			subst = FunctionParameterSubstitution.create(binder, m, s.call.targs);
        		}
        	} 
        	methodTypeSubst = subst;
        	
        	if (isGeneric() || search.usesDiamondOp) {
        		substMap = new HashMap<IJavaType,IJavaType>();
        	} else {
        		substMap = Collections.emptyMap();
        	}
    	}

		void initSubstMap(IBinder binder) { 	
	    	if (isGeneric()) {
	    		int i = 0;
	    		for(IRNode tf : JJNode.tree.children(typeFormals)) {
	    			IJavaTypeFormal jtf = JavaTypeFactory.getTypeFormal(tf);
	    			/*
		    		System.out.println("Mapping "+jtf+" within "+
		    				JavaNames.getFullName(VisitUtil.getEnclosingDecl(jtf.getDeclaration())));
	    			 */
	    			if (search.numTypeArgs == 0) {    		
	    				IJavaType subst = bind.convertType(binder, jtf); 
	    				substMap.put(jtf, subst); // FIX slow lookup
	    			} else {
	    				IRNode targ = search.call.targs[i];
	    				IJavaType targT = binder.getJavaType(targ);
	    				substMap.put(jtf, targT);    			
	    			}
	    			i++;
	    		}
	    	}
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
    	/*
    	if ("java.util.EnumSet.of(testBinder.hadoop_yarn_common.HamletImpl . EOpt.ENDTAG)".equals(DebugUnparser.toString(s.call.call))) {
    		System.out.println("Debugging bad subst");
    	}
    	*/
    	m.initSubstMap(binder);
    	
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
     	final TypeUtils.Constraints constraints =
     		s.utils.getEmptyConstraints(s.call, m.bind, m.substMap, allowBoxing, allowVarargs);    	
     	/*
     	final String[] issues = {
     			"EnumSet.of(anElement, otherElements)"     			
     			//"new ExpirableCache <K, V> (cache)",
     			//"new ImmutableEnumSet <E> (EnumSet.of(#, #))",
     			//"new RegularImmutableSortedSet <E> (ImmutableList.of(#), Ordering.natural)",
     			//"new ObjectStreamField (\"scope_id\", int . class)",
     			//"<implicit>.hasSameComparator(comparator, elements)",     			
     	};
     	final String unparse = DebugUnparser.toString(s.call.call);
     	for(String i : issues) {
     		if (unparse.equals(i)) {
     	 		System.out.println("Looking at "+unparse+": "+JavaNames.genRelativeFunctionName(m.bind.getNode()));
     	 		break;
     		}
     	} 
     	*/
    	final Iterator<IRNode> fe = JJNode.tree.children(m.formals);
    	IJavaType varArgBase = null;
    	boolean debug = false;
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
    			IJavaType tempFty = binder.getTypeEnvironment().convertNodeTypeToIJavaType(ptype);
    			
    			fty = m.bind.convertType(binder, tempFty);
    			/*
    			if (fty == null) {
    				System.out.println("Null parameter type was "+tempFty);
    				m.bind.convertType(binder, tempFty);
    			}
    			*/
    			
    			if (allowVarargs && ptype == varType) {
    				// Check if I need to use varargs
    				// 1. Last formal (varargs) is before the last argument
    				//	  f(Object, Object...) vs f(a, b1, b2) 
    				// 2. Last formal matches up with the last argument, and is not call compatible
    				if (i < s.argTypes.length-1 || 
    					(i==s.argTypes.length-1 && !isCallCompatible(fty, s.argTypes[i], false))) {
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
    		if (m.isGeneric() && s.numTypeArgs == 0) {
    			// Need to infer the types
    			constraints.addConstraints(fty, s.argTypes[i]);
    		}
    	}
    	if (s.usesDiamondOp) {
    		// Add constraints for the type's formal parameters 
    		final IJavaDeclaredType dt = (IJavaDeclaredType) s.call.receiverType;
    		constraints.addConstraintsForType(dt.getDeclaration());    		
    	}
    	/*
    	if ("ordering.reverse".equals(DebugUnparser.toString(s.call.call))) {
    		System.out.println("Looking at ordering.reverse");
    	}
    	*/
    	final TypeUtils.Mapping map = constraints.computeTypeMapping();
    	int numUsingErasure = 0;
    	
    	// Then, substitute and check if compatible
    	final boolean isVarArgs = varType != null;
    	for (int i=0; i < s.argTypes.length; ++i) {       
    		IJavaType fty      = s.tmpTypes[i];
    		// TODO actually just need to replace the type variables for the method
    		IJavaType captured = m.isGeneric() && m.substMap.isEmpty() ? 
    				constraints.substituteRawMapping(typeEnvironment, m.typeFormals, fty) :
    				//binder.getTypeEnvironment().computeErasure(fty) :     			
    				map.substitute(fty);
    	    if (s.call.needsExactInvocation()) {    	    
    	    	if (captured.equals(s.argTypes[i])) {
    	    		continue;
    	    	} else {
    	    		return null;
    	    	}
    		}
    		if (!isCallCompatible(captured,s.argTypes[i], false)) {        	
    			if (isCallCompatible(captured,s.argTypes[i], true)) {
    				numUsingErasure++;
    				continue;
    			}
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
    				if (allowVarargs && s.call.args != null) {
    					final IRNode varArg = s.call.args[i]; 
    					if (VarArgsExpression.prototype.includes(varArg)) {
    						inner:
    							for(IRNode arg : VarArgsExpression.getArgIterator(varArg)) {
    								final IJavaType argType = binder.getJavaType(arg);
    								if (!isCallCompatible(eltType, argType, false)) {        	
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
    	/*
    	if (map.hasUnresolvedVars()) {	    	
	    	System.err.println(DebugUnparser.toString(map.call.call)+": has unresolved vars for "+JavaNames.genRelativeFunctionName(map.method.getNode()));
    	}
    	*/

    	final IJavaDeclaredType oldContext = m.bind.getContextType();
    	final IJavaDeclaredType context;
    	final IJavaTypeSubstitution subst;
    	if (!m.substMap.isEmpty() && m.methodTypeSubst == IJavaTypeSubstitution.NULL) {
    		//System.out.println("Making method subst for "+JavaNames.getFullName(mbind.getNode()));
    		subst = 
    			FunctionParameterSubstitution.create(binder, m.bind.getNode(), m.substMap);
    		if (s.usesDiamondOp) {
    			context = computeNewContext(m, oldContext);
    		} else {
    			context = oldContext;
    		}
    	} else {
    		/*
    		if (mSubst != IJavaTypeSubstitution.NULL) {
    			System.out.println("Using explicit type arguments at call: "+mSubst);
    		}
    		*/
    		subst = m.methodTypeSubst;
    		context = oldContext;
    	}
    	IBinding newB;
    	if (subst != IJavaTypeSubstitution.NULL) {
    		newB = IBinding.Util.makeMethodBinding(m.bind, context, subst, s.call.receiverType, typeEnvironment);
    	} else {
    		newB = IBinding.Util.addReceiverType(m.bind, s.call.receiverType, typeEnvironment);
    	}
    	return new BindingInfo(newB, numBoxed, isVarArgs, numUsingErasure);
	}
    
	
	private IJavaDeclaredType computeNewContext(MethodState m, IJavaDeclaredType oldContext) {
		final IRNode decl = oldContext.getDeclaration();
		final IRNode typeParams = ClassDeclaration.getTypes(decl);
		final List<IJavaType> params = new ArrayList<IJavaType>(JJNode.tree.numChildren(typeParams));
		for(IRNode formal : TypeFormals.getTypeIterator(typeParams)) {
			final IJavaTypeFormal tf = JavaTypeFactory.getTypeFormal(formal);
			final IJavaType subst = m.substMap.get(tf);
			if (subst == null || subst == tf) {
				throw new IllegalStateException("Bad substitution for "+tf);
			}
			params.add(subst);
		}
		return JavaTypeFactory.getDeclaredType(decl, params, oldContext.getOuterType());
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

    /*
    TODO fix calls to isCallCompatible
    Math.min(...)
    */
	private boolean onlyNeedsBoxing(IJavaType formal, IJavaType arg) {
    	if (formal instanceof IJavaPrimitiveType) {
       		// Could unbox arg?
    		if (arg instanceof IJavaDeclaredType) {        	
    			IJavaDeclaredType argD = (IJavaDeclaredType) arg;
    			IJavaType unboxed = JavaTypeFactory.getCorrespondingPrimType(argD);
    			return unboxed != null && isCallCompatible(formal, unboxed, false);  
    		} 
    		else if (arg instanceof IJavaReferenceType) {
    			IJavaPrimitiveType formalP = (IJavaPrimitiveType) formal;
    			IJavaType boxedEquivalent = JavaTypeFactory.getCorrespondingDeclType(typeEnvironment, formalP);
    			return boxedEquivalent != null && isCallCompatible(boxedEquivalent, arg, false);
    		}
    	}
    	else if (formal instanceof IJavaReferenceType && arg instanceof IJavaPrimitiveType) {
    		// Could box arg?
    		IJavaPrimitiveType argP = (IJavaPrimitiveType) arg;
    		IJavaType boxed         = JavaTypeFactory.getCorrespondingDeclType(typeEnvironment, argP);
    		return boxed != null && isCallCompatible(formal, boxed, false); 
    	}
    	else if (formal instanceof IJavaDeclaredType && arg instanceof IJavaDeclaredType) {
    		IJavaDeclaredType fdt = (IJavaDeclaredType) formal;
    		IJavaDeclaredType adt = (IJavaDeclaredType) arg;    		
    		// Hack since Class can take primitive types
    		final IRNode cls = typeEnvironment.findNamedType("java.lang.Class");
    		if (fdt.getDeclaration().equals(cls) && adt.getDeclaration().equals(cls)) {
    			return onlyNeedsBoxing(fdt.getTypeParameters().get(0), adt.getTypeParameters().get(0));
    		}
    	}
    	return false;
    }

	static IJavaType[] getFormalTypes(IBinder binder, IJavaDeclaredType t, IRNode mdecl) {
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
		} else if (op instanceof ConstructorDeclaration) {
			formals = ConstructorDeclaration.getParams(mdecl);
		} else if (op instanceof AnnotationElement) {
			formals = AnnotationElement.getParams(mdecl);
		} else {
			throw new IllegalStateException("Unexpected: "+JavaNames.getFullName(mdecl));
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
	final int numUsingErasure;
	final boolean usedVarArgs;

	BindingInfo(IBinding m, int boxed, boolean var, int erasure) {
		if (m == null) {
			throw new IllegalArgumentException();
		}
		method = m;
		numBoxed = boxed;
		numUsingErasure = erasure;
		usedVarArgs = var;
	}
}