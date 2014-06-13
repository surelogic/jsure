package edu.cmu.cs.fluid.java.bind;

import static edu.cmu.cs.fluid.java.bind.IMethodBinder.*;

import java.util.*;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class TypeInference8 {
	final MethodBinder8 mb;
	final ITypeEnvironment tEnv;

	TypeInference8(MethodBinder8 b) {
		mb = b;
		tEnv = mb.tEnv;
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
		BoundSet b_0 = BoundSet.constructInitialSet(m.typeFormals);
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
	static class Bound {
		// TODO
	}
	
	/**
	 * An important intermediate result of inference is a bound set. It is sometimes
	 * convenient to refer to an empty bound set with the symbol true; this is merely out
	 * of convenience, and the two are interchangeable
	 * 
	 * When inference begins, a bound set is typically generated from a list of type
	 * parameter declarations P 1 , ..., P p and associated inference variables α 1 , ..., α p . Such
	 * a bound set is constructed as follows. For each l (1 ≤ l ≤ p):
	 * 
	 * • If P l has no TypeBound, the bound α l <: Object appears in the set.
	 * 
	 * • Otherwise, for each type T delimited by & in the TypeBound, the bound α l <:
	 * 
	 * T[P 1 :=α 1 , ..., P p :=α p ] appears in the set; if this results in no proper upper bounds
	 * for α l (only dependencies), then the bound α l <: Object also appears in the set.
	 */
	static class BoundSet {
		private boolean isFalse = false;
		
		void addFalse() {
			isFalse = true;
		}

		void addTrue() {
			// TODO what is there to do?
		}
		
		static BoundSet constructInitialSet(IRNode typeFormals) {
			throw new NotImplemented(); // TODO
		}

		void addEqualityBound(IJavaType s, IJavaType t) {
			throw new NotImplemented(); // TODO
		}

		void addSubtypeBound(IJavaType s, IJavaType t) {
			throw new NotImplemented(); // TODO
		}		
	}
	
	
	private boolean isStandaloneExpr(IRNode e) {
		throw new NotImplemented(); // TODO
	}
	
	boolean isProperType(IJavaType t) {
		throw new NotImplemented(); // TODO
	}
 	
	boolean isInferenceVariable(IJavaType t) {
		throw new NotImplemented(); // TODO
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
		if (isProperType(t)) {
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
		if (isProperType(s) && isProperType(t)) {
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
		if (isProperType(s) && isProperType(t)) {
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
		if (isProperType(s) && isProperType(t)) {
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
    			if (!isProperType(pt)) {
    				bounds.addFalse();
    	    		return;
    			}
    		}
    	}
    	if (!(targetFuncType.getReturnType() instanceof IJavaVoidType) || 
    		!isProperType(targetFuncType.getReturnType())) {
			bounds.addFalse();
    		return;
    	}
    	final List<IJavaType> improperThrows = new ArrayList<IJavaType>();
    	for(IJavaType ex : targetFuncType.getExceptions()) {
    		if (!isProperType(ex)) {
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
    			if (!isProperType(pt)) {
    				bounds.addFalse();
    	    		return;
    			}
    		}
		}
		throw new NotImplemented(); // TODO
	}
}
