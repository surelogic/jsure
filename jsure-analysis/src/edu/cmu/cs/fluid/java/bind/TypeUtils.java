package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment.InvocationKind;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.CallState;
import edu.cmu.cs.fluid.java.bind.MethodBinder8.MethodBinding8;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class TypeUtils {
	static final boolean useNewTypeInference = true;
	
	private final ITypeEnvironment tEnv;
	private final HashMap<Pair<IJavaReferenceType, IJavaReferenceType>, JavaRefTypeProxy> proxyCache = 
		new HashMap<Pair<IJavaReferenceType,IJavaReferenceType>, JavaRefTypeProxy>();
	
	public TypeUtils(ITypeEnvironment te) {
		tEnv = te;
	}
	
	// Utility methods
	//
	public static IRNode getParametersForType(IRNode tdecl) {
		final Operator op = JJNode.tree.getOperator(tdecl);
		if (ClassDeclaration.prototype.includes(op)) {
			return ClassDeclaration.getTypes(tdecl);
		}
		else if (InterfaceDeclaration.prototype.includes(op)) {
			return InterfaceDeclaration.getTypes(tdecl);
		}
		return null;
	}
	
	/**
	 * Creates a proxy if there isn't one
	 * Gets the existing one otherwise
	 */
	private JavaRefTypeProxy getProxy(IJavaReferenceType u, IJavaReferenceType v) {
		/*
        if (u == null || v == null) {
			return null;
		}
		*/
		// Make the order consistent, since it doesn't matter which order they're in
		if (u.hashCode() > v.hashCode()) {
			IJavaReferenceType tmp = v;
			v = u;
			u = tmp;
		}
		Pair<IJavaReferenceType, IJavaReferenceType> key = Pair.getInstance(u, v);
		JavaRefTypeProxy p = proxyCache.get(key);
		if (p == null) {
			p = new JavaRefTypeProxy(u, v);
			proxyCache.put(key, p);
		}
		return p;		
	}
	
	/* XXX: 2012-11-14 Swapped upper and lower in the formal argument list because
	 * these were incorrectly reversed in the implementation, and now all calls to
	 * JavaTypeFactory.getWildcardType() need to reverse the upper and lower
	 * bounds.  Easier to swap the formals in this method than to fix the
	 * callsite of this method.
	 */
	private IJavaWildcardType getWildcardType(IJavaReferenceType lower, IJavaReferenceType upper) {
		if (lower == null && tEnv.getObjectType().equals(upper)) {
			// Simplify
			return JavaTypeFactory.wildcardType;
		}
		return JavaTypeFactory.getWildcardType(upper, lower);
	}
	
	// sec 15.12.2.7
	//	Then, for each remaining type variable Tj, the constraints Tj :> U are considered.
	//	Given that these constraints are Tj :> U1 ... Tj :> Uk, the type of Tj is inferred
	//	as lub(U1 ... Uk), computed as follows:
	//
	//  For a type U, we write ST(U) for the set of supertypes of U, 		
	//
	//  Note: this needs to include the type itself
    private Iterable<IJavaDeclaredType> getST(IJavaReferenceType... types) {
		Set<IJavaDeclaredType> st = new HashSet<IJavaDeclaredType>();
		for(IJavaReferenceType t : types) {
			if (t == null) {
				continue;
			}
			getST(st, t);
			/*
			for(IJavaType s : t.getSupertypes(tEnv)) {
				getST(st, (IJavaDeclaredType) s);
			}
			*/
		}
		return st;
    }
    
    private void getST(Set<IJavaDeclaredType> st, IJavaReferenceType t) {
    	if (st.contains(t)) {
    		return; // Already handled
    	}
    	if (t instanceof IJavaDeclaredType) {
    		st.add((IJavaDeclaredType) t);
    	} else {
    		//System.out.println("Excluded from ST: "+t);
    	}
    	
		for(IJavaType s : t.getSupertypes(tEnv)) {
			getST(st, (IJavaReferenceType) s);
		}
	}
    
	//  and define the erased supertype set of U,
	//  EST(U) = { V | W in ST(U) and V = |W| }
	//  where|W| is the erasure (�4.6) of W.
	private Set<IRNode> getEST(IJavaReferenceType t) {
		Set<IRNode> est = new HashSet<IRNode>();
		for(IJavaDeclaredType s : getST(t)) {
			est.add(s.getDeclaration());
		}
		return est;
	}
	
	//  The erased candidate set for type parameter Tj , EC, is the intersection of all
	//  the sets EST(U) for each U in U1 .. Uk. 
	private Set<IRNode> getEC(IJavaReferenceType... types) {
		final Set<IRNode> ec = new HashSet<IRNode>();
		boolean first = true;
		for(IJavaReferenceType t : types) {
			if (first) {
				ec.addAll(getEST(t));
				first = false;
			} else if (t instanceof IJavaNullType) {
				// JLS 4.10.2: 
				// The direct supertypes of the null type are all reference 
				// types other than the null type itself.
				continue;
			} else {
				ec.retainAll(getEST(t));
			}
		}
		if (ec.isEmpty()) {
			System.err.println("Empty candidate set ...");
			for(IJavaReferenceType t : types) {
				System.err.println("STs for "+t+":");
				for(IJavaDeclaredType s : getST(t)) {
					System.err.println("\t"+s);
				}
			}
			System.err.println();
		}
		return ec;
	}
	
	//  The minimal erased candidate set for Tj is
	//  MEC = { V | V in EC, and for all in EC, it is not the case that W <: V}
	private Iterable<IJavaReferenceType> getMEC(IJavaReferenceType... types) {
		final Set<IJavaType> ec = new HashSet<IJavaType>();
		for(IRNode n : getEC(types)) {
			ec.add(JavaTypeFactory.convertNodeTypeToIJavaType(n, tEnv.getBinder()));
		}
		final List<IJavaReferenceType> mec = new ArrayList<IJavaReferenceType>();
	   outer:
		for(IJavaType v : ec) {
			for(IJavaType w : ec) {
				if (v == w) {
					continue;
					//
				} else if (tEnv.isRawSubType(w, v)) {
					continue outer;
				}
			}
			mec.add((IJavaReferenceType) v);
		}
		return mec;
	}
	
	//  For any element G of MEC that is a generic type declaration, define the relevant
	//  invocations of G, Inv(G) to be:
	//  Inv(G) = { V | 1<=i<=k, V in ST(Ui), V = G<...>}
	//
	//  a.k.a Relevant(G) 
	private Iterable<IJavaDeclaredType> getInv(Iterable<IJavaDeclaredType> st, IJavaDeclaredType g) {
		if (!isGeneric(g)) {
			throw new IllegalArgumentException("Not generic: "+g);
		}
		Set<IJavaDeclaredType> inv = new HashSet<IJavaDeclaredType>();
		for(IJavaDeclaredType v : st) {
			if (g.getDeclaration().equals(v.getDeclaration())) {
				inv.add(v);
			}
		}
		return inv;
	}
	
	//  and let CandidateInvocation(G) = lci(Inv(G)) where lci, the least containing
	//  invocation is defined
	//
	//  a.k.a. Candidate()
	private IJavaReferenceType getCandidateInvocation(Iterable<IJavaDeclaredType> st, IJavaDeclaredType g) {
		return getLCI(getInv(st, g));
	}
	
	//  lci(S) = lci(e1, ..., en) where ei in S,
	//  lci(e1, ..., en) = lci(lci(e1, e2), e3, ..., en)
	//
	//  a.k.a. lcp()
	private IJavaDeclaredType getLCI(Iterable<IJavaDeclaredType> invocations) {	
		IJavaDeclaredType result = null;
		int num = 0;
		for(IJavaDeclaredType t : invocations) {
			if (result != null) {
				result = getLCI(result, t);
			} else {
				result = t;
			}
			num++;
		}
		/* TODO how does this ever help?
		if (num == 1) {
			return getLCP(result);
		}
		*/
		return result;
	}
	
	//  lci(G<X1, ..., Xn>, G<Y1, ..., Yn>) = G<lcta(X1, Y1),..., lcta(Xn, Yn)>
	private IJavaDeclaredType getLCI(IJavaDeclaredType t1, IJavaDeclaredType t2) {
		List<IJavaType> params = new ArrayList<IJavaType>();

		// TODO is this right?
		if (t1.getTypeParameters().size() == 0) {
			//System.out.println("Got raw type: "+t1);
			return t1;
		}
		if (t2.getTypeParameters().size() == 0) {
			//System.out.println("Got raw type: "+t2);
			return t2;
		}
		for(int i=0; i<t1.getTypeParameters().size();i++) {
			params.add(getLCTA(t1.getTypeParameters().get(i), 
					           t2.getTypeParameters().get(i)));
		}
		// TODO is the outer type correct?
		return JavaTypeFactory.getDeclaredType(t1.getDeclaration(), params, t1.getOuterType());
	}

	// lcp( G<X 1 , ..., X n > ) = G< lcta( X 1 ), ..., lcta( X n ) >
	private IJavaDeclaredType getLCP(IJavaDeclaredType g) {
		List<IJavaType> params = new ArrayList<IJavaType>();
		for(int i=0; i<g.getTypeParameters().size();i++) {
			params.add(getLCTA(g.getTypeParameters().get(i)));
		}
		// TODO is the outer type correct?
		return JavaTypeFactory.getDeclaredType(g.getDeclaration(), params, g.getOuterType());
	}
	
	// lcta( U ) = ? if U 's upper bound is Object , otherwise ? extends lub( U , Object )
	private IJavaType getLCTA(IJavaType u) {
		boolean upperBoundIsObject = false;
		if (u instanceof IJavaTypeVariable) {
			IJavaTypeVariable v = (IJavaTypeVariable) u;
			IJavaReferenceType upper = v.getUpperBound(tEnv);
			upperBoundIsObject = upper == null || upper.equals(tEnv.getObjectType());
		}
		else if (u instanceof IJavaWildcardType) {
			IJavaWildcardType v = (IJavaWildcardType) u;
			IJavaReferenceType upper = v.getUpperBound();
			upperBoundIsObject = upper == null || upper.equals(tEnv.getObjectType());
		}		
		
		if (upperBoundIsObject) {
			return JavaTypeFactory.wildcardType;
		}
		return getWildcardType(null, this.getLowestUpperBound((IJavaReferenceType) u, tEnv.getObjectType()));
	}
	
	//  where lcta() is the the least containing type argument function defined
	//  (assuming U and V are type expressions) as:
	//
	//  lcta(U, V) = U if U = V, ? extends lub(U, V) otherwise
	//
	//  Note that this could result in an infinite type due to the call to lub()
	private IJavaType getLCTA(IJavaType u, IJavaType v) {
		if (u instanceof IJavaWildcardType) {
			if (v instanceof IJavaWildcardType) {
				return getLCTA((IJavaWildcardType) u, (IJavaWildcardType) v);
			} else {
				if (v instanceof IJavaPrimitiveType) {
					// TODO is this right?
					//
					// This is a type bound, so most likely comes from something like int.class
					return JavaTypeFactory.wildcardType;
				}
				return getLCTA((IJavaReferenceType) v, (IJavaWildcardType) u);
			}
		}
		else if (v instanceof IJavaWildcardType) {
			if (u instanceof IJavaPrimitiveType) {
				// TODO is this right?
				//
				// This is a type bound, so most likely comes from something like int.class
				return JavaTypeFactory.wildcardType;
			}
			return getLCTA((IJavaReferenceType) u, (IJavaWildcardType) v);			
		}
		
		if (u.equals(v)) {
			return u;
		} 
		if (u instanceof IJavaPrimitiveType || v instanceof IJavaPrimitiveType) {
			// TODO is this right?
			//
			// This is a type bound, so most likely comes from something like int.class
			return JavaTypeFactory.wildcardType;
		}
		return getWildcardType(null, getLowestUpperBound((IJavaReferenceType) u, (IJavaReferenceType) v));
	}
	
	//  lcta(U, ? extends V) = ? extends lub(U, V)
	//  lcta(U, ? super V) = ? super glb(U, V)
	private IJavaType getLCTA(IJavaReferenceType u, IJavaWildcardType v) {
		if (v.getLowerBound() != null) {
			return getWildcardType(getGreatestLowerBound(u, v.getLowerBound()), null); 
		}
		if (v.getUpperBound() != null) {
			return getWildcardType(null, getLowestUpperBound(u, v.getUpperBound()));
		}
		// TODO does this just simplify to a null/Object bound?
		return getWildcardType(null, getLowestUpperBound(u, tEnv.getObjectType()));
	}

	//  1. lcta(? extends U, ? extends V) = ? extends lub(U, V)
	//  2. lcta(? extends U, ? super V) = U if U = V, ? otherwise
	//  3. lcta(? super U, ? super V) = ? super glb(U, V)
	//  where glb() is as defined in (�5.1.10).
	private IJavaType getLCTA(IJavaWildcardType u, IJavaWildcardType v) {
		if (u.getLowerBound() != null) {
			return getLCTA_super(u.getLowerBound(), v);
		}
		if (v.getLowerBound() != null) {
			return getLCTA_super(v.getLowerBound(), u);
		}
		// Case 1
		IJavaReferenceType ub = getUpperBound(u);		
		IJavaReferenceType vb = getUpperBound(v);
		return getWildcardType(null, getLowestUpperBound(ub, vb));
	}
	
	private IJavaReferenceType getUpperBound(IJavaWildcardType t) {
		IJavaReferenceType rv = t.getUpperBound();
		if (rv == null) {
			return tEnv.getObjectType();
		}
		return rv;
	}
	
	/**
	 * At least one bound is the lower bound
	 */
	private IJavaType getLCTA_super(IJavaReferenceType lower, IJavaWildcardType t) {
		if (t.getLowerBound() != null) {
			// Case 3
			return getWildcardType(getGreatestLowerBound(t.getLowerBound(), lower), null); 
		} 
		if (t.getUpperBound() != null) {
			// Case 2
			return t.getUpperBound().equals(lower) ? t.getUpperBound() : JavaTypeFactory.wildcardType;
		}
		// FIX case 2?
		return tEnv.getObjectType().equals(lower) ? tEnv.getObjectType() : JavaTypeFactory.wildcardType;
	}

	//  Then, define Candidate(W) = CandidateInvocation(W) if W is generic, W otherwise.
	//
	//  a.k.a. Best()
	private IJavaReferenceType getCandidate(Iterable<IJavaDeclaredType> allSupers, IJavaReferenceType t) {
		if (t instanceof IJavaDeclaredType) {
			IJavaDeclaredType d = (IJavaDeclaredType) t;
			if (isGeneric(d)) {
				return getCandidateInvocation(allSupers, d);
			}
		}
		return t;
	}
	
	private boolean isGeneric(IJavaDeclaredType t) {
		final IRNode n = t.getDeclaration();
		IRNode typeParams = getParametersForType(n);
		if (typeParams == null) {
			return false;
		}
		return JJNode.tree.hasChildren(typeParams);
	}
	
	// JLS 8 sec 4.10.4
	//
	//  Then the inferred type for Tj is
	//  lub(U1 ... Uk) = Candidate(W1) & ... & Candidate(Wr) where Wi, , are
	//  the elements of MEC.
	synchronized IJavaReferenceType getLowestUpperBound(IJavaReferenceType... types) {
		if (types.length == 1) {
			return types[0];
		}
		// Check for a proxy
		JavaRefTypeProxy p = null;
		if (types.length == 2) {
			// This is the only case used internally
			p = getProxy(types[0], types[1]);
			Boolean complete = p.isComplete();
			if (complete != null) {
				if (complete) {
					return p.getResult();
				}
				return p;				
			}
			// otherwise, we haven't started computing this yet
			p.start("lub("+types[0]+", "+types[1]+")");
		}
		
		Iterable<IJavaDeclaredType> allSupers = getST(types);
		IJavaReferenceType result = null;
		for(IJavaReferenceType t : getMEC(types)) {
			/*
			String unparse = t.toString();
			if (unparse.equals("java.lang.Comparable<T extends java.lang.Object in Comparable>")) {
				System.out.println("Potential StackOverflow");
			}
			*/
			try {
				IJavaReferenceType c = getCandidate(allSupers, t);
				if (result == null) {
					result = c;
				} else {
					result = JavaTypeFactory.getIntersectionType(c, result);
				}
			} catch(StackOverflowError e) {
				StackTraceElement[] trace = e.getStackTrace();
				System.err.println("StackOverflow on "+t+": "+trace.length);
				StackTraceElement first = trace[0];
				for(int i=0; i<15; i++) {
					if (trace[i].equals(first)) {
						break;
					}
					System.err.println("\tat "+trace[i]);
				}
			}
		}
		/*
		if (result == null) {
			return tEnv.getObjectType();
		}
		*/
		if (p != null) {
			p.finishType(result);
		}
		/*
		try {
			result.isValid();
		} catch(StackOverflowError e) {
			e = null;
			new Throwable().printStackTrace();
			System.out.println();
		}
		*/
		return result;
	}
	
	// (�5.1.10) - no formal definition there
	public IJavaReferenceType getGreatestLowerBound(IJavaReferenceType... bounds) {
		IJavaReferenceType result = null;
		if (bounds.length == 1) {
			// No need to compare, since there's only one
			result = bounds[0];
		} else {
			// Remove supertypes of other types in the set
			final Set<IJavaReferenceType> reduced = new HashSet<IJavaReferenceType>(bounds.length);
			for (IJavaReferenceType bt : bounds) {
				if (bt == null) {
					continue;
				}
				reduced.add(bt);
			}
			for (IJavaReferenceType bt : bounds) {
				if (bt == null) {
					continue;
				}
				for(IJavaReferenceType possibleSub : reduced) {
					if (!bt.equals(possibleSub) && possibleSub.isSubtype(tEnv, bt)) {
						// Since this is a greatest lower bound, possibleSub subsumes bt
						reduced.remove(bt);
						break;
					}
				}
			}    		  
			// order the bounds
			final List<IJavaReferenceType> ordered = new ArrayList<IJavaReferenceType>(reduced);
			Collections.sort(ordered, new Comparator<IJavaReferenceType>() {
				@Override
        public int compare(IJavaReferenceType t1, IJavaReferenceType t2) {
					// TODO is there something better?
					return t1.toString().compareTo(t2.toString());
				}    			  
			});
			//System.out.println(bounds+" -> "+ordered);
			for(IJavaReferenceType bt : ordered) {
				if (result == null) {
					result = bt;
					/*
		  } else if (result.isSubtype(binder.getTypeEnvironment(), bt)) {
			  // Nothing to do, since result subsumes bt
		  } else if (bt.isSubtype(binder.getTypeEnvironment(), result)) {
			  // bt is more specific than result
			  result = bt;
					 */
				} else {
					// No relationship between result and bt already
					result = JavaTypeFactory.getIntersectionType(result, bt);
				}    	
			}
		}
		if (result == null) {
			return tEnv.getObjectType();
		}
		return result;
	}	
		
	public Constraints getEmptyConstraints(CallState call, IBinding method, Map<IJavaType, IJavaType> substMap, 
			boolean allowBoxing, boolean allowVarargs) {
		return new Constraints(call, method, substMap, allowBoxing, allowVarargs);
	}
	
	public class Constraints {
		final boolean allowBoxing; 
		final boolean allowVarargs;
		final Set<TypeConstraint> constraints = new HashSet<TypeConstraint>();
		final Mapping map;

		/**
		 * Constructor for a new set of constraints.
		 * The John-approved interface.
		 * @param method method being called, should not be null
		 * @param typeFormals, type formals of the method or of class for constructor
		 * @param ikind kind of invocation context (must not be null)
		 */
		public Constraints(IBinding method, List<IJavaTypeFormal> typeFormals, InvocationKind ikind) {
			Map<IJavaType,IJavaType> initialMap = new HashMap<IJavaType,IJavaType>();
			if (typeFormals != null) {
				for (IJavaTypeFormal tf : typeFormals) {
					initialMap.put(tf, tf);
				}
			}
			map = new Mapping(null, method, initialMap);
			allowBoxing = ikind.canBoxUnbox();
			allowVarargs = ikind.isVariable();
		}
		
		public Constraints(CallState call, IBinding method, Map<IJavaType, IJavaType> substMap, 
				boolean box, boolean varargs) {			
			map = new Mapping(call, method, substMap);
			allowBoxing = box;
			allowVarargs = varargs;
		}
		
		public void addConstraints(IJavaType formal, IJavaType actual) {
			// Obsolete:
			// capture(map.subst, formal, actual);
			
			// New code to match JLS 3 section 15.12.2.7
			derive(formal, Constraint.CONVERTIBLE_FROM, actual);			
		}
		
		/**
		 * Check that the actual parameter types work for formal parameter types.
		 * If this is a var-arg method and allowVarArgs is true, 
		 * then var-arg invocation is possible.
		 * XXX: This method will need to be updated for Java 8 if it is to be used
		 * for applicability checking.  For Java 8, we don't always have the
		 * parameter types.
		 * @param formals list of formal types, must not be null
		 * @param actuals list of actual types, must not be null
		 * @param isVar the last formal is a var-arg formal
		 * @return whether we can call this method with the given actual types.
		 */
		public boolean deriveForParameters(List<? extends IJavaType> formals, List<? extends IJavaType> actuals, boolean isVar) {
			int fsize = formals.size();
			int asize = actuals.size();
			boolean OK = true;
			
			if (fsize == asize) {
				 OK = true;
				 for (int i=0; OK && i < fsize; ++i) {
					 OK = derive(formals.get(i),Constraint.CONVERTIBLE_FROM,actuals.get(i));
				 }
				 if (OK) return true;
			}
			if (!allowVarargs || !isVar || asize < fsize-1) return false;
			for (int i=0; OK && i < fsize-1; ++i) {
				 OK = derive(formals.get(i),Constraint.CONVERTIBLE_FROM,actuals.get(i));				
			}
			IJavaType restType = ((IJavaArrayType)formals.get(fsize-1)).getElementType();
			for (int i=fsize-1; OK && i < asize; ++i) {
				 OK = derive(restType,Constraint.CONVERTIBLE_FROM,actuals.get(i));
			}
			return OK;
		}
		
		/**
		 * 15.12.2.7 Inferring Type Arguments Based on Actual Arguments
		 * (really for polymorphic methods)
		 * 
		 * @return true if derived some constraints
		 */
		boolean derive(IJavaType formal, Constraint constraint, IJavaType actual) {
			//System.out.println("Adding: "+formal+" "+constraint+" "+actual);
			if (formal instanceof IJavaPrimitiveType || actual instanceof IJavaNullType) {
				// Nothing to do since there can't be any type variables to deal with here
				//   or
				// p.453: If A is the type of null, no constraint is implied on Tj.
				return false;
			}
			// Otherwise a ref type
			if (actual instanceof IJavaPrimitiveType) {
				// p.453: 
				// If A is a primitive type, then A is converted to a reference type U via boxing
				// conversion and this algorithm is applied recursively to the constraint
				// U << F.
				IJavaPrimitiveType a = (IJavaPrimitiveType) actual;
				return derive(formal, constraint, JavaTypeFactory.getCorrespondingDeclType(tEnv, a));
			}
			else if (formal instanceof IJavaArrayType) {
				// p.453: 
				// If F = U[], where the type U involves Tj, then if A is an array type V[], or
				// a type variable with an upper bound that is an array type V[], where V is a
				// reference type, this algorithm is applied recursively to the constraint V<<U.
				final IJavaArrayType f = (IJavaArrayType) formal;
				if (actual instanceof IJavaArrayType) {
					IJavaArrayType a = (IJavaArrayType) actual;
					return derive(f.getElementType(), constraint/*.simplify()?*/, a.getElementType());
				} 
				else if (actual instanceof IJavaTypeVariable) {
					IJavaTypeVariable a = (IJavaTypeVariable) actual;
					return deriveForArray(f.getElementType(), constraint/*.simplify()?*/, a.getSuperclass(tEnv));
				} 
				map.markAsUnsatisfiable();
			}
			else if (formal instanceof IJavaDeclaredType) {
				IJavaDeclaredType f = (IJavaDeclaredType) formal;
				if (constraint == Constraint.CONVERTIBLE_TO) {
					if (actual instanceof IJavaIntersectionType) {
						// TODO is this right?
						IJavaIntersectionType it = (IJavaIntersectionType) actual;											
						return derive(formal, constraint, it.getPrimarySupertype()) | derive(formal, constraint, it.getSecondarySupertype());
					}
					else if (actual instanceof IJavaCaptureType) {
						// TODO is this right?
						IJavaCaptureType ct = (IJavaCaptureType) actual;
						return derive(formal, constraint, ct.getLowerBound());
					}
					// This turns out to be pretty different from the cases below
					else if (actual instanceof IJavaDeclaredType) {
						return deriveForDeclaredType_to(f, (IJavaDeclaredType) actual);
					}
				}
				if (actual == JavaTypeFactory.anyType) {
					// No constraint
					return false;
				}
				// Find bounds that match f
				return deriveForDeclaredType(f, constraint, (IJavaReferenceType) actual);							
			}
			else if (formal instanceof IJavaTypeVariable) {
				// TODO check if this is one of the relevant type variables
				if (MethodBinder.captureTypes2 || map.subst.containsKey(formal)) {
				    // TODO is this right to handle any type variable?
					// p.453: Otherwise, if F = Tj, then the constraint Tj :> A is implied.
					IJavaTypeVariable f = (IJavaTypeVariable) formal;
					IJavaReferenceType a = (IJavaReferenceType) actual;
					constraints.add(new TypeConstraint(f, constraint.simplify(), a));
					return true;
				}
			}
			else if (formal instanceof IJavaIntersectionType) {
				// TODO where is this documented?
				IJavaIntersectionType it = (IJavaIntersectionType) formal;
				return derive(it.getPrimarySupertype(), constraint, actual) |
					   derive(it.getSecondarySupertype(), constraint, actual);
			}
			else if (formal instanceof IJavaWildcardType) {
				SLLogger.getLogger().info("Ignoring constraint: "+formal+" "+constraint+" "+actual);
				return false;
			}
			else throw new IllegalStateException("Unexpected type: "+formal);
			
			return false;
		}
			
		private boolean deriveForArray(IJavaType elementType, Constraint constraint, IJavaType formalBound) {
			// TODO Auto-generated method stub
			// throw new UnsupportedOperationException();
			return false;
		}
		
		/**
		 * p.453-4:
		 * Look for a supertype of a that matches f
		 * @param c 
		 */
		private boolean deriveForDeclaredType(final IJavaDeclaredType f, Constraint c, IJavaReferenceType actual) {
			boolean derived = false;
			if (actual instanceof IJavaDeclaredType) {
				IJavaDeclaredType a = (IJavaDeclaredType) actual;
				if (f.getDeclaration().equals(a.getDeclaration())) {
					if (a.getTypeParameters().size() == 0) {
						// The actual is raw, so there's nothing else to do?
						return false;
					}
					final int num = f.getTypeParameters().size();				
					for(int i=0; i<num; i++) {
						switch(c) {
						case CONVERTIBLE_FROM: // A << F
							derived |= deriveForTypeParameter(f.getTypeParameters().get(i), 
									                          a.getTypeParameters().get(i));
							break;
						case EQUAL: // A = F
							derived |= deriveForTypeParameter_equal(f.getTypeParameters().get(i), 
			                                                        a.getTypeParameters().get(i));
							break;
							/*
						case CONVERTIBLE_TO: // A >> F
							derived |= deriveForTypeParameter_to(f.getTypeParameters().get(i), 
									                             a.getTypeParameters().get(i));
							break;
							*/
						default:
							throw new UnsupportedOperationException();
						}
					}
					return derived;
				}
			}
//			Keep looking
			for(IJavaType s : actual.getSupertypes(tEnv)) {
				derived |= deriveForDeclaredType(f, c, (IJavaReferenceType) s);
			}
			return derived;
		}

		private boolean deriveForTypeParameter(IJavaType fParam, IJavaType aParam) {
			if (fParam instanceof IJavaWildcardType) {
				// JLS 3 p.454:
				// If F has the form G<..., Yk-1, ? extends U, Yk+1, ...>, where U involves Tj,
				// then if A has a supertype that is one of:
				IJavaWildcardType f = (IJavaWildcardType) fParam;				
				IJavaType u = f.getUpperBound();
				if (u != null) {
					if (aParam instanceof IJavaWildcardType) {
						// G<..., Xk-1, ? extends V, Xk+1, ...>. Then this algorithm is applied recursively
						// to the constraint V << U.
						IJavaWildcardType a = (IJavaWildcardType) aParam;
						IJavaType v = a.getUpperBound();						
						if (v != null) {
							// U >> V
							return derive(u, Constraint.CONVERTIBLE_FROM, v);
						}
						// Otherwise, no constraint is implied on Tj.
					} else {
						// G<..., Xk-1, V, Xk+1, ...>, where V is a type expression. Then this algorithm						
						// is applied recursively to the constraint V << U.						
						return derive(u, Constraint.CONVERTIBLE_FROM, aParam);
					}
				}
				// p.455:
				// If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, where U involves Tj,
				// then if A has a supertype that is one of:
				else if (f.getLowerBound() != null) {
					if (aParam instanceof IJavaWildcardType) {
						// G<..., Xk-1, ? super V, Xk+1, ...>. Then this algorithm is applied recursively
						// to the constraint V >> U.
						IJavaWildcardType a = (IJavaWildcardType) aParam;
						IJavaType v = a.getLowerBound();						
						if (v != null) {
							// U << V
							return derive(f.getLowerBound(), Constraint.CONVERTIBLE_TO, v);
						}
						// Otherwise, no constraint is implied on Tj.
					} else {
						// G<..., Xk-1, V, Xk+1, ...>. Then this algorithm is applied recursively to
						// the constraint V >> U.
						return derive(f.getLowerBound(), Constraint.CONVERTIBLE_TO, aParam);
					}				
				}
			} else {
				// p.453:
				// If F has the form G<..., Yk-1, U, Yk+1, ...>, where U is a type
				// expression that involves Tj, then if A has a supertype of the form G<...,
				// Xk-1, V, Xk+1, ...> where V is a type expression, this algorithm is applied
				// recursively to the constraint V = U.
				return derive(fParam, Constraint.EQUAL, aParam);
			}
			return false;
		}

		// JLS 3 p.456-7
		private boolean deriveForTypeParameter_equal(IJavaType fParam, IJavaType aParam) {
			if (fParam instanceof IJavaWildcardType) {
				// If F has the form G<..., Yk-1, ? extends U, Yk+1, ...>, where U involves Tj,
				// then if A is one of:
				IJavaWildcardType f = (IJavaWildcardType) fParam;				
				IJavaType u = f.getUpperBound();
				if (u != null) {
					if (aParam instanceof IJavaWildcardType) {
						// G<..., Xk-1, ? extends V, Xk+1, ...>. Then this algorithm is applied recursively
						// to the constraint V = U.
						IJavaWildcardType a = (IJavaWildcardType) aParam;
						IJavaType v = a.getUpperBound();						
						if (v != null) {
							return derive(u, Constraint.EQUAL, v);
						}						
					} 
					// Otherwise, no constraint is implied on Tj.
				}
				// If F has the form G<..., Yk-1, ? super U, Yk+1 ,...>, where U involves Tj,
				// then if A is one of:
				else if (f.getLowerBound() != null) {
					if (aParam instanceof IJavaWildcardType) {
						// G<..., Xk-1, ? super V, Xk+1, ...>. Then this algorithm is applied recursively
						// to the constraint V = U.
						IJavaWildcardType a = (IJavaWildcardType) aParam;
						IJavaType v = a.getLowerBound();						
						if (v != null) {
							return derive(u, Constraint.EQUAL, v);
						}
					} 	
					// Otherwise, no constraint is implied on Tj.
				}
			} else {
				// If F has the form G<..., Yk-1, U, Yk+1, ...>, where U is type
				// expression that involves Tj, then if A is of the form G<..., Xk-1, V,
				// Xk+1,...> where V is a type expression, this algorithm is applied recursively
				// to the constraint V = U.
				return derive(fParam, Constraint.EQUAL, aParam);
			}
			return false;
		}
		
		/** 
		 * JLS 7 p.471-4: if the constraint has the form A >> F
		 */
		private boolean deriveForDeclaredType_to(IJavaDeclaredType f, IJavaDeclaredType a) {
			if (a.getTypeParameters().isEmpty()) {
				// If A is an instance of a non-generic type, then no constraint is implied on Tj.
				return false;
			}
			// A generic type of some kind ...
			//
			// If F has the form G< ..., Y k-1 , U , Y k+1 , ... > , 
			// where U is a type expression that involves T_j , then:
			//			
			//   If A is an invocation of a generic type declaration H, 
			//   where H is either G or superclass or superinterface of G, then:
			/*
			IJavaDeclaredType h = findLowestCommonAncestor_raw(f, a);			
			if (h != null) {
			*/
			if (tEnv.isRawSubType(f, a)) {
				// TODO need to compensate for the difference in types
				if (a.getTypeParameters().size() == 0) {
					// TODO The actual is raw, so there's nothing else to do?
					return false;
				}
				final int num = f.getTypeParameters().size();				
				for(int i=0; i<num; i++) {
					deriveForTypeParameter_to(f, f.getTypeParameters().get(i), a, i);
				}
			}			 
			return false;
		}

		private boolean deriveForTypeParameter_to(IJavaDeclaredType f, IJavaType fParam, IJavaDeclaredType a, int i) {
			if (!f.getDeclaration().equals(a.getDeclaration())) {
				// If H != G, then let S1, ..., Sn be the type parameters of G, and 
				// let H<U1, ..., Ul> be the unique invocation of H that is a supertype of G<S1, ..., Sn>
				// ...	
				// make a version of F with just the type variables
				final IJavaDeclaredType pureF = 
					(IJavaDeclaredType) tEnv.convertNodeTypeToIJavaType(f.getDeclaration());
				
				final Map<IJavaType,IJavaType> subst = 
					Collections.singletonMap(pureF.getTypeParameters().get(i), fParam);
				
				final IJavaDeclaredType h = computeSuperTypeH(a.getDeclaration(), pureF);					
				final IJavaDeclaredType v;
				if (fParam instanceof IJavaWildcardType) {
					IJavaWildcardType fw = (IJavaWildcardType) fParam;
					if (fw.getUpperBound() != null) {
						// let V = H<? extends U1, ..., ? extends Ul>[Sk=U]. 
						v = (IJavaDeclaredType) substitute(subst, transformParametersToWildcards(h, false));
						// Then this algorithm is applied recursively to the constraint A >> V.
						return derive(v, Constraint.CONVERTIBLE_TO, a);
					} 
					else if (fw.getLowerBound() != null) {
						// let V = H<? super U1, ..., ? super Ul>[Sk=U]. 
						v = (IJavaDeclaredType) substitute(subst, transformParametersToWildcards(h, true));
						// Then this algorithm is applied recursively to the constraint A >> V.
						return derive(v, Constraint.CONVERTIBLE_TO, a);
					}
				} else {
					// let V = H<U1, ..., Ul>[Sk=U]. 
					v = (IJavaDeclaredType) substitute(subst, h);
					// Then, if V :> F this algorithm is applied recursively to the constraint A >> V.
					if (tEnv.isSubType(f, v)) {
						// V << A
						return derive(v, Constraint.CONVERTIBLE_TO, a);
					}
				}	
				return false;
			} 
			// A is of the form G<...>
			final IJavaType aParam = a.getTypeParameters().get(i);
			IJavaWildcardType aw;
			if (aParam instanceof IJavaWildcardType) {
				aw = (IJavaWildcardType) aParam;
			} else {
				aw = null;
			}
			if (fParam instanceof IJavaWildcardType) {
				IJavaWildcardType fw = (IJavaWildcardType) fParam;
				// If F has the form G<..., Yk-1, ? extends U, Yk+1, ...>, 
				// where U is a type expression that involves Tj, then:
				if (fw.getUpperBound() != null) {
					// Otherwise, if A is of the form G<..., Xk-1, ? extends W, Xk+1, ...>, 
					// this algorithm is applied recursively to the constraint W >> U.
					if (aw != null && aw.getUpperBound() != null) {
						// U << W
						return derive(fw.getUpperBound(), Constraint.CONVERTIBLE_TO, aw.getUpperBound());
					}
				}
				// If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, 
				// where U is a type expression that involves Tj, then A is either:
				else if (fw.getLowerBound() != null) {					
					// Otherwise, if A is of the form G<..., Xk-1, ? super W, ..., Xk+1, ...>, 
					// this algorithm is applied recursively to the constraint W << U.
					if (aw != null && aw.getLowerBound() != null) {
						// U >> W
						return derive(fw.getLowerBound(), Constraint.CONVERTIBLE_FROM, aw.getLowerBound());
					}
				}				
			} else { 
				// If F has the form G<..., Yk-1, U, Yk+1, ...>, 
				// where U is a type expression that involves Tj, then:
				if (aw != null) {
					// Otherwise, if A is of the form G<..., Xk-1, ? extends W, Xk+1, ...>, 
					// this algorithm is applied recursively to the constraint W >> U.
					if (aw.getUpperBound() != null) {
						// U << W
						return derive(fParam, Constraint.CONVERTIBLE_TO, aw.getUpperBound());
					}
					// Otherwise, if A is of the form G<..., Xk-1, ? super W, Xk+1, ...>, 
					// this algorithm is applied recursively to the constraint W << U.
					else if (aw.getLowerBound() != null) {
						// U >> W
						return derive(fParam, Constraint.CONVERTIBLE_FROM, aw.getLowerBound());
					}
				} else {
					// Otherwise, if A is of the form G<..., Xk-1, W, Xk+1, ...>, 
					// where W is a type expression, this algorithm is applied recursively to the constraint W = U.
					return derive(fParam, Constraint.EQUAL, aParam);
				}
			}
			return false;
		}

		private IJavaDeclaredType computeSuperTypeH(final IRNode hDecl, final IJavaDeclaredType here) {
			if (hDecl.equals(here.getDeclaration())) {
				return here;
			}
			for(IJavaType s : here.getSupertypes(tEnv)) {
				IJavaDeclaredType rv = computeSuperTypeH(hDecl, (IJavaDeclaredType) s);
				if (rv != null) {
					return rv;
				}					
			}
			return null;
		}
		
		private IJavaType transformParametersToWildcards(IJavaDeclaredType h, boolean useAsUpperBounds) {
			final int size = h.getTypeParameters().size();
			if (size == 0) {
				return h;
			}
			List<IJavaType> newParameters = new ArrayList<IJavaType>(size);
			for(int i=0; i<size; i++) {
				IJavaReferenceType old = (IJavaReferenceType) h.getTypeParameters().get(i);
				newParameters.add(useAsUpperBounds ? JavaTypeFactory.getWildcardType(null, old) : 
					                                 JavaTypeFactory.getWildcardType(old, null));
			}
			return JavaTypeFactory.getDeclaredType(h.getDeclaration(), newParameters, h.getOuterType());
		}
		
		/**		 
		 * @return null if unsatisfiable
		 */
		public Mapping computeTypeMapping() {
			if (useNewTypeInference) {
				GeneratedConstraints generated = inferTypeParameters(map, constraints);		
				if (map.call != null) {
					Constraints constraints = handleUnresolvedVariables(map, generated);
					if (constraints != null) {
						// Copy over the map
						map.subst.clear();
						map.subst.putAll(constraints.map.subst);
					}
				}				
				// Make sure that the variables are at worst the same as the original bound
				for(Map.Entry<IJavaType,IJavaType> e : map.subst.entrySet()) {
					final IJavaType orig = e.getKey();
					final IJavaType inferred = e.getValue();
					final IJavaType origBound = e.getKey().getSuperclass(tEnv);
					final IJavaType origSubst = substitute(map.subst, origBound);
					if (!tEnv.isSubType(e.getValue(), origSubst)) {						
						//e.setValue(origSubst);
						System.err.println(inferred+" is not a subtype of "+origSubst);
						continue;
					}
				}
			}
			return map;
		}
		
		public IJavaTypeSubstitution getSubstitution() {
			if (map.subst.isEmpty()) return IJavaTypeSubstitution.NULL;
			if (!map.checkIfSatisfiesBounds()) return null;
			List<IJavaTypeFormal> formals = new ArrayList<IJavaTypeFormal>();
			List<IJavaType> actuals = new ArrayList<IJavaType>();
			for (Map.Entry<IJavaType,IJavaType> e : map.subst.entrySet()) {
				formals.add((IJavaTypeFormal)e.getKey());
				actuals.add(e.getValue());
			}
			return new SimpleTypeSubstitution(tEnv.getBinder(), formals,actuals);
		}

		void addConstraintsForType(IRNode tdecl) {
			// Create "null" entries for these type variables
			final IRNode typeParams = getParametersForType(tdecl);
			for(IRNode tf : TypeFormals.getTypeIterator(typeParams)) {
				final IJavaType fty = JavaTypeFactory.getTypeFormal(tf);
				map.subst.put(fty, fty);
			}
		}

		public IJavaType substituteRawMapping(ITypeEnvironment env, IRNode typeFormals, IJavaType fty) {
			final Map<IJavaType,IJavaType> subst = new HashMap<IJavaType, IJavaType>();
			for(IRNode f : TypeFormals.getTypeIterator(typeFormals)) {
				IJavaTypeFormal jtf = (IJavaTypeFormal) env.convertNodeTypeToIJavaType(f);
				subst.put(jtf, JavaTypeFactory.voidType);
			}
			return TypeUtils.this.substitute(subst, fty);
		}
	}
	
	public class Mapping {
		final CallState call;
		final IBinding method;
		final Map<IJavaType, IJavaType> subst;
		boolean isUnsatisfiable = false;

		Mapping(CallState call, IBinding method, Map<IJavaType, IJavaType> substMap) {
			this.call = call;
			this.method = method;
			subst = MethodBinder.captureTypes2 ?
					new IdentityHashMap<IJavaType, IJavaType>(substMap) :
					new HashMap<IJavaType, IJavaType>(substMap);
		}

		void markAsUnsatisfiable() {
			isUnsatisfiable = true;
		}

		public IJavaType substitute(IJavaType fty) {
			return TypeUtils.this.substitute(subst, fty);
		}

		public void export(Map<IJavaType, IJavaType> substMap) {
			substMap.putAll(subst);
		}

		/**
		 * JLS v3 sec 15.12.2.5
		 * Checks if Al <: Bl[R1 = A1, ..., Rp = Ap], 
		 *            extends
		 */
		public boolean checkIfSatisfiesBounds() {
			for(Map.Entry<IJavaType, IJavaType> e : subst.entrySet()) {
				final IJavaTypeVariable r = (IJavaTypeVariable) e.getKey();
				final IJavaType b = r.getSuperclass(tEnv);
				final IJavaType a = e.getValue();
				if (!tEnv.isSubType(a, substitute(b))) {
					return false;
				}
			}
			return true;
		}
		
		boolean hasUnresolvedVars() {
			return !findUnresolved(subst).isEmpty();
		}
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
          IJavaType extendsT = mapping.getSuperclass(tEnv);          
          map.put(fty, argType);
          // TODO capture(map, extendsT, argType);
          
          if (tEnv.isSubType(argType, substitute(map, extendsT))) {                      
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
    		IJavaDeclaredType superT = adt.getSuperclass(tEnv);
    		if (superT != null) {
    			captureDeclaredType(map, fdt, superT);
    		}
    	} else {
    		// FIX is this right?
    		for(IJavaType superT : adt.getSupertypes(tEnv)) {
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
	  IRNode formals = getParametersForType(adt.getDeclaration());
	  if (formals == null) {
        return; // nothing to do        
      }
      Iterator<IJavaType> fdtParams = fdt.getTypeParameters().iterator(); 
      for(IRNode tf : TypeFormals.getTypeIterator(formals)) {
        IJavaType fT = JavaTypeFactory.getTypeFormal(tf);
        capture(map, fdtParams.next(), fT.getSuperclass(tEnv));
      }
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
    		if (rv == fty) {
    			return fty;
    		}
    		else if (rv == JavaTypeFactory.voidType) {
    			return tEnv.computeErasure(fty);
    		}
    		else if (rv != null) {
    			/*
    			try {
    			return substitute(map, rv);
    			} catch(StackOverflowError e) {
    				System.err.println("Stack overflow on "+rv);
    				throw new IllegalStateException();
    			}
    			*/
    			return rv;
    		} else {
    			return fty; // No subst
    		}
    	}
    	else if (fty instanceof IJavaDeclaredType) {
    		IJavaDeclaredType dt = (IJavaDeclaredType) fty;
    		// copied from captureDeclaredType
    		final int size   = dt.getTypeParameters().size(); 
    		boolean captured = false;
    		boolean allNull = true;
    		List<IJavaType> params = new ArrayList<IJavaType>(size);
    		for(int i=0; i<size; i++) {
    			IJavaType oldT = dt.getTypeParameters().get(i);            
    			IJavaType newT = substitute(map, oldT); 
    			params.add(newT);
    			if (!oldT.equals(newT)) {
    				captured = true;                
    				
        			if (newT != null) {
        				allNull = false;
        			}
    			}
    		}
    		if (captured) {
    			if (allNull) {
    				params = Collections.emptyList();
    			}
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
    	else if (fty instanceof IJavaCaptureType) {
    		IJavaCaptureType ct = (IJavaCaptureType) fty;
    		IJavaType baseT   = ct.getWildcard();
    		IJavaWildcardType newBase = (IJavaWildcardType) substitute(map, baseT);
    		IJavaReferenceType newLower = (IJavaReferenceType) substitute(map, ct.getLowerBound());
    		IJavaReferenceType newUpper = (IJavaReferenceType) substitute(map, ct.getUpperBound());
    		if (newBase != baseT || newLower != ct.getLowerBound() || newUpper != ct.getUpperBound()) {
    			return JavaTypeFactory.getCaptureType(newBase, newLower, newUpper);
    		}
    	}
    	return fty;
    }
    
    private IJavaReferenceType substituteWildcardType(
    		Map<IJavaType, IJavaType> map, IJavaType fty) {
    	IJavaWildcardType wt     = (IJavaWildcardType) fty;
    	if (wt.getLowerBound() != null) {
    		IJavaReferenceType lower = (IJavaReferenceType) substitute(map, wt.getLowerBound());
    		if (!lower.equals(wt.getLowerBound())) {
    			return JavaTypeFactory.getWildcardType(null, lower);
    		}
    	}
    	else if (wt.getUpperBound() != null) {
    		IJavaReferenceType upper = (IJavaReferenceType) substitute(map, wt.getUpperBound());
    		if (!upper.equals(wt.getUpperBound())) {
    			return JavaTypeFactory.getWildcardType(upper, null);
    		}
    	}
    	return wt;
    }
    
	/**
	 * 15.12.2.7 Inferring Type Arguments Based on Actual Arguments
	 */
	private final class TypeConstraint {
		final IJavaTypeVariable variable;
		final Constraint constraint;
		final IJavaReferenceType bound;
		
		TypeConstraint(IJavaTypeVariable v, Constraint c, IJavaReferenceType x) {
			variable = v;
			constraint = c;
			bound = x;
		}
		
		@Override
		public int hashCode() {
			return variable.hashCode() + constraint.hashCode() + bound.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof TypeConstraint) {
				TypeConstraint o = (TypeConstraint) other;
				return constraint == o.constraint &&
				       variable.equals(o.variable) && 
				       bound.equals(o.bound);				
			}
			return false;
		}
		
		@Override
		public String toString() {
			return variable+" "+constraint+" "+bound;
		}
	}
	
	public enum Constraint {
		EQUAL("=") {
			@Override Constraint reverse() {
				return this;
			}
		}, 
		/**
		 * U << V indicates that type U is convertible to type V by method invocation conversion (�5.3)
		 */
		CONVERTIBLE_TO("<<") {
			@Override Constraint simplify() {
				return SUBTYPE_OF;
			}
			@Override Constraint reverse() {
				return CONVERTIBLE_FROM;
			}
		}, 
		/**
		 * U >> V indicates that type V is convertible to type U by method invocation conversion.
		 */
		CONVERTIBLE_FROM(">>") {
			@Override Constraint simplify() {
				return SUPERTYPE_OF;
			}
			@Override Constraint reverse() {
				return CONVERTIBLE_TO;
			}
		},
		/** X is equal or a subtype of Y */
		SUBTYPE_OF("<:") {
			@Override Constraint reverse() {
				return SUPERTYPE_OF;
			}
		}, 
		/** X is equal or a supertype of Y */
		SUPERTYPE_OF(">:") {
			@Override Constraint reverse() {
				return SUBTYPE_OF;
			}
		};

		final String label;
		
		Constraint(String l) {
			label = l;
		}
		
		@Override
		public String toString() {
			return label;
		}
		
		Constraint simplify() {
			return this;
		}
		abstract Constraint reverse();
	}

	private class GeneratedConstraints {
		final Map<IJavaType,IJavaType> equalities = new HashMap<IJavaType, IJavaType>();
		final MultiMap<IJavaTypeVariable, TypeConstraint> inequalities = 
			new MultiHashMap<IJavaTypeVariable, TypeConstraint>();
	}
	
	/**
	 * JLS 3 p.463
     * -- If U is not one of the type parameters of the method, then U is the type
     *    inferred for Tj. Then all remaining constraints involving Tj are rewritten such
     *    that Tj is replaced with U. There are necessarily no further equality constraints
     *    involving Tj, and processing continues with the next type parameter, if any.

     * -- Otherwise, the constraint is of the form Tj = Tk for . Then all constraints
     *    involving Tj are rewritten such that Tj is replaced with Tk, and processing continues
     *    with the next type variable.
	 * @return 
	 */
	GeneratedConstraints inferTypeParameters(Mapping map, Set<TypeConstraint> constraints) {
		/*
		if (constraints.size() > 1) {
			System.out.println("Inferring from "+constraints.size()+" constraints");
		}
		*/
		List<TypeConstraint> bounds = new ArrayList<TypeConstraint>();
		GeneratedConstraints generated = new GeneratedConstraints();
		for(TypeConstraint c : constraints) {
			/*
			 * Next, for each type variable Tj, , the implied equality constraints are
		     * resolved as follows:
		     * For each implied equality constraint Tj = U or U = Tj:
		     */
			if (c.constraint == Constraint.EQUAL) {
				final IJavaType old = generated.equalities.get(c.variable);
				if (c.variable.equals(c.bound) || c.bound.equals(old)) {
					// Otherwise, if U is Tj, then this constraint carries no information and may be
				    // discarded.
					continue;
				}
				else if (old != null) {
					// Already checked above if the bound is the same as one processed earlier
					//throw new IllegalStateException(c.variable+" already set to "+old+", now to "+c.bound);
					generated.equalities.put(c.variable, getLowestUpperBound((IJavaReferenceType) old, c.bound));
					// TODO could be more efficient to use a multimap
				}
				else {
					generated.equalities.put(c.variable, c.bound);
				}
			} else {
				bounds.add(c);
			}
		}
		// Do substitutions on the bounds using the equalities above, and organize by variable
		for(TypeConstraint c : bounds) {
			IJavaType newBound = substitute(generated.equalities, c.bound);
			if (c.bound.equals(newBound)) {
				generated.inequalities.put(c.variable, c);
			} else {
				generated.inequalities.put(c.variable, 
						new TypeConstraint(c.variable, c.constraint, (IJavaReferenceType) newBound));
			}
		}
		/*
		 * Then, for each remaining type variable Tj, the constraints Tj :> U are considered.
		 * Given that these constraints are Tj :> U1 ... Tj :> Uk, the type of Tj is inferred
		 * as lub(U1 ... Uk), computed as follows: 
		 */
		for(Map.Entry<IJavaTypeVariable, Collection<TypeConstraint>> e : generated.inequalities.entrySet()) {
			Set<IJavaReferenceType> temp = new HashSet<IJavaReferenceType>(e.getValue().size());
			for(TypeConstraint c : e.getValue()) {
				if (c.constraint == Constraint.SUPERTYPE_OF) {
					temp.add(c.bound);
				}				
			}
			if (temp.size() <= 0) {
				continue; 
			}
			IJavaReferenceType[] inputs = temp.toArray(new IJavaReferenceType[temp.size()]);
			IJavaReferenceType lub = getLowestUpperBound(inputs);
			/*
			if (inputs.length > 1) {
				System.out.println("Orig: "+e.getKey()+" => "+map.subst.get(e.getKey())+", now "+lub);
			}
			*/
			map.subst.put(e.getKey(), lub);
		}
		map.subst.putAll(generated.equalities);
		return generated;
	}

	/**
	 * JLS 7 sec 15.12.2.28:
	 * 
	 * If any of the method's type arguments were not inferred from the types of the actual
	 * arguments, they are now inferred as follows.
     * -- If the method result occurs in a context where it will be subject to assignment
     *    conversion (�5.2) to a type S, then ... see findAssignmentType() below
     *    
     *    let R be the declared result type of the method, and let R' = R[T1=B(T1) ... Tn=B(Tn)], 
     *    where B(Ti) is the type inferred for Ti in the previous section or Ti if no type was inferred.
     *    
     *    Then, a set of initial constraints consisting of (see below)
 
     *    is created and used to infer constraints on the type arguments using the algorithm of �15.12.2.7.
     *    
     *    Any equality constraints are resolved, and then, for each remaining constraint of
     *    the form Ti <: Uk, the argument Ti is inferred to be glb(U1, ..., Uk) (�5.1.10).
     *    If Ti appears as a type argument in any Uk, then Ti is inferred to be a type variable
     *    X whose upper bound is the parameterized type given by glb(U1[Ti=X], ...,
     *    Uk[Ti=X]) and whose lower bound is the null type.
     *    Any remaining type variable T that has not yet been inferred is then inferred
     *    to have type Object. If a previously inferred type variable P uses T, then P is
     *    inferred to be P[T=Object].
     *    
     * -- Otherwise, the unresolved type arguments are inferred by invoking the procedure
     *    described in this section under the assumption that the method result was
     *    assigned to a variable of type Object.
	 */
	Constraints handleUnresolvedVariables(final Mapping map, final GeneratedConstraints generated) {
		// Check for unresolved vars
		final Set<IJavaType> unresolved = findUnresolved(map.subst);
		if (unresolved.isEmpty()) {
			return null;
		}
		final Constraints constraints = getEmptyConstraints(map.call, map.method, map.subst, false, false);
	    //    -- the constraint S' >> R', provided R is not void; and
		if (map.call.call != null) { // check in case we're just looking for overridden parents			
 		    final IJavaType s_prime = findAssignmentType(map.call.call);
		    final IJavaType r_prime = computeReturnType(map);
		    if (s_prime != null && !(r_prime instanceof IJavaVoidType)) {
			    constraints.derive(r_prime, Constraint.CONVERTIBLE_TO, s_prime);
		    }
		}
		for(Map.Entry<IJavaType,IJavaType> e : map.subst.entrySet()) {			
		    //    -- additional constraints Bi[T1=B(T1) ... Tn=B(Tn)] >> Ti, where Bi is the declared bound of Ti,
		    //    -- additional constraints B(Ti) << Bi[T1=B(T1) ... Tn=B(Tn)], where Bi is the declared bound of Ti,
			final IJavaType t_i = e.getKey();
			final IJavaType b_i_subst = substitute(map.subst, t_i.getSuperclass(tEnv));
			constraints.derive(t_i, Constraint.CONVERTIBLE_TO, b_i_subst); // flipped around
			constraints.derive(b_i_subst, Constraint.CONVERTIBLE_FROM, e.getValue());
		}
	    //    -- for any constraint of the form V >> Ti generated in �15.12.2.7: a constraint V[T1=B(T1) ... Tn=B(Tn)] >> Ti.
		for (TypeConstraint c : generated.inequalities.values()) {
			if (c.constraint == Constraint.CONVERTIBLE_TO && map.subst.containsKey(c.variable)) {
				constraints.derive(c.variable, Constraint.CONVERTIBLE_TO, substitute(map.subst, c.bound));
			}			
		}
    	//    -- for any constraint of the form Ti = V generated in �15.12.2.7: a constraint Ti = V[T1=B(T1) ... Tn=B(Tn)].	        	
	    for (Map.Entry<IJavaType, IJavaType> e : generated.equalities.entrySet()) {
	    	constraints.derive(e.getKey(), Constraint.EQUAL, substitute(map.subst, e.getValue()));
	    }
	    final Mapping newMap = constraints.map;
	    inferTypeParameters(newMap, constraints.constraints);
	    
	    final Set<IJavaType> stillUnresolved = findUnresolved(newMap.subst);
	    if (!stillUnresolved.isEmpty()) {
	    	//System.err.println(DebugUnparser.toString(map.call.call)+" not fully resolved "+JavaNames.genRelativeFunctionName(map.method.getNode()));
	    	//SLLogger.getLogger().warning("Still has unresolved types: "+stillUnresolved);
	    	
	    	// Set any unresolved vars to their erasures
	    	for(IJavaType tf : stillUnresolved) {
	    		newMap.subst.put(tf, tf.getSuperclass(tEnv));
	    	}
	    }	    
		return constraints;
	}

	Set<IJavaType> findUnresolved(Map<IJavaType, IJavaType> subst) {
		Set<IJavaType> rv = new HashSet<IJavaType>();
		for(Map.Entry<IJavaType,IJavaType> e : subst.entrySet()) {
			if (e.getValue().equals(e.getKey())) {
				rv.add(e.getKey());
			}
		}	
		return rv;
	}
	
	/**
	 * Check if the method result occurs in a context where it will be subject to assignment
     *    conversion (�5.2) to a type S ...
     *    
     *    If S is a reference type, then let S' be S. Otherwise, if S is a primitive type, then
     *    let S' be the result of applying boxing conversion (�5.1.7) to S.
	 */
	private IJavaType findAssignmentType(IRNode call) {
		final IRNode parent = JJNode.tree.getParent(call);
		final Operator pop = JJNode.tree.getOperator(parent);
		IRNode type = null;
		if (Initialization.prototype.includes(pop)) {
			// Assuming it's in a vdecl
			final IRNode vdecl = JJNode.tree.getParent(parent);
			type = VariableDeclarator.getType(vdecl);
		}
		else if (AssignmentExpression.prototype.includes(pop)) {
			type = AssignmentExpression.getOp1(parent);
		}
		else if (ReturnStatement.prototype.includes(pop)) {
			final IRNode method = VisitUtil.getEnclosingClassBodyDecl(parent);
			type = MethodDeclaration.getReturnType(method);
		}
		// TODO what other cases are there?
		
		if (type != null) {
			return boxIfNeeded(tEnv.getBinder().getJavaType(type));
		}
		return null;
	}

	private IJavaType boxIfNeeded(IJavaType origType) {
		if (origType instanceof IJavaPrimitiveType) {
			return JavaTypeFactory.getCorrespondingDeclType(tEnv, (IJavaPrimitiveType) origType);
		}
		return origType;
	}
	
	private IJavaType computeReturnType(Mapping map) {
		IJavaType rt = JavaTypeVisitor.computeReturnType(tEnv.getBinder(), map.method, map.call);
		if (rt instanceof IJavaVoidType) {
			return rt;
		}
		else if (map.call.usesDiamondOp()) {
			IJavaDeclaredType dt = (IJavaDeclaredType) rt;
			if (dt.getTypeParameters().isEmpty()) {
				rt = tEnv.convertNodeTypeToIJavaType(dt.getDeclaration());
			}
		}		
		return substitute(map.subst, rt);
		//return rt.subst(FunctionParameterSubstitution.create(tEnv.getBinder(), map.method.getNode(), map.subst));
		// TODO what about the info I already inferred?		
	}
	
	/**
	 * Return the target type for a poly expression.
	 * @param pe poly expression node
	 * @return type this expression is expected to be
	 */
	public IJavaType getPolyExpressionTargetType(IRNode pe) {
		return getPolyExpressionTargetType(pe, false);
	}
	
	public IJavaType getPolyExpressionTargetType(IRNode pe, boolean eliminateTypeVars) {
		  IRNode p = JJNode.tree.getParent(pe);
		  IRLocation loc = JJNode.tree.getLocation(pe);
		  Operator op = JJNode.tree.getOperator(p);
		  //System.out.println("getPolyExpressionTargetType: "+op.name());
		  if (AssignExpression.prototype.includes(op)) {
			  if (loc.equals(AssignExpression.op2Location)) {
				  return tEnv.getBinder().getJavaType(AssignExpression.getOp1(p));
			  }
		  } else if (Initialization.prototype.includes(op)) {
			  IRNode gp = JJNode.tree.getParent(p);
			  return tEnv.getBinder().getJavaType(VariableDeclarator.getType(gp));
 		  } else if (CastExpression.prototype.includes(op)) {
			  return tEnv.getBinder().getJavaType(p);
		  } else if (ConditionalExpression.prototype.includes(op)) {
			  if (loc.equals(ConditionalExpression.condLoc)) {
				  return JavaTypeFactory.booleanType;
			  } else {
				  return getPolyExpressionTargetType(p, eliminateTypeVars);
			  }
		  } else if (ParenExpression.prototype.includes(op)) {
			  return getPolyExpressionTargetType(p, eliminateTypeVars);
		  } else if (Arguments.prototype.includes(op)) {
			  IRNode call = JJNode.tree.getParent(p);
			  int i = JJNode.tree.childLocationIndex(p, loc);
			  MethodBinding8 bi = (MethodBinding8) tEnv.getBinder().getIBinding(call);
			  /*
			  if (bi != null) {
				  IRNode decl = bi.getNode();
				  if (decl != null) {
					  final IRNode formals, targs;
					  Operator dop = JJNode.tree.getOperator(decl);
					  if (MethodDeclaration.prototype.includes(dop)) {
						  formals = MethodDeclaration.getParams(decl);
						  targs = MethodCall.getTypeArgs(call);						
					  } else if (ConstructorDeclaration.prototype.includes(dop)) {
						  formals = ConstructorDeclaration.getParams(decl);
						  // TODO what about ConstructorCall/ACE?
						  targs = NewExpression.getTypeArgs(call);
					  } else {
						  //LOG.warning("what could a call be bound to? " + dop);
						  return null;
					  }
					  //return bi.convertType(tEnv.getBinder(), tEnv.getBinder().getJavaType(JJNode.tree.getChild(formals, i)));
					  MethodBinder8 mb = new MethodBinder8((AbstractJavaBinder) tEnv.getBinder(), false);
					  CallState state = new CallState(tEnv.getBinder(), call, targs, p, bi.getReceiverType());
					  IJavaFunctionType ftype = mb.computeInvocationType(state, bi);
					  //return ftype.getReturnType();		
					  IJavaType ptype = ftype.getParameterTypes().get(TypeUtil.isStatic(bi.getNode()) ? i : i+1);			 
					  return bi.convertType(tEnv.getBinder(), ptype);
				  }
			  }
			  */
			  IJavaFunctionType ftype = computeInvocationTypeForCall(call, p, eliminateTypeVars, bi);
			  if (ftype == null) {
				  throw new IllegalStateException();
			  }
			  return ftype.getParameterTypes().get(bi.isConstructor || TypeUtil.isStatic(bi.getNode()) ? i : i+1);
		  } else if (MethodCall.prototype.includes(op)) { // As receiver
			  MethodCall call = (MethodCall) op;
			  IJavaFunctionType ftype = computeInvocationTypeForCall(p, call.get_Args(p), eliminateTypeVars);
			  if (ftype == null) {
				  computeInvocationTypeForCall(p, call.get_Args(p), eliminateTypeVars);
				  throw new IllegalStateException();
			  }
			  return ftype.getParameterTypes().get(0);
		  } else if (ArrayInitializer.prototype.includes(op)) {
			  // Only appearing in Initialization or ArrayCreationExpression
			  IRNode parent = JJNode.tree.getParent(p);
			  Operator parentOp = JJNode.tree.getOperator(parent);
			  IJavaArrayType at;
			  if (Initialization.prototype.includes(parentOp)) {
				  IRNode varDecl = JJNode.tree.getParent(p);
				  at = (IJavaArrayType) tEnv.getBinder().getJavaType(varDecl);
			  }
			  else if (ArrayCreationExpression.prototype.includes(parentOp)) {
				  at = (IJavaArrayType) tEnv.getBinder().getJavaType(parent);
			  } else {
				  return null;
			  }
			  return (IJavaDeclaredType) at.getElementType();
		  }
		  else if (ReturnStatement.prototype.includes(op)) {
			  return tEnv.getBinder().getJavaType(p);
		  }
		  // We make wish to make this a "fine" warning if all method call invocations
		  // are treated as something that could learn from the target type.
		  //LOG.warning("poly expression has bad context: " + op);
		  return null;
	  }
	
	private IJavaFunctionType computeInvocationTypeForCall(IRNode call, IRNode args, boolean eliminateTypeVars) {
		MethodBinding8 bi = (MethodBinding8) tEnv.getBinder().getIBinding(call);
		return computeInvocationTypeForCall(call, args, eliminateTypeVars, bi);
	}
	
	private IJavaFunctionType computeInvocationTypeForCall(IRNode call, IRNode args, boolean eliminateTypeVars, MethodBinding8 bi) {
		if (bi != null) {
			IRNode decl = bi.getNode();
			if (decl != null) {
				MethodBinder8 mb = new MethodBinder8((IPrivateBinder) tEnv.getBinder(), false);
				CallState state = mb.getCallState(call, bi);
				IJavaFunctionType ftype = mb.computeInvocationType(state, bi, eliminateTypeVars);
				return ftype.instantiate(ftype.getTypeFormals(), bi.getSubst());
				// IJavaType ptype = ftype.getParameterTypes().get(TypeUtil.isStatic(bi.getNode()) ? i : i+1);			 
				//return bi.convertType(tEnv.getBinder(), ptype);
			}
		}
		return null;
	}

	/**
	 * Find H, where A is the invocation of a generic type declaration H, 
	 * where H is either G or superclass or superinterface of G
	 * where G is the generic type of F
	 */
	public IJavaDeclaredType findLowestCommonAncestor_raw(IJavaDeclaredType f, IJavaDeclaredType a) {
		IJavaDeclaredType g = (IJavaDeclaredType) tEnv.computeErasure(f);		
		return findLowestCommonAncestor_private(g, a);
	}
	
	public IJavaDeclaredType findLowestCommonAncestor_private(final IJavaDeclaredType g, IJavaDeclaredType a) {		
		if (g.getDeclaration().equals(a.getDeclaration())) {
			return a;
		}
		for(IJavaType g_st : g.getSupertypes(tEnv)) {
			IJavaDeclaredType temp = findLowestCommonAncestor_private((IJavaDeclaredType) g_st, a);
			if (temp != null) {
				return temp;
			}
			// TODO what if more than one match?
		}
		return null;
	}
}
