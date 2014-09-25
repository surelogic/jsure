package edu.cmu.cs.fluid.java.bind;

import static edu.cmu.cs.fluid.java.bind.IMethodBinder.*;

import java.io.IOException;
import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class TypeInference8 {
	final MethodBinder8 mb;
	final ITypeEnvironment tEnv;
	final TypeUtils utils;
	
	TypeInference8(MethodBinder8 b) {
		mb = b;
		tEnv = mb.tEnv;
		utils = new TypeUtils(tEnv);
	}

	// TODO how to distinguish from each other
	// TODO how to keep from polluting the normal caches?
	static class InferenceVariable extends JavaReferenceType {
		final IRNode formal;
		int index;
		int lowlink;
		
		InferenceVariable(IRNode tf) {
			formal = tf;
		}

		@Override
		public boolean isProperType() {
			return false;
		}
		
		@Override
		public void getReferencedInferenceVariables(Collection<TypeInference8.InferenceVariable> vars) {
			vars.add(this);
		}
		
		@Override
		void writeValue(IROutput out) throws IOException {
			throw new UnsupportedOperationException();
		}		
	}
	
	/**
	 * 18.5.1 Invocation Applicability Inference
	 * 
	 * Given a method invocation that provides no explicit type arguments, the process
	 * to determine whether a potentially applicable generic method m is applicable is as
	 * follows:
	 * 
	 * • Where P 1 , ..., P p (p ≥ 1) are the type parameters of m , let α 1 , ..., α p be inference
	 *   variables, and let θ be the substitution [P 1 :=α 1 , ..., P p :=α p ] .
	 * 
	 * • An initial bound set, B 0 , is constructed from the declared bounds of P 1 , ..., P p , as
	 *   described in §18.1.3.
	 * 
	 * • For all i (1 ≤ i ≤ p), if P i appears in the throws clause of m , then the bound throws
	 *   α i is implied. These bounds, if any, are incorporated with B 0 to produce a new
	 *   bound set, B 1 .
	 *   
	 * • A set of constraint formulas, C , is constructed as follows.
	 * 
	 *   Let F 1 , ..., F n be the formal parameter types of m , and let e 1 , ..., e k be the actual
	 *   argument expressions of the invocation. Then:
	 *   
	 *   – To test for applicability by strict invocation:
	 *   
	 *     If k ≠ n, or if there exists an i (1 ≤ i ≤ n) such that e i is pertinent to applicability
	 *     (§15.12.2.2) and either i) e i is a standalone expression of a primitive type but
	 *     F i is a reference type, or ii) F i is a primitive type but e i is not a standalone
	 *     expression of a primitive type; then the method is not applicable and there is
	 *     no need to proceed with inference.
	 *     
	 *     Otherwise, C includes, for all i (1 ≤ i ≤ k) where e i is pertinent to applicability,
	 *     ‹ e i → F i θ›.
	 *     
	 *   – To test for applicability by loose invocation:
	 *   
	 *     If k ≠ n, the method is not applicable and there is no need to proceed with inference.
	 *     Otherwise, C includes, for all i (1 ≤ i ≤ k) where e i is pertinent to applicability,
	 *     ‹ e i → F i θ›.
	 * 
	 *   – To test for applicability by variable arity invocation:
	 *   
	 *     Let F' 1 , ..., F' k be the first k variable arity parameter types of m (§15.12.2.4). C
	 *     includes, for all i (1 ≤ i ≤ k) where e i is pertinent to applicability, ‹ e i → F' i θ›.
	 *     • C is reduced (§18.2) and the resulting bounds are incorporated with B 1 to produce
	 *     a new bound set, B 2 .
	 *     
	 *     Finally, the method m is applicable if B 2 does not contain the bound false and
     *     resolution of all the inference variables in B 2 succeeds (§18.4).
	 */
	boolean inferForInvocationApplicability(CallState call, MethodBinding m, InvocationKind kind) {
		BoundSet b_0 = constructInitialSet(m.typeFormals);
		// TODO b_1 -- check if type params appear in throws clause
		switch (kind) {
		case STRICT:
			break;
		case LOOSE:
			break;
		case VARARGS:
			break;
		}
		throw new NotImplemented(); // TODO
	}
	
	/**
	 * 18.5.2 Invocation Type Inference
	 * 
     * Given a method invocation that provides no explicit type arguments, and a
     * corresponding most specific applicable generic method m , the process to infer the
     * invocation type (§15.12.2.6) of the chosen method is as follows:
     * 
     * ...
	 */
	void inferForInvocationType() {
		
	}
	
	/**
	 * 18.5.3 Functional Interface Parameterization Inference
	 * 
     * Where a lambda expression with explicit parameter types P 1 , ..., P n targets a
     * functional interface type F<A 1 , ..., A m > with at least one wildcard type argument,
     * then a parameterization of F may be derived as the ground target type of the lambda
     * expression as follows.
     * 
     * Let Q 1 , ..., Q k be the parameter types of the function type of the type F< α 1 , ..., α m > ,
     * where α 1 , ..., α m are fresh inference variables.
     * 
     * If n ≠ k, no valid parameterization exists. Otherwise, a set of constraint formulas is
     * formed with, for all i (1 ≤ i ≤ n), ‹ P i = Q i ›. This constraint formula set is reduced
     * to form the bound set B .
     * 
     * If B contains the bound false, no valid parameterization exists. Otherwise, a new
     * parameterization of the functional interface type, F<A' 1 , ..., A' m > , is constructed as
     * follows, for 1 ≤ i ≤ m:
     * 
     * • If B contains an instantiation for α i , T , then A' i = T .
     * • Otherwise, A' i = A i .
     * 
     * If F<A' 1 , ..., A' m > is not a well-formed type (that is, the type arguments are
     * not within their bounds), or if F<A' 1 , ..., A' m > is not a subtype of F<A 1 , ..., A m >, 
     * no valid parameterization exists. Otherwise, the inferred parameterization is either 
     * F<A' 1 , ..., A' m > , if all the type arguments are types, or the non-wildcard parameterization 
     * (§9.8) of F<A' 1 , ..., A' m > , if one or more type arguments are still wildcards.
	 */
	void inferForFunctionalInterfaceParameterization() {
		
	}
	
	/**
	 * 18.5.4 More Specific Method Inference
	 * 
     * When testing that one applicable method is more specific than another (§15.12.2.5),
     * where the second method is generic, it is necessary to test whether some
     * instantiation of the second method's type parameters can be inferred to make the
     * first method more specific than the second.
     * 
     * Let m 1 be the first method and m 2 be the second method. Where m 2 has type
     * parameters P 1 , ..., P p , let α 1 , ..., α p be inference variables, and let θ be the
     * substitution [P 1 :=α 1 , ..., P p :=α p ] .
     * 
     * Let e 1 , ..., e k be the argument expressions of the corresponding invocation. Then:
     * 
     * • If m 1 and m 2 are applicable by strict or loose invocation (§15.12.2.2, §15.12.2.3),
     *   then let S 1 , ..., S k be the formal parameter types of m 1 , and let T 1 , ..., T k be the
     *   result of θ applied to the formal parameter types of m 2 .
     * 
     * • If m 1 and m 2 are applicable by variable arity invocation (§15.12.2.4), then let S 1 , ...,
     *   S k be the first k variable arity parameter types of m 1 , and let T 1 , ..., T k be the result
     *   of θ applied to the first k variable arity parameter types of m 2 .
     * 
     *     Note that no substitution is applied to S 1 , ..., S k ; even if m 1 is generic, the type parameters
     *     of m 1 are treated as type variables, not inference variables.
     * 
     * The process to determine if m 1 is more specific than m 2 is as follows:
     * 
     * • First, an initial bound set, B , is constructed from the declared bounds of P 1 , ...,
     *   P p , as specified in §18.1.3.
     * 
     * • Second, for all i (1 ≤ i ≤ k), a set of constraint formulas or bounds is generated.
     *   
     *   If T i is a proper type, the result is true if S i is more specific than T i for e i
     *   (§15.12.2.5), and false otherwise. (Note that S i is always a proper type.)
     *   
     *   Otherwise, if T i is not a functional interface type, the constraint formula ‹ S i <:
     *   T i › is generated.
     * 
     *   Otherwise, T i is a parameterization of a functional interface, I . It must be
     *   determined whether S i satisfies the following five constraints:
     *   
     *   – S i is a functional interface type.
     *   – S i is not a superinterface of I , nor a parameterization of a superinterface of I .
     *   – S i is not a subinterface of I , nor a parameterization of a subinterface of I .
     *   – If S i is an intersection type, at least one element of the intersection is not a
     *   superinterface of I , nor a parameterization of a superinterface of I .
     *   – If S i is an intersection type, no element of the intersection is a subinterface of
     *   I , nor a parameterization of a subinterface of I .
     *   If all of the above are true, then the following constraint formulas or bounds are
     *   generated (where U 1 ... U k and R 1 are the parameter types and return type of the
     *   function type of the capture of S i , and V 1 ... V k and R 2 are the parameter types and
     *   return type of the function type of T i ):
     *   – If e i is an explicitly typed lambda expression:
     *   › If R 2 is void , true.
     *   › Otherwise, if R 1 and R 2 are functional interface types, and neither interface
     *   is a subinterface of the other, then these rules are applied recursively to R 1
     *   and R 2 , for each result expression in e i .
     *   › Otherwise, if R 1 is a primitive type and R 2 is not, and each result expression
     *   of e i is a standalone expression (§15.2) of a primitive type, true.
     *   › Otherwise, if R 2 is a primitive type and R 1 is not, and each result expression of
     *   e i is either a standalone expression of a reference type or a poly expression,
     *   true.
     *   › Otherwise, ‹ R 1 <: R 2 ›.
     *   – If e i is an exact method reference:
     *   › For all j (1 ≤ j ≤ k), ‹ U j = V j ›.
     *   › If R 2 is void , true.
     *   › Otherwise, if R 1 is a primitive type and R 2 is not, and the compile-time
     *   declaration for e i has a primitive return type, true.
     *   › Otherwise if R 2 is a primitive type and R 1 is not, and the compile-time
     *   declaration for e i has a reference return type, true.
     *   › Otherwise, ‹ R 1 <: R 2 ›.
     *   – If e i is a parenthesized expression, these rules are applied recursively to the
     *   contained expression.
     *   
     *   – If e i is a conditional expression, these rules are applied recursively to each of
     *   the second and third operands.
     *   – Otherwise, false.
     *   If the five constraints on S i are not satisfied, the constraint formula ‹ S i <: T i ›
     *   is generated instead.
     *   • Third, if m 2 is applicable by variable arity invocation and has k+1 parameters,
     *   then where S k+1 is the k+1'th variable arity parameter type of m 1 and T k+1 is the
     *   result of θ applied to the k+1'th variable arity parameter type of m 2 , the constraint
     *   ‹ S k+1 <: T k+1 › is generated.
     *   • Fourth, the generated bounds and constraint formulas are reduced and
     *   incorporated with B to produce a bound set B' .
     *   If B' does not contain the bound false, and resolution of all the inference variables
     *   in B' succeeds, then m 1 is more specific than m 2 .
     *   Otherwise, m 1 is not more specific than m 2 .
	 */
	void inferForMoreSpecificMethod() {
		
	}
	
	/**
	 * 18.1.2 Constraint Formulas
	 * 
	 * Constraint formulas are assertions of compatibility or subtyping that may involve
	 * inference variables. The formulas may take one of the following forms:
	 * 
	 * • ‹Expression → T ›: An expression is compatible in a loose invocation context
	 *   with type T (§5.3).
	 *   
	 * • ‹ S → T ›: A type S is compatible in a loose invocation context with type T (§5.3).
	 * 
	 * • ‹ S <: T ›: A reference type S is a subtype of a reference type T (§4.10).
	 * 
	 * • ‹ S <= T ›: A type argument S is contained by a type argument T (§4.5.1).
	 * 
	 * • ‹ S = T ›: A reference type S is the same as a reference type T (§4.3.4), or a type
	 *   argument S is the same as type argument T .
	 * 
	 * • ‹LambdaExpression → throws T ›: The checked exceptions thrown by the body of
	 *   the LambdaExpression are declared by the throws clause of the function type
	 *   derived from T .
	 *   
	 * • ‹MethodReference → throws T ›: The checked exceptions thrown by the referenced
	 *   method are declared by the throws clause of the function type derived from T .
	 */
	static class ConstraintFormula {
		final IRNode expr;	
		final IJavaType stype;		
		final FormulaConstraint constraint;
		final IJavaType type;
	
		ConstraintFormula(IJavaType s, FormulaConstraint c, IJavaType t) {
			expr = null;
			stype = s;
			constraint = c;
			type = t;
		}
			
		ConstraintFormula(IRNode e, FormulaConstraint c, IJavaType t) {
			expr = e;
			stype = null;
			constraint = c;
			type = t;
		}
	}
	
	enum FormulaConstraint {
		IS_COMPATIBLE, IS_SUBTYPE, IS_CONTAINED_BY_TYPE_ARG, IS_SAME, THROWS
	}
	
	/**
	 * 18.1.3 Bounds
	 * 
	 * During the inference process, a set of bounds on inference variables is maintained.
	 * A bound has one of the following forms:
	 * • S = T , where at least one of S or T is an inference variable: S is the same as T .
	 * 
	 * • S <: T , where at least one of S or T is an inference variable: S is a subtype of T .
	 * 
	 * • false: No valid choice of inference variables exists.
	 * 
	 * • G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > ): The variables α 1 , ..., α n represent the result
	 *   of capture conversion (§5.1.10) applied to G<A 1 , ..., A n > (where A 1 , ..., A n may be
	 *   types or wildcards and may mention inference variables).
	 *   
	 * • throws α: The inference variable α appears in a throws clause.
	 * 
	 * A bound is satisfied by an inference variable substitution if, after applying the
	 * substitution, the assertion is true. The bound false can never be satisfied.
	 * 
	 * Some bounds relate an inference variable to a proper type. Let T be a proper type.
	 * Given a bound of the form α = T or T = α, we say T is an instantiation of α. Similarly,
	 * given a bound of the form α <: T , we say T is a proper upper bound of α, and given
	 * a bound of the form T <: α, we say T is a proper lower bound of α.
	 * 
	 * Other bounds relate two inference variables, or an inference variable to a type that
	 * contains inference variables. Such bounds, of the form S = T or S <: T , are called
	 * dependencies.
	 * 
	 * A bound of the form G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > ) indicates that α 1 , ..., α n
	 * are placeholders for the results of capture conversion. This is necessary because
	 * capture conversion can only be performed on a proper type, and the inference
	 * variables in A 1 , ..., A n may not yet be resolved.
	 * 
	 * A bound of the form throws α is purely informational: it directs resolution to
	 * optimize the instantiation of α so that, if possible, it is not a checked exception type.
	 */
	static class Bound<T extends IJavaReferenceType> {
		final T s, t;
		
		Bound(T s, T t) {
			this.s = s;
			this.t = t;
			
			if (s instanceof InferenceVariable || t instanceof InferenceVariable) {
				// Nothing to do
			} else {
				throw new IllegalStateException();
			}
		}
		
		@Override
		public int hashCode() {
			return s.hashCode() + t.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Bound) {
				Bound<?> other = (Bound<?>) o;
				return s.equals(other.s) && t.equals(other.t);
			}
			return false;
		}
	}
	
	static class EqualityBound extends Bound<IJavaReferenceType> {
		EqualityBound(IJavaReferenceType s, IJavaReferenceType t) {
			super(s, t);
		}
	}
	
	static class SubtypeBound extends Bound<IJavaReferenceType> {
		SubtypeBound(IJavaReferenceType s, IJavaReferenceType t) {
			super(s, t);
		}
	}
	
	static class CaptureBound extends Bound<IJavaDeclaredType>{
		CaptureBound(IJavaDeclaredType vars, IJavaDeclaredType needCapture) {
			super(vars, needCapture);
		}
		
		boolean refersTo(final Set<InferenceVariable> vars) {
			for(IJavaType param : s.getTypeParameters()) {
				if (vars.contains(param)) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * From §18.1.3:
	 * 
	 * When inference begins, a bound set is typically generated from a list of type
	 * parameter declarations P 1 , ..., P p and associated inference variables α 1 , ..., α p . Such
	 * a bound set is constructed as follows. For each l (1 ≤ l ≤ p):
	 * 
	 * • If P l has no TypeBound, the bound α l <: Object appears in the set.
	 * 
	 * • Otherwise, for each type T delimited by & in the TypeBound, 
	 *   the bound α l <: T[P 1 :=α 1 , ..., P p :=α p ] appears in the set; 
	 *   if this results in no proper upper bounds for α l (only dependencies), 
	 *   then the bound α l <: Object also appears in the set.
	 */
	BoundSet constructInitialSet(IRNode typeFormals) {
		// Setup inference variables
		final int numFormals = JJNode.tree.numChildren(typeFormals);
		final InferenceVariable[] vars = new InferenceVariable[numFormals];
		int i=0;
		for(IRNode tf : TypeFormals.getTypeIterator(typeFormals)) {
			vars[i] = new InferenceVariable(tf);
			i++;
		}
		final BoundSet set = new BoundSet(typeFormals, vars);
		i=0;
		for(IRNode tf : TypeFormals.getTypeIterator(typeFormals)) {
			IRNode bounds = TypeFormal.getBounds(tf);
			boolean noBounds = true;
			boolean gotProperBound = false;
			for(IRNode bound : MoreBounds.getBoundIterator(bounds)) {
				final IJavaType t = tEnv.getBinder().getJavaType(bound);
				final IJavaType t_subst = t.subst(null); //TODO subst vars
				noBounds = false;
				set.addSubtypeBound(vars[i], t_subst);
				if (t_subst.isProperType()) {
					gotProperBound = true;
				}
			}
			if (noBounds || !gotProperBound) {
				set.addSubtypeBound(vars[i], tEnv.getObjectType());
			}
			i++;
		}
		throw new NotImplemented(); // TODO
	}
	
	/**
	 * An important intermediate result of inference is a bound set. It is sometimes
	 * convenient to refer to an empty bound set with the symbol true; this is merely out
	 * of convenience, and the two are interchangeable
	 */
	class BoundSet {
		private boolean isFalse = false;
		private final Set<InferenceVariable> thrownSet = new HashSet<InferenceVariable>();
		private final Set<EqualityBound> equalities = new HashSet<EqualityBound>();
		private final Set<SubtypeBound> subtypeBounds = new HashSet<SubtypeBound>();
		private final Set<CaptureBound> captures = new HashSet<CaptureBound>();
		
		/**
		 * The original bound that eventually created this one
		 */
		private final BoundSet original;
		
		/**
		 * Mapping from the original type variables to the corresponding inference variables
		 */
		private final Map<IJavaReferenceType,InferenceVariable> variableMap = new HashMap<IJavaReferenceType,InferenceVariable>();
		
		/**
		 * The result of resolution
		 */
		private final Map<InferenceVariable, IJavaType> instantiations = new HashMap<InferenceVariable, IJavaType>();
		
		BoundSet(final IRNode typeFormals, final InferenceVariable[] vars) {
			original = null;
			
			int i=0;
			for(IRNode tf : TypeFormals.getTypeIterator(typeFormals)) {
				variableMap.put(JavaTypeFactory.getTypeFormal(tf), vars[i]);
				i++;
			}
		}

		BoundSet(BoundSet orig) {
			original = orig.original == null ? orig : orig.original;
			isFalse = orig.isFalse;
			thrownSet.addAll(orig.thrownSet);
			equalities.addAll(orig.equalities);
			subtypeBounds.addAll(orig.subtypeBounds);
			captures.addAll(orig.captures);
			instantiations.putAll(orig.instantiations);
			variableMap.putAll(orig.variableMap);
		}
		
		private void addInferenceVariables(Map<InferenceVariable, InferenceVariable> newMappings) {
			variableMap.putAll(newMappings);
		}
		
		void addFalse() {
			isFalse = true;
		}

		void addTrue() {
			// TODO what is there to do?
		}
		
		void addEqualityBound(IJavaType s, IJavaType t) {
			if (t == null) {
				throw new NullPointerException("No type for equality bound");
			}
			equalities.add(new EqualityBound((IJavaReferenceType) s, (IJavaReferenceType) t));
		}

		// s <: t
		void addSubtypeBound(IJavaType s, IJavaType t) {
			subtypeBounds.add(new SubtypeBound((IJavaReferenceType) s, (IJavaReferenceType) t));
		}	
		
		void addCaptureBound(IJavaType s, IJavaType t) {
			captures.add(new CaptureBound((IJavaDeclaredType) s, (IJavaDeclaredType) t));
		}
		
		void addThrown(InferenceVariable v) {
			thrownSet.add(v);
		}
		
		void addInstantiation(InferenceVariable v, IJavaType t) {
			if (v == null || t == null) {
				throw new NullPointerException("Bad instantiation: "+v+" = "+t);
			}
			instantiations.put(v, t);
			addEqualityBound(v, t);
		}
		
		private void collectVariablesFromBounds(Set<InferenceVariable> vars, Set<? extends Bound<?>> bounds) {
			for(Bound<?> b : bounds) {
				b.s.getReferencedInferenceVariables(vars);
				b.t.getReferencedInferenceVariables(vars);
			}
		}
		
		Set<InferenceVariable> collectVariables() {
			Set<InferenceVariable> vars = new HashSet<InferenceVariable>(thrownSet);
			collectVariablesFromBounds(vars, equalities);
			collectVariablesFromBounds(vars, subtypeBounds);
			collectVariablesFromBounds(vars, captures);
			return vars;
		}
		
		Set<InferenceVariable> chooseUninstantiated() {
			final Set<InferenceVariable> vars = collectVariables();
			final Set<InferenceVariable> uninstantiated = new HashSet<InferenceVariable>(vars);
			uninstantiated.removeAll(instantiations.keySet());
						
			VarDependencies deps = computeVarDependencies();
			return deps.chooseUninstantiated(uninstantiated);
		}
		
		private VarDependencies computeVarDependencies() {
			VarDependencies deps = new VarDependencies();
			deps.recordDependencies(equalities);
			deps.recordDependencies(subtypeBounds);
			for(CaptureBound b : captures) {
				deps.recordDepsForCapture(b);
			}
			return deps;
		}
		
		/**
		 * Find bounds where a = b
		 * @return
		 */
		private MultiMap<InferenceVariable,InferenceVariable> collectIdentities() {
			MultiMap<InferenceVariable,InferenceVariable> rv = new MultiHashMap<InferenceVariable,InferenceVariable> ();
			for(Bound<?> bound : equalities) {
				if (bound.s instanceof InferenceVariable && bound.t instanceof InferenceVariable) {
					InferenceVariable a = (InferenceVariable) bound.s;
					InferenceVariable b = (InferenceVariable) bound.t;
					rv.put(a, b);
					rv.put(b, a);
				}
			}
			return rv;
		}
		
		boolean hasNoCaptureBoundInvolvingVars(Set<InferenceVariable> vars) {
			for(CaptureBound b : captures) {
				if (b.refersTo(vars)) {
					return false;
				}
			}
			return true;
		}	
		
		private void removeAssociatedCaptureBounds(Set<InferenceVariable> vars) {
			Iterator<CaptureBound> it = captures.iterator();
			while (it.hasNext()) {
				CaptureBound b = it.next();
				if (b.refersTo(vars)) {
					it.remove();
				}
			}
		}
		
		/**
		 *   – If α i has one or more proper lower bounds, L 1 , ..., L k , then T i = lub( L 1 , ..., L k ) (§4.10.4).
		 *   
		 *   – Otherwise, if the bound set contains throws α i , and the proper upper
		 *     bounds of α i are, at most, Exception , Throwable , and Object , then T i = RuntimeException .
		 *   
		 *   – Otherwise, where α i has proper upper bounds U 1 , ..., U k , T i = glb( U 1 , ..., U k ) (§5.1.10).
		 *  
		 *   The bounds α 1 = T 1 , ..., α n = T n are incorporated with the current bound set.
		 * 
		 *   If the result does not contain the bound false, then the result becomes the new bound set, and 
		 *   resolution proceeds by selecting a new set of variables to instantiate (if necessary), as described above.
		 * 
		 *   Otherwise, the result contains the bound false, so a second attempt is made to
		 *   instantiate { α 1 , ..., α n } by performing the step below.
		 * @param subset 
		 */
		BoundSet instantiateFromBounds(Set<InferenceVariable> subset) {
			final ProperBounds bounds = collectProperBounds();
			final BoundSet rv = new BoundSet(this);
			for(InferenceVariable a_i : subset) {
				Collection<IJavaType> lower = bounds.lowerBounds.get(a_i);
				if (lower != null && !lower.isEmpty()) {
					rv.addInstantiation(a_i, utils.getLowestUpperBound(toArray(lower)));
					continue;
				}
				Collection<IJavaType> upper = bounds.upperBounds.get(a_i);
				if (thrownSet.contains(a_i) && qualifiesAsRuntimeException(upper)) {
					rv.addInstantiation(a_i, tEnv.findJavaTypeByName("java.lang.RuntimeException"));
					continue;
				}			
				if (upper != null && !upper.isEmpty()) {
					rv.addInstantiation(a_i, utils.getGreatestLowerBound(toArray(upper)));
				} else {
					throw new IllegalStateException("what do I do otherwise?"); // TODO
				}
			}
			return rv;
		}

		private boolean qualifiesAsRuntimeException(Collection<IJavaType> upper) {
			if (upper.isEmpty()) {
				return true;
			}
			IJavaType exception = tEnv.findJavaTypeByName("java.lang.Exception");
			IJavaType throwable = tEnv.findJavaTypeByName("java.lang.Throwable");
			IJavaType object = tEnv.getObjectType();
			Set<IJavaType> temp = new HashSet<IJavaType>(upper);
			temp.remove(exception);
			temp.remove(throwable);
			temp.remove(object);
			return temp.isEmpty();
		}

		private IJavaReferenceType[] toArray(Collection<IJavaType> types) {
			IJavaReferenceType[] rv = new IJavaReferenceType[types.size()];
			int i=0;
			for(IJavaType t : types) {
				rv[i] = (IJavaReferenceType) t;
				i++;
			}
			return rv;
		}

		private ProperBounds collectProperBounds() {
			final ProperBounds bounds = new ProperBounds();
			for(SubtypeBound b : subtypeBounds) {
				if (b.s instanceof InferenceVariable) {
					if (b.t.isProperType()) {
						bounds.upperBounds.put((InferenceVariable) b.s, b.t);
					}
				}
				else if (b.t instanceof InferenceVariable) {
					if (b.s.isProperType()) {
						bounds.lowerBounds.put((InferenceVariable) b.t, b.s);
					}
				}
			}
			return bounds;
		}
		
		// TODO What about duplicates?
		class ProperBounds {
			MultiMap<InferenceVariable,IJavaType> lowerBounds = new MultiHashMap<InferenceVariable,IJavaType>();
			MultiMap<InferenceVariable,IJavaType> upperBounds = new MultiHashMap<InferenceVariable,IJavaType>();
		}
		
		/**
		 *  then let Y 1 , ..., Y n be fresh type variables whose bounds are as follows:
		 *  
		 *   – For all i (1 ≤ i ≤ n), if α i has one or more proper lower bounds L 1 , ..., L k , then
		 *     let the lower bound of Y i be lub( L 1 , ..., L k ); if not, then Y i has no lower bound.
		 *   
		 *   – For all i (1 ≤ i ≤ n), where α i has upper bounds U 1 , ..., U k , let the upper bound
		 *     of Y i be glb( U 1 θ, ..., U k θ), where θ is the substitution [ α 1 := Y 1 , ..., α n := Y n ] .
		 *   
		 *   If the type variables Y 1 , ..., Y n do not have well-formed bounds (that is, a lower
		 *   bound is not a subtype of an upper bound, or an intersection type is inconsistent), then resolution fails.
		 *  
		 *   Otherwise, for all i (1 ≤ i ≤ n), all bounds of the form G< ..., α i , ... > =
		 *   capture( G< ... > ) are removed from the current bound set, and the bounds α 1 = Y 1 , ..., α n = Y n are incorporated.
		 *   
		 *   If the result does not contain the bound false, then the result becomes the
		 *   new bound set, and resolution proceeds by selecting a new set of variables to
		 *   instantiate (if necessary), as described above.
		 * 
		 *   Otherwise, the result contains the bound false, and resolution fails.
		 *   
		 * @return the new bound set to try to resolve
		 */
		BoundSet instantiateViaFreshVars(Set<InferenceVariable> subset) {
			final ProperBounds bounds = collectProperBounds();
			Map<InferenceVariable,InferenceVariable> y_subst = new HashMap<InferenceVariable,InferenceVariable>(subset.size());
			for(InferenceVariable a_i : subset) {
				final InferenceVariable y_i = new InferenceVariable(a_i.formal); // TODO unique?
				y_subst.put(a_i, y_i);
			}
			final BoundSet rv = new BoundSet(this);
			rv.removeAssociatedCaptureBounds(subset);			
			rv.addInferenceVariables(y_subst);
			for(InferenceVariable a_i : subset) {
				final InferenceVariable y_i = y_subst.get(a_i);
				Collection<IJavaType> lower = bounds.lowerBounds.get(a_i);
				IJavaType l_i = null;
				if (lower != null && !lower.isEmpty()) {
					l_i = utils.getLowestUpperBound(toArray(lower));
				}
				Collection<IJavaType> upper = bounds.upperBounds.get(a_i);
				IJavaType u_i = null;
				if (upper != null && !upper.isEmpty()) {
					u_i = utils.getGreatestLowerBound(toArray(upper)); // TODO subst for y_i
				}
				// Check if the bounds are well-formed
				if (l_i != null && u_i != null && !l_i.isSubtype(tEnv, u_i)) {
					throw new IllegalStateException("resolution failed");
				}
				// TODO how to check for intersection type?
				
				// add new bounds
				if (l_i != null) {
					rv.addSubtypeBound(l_i, y_i);
				}
				if (u_i != null) {
					rv.addSubtypeBound(y_i, u_i);
				}
				rv.addEqualityBound(a_i, y_i);
			}		
			return rv;
		}
	}
	
	/** 
	 * 18.4 Resolution
	 * 
	 * Given a set of inference variables to resolve, let V be the union of this set and all
	 * variables upon which the resolution of at least one variable in this set depends.
	 * 
	 * If every variable in V has an instantiation, then resolution succeeds and this procedure terminates.
	 * 
	 * Otherwise, let { α 1 , ..., α n } be a non-empty subset of uninstantiated variables in
	 * V such that i) for all i (1 ≤ i ≤ n), if α i depends on the resolution of a variable β,
	 * then either β has an instantiation or there is some j such that β = α j ; and ii) there
	 * exists no non-empty proper subset of { α 1 , ..., α n } with this property. Resolution
	 * proceeds by generating an instantiation for each of α 1 , ..., α n based on the bounds in the bound set:
	 * 
	 * • If the bound set does not contain a bound of the form G< ..., α i , ... > =
	 *   capture( G< ... > ) for all i (1 ≤ i ≤ n), then a candidate instantiation T i is defined for each α i :
	 * 
	 *   ...
	 * 
	 * • If the bound set contains a bound of the form G< ..., α i , ... > = capture( G< ... > ) for some i (1 ≤ i ≤ n), or;
	 *   
	 *   If the bound set produced in the step above contains the bound false;
	 *   
	 *   ...
	 *   
	 * (keep trying if there are uninstantiated variables)
	 */
	static BoundSet resolve(final BoundSet bounds) {
		if (bounds == null || bounds.isFalse) {
			return null;
		}
		final Set<InferenceVariable> subset = bounds.chooseUninstantiated();
		if (bounds.hasNoCaptureBoundInvolvingVars(subset)) {
			BoundSet fresh = bounds.instantiateFromBounds(subset);
			BoundSet rv = resolve(fresh);
			if (rv != null) {					
				return rv;
			}
			// Otherwise, try below
		}
		BoundSet fresh = bounds.instantiateViaFreshVars(subset);
		return resolve(fresh);		
	}
	
	class VarDependencies {
		final MultiMap<InferenceVariable,InferenceVariable> dependsOn = new MultiHashMap<InferenceVariable,InferenceVariable>();

		private void markDependsOn(InferenceVariable alpha, InferenceVariable beta) {
			dependsOn.put(alpha, beta);
		}	
		
		public Set<InferenceVariable> chooseUninstantiated(final Set<InferenceVariable> uninstantiated) {
			computeStronglyConnectedComponents();
			
			final List<InferenceVariable> ordering = computeTopologicalSort();
			final Set<InferenceVariable> rv = new HashSet<InferenceVariable>();
			boolean foundComponent = false;
			for(final InferenceVariable v : ordering) {
				final Collection<InferenceVariable> vars = components.get(v); 
				for(final InferenceVariable w : ordering) {
					if (uninstantiated.contains(w)) {
						rv.add(w);
						foundComponent = true;
					}					
					// TODO do i need to check that all the variables in a component are uninstantiated?
				}
				if (foundComponent) {
					return rv;
				}
			}
			return Collections.emptySet();
		}

		/**
		 * 18.4 Resolution
		 * 
		 * Given a bound set that does not contain the bound false, a subset of the inference variables mentioned by the 
		 * bound set may be resolved. This means that a satisfactory instantiation may be added to the set for each 
		 * inference variable, until all the requested variables have instantiations.
		 * 
		 * Dependencies in the bound set may require that the variables be resolved in a particular order, or that 
		 * additional variables be resolved. Dependencies are specified as follows:
		 * 
		 * • Given a bound of one of the following forms, where T is either an inference variable β or a type that mentions β:
		 *   – α = T
		 *   – α <: T
		 *   – T = α
		 *   – T <: α
		 * 
		 *   If α appears on the left-hand side of another bound of the form G< ..., α, ... > =
		 *   capture( G< ... > ), then β depends on the resolution of α. Otherwise, α depends on the resolution of β.
		 * 
		 * • An inference variable α appearing on the left-hand side of a bound of the form
		 *   G< ..., α, ... > = capture( G< ... > ) depends on the resolution of every other inference
		 *   variable mentioned in this bound (on both sides of the = sign).
		 * 
		 * • An inference variable α depends on the resolution of an inference variable β if
		 *   there exists an inference variable γ such that α depends on the resolution of γ and
		 *   γ depends on the resolution of β.
		 * 
		 * • An inference variable α depends on the resolution of itself.
		 */				
		void recordDependencies(Set<? extends Bound<?>> bounds) {
			final Set<InferenceVariable> temp = new HashSet<InferenceVariable>();
			for(Bound<?> b : bounds) {
				if (b.s instanceof InferenceVariable) {
					InferenceVariable alpha = (InferenceVariable) b.s;
					b.t.getReferencedInferenceVariables(temp);
					for(InferenceVariable beta : temp) {
						markDependsOn(alpha, beta);
					}
				}
				else if (b.t instanceof InferenceVariable) {
					InferenceVariable alpha = (InferenceVariable) b.t;
					b.s.getReferencedInferenceVariables(temp);
					for(InferenceVariable beta : temp) {
						markDependsOn(alpha, beta);
					}
				}
				temp.clear(); 
			}
		}		
		
		void recordDepsForCapture(CaptureBound b) {
			final Set<InferenceVariable> vars = new HashSet<InferenceVariable>();
			b.s.getReferencedInferenceVariables(vars);
			b.t.getReferencedInferenceVariables(vars);
			
			// For each parameter on the left-hand side
			for(IJavaType param : b.s.getTypeParameters()) {
				if (param instanceof InferenceVariable) {
					InferenceVariable alpha = (InferenceVariable) param;
					for(InferenceVariable beta : vars) {
						markDependsOn(alpha, beta);
					}
				}
			}
		}	
		
		int index = 0;
		final Stack<InferenceVariable> s = new Stack<InferenceVariable>();
		final MultiMap<InferenceVariable,InferenceVariable> components = new MultiHashMap<InferenceVariable,InferenceVariable>();
		
		// http://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
		private void computeStronglyConnectedComponents() {
		  // Reset info
		  for(InferenceVariable v : dependsOn.keySet()) {
			  v.index = -1;
			  v.lowlink = Integer.MAX_VALUE;
		  }
		  /*
		   * algorithm tarjan is
		   * input: graph G = (V, E)
		   * output: set of strongly connected components (sets of vertices)
		   * 
		   * index := 0
		   * S := empty
		   * for each v in V do
		   *   if (v.index is undefined) then
		   *     strongconnect(v)
		   *   end if
		   * end for
		   */
		  index = 0;
		  s.clear();
		  components.clear();
		  
		   for(InferenceVariable v : dependsOn.keySet()) {
			   if (v.index < 0) {
				   strongConnect(v);
			   }
		   }
		}		  
		
		private void strongConnect(final InferenceVariable v) {
		  /*		
		  function strongconnect(v)
		    // Set the depth index for v to the smallest unused index
		    v.index := index
		    v.lowlink := index
		    index := index + 1
		    S.push(v)
		    */
			v.index = index;
			v.lowlink = index;
			index++;
			s.push(v);
			
		    /* Consider successors of v
		    for each (v, w) in E do
		      if (w.index is undefined) then
		        // Successor w has not yet been visited; recurse on it
		        strongconnect(w)
		        v.lowlink  := min(v.lowlink, w.lowlink)
		      else if (w is in S) then
		        // Successor w is in stack S and hence in the current SCC
		        v.lowlink  := min(v.lowlink, w.index)
		      end if
		    end for
		    */
			for(InferenceVariable w : dependsOn.get(v)) {
				if (w.index < 0) {
					strongConnect(w);
					v.lowlink = Math.min(v.lowlink, w.lowlink);
				}
				else if (s.contains(w)) {
					v.lowlink = Math.min(v.lowlink, w.index);
				}
			}

		    /* If v is a root node, pop the stack and generate an SCC
		    if (v.lowlink = v.index) then
		      start a new strongly connected component
		      repeat
		        w := S.pop()
		        add w to current strongly connected component
		      until (w = v)
		      output the current strongly connected component
		    end if
		    */
			if (v.lowlink == v.index) {
				InferenceVariable w = null;
				do {
					w = s.pop();
					components.put(v, w);
				} 
				while (v != w);
			}
		  //end function		  
		}
		
		// http://en.wikipedia.org/wiki/Topological_sorting
		private List<InferenceVariable> computeTopologicalSort() {
			/*
			L ← Empty list that will contain the sorted nodes
			while there are unmarked nodes do
			    select an unmarked node n
			    visit(n) 
			*/
			final List<InferenceVariable> l = new LinkedList<InferenceVariable>();
			for(InferenceVariable v : components.keySet()) {
				v.index = -1;
			}
			final Set<InferenceVariable> toVisit = new HashSet<InferenceVariable>(components.keySet());
			
			while (!toVisit.isEmpty()) {
				InferenceVariable n = toVisit.iterator().next();
				visitForSort(l, toVisit, n);
			}
			return l;
		}
		
		private void visitForSort(final List<InferenceVariable> l, final Set<InferenceVariable> toVisit, final InferenceVariable n) {
			/*
			function visit(node n)
			    if n has a temporary mark then stop (not a DAG)
			    if n is not marked (i.e. has not been visited yet) then
			        mark n temporarily
			        for each node m with an edge from n to m do
			            visit(m)
			        mark n permanently
			        unmark n temporarily
			        add n to head of L
			 */
			if (n.index == 0) {
				throw new IllegalStateException("Not a DAG");
			}
			if (n.index < 0) {
				toVisit.remove(n);
				n.index = 0;
				for(InferenceVariable m : dependsOn.get(n)) {
					if (components.containsKey(m)) { // filter to the component roots
						visitForSort(l, toVisit, m);
					}
				}
				n.index = 100;
				l.add(0, n);
			}
		}
	}
	
	private boolean isStandaloneExpr(IRNode e) {
		throw new NotImplemented(); // TODO
	}
 	
	static boolean isInferenceVariable(IJavaType t) {
		return t instanceof InferenceVariable;
	}
	
	// (§9.8)
	private boolean isFunctionalInterfaceType(IJavaType t) {
		throw new NotImplemented(); // TODO
	}
	
	IJavaReferenceType box(IJavaType t) {
		throw new NotImplemented(); // TODO
	}
	
	/**
	 * @return true if T is a parameterized type of the form G<T 1 , ..., T n > , and there exists
	 * no type of the form G< ... > that is a supertype of S , but the raw type G is a supertype
	 * of S
	 */
	boolean hasRawSuperTypeOf(IJavaType s, IJavaType t) {
		//tEnv.isRawSubType(s, t);
		throw new NotImplemented(); // TODO
	}
	
	/**
	 * 18.2 Reduction
	 * 
     * Reduction is the process by which a set of constraint formulas (§18.1.2) is
     * simplified to produce a bound set (§18.1.3).
     * 
     * Each constraint formula is considered in turn. The rules in this section specify how
     * the formula is reduced to one or both of:
     * 
     * • A bound or bound set, which is to be incorporated with the "current" bound set.
     *   Initially, the current bound set is empty.
     *   
     * • Further constraint formulas, which are to be reduced recursively.
     *   Reduction completes when no further constraint formulas remain to be reduced.
	 */
	void reduceConstraintFormula(BoundSet bounds, ConstraintFormula f) {
		switch (f.constraint) {
		case IS_COMPATIBLE:
			if (f.expr != null) {
				reduceExpressionCompatibilityConstraints(bounds, f.expr, f.type);
	
			} else {
				reduceTypeCompatibilityConstraints(bounds, f.stype, f.type);
			}
			break;
		case IS_SUBTYPE:
			reduceSubtypingConstraints(bounds, f.stype, f.type);
			break;
		case IS_CONTAINED_BY_TYPE_ARG:
			reduceTypeArgContainmentConstraints(bounds, f.stype, f.type);
			break;
		case IS_SAME:
			reduceTypeEqualityConstraints(bounds, f.stype, f.type);
			break;
		case THROWS:
			if (LambdaExpression.prototype.includes(f.expr)) {
				reduceLambdaCheckedExceptionConstraints(bounds, f.expr, f.type);
			} else {
				reduceMethodRefCheckedExceptionConstraints(bounds, f.expr, f.type);
			}
			break;
		default:
			throw new IllegalStateException();
		}
	}

	/**
	 * 18.2.1 Expression Compatibility Constraints
	 * 
	 * A constraint formula of the form ‹Expression → T › is reduced as follows:
	 * 
	 * • If T is a proper type, the constraint reduces to true if the expression is compatible
	 *   in a loose invocation context with T (§5.3), and false otherwise.
	 *   
	 * • Otherwise, if the expression is a standalone expression (§15.2) of type S , the
	 *   constraint reduces to ‹ S → T ›.
	 *   
	 * • Otherwise, the expression is a poly expression (§15.2). The result depends on
	 *   the form of the expression:
	 * 
	 *   – If the expression is a parenthesized expression of the form ( Expression' ) , the
	 *     constraint reduces to ‹Expression' → T ›.
	 *     
	 *   – If the expression is a class instance creation expression or a method invocation
	 *     expression, the constraint reduces to the bound set B 3 which would be used
	 *     to determine the expression's invocation type when targeting T , as defined in
	 *     §18.5.2. (For a class instance creation expression, the corresponding "method"
	 *     used for inference is defined in §15.9.3).
	 *     This bound set may contain new inference variables, as well as dependencies
	 *     between these new variables and the inference variables in T .
	 *     
	 *   – If the expression is a conditional expression of the form e 1 ? e 2 : e 3 , the
	 *     constraint reduces to two constraint formulas, ‹ e 2 → T › and ‹ e 3 → T ›.
	 *     
	 *   – If the expression is a lambda expression or a method reference expression, the
	 *     result is specified below.
	 * @param bounds 
	 */
	void reduceExpressionCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
		if (t.isProperType()) {
			if (mb.LOOSE_INVOCATION_CONTEXT.isCompatible(e, null, null, t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		else if (isStandaloneExpr(e)) {
			IJavaType s = tEnv.getBinder().getJavaType(e);
			reduceTypeCompatibilityConstraints(bounds, s, t);
		} else {
			Operator op = JJNode.tree.getOperator(e);
			if (ParenExpression.prototype.includes(op)) {
				reduceExpressionCompatibilityConstraints(bounds, ParenExpression.getOp(e), t);
			}
			else if (NewExpression.prototype.includes(op) || MethodCall.prototype.includes(op)) {
				throw new NotImplemented(); // TODO
			}
			else if (ConditionalExpression.prototype.includes(op)) {
				reduceExpressionCompatibilityConstraints(bounds, ConditionalExpression.getIffalse(e), t);
				reduceExpressionCompatibilityConstraints(bounds, ConditionalExpression.getIftrue(e), t);
			}
			else if (LambdaExpression.prototype.includes(op)) {
				reduceLambdaCompatibilityConstraints(bounds, e, t);
			}
			else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
				reduceMethodReferenceCompatibilityConstraints(bounds, e, t);
			} else {
				throw new IllegalStateException();
			}
		}		
	}

	/**
	 * A constraint formula of the form ‹LambdaExpression → T ›, where T mentions at
	 * least one inference variable, is reduced as follows:
	 * 
	 * • If T is not a functional interface type (§9.8), the constraint reduces to false.
	 * 
	 * • Otherwise, let T' be the ground target type derived from T , as specified in
	 *   §15.27.3. If §18.5.3 is used to derive a functional interface type which is
	 *   parameterized, then the test that F<A' 1 , ..., A' m > is a subtype of F<A 1 , ..., A m > is
	 *   not performed (instead, it is asserted with a constraint formula below). Let the
	 *   target function type for the lambda expression be the function type of T' . Then:
	 *   
	 *   – If no valid function type can be found, the constraint reduces to false.
	 *   – Otherwise, the congruence of LambdaExpression with the target function type
	 *     is asserted as follows:
	 *     
	 *     › If the number of lambda parameters differs from the number of parameter
	 *       types of the function type, the constraint reduces to false.
	 *       
	 *     › If the lambda expression is implicitly typed and one or more of the function
	 *       type's parameter types is not a proper type, the constraint reduces to false.
	 *       
	 *     › If the function type's result is void and the lambda body is neither a
	 *       statement expression nor a void-compatible block, the constraint reduces to false.
	 *       
	 *     › If the function type's result is not void and the lambda body is a block that
	 *       is not value-compatible, the constraint reduces to false.
	 *       
	 *     › Otherwise, the constraint reduces to all of the following constraint formulas:
	 *     
	 *       » If the lambda parameters have explicitly declared types F 1 , ..., F n and the
	 *         function type has parameter types G 1 , ..., G n , then i) for all i (1 ≤ i ≤ n),
	 *         ‹ F i = G i ›, and ii) ‹ T' <: T ›.
	 *         
	 *       » If the function type's return type is a (non- void ) type R , assume the
	 *         lambda's parameter types are the same as the function type's parameter
	 *         types. Then:
	 *         
	 *         • If R is a proper type, and if the lambda body or some result expression
	 *           in the lambda body is not compatible in an assignment context with R ,
	 *           then false.
	 *           
	 *         • Otherwise, if R is not a proper type, then where the lambda body has the
	 *           form Expression, the constraint ‹Expression → R ›; or where the lambda
	 *           body is a block with result expressions e 1 , ..., e m , for all i (1 ≤ i ≤ m),
	 *           ‹ e i → R ›.
	 */
	void reduceLambdaCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
		throw new NotImplemented(); // TODO
	}
	
	/** 
	 * A constraint formula of the form ‹MethodReference → T ›, where T mentions at	 
	 * least one inference variable, is reduced as follows:
	 * 
	 * • If T is not a functional interface type, or if T is a functional interface type that
	 *   does not have a function type (§9.9), the constraint reduces to false.
	 *   
	 * • Otherwise, if there does not exist a potentially applicable method for the method
	 *   reference when targeting T , the constraint reduces to false.
	 *   
	 * • Otherwise, if the method reference is exact (§15.13.1), then let P 1 , ..., P n be the
	 *   parameter types of the function type of T , and let F 1 , ..., F k be the parameter
	 *   types of the potentially applicable method. The constraint reduces to a new set
	 *   of constraints, as follows:
	 * 	
	 *   – In the special case where n = k+1, the parameter of type P 1 is to act as the target
	 *     reference of the invocation. The method reference expression necessarily
	 *     has the form ReferenceType :: [TypeArguments] Identifier. The constraint
	 *     reduces to ‹ P 1 <: ReferenceType› and, for all i (2 ≤ i ≤ n), ‹ P i → F i-1 ›.
	 *     In all other cases, n = k, and the constraint reduces to, for all i (1 ≤ i ≤ n),
	 *     ‹ P i → F i ›.
	 *     
	 *   – If the function type's result is not void , let R be its return type. Then, if the result
	 *     of the potentially applicable compile-time declaration is void , the constraint
	 *     reduces to false. Otherwise, the constraint reduces to ‹ R ' → R ›, where R ' is
	 *     the result of applying capture conversion (§5.1.10) to the return type of the
	 *     potentially applicable compile-time declaration.
	 *     
	 * • Otherwise, the method reference is inexact, and:
	 *   – If one or more of the function type's parameter types is not a proper type, the
	 *     constraint reduces to false.
	 *     
	 *   – Otherwise, a search for a compile-time declaration is performed, as specified
	 *     in §15.13.1. If there is no compile-time declaration for the method reference,
	 *     the constraint reduces to false. Otherwise, there is a compile-time declaration,
	 *     and:
	 *     › If the result of the function type is void , the constraint reduces to true.
	 *     
	 *     › Otherwise, if the method reference expression elides TypeArguments, and
	 *       the compile-time declaration is a generic method, and the return type of
	 *       the compile-time declaration mentions at least one of the method's type
	 *       parameters, then the constraint reduces to the bound set B 3 which would be
	 *       used to determine the method reference's invocation type when targeting the
	 *       return type of the function type, as defined in §18.5.2. B 3 may contain new
	 *       inference variables, as well as dependencies between these new variables
	 *       and the inference variables in T .
	 *     
	 *     › Otherwise, let R be the return type of the function type, and let R ' be the result
	 *       of applying capture conversion (§5.1.10) to the return type of the invocation
	 *       type (§15.12.2.6) of the compile-time declaration. If R ' is void , the constraint
	 *       reduces to false; otherwise, the constraint reduces to ‹ R ' → R ›.
	*/
	void reduceMethodReferenceCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
		throw new NotImplemented(); // TODO
	}
	
	/**
	 * 18.2.2 Type Compatibility Constraints
	 * 
	 * A constraint formula of the form ‹ S → T › is reduced as follows:
	 * 
	 * • If S and T are proper types, the constraint reduces to true if S is compatible in a
	 *   loose invocation context with T (§5.3), and false otherwise.
	 * 
	 * • Otherwise, if S is a primitive type, let S' be the result of applying boxing
	 *   conversion (§5.1.7) to S . Then the constraint reduces to ‹ S' → T ›.
	 * 
	 * • Otherwise, if T is a primitive type, let T' be the result of applying boxing
	 *   conversion (§5.1.7) to T . Then the constraint reduces to ‹ S = T' ›.
	 * 
	 * • Otherwise, if T is a parameterized type of the form G<T 1 , ..., T n > , and there exists
	 *   no type of the form G< ... > that is a supertype of S , but the raw type G is a supertype
	 *   of S , then the constraint reduces to true.
	 * 
	 * • Otherwise, if T is an array type of the form G<T 1 , ..., T n >[] k , and there exists no
	 *   type of the form G< ... >[] k that is a supertype of S , but the raw type G[] k is a
	 *   supertype of S , then the constraint reduces to true. (The notation [] k indicates
	 *   an array type of k dimensions.)
	 *   
	 * • Otherwise, the constraint reduces to ‹ S <: T ›.
	 *   The fourth and fifth cases are implicit uses of unchecked conversion (§5.1.9).
	 *   These, along with any use of unchecked conversion in the first case, may result in
	 *   compile-time unchecked warnings, and may influence a method's invocation type
	 *   (§15.12.2.6).
	 */
	void reduceTypeCompatibilityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		// Case 1
		if (s.isProperType() && t.isProperType()) {
			if (mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, s, null, t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		else if (s instanceof IJavaPrimitiveType) {
			reduceTypeCompatibilityConstraints(bounds, box(s), t);
		}
		else if (t instanceof IJavaPrimitiveType) {
			reduceTypeCompatibilityConstraints(bounds, s, box(t));
		}
		else if (hasRawSuperTypeOf(s, t)) {
			bounds.addTrue();
		}
		else if (t instanceof IJavaArrayType && s instanceof IJavaArrayType) {
			IJavaArrayType ta = (IJavaArrayType) t;
			IJavaArrayType sa = (IJavaArrayType) s;
			if (ta.getDimensions() == sa.getDimensions() && hasRawSuperTypeOf(sa.getBaseType(), ta.getBaseType())) {
				bounds.addTrue();
			} else {
				reduceSubtypingConstraints(bounds, s, t);
			}
		} else {
			reduceSubtypingConstraints(bounds, s, t);
		}
	}

	/**
	 * 18.2.3 Subtyping Constraints
	 * 
	 * A constraint formula of the form ‹ S <: T › is reduced as follows:
	 * • If S and T are proper types, the constraint reduces to true if S is a subtype of T
	 *   (§4.10), and false otherwise.
	 * • Otherwise, if S is the null type, the constraint reduces to true.
	 * • Otherwise, if T is the null type, the constraint reduces to false.
	 * • Otherwise, if S is an inference variable, α, the constraint reduces to the bound α <: T .
	 * • Otherwise, if T is an inference variable, α, the constraint reduces to the bound S <: α.
	 * • Otherwise, the constraint is reduced according to the form of T :
	 * 
	 *   – If T is a parameterized class or interface type, or an inner class type of a
	 *     parameterized class or interface type (directly or indirectly), let A 1 , ..., A n be
	 *     the type arguments of T . Among the supertypes of S , a corresponding class
	 *     or interface type is identified, with type arguments B 1 , ..., B n . If no such type
	 *     exists, the constraint reduces to false. Otherwise, the constraint reduces to the
	 *     following new constraints: for all i (1 ≤ i ≤ n), ‹ B i <= A i ›.
	 *     
	 *   – If T is any other class or interface type, then the constraint reduces to true if T
	 *     is among the supertypes of S , and false otherwise.
	 *     
	 *   – If T is an array type, T'[] , then among the supertypes of S that are array types,
	 *     a most specific type is identified, S'[] (this may be S itself). If no such array
	 *     type exists, the constraint reduces to false. Otherwise:
	 *     
	 *     › If neither S' nor T' is a primitive type, the constraint reduces to ‹ S' <: T' ›.
	 *     › Otherwise, the constraint reduces to true if S' and T' are the same primitive
	 *       type, and false otherwise.
	 *     
	 *   – If T is a type variable, there are three cases:
	 *     › If S is an intersection type of which T is an element, the constraint reduces to true.
	 *     › Otherwise, if T has a lower bound, B , the constraint reduces to ‹ S <: B ›.
	 *     › Otherwise, the constraint reduces to false.
	 *     
	 *   – If T is an intersection type, I 1 & ... & I n , the constraint reduces to the following
	 *     new constraints: for all i (1 ≤ i ≤ n), ‹ S <: I i ›.
	 */
	private void reduceSubtypingConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (s.isProperType() && t.isProperType()) {
			if (tEnv.isSubType(s, t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		else if (s instanceof IJavaNullType) {
			bounds.addTrue();
		}
		else if (t instanceof IJavaNullType) {
			bounds.addFalse();
		}
		else if (isInferenceVariable(s) || isInferenceVariable(t)) {
			bounds.addSubtypeBound(s, t);
		}
		
		if (t instanceof IJavaDeclaredType) {
			// TODO subcase 1
			if (tEnv.isSubType(s, t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		else if (t instanceof IJavaArrayType) {
			throw new NotImplemented(); // TODO
		}
		else if (t instanceof IJavaTypeVariable) {
			IJavaTypeVariable tv = (IJavaTypeVariable) t;
			// TODO intersection
			if (tv.getLowerBound() != null) {
				reduceSubtypingConstraints(bounds, s, tv.getLowerBound());
			} else {
				bounds.addFalse();
			}
		}
		else if (t instanceof IJavaIntersectionType) {
			IJavaIntersectionType it = (IJavaIntersectionType) t;
			reduceSubtypingConstraints(bounds, s, it.getPrimarySupertype());
			reduceSubtypingConstraints(bounds, s, it.getSecondarySupertype());
		} else {
			throw new NotImplemented(); // TODO
		}
	}
	
	/**
	 * A constraint formula of the form ‹ S <= T ›, where S and T are type arguments
	 * (§4.5.1), is reduced as follows:
	 * • If T is a type:
	 *   – If S is a type, the constraint reduces to ‹ S = T ›.
	 *   – If S is a wildcard, the constraint reduces to false.
	 *   
	 * • If T is a wildcard of the form ? , the constraint reduces to true.
	 * 
	 * • If T is a wildcard of the form ? extends T' :
	 *   – If S is a type, the constraint reduces to ‹ S <: T' ›.
	 *   – If S is a wildcard of the form ? , the constraint reduces to ‹ Object <: T' ›.
	 *   – If S is a wildcard of the form ? extends S' , the constraint reduces to ‹ S' <: T' ›.
	 *   – If S is a wildcard of the form ? super S' , the constraint reduces to ‹ Object = T' ›.
	 *   
	 * • If T is a wildcard of the form ? super T' :
	 *   – If S is a type, the constraint reduces to ‹ T' <: S ›.
	 *   – If S is a wildcard of the form ? super S' , the constraint reduces to ‹ T' <: S' ›.
	 *   – Otherwise, the constraint reduces to false.
	 */
	private void reduceTypeArgContainmentConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (t instanceof IJavaWildcardType) {
			IJavaWildcardType wt = (IJavaWildcardType) t;
			if (wt.getUpperBound() != null) { 
				// Case 3: ? extends X
				if (s instanceof IJavaWildcardType) {
					IJavaWildcardType ws = (IJavaWildcardType) s;
					if (ws.getUpperBound() != null) {
						bounds.addSubtypeBound(ws.getUpperBound(), wt.getUpperBound());
					}
					else if (ws.getLowerBound() != null) {
						bounds.addEqualityBound(tEnv.getObjectType(), wt.getUpperBound());
					} else {
						bounds.addSubtypeBound(tEnv.getObjectType(), wt.getUpperBound());
					}
				} else {
					bounds.addSubtypeBound(s, wt.getUpperBound());
				}
			}
			else if (wt.getLowerBound() != null) { 
				// Case 4: ? super X
				if (s instanceof IJavaWildcardType) {
					IJavaWildcardType ws = (IJavaWildcardType) s;
					if (ws.getLowerBound() != null) {
						bounds.addSubtypeBound(wt.getLowerBound(), ws.getLowerBound());
					} else {
						bounds.addFalse();
					}
				} else {
					bounds.addSubtypeBound(wt.getLowerBound(), s);
					
				}
			}
			// Case 2: ?
 			bounds.addTrue();
		}
		// Case 1
		else if (s instanceof IJavaWildcardType) {
			bounds.addFalse();
		} 
		else {
			bounds.addEqualityBound(s, t);
		}
	}
	
	/**
	 * 18.2.4 Type Equality Constraints
	 * 
	 * A constraint formula of the form ‹ S = T ›, where S and T are types, is reduced as
	 * follows:
	 * 
	 * • If S and T are proper types, the constraint reduces to true if S is the same as T
	 *   (§4.3.4), and false otherwise.
	 *   
	 * • Otherwise, if S is an inference variable, α, the constraint reduces to the bound α = T .
	 * • Otherwise, if T is an inference variable, α, the constraint reduces to the bound S = α.
	 * 
	 * • Otherwise, if S and T are class or interface types with the same erasure, where S
	 *   has type arguments B 1 , ..., B n and T has type arguments A 1 , ..., A n , the constraint
	 *   reduces to the following new constraints: for all i (1 ≤ i ≤ n), ‹ B i = A i ›.
	 *   
	 * • Otherwise, if S and T are array types, S'[] and T'[] , the constraint reduces to ‹ S' = T' ›.
	 * • Otherwise, the constraint reduces to false.
	 */
	private void reduceTypeEqualityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (s.isProperType() && t.isProperType()) {
			if (s.equals(t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		if (isInferenceVariable(s) || isInferenceVariable(t)) {
			bounds.addEqualityBound(s, t);
		}
		if (s instanceof IJavaDeclaredType && t instanceof IJavaDeclaredType) {
			IJavaDeclaredType sd = (IJavaDeclaredType) s;
			IJavaDeclaredType td = (IJavaDeclaredType) t;			
			if (sd.getDeclaration().equals(td.getDeclaration()) && sd.getTypeParameters().size() == td.getTypeParameters().size()) {
				int i=0;
				for(IJavaType sp : sd.getTypeParameters()) {
					IJavaType tp = td.getTypeParameters().get(i);
					reduceTypeArgumentEqualityConstraints(bounds, sp, tp);
					i++;
				}
			} else {
				bounds.addFalse(); // TODO
			}
		}
		if (s instanceof IJavaArrayType && t instanceof IJavaArrayType) {
			IJavaArrayType sa = (IJavaArrayType) s;
			IJavaArrayType ta = (IJavaArrayType) t;
			reduceTypeEqualityConstraints(bounds, sa.getElementType(), ta.getElementType());
		}
		else {
			bounds.addFalse();
		}
	}
	
	/**
	 * A constraint formula of the form ‹ S = T ›, where S and T are type arguments (§4.5.1),
	 * is reduced as follows:
	 * 
	 * • If S and T are types, the constraint is reduced as described above.
	 * • If S has the form ? and T has the form ? , the constraint reduces to true.
	 * • If S has the form ? and T has the form ? extends T' , the constraint reduces to ‹ Object = T' ›.
	 * • If S has the form ? extends S' and T has the form ? , the constraint reduces to ‹ S' = Object ›.
	 * 
	 * • If S has the form ? extends S' and T has the form ? extends T' , the constraint
	 *   reduces to ‹ S' = T' ›.
	 *   
	 * • If S has the form ? super S' and T has the form ? super T' , the constraint reduces
	 *   to ‹ S' = T' ›.
	 *   
	 * • Otherwise, the constraint reduces to false.
	 * @param bounds 
	 */
	private void reduceTypeArgumentEqualityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		// TODO case 1
		if (s instanceof IJavaWildcardType && s instanceof IJavaWildcardType) {
			IJavaWildcardType sw = (IJavaWildcardType) s;
			IJavaWildcardType tw = (IJavaWildcardType) t;
			if (sw.getUpperBound() != null) {
				if (tw.getUpperBound() != null) {
					// Case 5
					reduceTypeEqualityConstraints(bounds, sw.getUpperBound(), tw.getUpperBound());
				} 
				else if (tw.getLowerBound() == null) {
					// Case 4 
					reduceTypeEqualityConstraints(bounds, sw.getUpperBound(), tEnv.getObjectType());
				} else {
					bounds.addFalse();
				}
			} 
			else if (sw.getLowerBound() != null) {
				// case 6
				if (tw.getLowerBound() != null) {
					reduceTypeEqualityConstraints(bounds, sw.getLowerBound(), tw.getLowerBound());
				} else {
					bounds.addFalse();
				}
			} else {
				if (tw.getUpperBound() != null) {
					// Case 3
					reduceTypeEqualityConstraints(bounds, tEnv.getObjectType(), tw.getUpperBound());
				}
				else if (tw.getLowerBound() == null) {									
					// Case 2
					bounds.addTrue();
				} else {
					bounds.addFalse();
				}				
			}
		} else {
			bounds.addFalse();
		}
	}

	/**
	 * 18.2.5 Checked Exception Constraints
	 * 
	 * A constraint formula of the form ‹LambdaExpression → throws T › is reduced as
	 * follows:
	 * 
	 * • If T is not a functional interface type (§9.8), the constraint reduces to false.
	 * 
	 * • Otherwise, let the target function type for the lambda expression be determined
	 *   as specified in §15.27.3. If no valid function type can be found, the constraint
	 *   reduces to false.
	 *   
	 * • Otherwise, if the lambda expression is implicitly typed, and one or more of the
	 *   function type's parameter types is not a proper type, the constraint reduces to
	 *   false.
	 *   
	 * • Otherwise, if the function type's return type is neither void nor a proper type,
	 *   the constraint reduces to false.
	 * 
	 * • Otherwise, let E 1 , ..., E n be the types in the function type's throws clause that are
	 *   not proper types. If the lambda expression is implicitly typed, let its parameter
	 *   types be the function type's parameter types. If the lambda body is a poly
	 *   expression or a block containing a poly result expression, let the targeted return
	 *   type be the function type's return type. Let X 1 , ..., X m be the checked exception
	 *   types that the lambda body can throw (§11.2). Then there are two cases:
	 *   
	 *   – If n = 0 (the function type's throws clause consists only of proper types), then
	 *     if there exists some i (1 ≤ i ≤ m) such that X i is not a subtype of any proper type
	 *     in the throws clause, the constraint reduces to false; otherwise, the constraint
	 *     reduces to true.
	 *     
	 *   – If n > 0 , the constraint reduces to a set of subtyping constraints: for all i (1 ≤
	 *     i ≤ m), if X i is not a subtype of any proper type in the throws clause, then the
	 *     constraints include, for all j (1 ≤ j ≤ n), ‹ X i <: E j ›. In addition, for all j (1 ≤ j
	 *     ≤ n), the constraint reduces to the bound throws E j .
	 */
	private void reduceLambdaCheckedExceptionConstraints(BoundSet bounds, IRNode lambda, IJavaType t) {
		if (!isFunctionalInterfaceType(t)) {
			bounds.addFalse();
			return;
		}
		TypeUtils utils = new TypeUtils(tEnv);
    	IJavaType targetType = utils.getPolyExpressionTargetType(lambda);
    	IJavaFunctionType targetFuncType = tEnv.isFunctionalType(targetType);
    	if (targetFuncType == null) {
    		bounds.addFalse();
    		return;
    	}
    	if (MethodBinder8.isImplicitlyTypedLambda(lambda)) {
    		for(IJavaType pt : targetFuncType.getParameterTypes()) {
    			if (!pt.isProperType()) {
    				bounds.addFalse();
    	    		return;
    			}
    		}
    	}
    	if (!(targetFuncType.getReturnType() instanceof IJavaVoidType) || 
    		!targetFuncType.getReturnType().isProperType()) {
			bounds.addFalse();
    		return;
    	}
    	final List<IJavaType> improperThrows = new ArrayList<IJavaType>();
    	for(IJavaType ex : targetFuncType.getExceptions()) {
    		if (!ex.isProperType()) {
    			improperThrows.add(ex);
    		}
    	}
		throw new NotImplemented(); // TODO
	}

	/**
	 * A constraint formula of the form ‹MethodReference → throws T › is reduced as
	 * follows:
	 * 
	 * • If T is not a functional interface type, or if T is a functional interface type but
	 *   does not have a function type (§9.9), the constraint reduces to false.
	 * 
	 * • Otherwise, let the target function type for the method reference expression be
	 *   the function type of T . If the method reference is inexact (§15.13.1) and one or
	 *   more of the function type's parameter types is not a proper type, the constraint
	 *   reduces to false.
	 *   
	 * • Otherwise, if the method reference is inexact and the function type's result is
	 *   neither void nor a proper type, the constraint reduces to false.
	 * 
	 * • Otherwise, let E 1 , ..., E n be the types in the function type's throws clause that
	 *   are not proper types. Let X 1 , ..., X m be the checked exceptions in the throws
	 *   clause of the invocation type of the method reference's compile-time declaration
	 *   (§15.13.2) (as derived from the function type's parameter types and return type).
	 *   Then there are two cases:
	 *   
	 *   – If n = 0 (the function type's throws clause consists only of proper types), then
	 *     if there exists some i (1 ≤ i ≤ m) such that X i is not a subtype of any proper type
	 *     in the throws clause, the constraint reduces to false; otherwise, the constraint
	 *     reduces to true.
	 *     
	 *   – If n > 0 , the constraint reduces to a set of subtyping constraints: for all i (1 ≤
	 *     i ≤ m), if X i is not a subtype of any proper type in the throws clause, then the
	 *     constraints include, for all j (1 ≤ j ≤ n), ‹ X i <: E j ›. In addition, for all j (1 ≤ j
	 *     ≤ n), the constraint reduces to the bound throws E j .
	 */
	private void reduceMethodRefCheckedExceptionConstraints(BoundSet bounds, IRNode ref, IJavaType t) {
		IJavaFunctionType targetFuncType = tEnv.isFunctionalType(t);
		if (targetFuncType == null) {
			bounds.addFalse();
			return;
		}
		if (!mb.isExactMethodReference(ref)) {
			for(IJavaType pt : targetFuncType.getParameterTypes()) {
    			if (!pt.isProperType()) {
    				bounds.addFalse();
    	    		return;
    			}
    		}
		}
		throw new NotImplemented(); // TODO
	}
}
