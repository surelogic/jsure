package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Hashtable2;

public class TypeUtils {
	private final ITypeEnvironment tEnv;
	private final Hashtable2<IJavaReferenceType, IJavaReferenceType, JavaRefTypeProxy> proxyCache = 
		new Hashtable2<IJavaReferenceType, IJavaReferenceType, JavaRefTypeProxy>();
	
	TypeUtils(ITypeEnvironment te) {
		tEnv = te;
	}
	
	// Utility methods
	//
	/**
	 * Creates a proxy if there isn't one
	 * Gets the existing one otherwise
	 */
	private JavaRefTypeProxy getProxy(IJavaReferenceType u, IJavaReferenceType v) {
		// Make the order consistent, since it doesn't matter which order they're in
		if (u.hashCode() > v.hashCode()) {
			IJavaReferenceType tmp = v;
			v = u;
			u = tmp;
		}
		JavaRefTypeProxy p = proxyCache.get(u, v);
		if (p == null) {
			p = new JavaRefTypeProxy();
			proxyCache.put(u, v, p);
		}
		return p;		
	}
	
	private IJavaWildcardType getWildcardType(IJavaReferenceType upper, IJavaReferenceType lower) {
		if (upper == null && tEnv.getObjectType().equals(lower)) {
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
    		System.out.println("Excluded from ST: "+t);
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
			for(IJavaReferenceType t : types) {
				System.err.println("STs for "+t);
				for(IJavaDeclaredType s : getST(t)) {
					System.err.println("\t"+s);
				}
			}
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
	private IJavaReferenceType getCandidateInvocation(Iterable<IJavaDeclaredType> st, IJavaDeclaredType g) {
		return getLCI(getInv(st, g));
	}
	
	//  lci(S) = lci(e1, ..., en) where ei in S,
	//  lci(e1, ..., en) = lci(lci(e1, e2), e3, ..., en)
	private IJavaDeclaredType getLCI(Iterable<IJavaDeclaredType> invocations) {
		IJavaDeclaredType result = null;
		for(IJavaDeclaredType t : invocations) {
			if (result != null) {
				result = getLCI(result, t);
			} else {
				result = t;
			}
		}
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

	//  where lcta() is the the least containing type argument function defined
	//  (assuming U and V are type expressions) as:
	//  lcta(U, V) = U if U = V, ? extends lub(U, V) otherwise
	//
	//  Note that this could result in an infinite type due to the call to lub()
	private IJavaType getLCTA(IJavaType u, IJavaType v) {
		if (u instanceof IJavaWildcardType) {
			if (v instanceof IJavaWildcardType) {
				return getLCTA((IJavaWildcardType) u, (IJavaWildcardType) v);
			} else {
				return getLCTA((IJavaReferenceType) v, (IJavaWildcardType) u);
			}
		}
		else if (v instanceof IJavaWildcardType) {
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
		if (v.getUpperBound() != null) {
			return getWildcardType(getGreatestLowerBound(u, v.getUpperBound()), null); 
		}
		if (v.getLowerBound() != null) {
			return getWildcardType(null, getLowestUpperBound(u, v.getLowerBound()));
		}
		// TODO does this just simplify to a null/Object bound?
		return getWildcardType(null, getLowestUpperBound(u, tEnv.getObjectType()));
	}

	//  1. lcta(? extends U, ? extends V) = ? extends lub(U, V)
	//  2. lcta(? extends U, ? super V) = U if U = V, ? otherwise
	//  3. lcta(? super U, ? super V) = ? super glb(U, V)
	//  where glb() is as defined in (�5.1.10).
	private IJavaType getLCTA(IJavaWildcardType u, IJavaWildcardType v) {
		if (u.getUpperBound() != null) {
			return getLCTA_super(u.getUpperBound(), v);
		}
		if (v.getUpperBound() != null) {
			return getLCTA_super(v.getUpperBound(), u);
		}
		// Case 1
		IJavaReferenceType ub = getLowerBound(u);		
		IJavaReferenceType vb = getLowerBound(v);
		return getWildcardType(null, getLowestUpperBound(ub, vb));
	}
	
	private IJavaReferenceType getLowerBound(IJavaWildcardType t) {
		IJavaReferenceType rv = t.getLowerBound();
		if (rv == null) {
			return tEnv.getObjectType();
		}
		return rv;
	}
	
	/**
	 * At least one bound is the upper bound
	 */
	private IJavaType getLCTA_super(IJavaReferenceType upper, IJavaWildcardType t) {
		if (t.getUpperBound() != null) {
			// Case 3
			return getWildcardType(getGreatestLowerBound(t.getUpperBound(), upper), null); 
		} 
		if (t.getLowerBound() != null) {
			// Case 2
			return t.getLowerBound().equals(upper) ? t.getLowerBound() : JavaTypeFactory.wildcardType;
		}
		// FIX case 2?
		return tEnv.getObjectType().equals(upper) ? tEnv.getObjectType() : JavaTypeFactory.wildcardType;
	}

	//  Then, define Candidate(W) = CandidateInvocation(W) if W is generic, W otherwise.
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
		Operator op = JJNode.tree.getOperator(n);
		IRNode typeParams;
		if (ClassDeclaration.prototype.includes(op)) {
			typeParams = ClassDeclaration.getTypes(n);
		}
		else if (InterfaceDeclaration.prototype.includes(op)) {
			typeParams = InterfaceDeclaration.getTypes(n);
		}
		else {
			return false;
		}
		return JJNode.tree.hasChildren(typeParams);
	}
	
	//  Then the inferred type for Tj is
	//  lub(U1 ... Uk) = Candidate(W1) & ... & Candidate(Wr) where Wi, , are
	//  the elements of MEC.
	IJavaReferenceType getLowestUpperBound(IJavaReferenceType... types) {
		// Check for a proxy
		if (types.length == 2) {
			// This is the only case used internally
			JavaRefTypeProxy p = getProxy(types[0], types[1]);
			Boolean complete = p.isComplete();
			if (complete != null) {
				return p;				
			}
			// otherwise, we haven't started computing this yet
			p.start();
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
				reduced.add(bt);
			}
			for (IJavaReferenceType bt : bounds) {
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
}
