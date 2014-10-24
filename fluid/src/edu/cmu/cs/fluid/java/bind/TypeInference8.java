package edu.cmu.cs.fluid.java.bind;

import static edu.cmu.cs.fluid.java.bind.IMethodBinder.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.ast.java.operator.ITypeFormalNode;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.CallState;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.MethodBinding;
import edu.cmu.cs.fluid.java.bind.MethodBinder8.MethodState;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.operator.CallInterface.NoArgs;
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
	static class InferenceVariable extends JavaReferenceType implements IJavaTypeFormal {
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

		@Override
		public String toString() {
			String rv = super.toString();
			if (formal == null) {
				int last = rv.lastIndexOf('$');
				return rv.substring(last+1);
			}
			int last = rv.lastIndexOf('@');			
			return JavaNames.getRelativeTypeName(formal)+rv.substring(last);
		}
		
		public IJavaReferenceType getLowerBound() {
			return null;
		}
		public IJavaReferenceType getUpperBound(ITypeEnvironment te) {
			return null;
		}
		public ITypeFormalNode getNode() {
			throw new UnsupportedOperationException();
		}
		public IRNode getDeclaration() {
			return null;
		}
		public IJavaReferenceType getExtendsBound(ITypeEnvironment te) {
			return null;
		}
		public IJavaReferenceType getExtendsBound() {
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
	 * - Where P 1 , ..., P p (p >= 1) are the type parameters of m , let α 1 , ..., α p be inference
	 *   variables, and let θ be the substitution [P 1 :=α 1 , ..., P p :=α p ] .
	 * 
	 * - An initial bound set, B 0 , is constructed from the declared bounds of P 1 , ..., P p , as
	 *   described in Â§18.1.3.
	 *   
	 *   ... see below
	 *   
	 * @return the bound set B 2 if the method is applicable
	 */   
	BoundSet inferForInvocationApplicability(CallState call, MethodBinding m, InvocationKind kind) {
		/*
		if ("test.TestMethodOverloading.foo(T,T2)".equals(m.toString())) {
			System.out.println("Found foo(T,T2)");
		}
		*/
		final BoundSet b_0 = constructInitialSet(m.typeFormals);
		/*
		 *  check if type params appear in throws clause						
		 *  
	     * - For all i (1 <= i <= p), if P i appears in the throws clause of m , then the bound throws
	     *   α i is implied. These bounds, if any, are incorporated with B 0 to produce a new
	     *   bound set, B 1 .
		 */		
		BoundSet b_1 = null;
		for(IJavaType thrown : m.getThrownExceptions(tEnv.getBinder())) {
			if (b_0.variableMap.keySet().contains(thrown)) {
				if (b_1 == null) {
					b_1 = new BoundSet(b_0);
				}
				b_1.addThrown((InferenceVariable) thrown);				 
			}
		}
		if (b_1 == null) {
			b_1 = b_0;
		}
		
		/*
		 * - A set of constraint formulas, C , is constructed as follows.
		 * 
		 *   Let F 1 , ..., F n be the formal parameter types of m , and let e 1 , ..., e k be the actual
		 *   argument expressions of the invocation. Then:
		 *   
		 *   - To test for applicability by strict invocation:
		 *   
		 *     If k !=  n, or if there exists an i (1 <= i <= n) such that e i is pertinent to applicability
		 *     (Â§15.12.2.2) and either i) e i is a standalone expression of a primitive type but
		 *     F i is a reference type, or ii) F i is a primitive type but e i is not a standalone
		 *     expression of a primitive type; then the method is not applicable and there is
		 *     no need to proceed with inference.
		 *     
		 *     Otherwise, C includes, for all i (1 <= i <= k) where e i is pertinent to applicability,
		 *     < e i -> F i θ>.
		 *     
		 *   - To test for applicability by loose invocation: 
		 *   
		 *     If k !=  n, the method is not applicable and there is no need to proceed with inference.
		 *     Otherwise, C includes, for all i (1 <= i <= k) where e i is pertinent to applicability,
		 *     < e i -> F i θ>.
		 *   
		 *   - To test for applicability by variable arity invocation:
		 *   
		 *     Let F' 1 , ..., F' k be the first k variable arity parameter types of m (Â§15.12.2.4). C
		 *     includes, for all i (1 <= i <= k) where e i is pertinent to applicability, < e i -> F' i θ>.
		 */		 
		if (kind != InvocationKind.VARARGS && m.numFormals != call.args.length) {
			return null;
		}		
		final BoundSet b_2 = new BoundSet(b_1);
		final IJavaTypeSubstitution theta = b_2.getInitialVarSubst();
		final IJavaType[] formalTypes = m.getParamTypes(tEnv.getBinder(), call.args.length, kind == InvocationKind.VARARGS);
		for(int i=0; i<call.args.length; i++) {
			final IRNode e_i = call.args[i];
			if (mb.isPertinentToApplicability(m, call.getNumTypeArgs() > 0, e_i)) {
				if (kind == InvocationKind.STRICT) {
					final boolean isPoly = mb.isPolyExpression(e_i);
					final IJavaType e_i_Type = tEnv.getBinder().getJavaType(e_i);
					if (!isPoly && e_i_Type instanceof IJavaPrimitiveType &&
							formalTypes[i] instanceof IJavaReferenceType) {
						return null;
					}
					if (formalTypes[i] instanceof IJavaPrimitiveType && 
							(isPoly || e_i_Type instanceof IJavaReferenceType)) {
						return null;
					}
				}
				reduceConstraintFormula(b_2, new ConstraintFormula(e_i, FormulaConstraint.IS_COMPATIBLE, formalTypes[i].subst(theta)));
			}
		}
		/*   - C is reduced (Â§18.2) and the resulting bounds are incorporated with B 1 to produce
		 *     a new bound set, B 2 .
		 *     
		 *     Finally, the method m is applicable if B 2 does not contain the bound false and
		 *     resolution of all the inference variables in B 2 succeeds (Â§18.4).
		 */
		final BoundSet result = resolve(b_2);
		// debug
		if (result == null || result.instantiations.isEmpty()) {
			resolve(b_2);
		}
		if (result != null && !result.isFalse && 
			result.instantiations.keySet().containsAll(result.variableMap.values())) {
			return b_2;
		}
		return null;
	}
	
	/**
	 * 18.5.2 Invocation Type Inference
	 * 
     * Given a method invocation that provides no explicit type arguments, and a
     * corresponding most specific applicable generic method m , the process to infer the
     * invocation type (Â§15.12.2.6) of the chosen method is as follows:
     * 
     * - Let θ be the substitution [P 1 :=α 1 , ..., P p :=α p ] defined in Â§18.5.1 to replace the
     *   type parameters of m with inference variables.
     * - Let B 2 be the bound set produced by reduction in order to demonstrate that m is
     *   applicable in Â§18.5.1. (While it was necessary in Â§18.5.1 to demonstrate that the
     *   inference variables in B 2 could be resolved, in order to establish applicability, the
     *   instantiations produced by this resolution step are not considered part of B 2 .)
     *   
     * ... see computeB_3
     * ... 
     * - A set of constraint formulas, C , is constructed as follows ...
     * 
     * - While C is not empty, the following process is repeated, starting with the bound
     *   set B 3 and accumulating new bounds into a "current" bound set, ultimately
     *   producing a new bound set, B 4 :
     *   
     *   1. A subset of constraints is selected in C , satisfying the property that, for each
     *      constraint, no input variable depends on the resolution (Â§18.4) of an output
     *      variable of another constraint in C . (input variable and output variable are
     *      defined below.)
     *      
     *      If this subset is empty, then there is a cycle (or cycles) in the graph of
     *      dependencies between constraints. In this case, all constraints are considered
     *      that participate in a dependency cycle (or cycles) and do not depend on any
     *      constraints outside of the cycle (or cycles). A single constraint is selected
     *      from the considered constraints, as follows:
     *      
     *      - If any of the considered constraints have the form <Expression -> T >,
     *        then the selected constraint is the considered constraint of this form that
     *        contains the expression to the left (Â§3.5) of the expression of every other
     *        considered constraint of this form.
     * 
     *      - If no considered constraint has the form <Expression -> T >, then the
     *        selected constraint is the considered constraint that contains the expression
     *        to the left of the expression of every other considered constraint.
     * 
     *   2. The selected constraint(s) are removed from C .
     *   
     *   3. The input variables α 1 , ..., α m of all the selected constraint(s) are resolved.
     *   
     *   4. Where T 1 , ..., T m are the instantiations of α 1 , ..., α m , the substitution
     *      [ α 1 := T 1 , ..., α m := T m ] is applied to every constraint.
     *      
     *   5. The constraint(s) resulting from substitution are reduced and incorporated
     *      with the current bound set.
     * 
     * - Finally, if B 4 does not contain the bound false, the inference variables in B 4 are resolved.
     * 
     *   If resolution succeeds with instantiations T 1 , ..., T p for inference variables α 1 , ...,
     *   α p , let θ' be the substitution [P 1 := T 1 , ..., P p := T p ] . Then:
     *   
     *   - If unchecked conversion was necessary for the method to be applicable during
     *     constraint set reduction in Â§18.5.1, then the parameter types of the invocation
     *     type of m are obtained by applying θ' to the parameter types of m 's type, and
     *     the return type and thrown types of the invocation type of m are given by the
     *     erasure of the return type and thrown types of m 's type.
     *     
     *   - If unchecked conversion was not necessary for the method to be applicable,
     *     then the invocation type of m is obtained by applying θ' to the type of m .
     * 
     *   If B 4 contains the bound false, or if resolution fails, then a compile-time error occurs.
*/
	BoundSet inferForInvocationType(CallState call, MethodBinding m, BoundSet b_2) {
		final BoundSet b_3 = computeB_3(call, m, b_2);
		final IJavaTypeSubstitution theta = b_3.getInitialVarSubst();
		Set<ConstraintFormula> c = createInitialConstraints(call, m, theta);
		computeInputOutput(null, null); // TODO
		
		BoundSet rv = b_3; // TODO
		return rv;
	}

	/**
	 * From 18.5.2 Invocation Type Inference
	 */
	private BoundSet computeB_3(CallState call, MethodBinding m, BoundSet b_2) {
		 /* 
	     * - If the invocation is not a poly expression, let the bound set B 3 be the same as B 2 .
	     */
		if (!mb.isPolyExpression(call.call)) {
			return b_2;
		}

		final IJavaType t = utils.getPolyExpressionTargetType(call.call);
		return computeB_3(call, m, b_2, t);
	}
	
	/**
	 * @param t the target type
	 */
	private BoundSet computeB_3(CallState call, MethodBinding m, BoundSet b_2, IJavaType t) {
	    /*   If the invocation is a poly expression, let the bound set B 3 be derived from B 2
	     *   as follows. Let R be the return type of m , let T be the invocation's target type,
	     *   and then:
	     *   
	     *   - If unchecked conversion was necessary for the method to be applicable during
	     *     constraint set reduction in Â§18.5.1, the constraint formula <| R | -> T > is reduced
	     *     and incorporated with B 2 .
	     */
		final IJavaType r = m.getReturnType(tEnv);
		final BoundSet b_3 = new BoundSet(b_2);
		final IJavaTypeSubstitution theta = b_3.getInitialVarSubst();
		if (b_3.usedUncheckedConversion) {
			reduceConstraintFormula(b_3, new ConstraintFormula(tEnv.computeErasure(r), FormulaConstraint.IS_COMPATIBLE, t));
			return b_3;
		}
	    /*   - Otherwise, if R θ is a parameterized type, G<A 1 , ..., A n > , and one of A 1 , ..., A n is
	     *     a wildcard, then, for fresh inference variables Î² 1 , ..., Î² n , the constraint formula
	     *     < G< Î² 1 , ..., Î² n > -> T > is reduced and incorporated, along with the bound 
	     *     G< Î² 1 , ..., Î² n > = capture( G<A 1 , ..., A n > ), with B 2 .
	     */
		final IJavaType r_subst = r.subst(theta); 
		final IJavaDeclaredType g = isWildcardParameterizedType(r_subst);
		if (g != null) {
			final int n = g.getTypeParameters().size();
			final List<IJavaType> newVars = new ArrayList<IJavaType>(n);
			for(int i=0; i<n; i++) {
				newVars.add(new InferenceVariable(null)); // TODO 
			}
			// TODO subst?
			final IJavaType g_beta = JavaTypeFactory.getDeclaredType(g.getDeclaration(), newVars, g.getOuterType());
			reduceConstraintFormula(b_3, new ConstraintFormula(g_beta, FormulaConstraint.IS_COMPATIBLE, t));
			b_3.addCaptureBound(g_beta, g);
			b_3.addInferenceVariables(null); // TODO
			return b_3;
		}
	    /*   - Otherwise, if R θ is an inference variable α, and one of the following is true:
	     *     
	     *     ... see below
	     *       
	     *     then α is resolved in B 2 , and where the capture of the resulting instantiation of
	     *     α is U , the constraint formula < U -> T > is reduced and incorporated with B 2 .
	     */
		if (r_subst instanceof InferenceVariable) {
			final InferenceVariable alpha = (InferenceVariable) r_subst;			
			final BoundSubset bounds = b_3.findAssociatedBounds(alpha);
			final IJavaDeclaredType g2;
			BoundCondition cond = null;
			
			if (t instanceof IJavaReferenceType && isWildcardParameterizedType(t) == null) {
				/*
			     *     > T is a reference type, but is not a wildcard-parameterized type, and either
			     *       i) B 2 contains a bound of one of the forms α = S or S <: α, where S is a
			     *       wildcard-parameterized type, or ii) B 2 contains two bounds of the forms S 1
			     *       <: α and S 2 <: α, where S 1 and S 2 have supertypes that are two different
			     *       parameterizations of the same generic class or interface.     
			     */
				cond = new BoundCondition() {
					public boolean examineEquality(IJavaType s, IJavaType t) {
						return isWildcardParameterizedType(s) != null || isWildcardParameterizedType(t) != null;
					}
					public boolean examineLowerBound(IJavaType t) {
						return false;
					}
					public boolean examineUpperBound(IJavaType t) {
						return isWildcardParameterizedType(t) != null;
						// TODO check for diff parameterizations?
					}					
				};				
			}
			else if ((g2 = isParameterizedType(t)) != null) {
				/*       
			     *     > T is a parameterization of a generic class or interface, G , and B 2 contains a
			     *       bound of one of the forms α = S or S <: α, where there exists no type of the
			     *       form G< ... > that is a supertype of S , but the raw type | G< ... > | is a supertype of S .
			     */				
				cond = new BoundCondition() {
					public boolean examineEquality(IJavaType s, IJavaType t) {
						return (s != alpha && onlyHasRawG_asSuperType(g2.getDeclaration(), s) == Boolean.TRUE) || 
							   (t != alpha && onlyHasRawG_asSuperType(g2.getDeclaration(), t) == Boolean.TRUE);
					}
					public boolean examineLowerBound(IJavaType t) {
						return false;
					}
					public boolean examineUpperBound(IJavaType t) {
						return onlyHasRawG_asSuperType(g2.getDeclaration(), t) == Boolean.TRUE;
					}				
				};	
			}
			else if (t instanceof IJavaPrimitiveType) {
				/*
				 *     > T is a primitive type, and one of the primitive wrapper classes mentioned in				 
			     *       Â§5.1.7 is an instantiation, upper bound, or lower bound for α in B 2 .
			     */
				final IJavaPrimitiveType pt = (IJavaPrimitiveType) t;
				final IJavaDeclaredType wrapper = JavaTypeFactory.getCorrespondingDeclType(tEnv, pt);				
				cond = new BoundCondition() {
					public boolean examineEquality(IJavaType s, IJavaType t) {
						return s == wrapper || t == wrapper;
					}
					public boolean examineLowerBound(IJavaType t) {
						return t == wrapper;
					}
					public boolean examineUpperBound(IJavaType t) {
						return t == wrapper;
					}					
				};
			}
			if (cond != null && bounds.examine(cond)) {
				IJavaType u = null;// TODO
				reduceConstraintFormula(b_3, new ConstraintFormula(u, FormulaConstraint.IS_COMPATIBLE, t));
				return b_3;
			}
		}
		/*
	     *   - Otherwise, the constraint formula < R θ -> T > is reduced and incorporated with B 2 .	 
		 */
		reduceConstraintFormula(b_3, new ConstraintFormula(r_subst, FormulaConstraint.IS_COMPATIBLE, t));
		return b_3;
	}

	/**
	 * From 18.5.2 Invocation Type Inference:
	 * 
     * Let e 1 , ..., e k be the actual argument expressions of the invocation. If m is
     * applicable by strict or loose invocation, let F 1 , ..., F k be the formal parameter
     * types of m ; if m is applicable by variable arity invocation, let F 1 , ..., F k the first k
     * variable arity parameter types of m (Â§15.12.2.4). Then:
     * 
     * - For all i (1 <= i <= k), if e i is not pertinent to applicability, C contains < e i -> F i θ>.
     */
	private Set<ConstraintFormula> createInitialConstraints(CallState call, MethodBinding m, IJavaTypeSubstitution theta) {
		final Set<ConstraintFormula> rv = new HashSet<ConstraintFormula>();
		final IJavaType[] formalTypes = m.getParamTypes(tEnv.getBinder(), call.args.length, false/*kind == InvocationKind.VARARGS*/);
		for(int i=0; i<call.args.length; i++) {
			final IRNode e_i = call.args[i];
			final IJavaType f_subst = formalTypes[i].subst(theta); 
			if (!mb.isPertinentToApplicability(m, call.getNumTypeArgs() > 0, e_i)) {
				rv.add(new ConstraintFormula(e_i, FormulaConstraint.IS_COMPATIBLE, f_subst));
			}
			createAdditionalConstraints(rv, f_subst, e_i);
		}
		return rv;
	}
	
	/**
	 * - For all i (1 <= i <= k), additional constraints may be included, depending on the
	 *   form of e i :
	 *   
	 *   > If e i is a LambdaExpression, C contains <LambdaExpression -> throws F i θ>.
	 *   
	 *   > If e i is a MethodReference, C contains <MethodReference -> throws F i θ>.
	 *   
	 *   > If e i is a poly class instance creation expression (Â§15.9) or a poly method
	 *     invocation expression (Â§15.12), C contains all the constraint formulas that
	 *     would appear in the set C generated by Â§18.5.2 when inferring the poly
	 *     expression's invocation type.
	 *     
	 *   > If e i is a parenthesized expression, these rules are applied recursively to the
	 *     contained expression.
	 *     
	 *   > If e i is a conditional expression, these rules are applied recursively to the
	 *     second and third operands.
	 */
	private void createAdditionalConstraints(Set<ConstraintFormula> c,	IJavaType f_subst, IRNode e_i) {
		final Operator op = JJNode.tree.getOperator(e_i);
		if (LambdaExpression.prototype.includes(op)) {
			c.add(new ConstraintFormula(e_i, FormulaConstraint.THROWS, f_subst));
		}
		else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
			c.add(new ConstraintFormula(e_i, FormulaConstraint.THROWS, f_subst));
		}
		else if (MethodCall.prototype.includes(op) || NewExpression.prototype.includes(op)) {
			if (mb.isPolyExpression(e_i)) {
				// TODO 
				throw new UnsupportedOperationException();
			}
		}
		else if (ParenExpression.prototype.includes(op)) {
			createAdditionalConstraints(c, f_subst, ParenExpression.getOp(e_i));
		}
		else if (ConditionalExpression.prototype.includes(op)) {
			createAdditionalConstraints(c, f_subst, ConditionalExpression.getIftrue(e_i));
			createAdditionalConstraints(c, f_subst, ConditionalExpression.getIffalse(e_i));
		}
	}

	/*
	* @return true if there exists no type of the form G< ... > that is a supertype of S , 
	*         but the raw type | G< ... > | is a supertype of S .
	*/
	boolean onlyHasRawG_asSuperType(IRNode g_decl, IJavaType t) {
		return onlyHasRawG_asSuperType(g_decl, false, t);
	}
	
	private Boolean onlyHasRawG_asSuperType(IRNode g_decl, boolean foundRawG, IJavaType t) {
		if (t instanceof IJavaDeclaredType) {
			IJavaDeclaredType dt = (IJavaDeclaredType) t;
			if (g_decl.equals(dt.getDeclaration())) {
				if (dt.isRawType(tEnv)) {
					foundRawG = true;
				} else {
					return Boolean.FALSE;
				}
			}			
		}
		Boolean result = null;
		for(IJavaType st : t.getSupertypes(tEnv)) {
			Boolean temp = onlyHasRawG_asSuperType(g_decl, foundRawG, st);
			if (temp == Boolean.FALSE) {
				return Boolean.FALSE; // Immediate fail
			}
			if (result == null) {
				result = temp;
			}
			// otherwise result is TRUE, and temp is null or TRUE
		}
		if (result == null && foundRawG) {
			return Boolean.TRUE;
		}
		return result;
	}	
	
	private IJavaDeclaredType isParameterizedType(IJavaType t) {
		if (t instanceof IJavaDeclaredType) {
			final IJavaDeclaredType g = (IJavaDeclaredType) t;
			if (!g.getTypeParameters().isEmpty()) {
				return g;
			}
		}
		return null;
	}
	
	private IJavaDeclaredType isWildcardParameterizedType(IJavaType t) {
		final IJavaDeclaredType g = isParameterizedType(t);
		if (g != null) {
			for(IJavaType p : g.getTypeParameters()) {
				if (p instanceof IJavaWildcardType) {
					return g;
				}
			}			
		}
		return null;
	}
	
	static class InputOutputVars {
		Set<InferenceVariable> input;
		Set<InferenceVariable> output;
	}
	
	/**
	 * Invocation type inference may require carefully sequencing the reduction of
	 * constraint formulas of the forms <Expression -> T >, <LambdaExpression -> throws T >,
	 * and <MethodReference -> throws T >. To facilitate this sequencing, the input variables
	 * of these constraints are defined as follows:
	 * 
	 * - For <LambdaExpression -> T >:
	 *   - If T is an inference variable, it is the (only) input variable.
	 *   
	 *   - If T is a functional interface type, and a function type can be derived from
	 *     T (Â§15.27.3), then the input variables include i) if the lambda expression
	 *     is implicitly typed, the inference variables mentioned by the function type's
	 *     parameter types; and ii) if the function type's return type, R , is not void , then
	 *     for each result expression e in the lambda body (or for the body itself if it is
	 *     an expression), the input variables of < e -> R >.
	 * 
	 *   - Otherwise, there are no input variables.
	 *   
	 * - For <MethodReference -> T >:
	 *   - If T is an inference variable, it is the (only) input variable.
	 *   - If T is a functional interface type with a function type, and if the method
	 *     reference is inexact (Â§15.13.1), the input variables are the inference variables
	 *     mentioned by the function type's parameter types.
	 *   - Otherwise, there are no input variables.
	 * 
	 * - For <Expression -> T >, if Expression is a parenthesized expression:
	 *   Where the contained expression of Expression is Expression', the input variables
	 *   are the input variables of <Expression' -> T >.
	 * 
	 * - For <ConditionalExpression -> T >:
	 *   Where the conditional expression has the form e 1 ? e 2 : e 3 , the input variables
	 *   are the input variables of < e 2 -> T > and < e 3 -> T >.
	 * 
	 * - For all other constraint formulas, there are no input variables.
	 * 
	 * The output variables of these constraints are all inference variables mentioned by
	 * the type on the right-hand side of the constraint, T , that are not input variables.
	 */
	void computeInputOutput(InputOutputVars vars, ConstraintFormula f) {
		f.type.getReferencedInferenceVariables(vars.output);
		computeInputVars(vars.input, f);
		vars.output.removeAll(vars.input);
	}
	
	private void computeInputVars(Set<InferenceVariable> vars, ConstraintFormula f) {
		switch (f.constraint) {
		case IS_COMPATIBLE:
			if (f.expr != null) {
				final Operator op = JJNode.tree.getOperator(f.expr);
				if (LambdaExpression.prototype.includes(op)) {
					if (f.type instanceof InferenceVariable) {
						vars.add((InferenceVariable) f.type);
					}
					final IJavaFunctionType funcType = tEnv.isFunctionalType(f.type);
					if (funcType != null) {
						if (MethodBinder8.isImplicitlyTypedLambda(f.expr)) {		
							for(IJavaType pt : funcType.getParameterTypes()) {
								pt.getReferencedInferenceVariables(vars);
							}
						}	
						if (funcType.getReturnType() != JavaTypeFactory.voidType) {
							for(IRNode e : findResultExprs(f.expr)) {
								computeInputVars(vars, new ConstraintFormula(e, FormulaConstraint.IS_COMPATIBLE, f.type));
							}
						}
					}
				}
				else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
					if (f.type instanceof InferenceVariable) {
						vars.add((InferenceVariable) f.type);
					}
					final IJavaFunctionType funcType = tEnv.isFunctionalType(f.type);
					if (funcType != null) {		
						if (!isExactMethodReference(f.expr)) {			
							for(IJavaType pt : funcType.getParameterTypes()) {
								pt.getReferencedInferenceVariables(vars);
							}
						}
					}
				}
				else if (ParenExpression.prototype.includes(op)) {
					computeInputVars(vars, new ConstraintFormula(ParenExpression.getOp(f.expr), FormulaConstraint.IS_COMPATIBLE, f.type));
				}
				else if (ConditionalExpression.prototype.includes(op)) {
					computeInputVars(vars, new ConstraintFormula(ConditionalExpression.getIftrue(f.expr), FormulaConstraint.IS_COMPATIBLE, f.type));
					computeInputVars(vars, new ConstraintFormula(ConditionalExpression.getIffalse(f.expr), FormulaConstraint.IS_COMPATIBLE, f.type));
				}
			}
			break;
		case THROWS:
			/*
			 * - For <LambdaExpression -> throws T >:
			 *   - If T is an inference variable, it is the (only) input variable.
			 *   - If T is a functional interface type, and a function type can be derived, as
			 *     described in Â§15.27.3, the input variables include i) if the lambda expression
			 *     is implicitly typed, the inference variables mentioned by the function type's
			 *     parameter types; and ii) the inference variables mentioned by the function
			 *     type's return type.
			 *   - Otherwise, there are no input variables.			 
			 *   
			 * - For <MethodReference -> throws T >:
			 *   - If T is an inference variable, it is the (only) input variable.
			 *   - If T is a functional interface type with a function type, and if the method
			 *     reference is inexact (Â§15.13.1), the input variables are the inference variables
			 *     mentioned by the function type's parameter types and the function type's return type.
			 *   - Otherwise, there are no input variables.
			 *
			 */
			if (f.type instanceof InferenceVariable) {
				vars.add((InferenceVariable) f.type);
			}
			final IJavaFunctionType funcType = tEnv.isFunctionalType(f.type);
			if (funcType != null) {
				final Operator op = JJNode.tree.getOperator(f.expr);				
				if (LambdaExpression.prototype.includes(op)) {
					if (MethodBinder8.isImplicitlyTypedLambda(f.expr)) {
						for(IJavaType pt : funcType.getParameterTypes()) {
							pt.getReferencedInferenceVariables(vars);
						}
					}
					funcType.getReturnType().getReferencedInferenceVariables(vars);
				}
				else if (!isExactMethodReference(f.expr)) {							
					funcType.getReturnType().getReferencedInferenceVariables(vars);
					for(IJavaType pt : funcType.getParameterTypes()) {
						pt.getReferencedInferenceVariables(vars);
					}
				}
			}
		default:
		}
	}
	
	private Iterable<IRNode> findResultExprs(IRNode expr) {
		// TODO Auto-generated method stub
		return null;
	}

	// TODO (Â§15.13.1)
	static boolean isExactMethodReference(IRNode n) {
		// TODO
		return false;
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
     * If n !=  k, no valid parameterization exists. Otherwise, a set of constraint formulas is
     * formed with, for all i (1 <= i <= n), < P i = Q i >. This constraint formula set is reduced
     * to form the bound set B .
     * 
     * If B contains the bound false, no valid parameterization exists. Otherwise, a new
     * parameterization of the functional interface type, F<A' 1 , ..., A' m > , is constructed as
     * follows, for 1 <= i <= m:
     * 
     * - If B contains an instantiation for α i , T , then A' i = T .
     * - Otherwise, A' i = A i .
     * 
     * If F<A' 1 , ..., A' m > is not a well-formed type (that is, the type arguments are
     * not within their bounds), or if F<A' 1 , ..., A' m > is not a subtype of F<A 1 , ..., A m >, 
     * no valid parameterization exists. Otherwise, the inferred parameterization is either 
     * F<A' 1 , ..., A' m > , if all the type arguments are types, or the non-wildcard parameterization 
     * (Â§9.9) of F<A' 1 , ..., A' m > , if one or more type arguments are still wildcards.
	 */
	IJavaType inferForFunctionalInterfaceParameterization(IJavaType targetType, IRNode lambda) {
		IJavaDeclaredType f = isWildcardParameterizedType(targetType);
		if (f == null) {
			return targetType; // Nothing to infer
		}
		IJavaDeclaredType f_alpha = f; // TODO
		IJavaFunctionType funcType = tEnv.isFunctionalType(f_alpha);
		IRNode lambdaParams = LambdaExpression.getParams(lambda);
		if (funcType.getParameterTypes().size() != JJNode.tree.numChildren(lambdaParams)) {
			return null; // No valid parameterization
		}
		final BoundSet b = new BoundSet(null, null); // TODO
		int i=0;
		for(IRNode paramD : Parameters.getFormalIterator(lambdaParams)) {
			IJavaType paramT = tEnv.getBinder().getJavaType(ParameterDeclaration.getType(paramD));
			reduceConstraintFormula(b, new ConstraintFormula(paramT, FormulaConstraint.IS_SAME, funcType.getParameterTypes().get(i)));
			i++;
		}
		if (b.isFalse) {
			return null; // No valid parameterization
		}		
		final BoundSet result = resolve(b);		
		List<IJavaType> a_prime = new ArrayList<IJavaType>(f.getTypeParameters().size());
		for(i=0; i<f.getTypeParameters().size(); i++) {
			IJavaType t = result.instantiations.get(f_alpha.getTypeParameters().get(i));
			a_prime.add(t != null ? t : f.getTypeParameters().get(i));
		}
		IJavaDeclaredType f_prime = JavaTypeFactory.getDeclaredType(f.getDeclaration(), a_prime, f.getOuterType());
		// TODO check if well formed
		if (!f_prime.isSubtype(tEnv, f)) {
			return null; // No valid parameterization
		}
		if (isWildcardParameterizedType(f_prime) != null) {
			return computeNonWildcardParameterization(f_prime);
		}
		return f_prime;
	}
	
	/**
	 * JLS 8 sec 9.9
	 * 
	 * • The function type of a parameterized functional interface type I<A1...An>, where one or more of A1...An is a wildcard, 
	 *   is the function type of the non- wildcard parameterization of I, I<T1...Tn>. The non-wildcard parameterization is 
	 *   determined as follows.
	 * 
	 *   Let P1...Pn be the type parameters of I with corresponding bounds B1...Bn. For all i (1 ≤ i ≤ n), Ti is derived 
	 *   according to the form of Ai:
	 * 
	 *   – If Ai is a type, then Ti = Ai.
	 *   – If Ai is a wildcard, and the corresponding type parameter's bound, Bi, mentions one of P1...Pn, then Ti is undefined
	 *     and there is no function type.
	 *   – Otherwise:
	 *     › If Ai is an unbound wildcard ?, then Ti = Bi.
	 *     › If Ai is a upper-bounded wildcard ? extends Ui, then Ti = glb(Ui, Bi) (§5.1.10).
	 *     › If Ai is a lower-bounded wildcard ? super Li, then Ti = Li.
	 */
	private IJavaType computeNonWildcardParameterization(IJavaDeclaredType iface) {
		final int n = iface.getTypeParameters().size();
		final List<IJavaType> t = new ArrayList<IJavaType>(n);
		final Set<IJavaTypeFormal> params = null;
		for(int i=0; i<n; i++) {
			final IJavaTypeFormal p_i = null;
			final IJavaReferenceType b_i = null;
			final IJavaType a_i = iface.getTypeParameters().get(i);
			final IJavaType t_i;
			if (a_i instanceof IJavaWildcardType) {
				if (refersTo(b_i, params)) {
					return null;
				}
				final IJavaWildcardType wt = (IJavaWildcardType) a_i;
				if (wt.getUpperBound() != null) {
					return utils.getGreatestLowerBound(b_i, wt.getUpperBound());
				}
				else if (wt.getLowerBound() != null) {
					return wt.getLowerBound();
				}
				else {
					return b_i;
				}
			} else {
				t_i = a_i;
			}
			t.add(t_i);
		}
		return JavaTypeFactory.getDeclaredType(iface.getDeclaration(), t, iface.getOuterType());
	}

	private boolean refersTo(IJavaReferenceType b_i, Set<IJavaTypeFormal> params) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 18.5.4 More Specific Method Inference
	 * 
     * When testing that one applicable method is more specific than another (Â§15.12.2.5),
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
     *   Note that no substitution is applied to S 1 , ..., S k ; even if m 1 is generic, the type parameters
     *   of m 1 are treated as type variables, not inference variables.
     * 
     * The process to determine if m 1 is more specific than m 2 is as follows:
     * 
     * • First, an initial bound set, B , is constructed from the declared bounds of P 1 , ...,
     *   P p , as specified in §18.1.3.
     * 
     * • Second, for all i (1 ≤ i ≤ k), a set of constraint formulas or bounds is generated.
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
     *   
     *   – S i is not a superinterface of I , nor a parameterization of a superinterface of I .
     *   
     *   – S i is not a subinterface of I , nor a parameterization of a subinterface of I .
     *   
     *   – If S i is an intersection type, at least one element of the intersection is not a
     *     superinterface of I , nor a parameterization of a superinterface of I .
     *     
     *   – If S i is an intersection type, no element of the intersection is a subinterface of
     *     I , nor a parameterization of a subinterface of I .
     *     
     *   If all of the above are true, then the following constraint formulas or bounds are
     *   generated (where U 1 ... U k and R 1 are the parameter types and return type of the
     *   function type of the capture of S i , and V 1 ... V k and R 2 are the parameter types and
     *   return type of the function type of T i ):
     *   
     *   – If e i is an explicitly typed lambda expression:
     *   
     *     › If R 2 is void , true.
     *     
     *     › Otherwise, if R 1 and R 2 are functional interface types, and neither interface
     *       is a subinterface of the other, then these rules are applied recursively to R 1
     *       and R 2 , for each result expression in e i .
     *       
     *     › Otherwise, if R 1 is a primitive type and R 2 is not, and each result expression
     *       of e i is a standalone expression (§15.2) of a primitive type, true.
     *
     *     › Otherwise, if R 2 is a primitive type and R 1 is not, and each result expression of
     *       e i is either a standalone expression of a reference type or a poly expression, true.
     *       
     *     › Otherwise, ‹ R 1 <: R 2 ›.
     *     
     *   – If e i is an exact method reference:
     *     
     *     › For all j (1 ≤ j ≤ k), ‹ U j = V j ›.
     * 
     *     › If R 2 is void , true.
     *     
     *     › Otherwise, if R 1 is a primitive type and R 2 is not, and the compile-time
     *       declaration for e i has a primitive return type, true.
     *       
     *     › Otherwise if R 2 is a primitive type and R 1 is not, and the compile-time
     *       declaration for e i has a reference return type, true.
     *       
     *     › Otherwise, ‹ R 1 <: R 2 ›.
     *     
     *   – If e i is a parenthesized expression, these rules are applied recursively to the
     *     contained expression.
     *     
     *   – If e i is a conditional expression, these rules are applied recursively to each of
     *     the second and third operands.
     *     
     *   – Otherwise, false.
     * 
     *   If the five constraints on S i are not satisfied, the constraint formula ‹ S i <: T i ›
     *   is generated instead.
     *   
     * • Third, if m 2 is applicable by variable arity invocation and has k+1 parameters,
     *   then where S k+1 is the k+1'th variable arity parameter type of m 1 and T k+1 is the
     *   result of θ applied to the k+1'th variable arity parameter type of m 2 , the constraint
     *   ‹ S k+1 <: T k+1 › is generated.
     *   
     * • Fourth, the generated bounds and constraint formulas are reduced and
     *   incorporated with B to produce a bound set B' .
     * 
     *   If B' does not contain the bound false, and resolution of all the inference variables
     *   in B' succeeds, then m 1 is more specific than m 2 .
     *   
     *   Otherwise, m 1 is not more specific than m 2 .
	 */
	boolean inferToBeMoreSpecificMethod(CallState call, MethodState m_1, InvocationKind kind, MethodState m_2) {
		if (m_2.bind.numTypeFormals <= 0) {
			return true;
		}
		// TODO
		return false;
	}
	
	/**
	 * 18.1.2 Constraint Formulas
	 * 
	 * Constraint formulas are assertions of compatibility or subtyping that may involve
	 * inference variables. The formulas may take one of the following forms:
	 * 
	 * - <Expression -> T >: An expression is compatible in a loose invocation context
	 *   with type T (Â§5.3).
	 *   
	 * - < S -> T >: A type S is compatible in a loose invocation context with type T (Â§5.3).
	 * 
	 * - < S <: T >: A reference type S is a subtype of a reference type T (Â§4.10).
	 * 
	 * - < S <= T >: A type argument S is contained by a type argument T (Â§4.5.1).
	 * 
	 * - < S = T >: A reference type S is the same as a reference type T (Â§4.3.4), or a type
	 *   argument S is the same as type argument T .
	 * 
	 * - <LambdaExpression -> throws T >: The checked exceptions thrown by the body of
	 *   the LambdaExpression are declared by the throws clause of the function type
	 *   derived from T .
	 *   
	 * - <MethodReference -> throws T >: The checked exceptions thrown by the referenced
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
		
		@Override
		public String toString() {
			if (expr == null) {
				return stype+" "+constraint+' '+type;
			} else {
				return DebugUnparser.toString(expr)+' '+constraint+' '+type;
			}
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
	 * - S = T , where at least one of S or T is an inference variable: S is the same as T .
	 * 
	 * - S <: T , where at least one of S or T is an inference variable: S is a subtype of T .
	 * 
	 * - false: No valid choice of inference variables exists.
	 * 
	 * - G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > ): The variables α 1 , ..., α n represent the result
	 *   of capture conversion (Â§5.1.10) applied to G<A 1 , ..., A n > (where A 1 , ..., A n may be
	 *   types or wildcards and may mention inference variables).
	 *   
	 * - throws α: The inference variable α appears in a throws clause.
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
	static abstract class Bound<T extends IJavaReferenceType> {
		final T s, t;
		
		Bound(T s, T t) {
			if (s == null || t == null) {
				throw new IllegalArgumentException();
			}
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
				return o.getClass().equals(other.getClass()) && s.equals(other.s) && t.equals(other.t);
			}
			return false;
		}
		
		@Override
		public abstract String toString();
	}
	
	static class EqualityBound extends Bound<IJavaReferenceType> {
		EqualityBound(IJavaReferenceType s, IJavaReferenceType t) {
			super(s, t);
		}
		@Override
		public String toString() {
			return s+" = "+t;
		}
	}
	
	// S is a subtype of T
	static class SubtypeBound extends Bound<IJavaReferenceType> {
		SubtypeBound(IJavaReferenceType s, IJavaReferenceType t) {
			super(s, t);
		}
		@Override
		public String toString() {
			return s+" <: "+t;
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
		@Override
		public String toString() {
			return s+" = capture("+t+")";
		}
	}
	
	/**
	 * From Â§18.1.3:
	 * 
	 * When inference begins, a bound set is typically generated from a list of type
	 * parameter declarations P 1 , ..., P p and associated inference variables α 1 , ..., α p . Such
	 * a bound set is constructed as follows. For each l (1 <= l <= p):
	 * 
	 * - If P l has no TypeBound, the bound α l <: Object appears in the set.
	 * 
	 * - Otherwise, for each type T delimited by & in the TypeBound, 
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
		final IJavaTypeSubstitution theta = set.getInitialVarSubst();
		i=0;
		for(IRNode tf : TypeFormals.getTypeIterator(typeFormals)) {
			IRNode bounds = TypeFormal.getBounds(tf);
			boolean noBounds = true;
			boolean gotProperBound = false;
			for(IRNode bound : MoreBounds.getBoundIterator(bounds)) {
				final IJavaType t = tEnv.getBinder().getJavaType(bound);
				final IJavaType t_subst = t.subst(theta);
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
		// TODO is there anything else to do?
		return set;
	}
	
	static class BoundSubset {
		private final Set<EqualityBound> equalities = new HashSet<EqualityBound>();
		private final Set<SubtypeBound> upperBounds = new HashSet<SubtypeBound>();
		private final Set<SubtypeBound> lowerBounds = new HashSet<SubtypeBound>();
		
		boolean examine(BoundCondition cond) {
			for(SubtypeBound b : upperBounds) {
				if (cond.examineUpperBound(b.s)) {
					return true;
				}
			}
			for(SubtypeBound b : lowerBounds) {
				if (cond.examineLowerBound(b.t)) {
					return true;
				}
			}
			for(EqualityBound b : equalities) {
				if (cond.examineEquality(b.s, b.t)) {
					return true;
				}
			}
			return false;
		}		
	}
	
	interface BoundCondition {
		boolean examineEquality(IJavaType s, IJavaType t);
		boolean examineLowerBound(IJavaType t);
		boolean examineUpperBound(IJavaType t);
	}
	
	/**
	 * An important intermediate result of inference is a bound set. It is sometimes
	 * convenient to refer to an empty bound set with the symbol true; this is merely out
	 * of convenience, and the two are interchangeable
	 */
	class BoundSet {
		private boolean isFalse = false;
		private boolean usedUncheckedConversion = false;
		private final Set<InferenceVariable> thrownSet = new HashSet<InferenceVariable>();
		private final Set<EqualityBound> equalities = new HashSet<EqualityBound>();
		private final Set<SubtypeBound> subtypeBounds = new HashSet<SubtypeBound>();
		private final Set<CaptureBound> captures = new HashSet<CaptureBound>();
		
		/**
		 * Queue for bounds that haven't been incorporated yet
		 */
		private final Queue<Bound<?>> unincorporated = new LinkedList<Bound<?>>();
		
		/**
		 * The original bound that eventually created this one
		 */
		private final BoundSet original;
		
		/**
		 * Mapping from the original type variables to the corresponding inference variables
		 */
		final Map<IJavaTypeFormal,InferenceVariable> variableMap = new HashMap<IJavaTypeFormal,InferenceVariable>();
		
		/**
		 * The result of resolution
		 */
		final Map<InferenceVariable, IJavaType> instantiations = new HashMap<InferenceVariable, IJavaType>();
		
		private BoundSet() {
			original = null;
		}
		
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
			usedUncheckedConversion = orig.usedUncheckedConversion;
			thrownSet.addAll(orig.thrownSet);
			equalities.addAll(orig.equalities);
			subtypeBounds.addAll(orig.subtypeBounds);
			captures.addAll(orig.captures);
			instantiations.putAll(orig.instantiations);
			variableMap.putAll(orig.variableMap);
		}
		
		void merge(BoundSet other) {
			isFalse |= other.isFalse;
			usedUncheckedConversion |= other.usedUncheckedConversion;
			thrownSet.addAll(other.thrownSet);
			variableMap.putAll(other.variableMap);
			unincorporated.addAll(other.equalities);
			unincorporated.addAll(other.subtypeBounds);
			unincorporated.addAll(other.captures);
			incorporate();
		}
		
		IJavaTypeSubstitution getInitialVarSubst() {
			return new TypeSubstitution(tEnv.getBinder(), variableMap);
		}

		IJavaTypeSubstitution getFinalTypeSubst() {
			final Map<IJavaTypeFormal,IJavaType> subst = new HashMap<IJavaTypeFormal,IJavaType>();
			for(Entry<IJavaTypeFormal, InferenceVariable> e : variableMap.entrySet()) {
				final IJavaType t = instantiations.get(e.getValue());
				if (t == null) {
					throw new IllegalStateException("No instantiation for "+e.getKey());
				}
				subst.put(e.getKey(), t);
			}
			return new TypeSubstitution(tEnv.getBinder(), subst);
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
			if (t instanceof InferenceVariable) {
				boolean swap = !(s instanceof InferenceVariable) || s.hashCode() > t.hashCode();					
				if (swap) {
					IJavaType temp = s;
					s = t;
					t = temp;
				}
			}
			incorporate(new EqualityBound((IJavaReferenceType) s, (IJavaReferenceType) t));		
		}

		// s <: (is a subtype of) t
		void addSubtypeBound(IJavaType s, IJavaType t) {
			incorporate(new SubtypeBound((IJavaReferenceType) s, (IJavaReferenceType) t));
		}	
		
		// G< α 1 , ..., α n > = capture( G<A 1 , ..., A n > )
		void addCaptureBound(IJavaType s, IJavaType t) {
			incorporate(new CaptureBound((IJavaDeclaredType) s, (IJavaDeclaredType) t));
		}
		
		void addThrown(InferenceVariable v) {
			thrownSet.add(v);			
		}
		
		void addInstantiation(InferenceVariable v, IJavaType t) {
			if (v == null || t == null) {
				throw new NullPointerException("Bad instantiation: "+v+" = "+t);
			}
			if (!t.isProperType()) {
				throw new IllegalStateException("Not a proper type: "+t);				
			}
			instantiations.put(v, t); // TODO
			addEqualityBound(v, t);
		}
		
		/**
		 * 18.3 Incorporation
		 * 
		 * As bound sets are constructed and grown during inference, it is possible that new bounds can be inferred 
		 * based on the assertions of the original bounds. The process of incorporation identifies these new bounds 
		 * and adds them to the bound set.
		 * 
		 * Incorporation can happen in two scenarios. One scenario is that the bound set contains complementary pairs 
		 * of bounds; this implies new constraint formulas, as specified in §18.3.1. The other scenario is that the 
		 * bound set contains a bound involving capture conversion; this implies new bounds and may imply new constraint
		 * formulas, as specified in §18.3.2. In both scenarios, any new constraint formulas are reduced, and any new 
		 * bounds are added to the bound set. This may trigger further incorporation; ultimately, the set will reach a 
		 * fixed point and no further bounds can be inferred.
		 * 
		 * If incorporation of a bound set has reached a fixed point, and the set does not contain the bound false, 
		 * then the bound set has the following properties:
		 * 
		 * • For each combination of a proper lower bound L and a proper upper bound U of an inference variable, L <: U.
		 * • If every inference variable mentioned by a bound has an instantiation, the bound is satisfied by the 
		 *   corresponding substitution.
		 * • Given a dependency α = β, every bound of α matches a bound of β, and vice versa.
		 * • Given a dependency α <: β, every lower bound of α is a lower bound of β, and every upper bound of β is an upper bound of α.
		 */
		private void incorporate(Bound<?>... newBounds) {
			for(Bound<?> b : newBounds) {
				unincorporated.add(b);
				//System.out.println("Added "+b);
			}
			while (!unincorporated.isEmpty()) {
				Bound<?> b = unincorporated.remove();
				
				// Check for combos and reduce the resulting constraints
				BoundSet temp = new BoundSet();
				if (b instanceof SubtypeBound) {
					SubtypeBound sb = (SubtypeBound) b;
					if (subtypeBounds.contains(sb)) {
						continue;
					} 
					incorporateSubtypeBound(temp, sb);
					subtypeBounds.add(sb);
				}
				else if (b instanceof EqualityBound) {
					EqualityBound eb = (EqualityBound) b;
					if (equalities.contains(eb)) {
						continue;
					} 
					incorporateEqualityBound(temp, eb);
					equalities.add(eb);
					
					if (eb.s instanceof InferenceVariable && eb.t.isProperType()) {
						IJavaType instantiation = instantiations.get(eb.s);
						if (instantiation == null) {
							instantiations.put((InferenceVariable) eb.s, eb.t);
						}
						else if (!instantiation.isEqualTo(tEnv, eb.t)) {
							SLLogger.getLogger().warning("Duplicate instantiation for "+eb.s+": "+instantiation+" vs "+eb.t);
							//throw new IllegalStateException("Duplicate instantiation?");
						} 						
					}
				}
				else {
					CaptureBound cb = (CaptureBound) b;
					if (captures.contains(cb)) {
						continue;
					}
					incorporateCaptureBound(temp, cb);
					captures.add(cb);
				}
				merge(temp);
			}
		}

		// See incorporateSubtypeBound() for details
		private void incorporateEqualityBound(BoundSet bounds, EqualityBound eb) {
			if (eb.s instanceof InferenceVariable) {
				final InferenceVariable alpha = (InferenceVariable) eb.s;
				final IJavaTypeSubstitution s = eb.s.isProperType() ? 
						new TypeSubstitution(tEnv.getBinder(), Collections.singletonMap(alpha, eb.s)) : null;
				for(EqualityBound b : equalities) {
					// case 1: α = S and α = T imply ‹S = T›
					if (alpha == b.s) {
						reduceTypeEqualityConstraints(bounds, eb.t, b.t);
					}
					// case 5: α = U and S = T imply ‹S[α:=U] = T[α:=U]›
					if (s != null) {
						reduceTypeEqualityConstraints(bounds, b.s.subst(s), b.t.subst(s)); // TODO check if the same?
					}
				}
				for(SubtypeBound b : subtypeBounds) {
					// case 2: α = S and α <: T imply ‹S <: T›
					if (alpha == b.s) {
						reduceSubtypingConstraints(bounds, eb.t, b.t);
					}
					// case 3: α = S and T <: α imply ‹T <: S›
					else if (alpha == b.t) {
						IJavaType t = b.s;
						reduceSubtypingConstraints(bounds, t, eb.s);
					}
					// case 6: α = U and S <: T imply ‹S[α:=U] <: T[α:=U]›
					if (s != null) {
						reduceSubtypingConstraints(bounds, b.s.subst(s), b.t.subst(s)); // TODO check if the same?
					}
				}
			}
		}

		/*
		// See incorporateSubtypeBound() for details
		private void incorporateEqualityBound2(BoundSet bounds, EqualityBound eb) {
			if (eb.s instanceof InferenceVariable) {
				final InferenceVariable alpha = (InferenceVariable) eb.s;
				final IJavaTypeSubstitution s = eb.s.isProperType() ? 
						new TypeSubstitution(tEnv.getBinder(), Collections.singletonMap(alpha, eb.s)) : null;
				for(EqualityBound b : equalities) {
					// case 1
					if (alpha == b.s) {
						reduceTypeEqualityConstraints(bounds, eb.s, b.t);
					}
					// case 5
					if (s != null) {
						reduceTypeEqualityConstraints(bounds, b.s.subst(s), b.t.subst(s)); // TODO check if the same?
					}
				}
				for(SubtypeBound b : subtypeBounds) {
					// case 2
					if (alpha == b.s) {
						reduceSubtypingConstraints(bounds, eb.s, b.t);
					}
					// case 3
					else if (alpha == b.t) {
						IJavaType t = b.s;
						reduceSubtypingConstraints(bounds, t, eb.s);
					}
					// case 6
					if (s != null) {
						reduceSubtypingConstraints(bounds, b.s.subst(s), b.t.subst(s)); // TODO check if the same?
					}
				}
			}
		}
		*/

		/**
		 * 18.3.1 Complementary Pairs of Bounds
		 *
		 * (In this section, S and T are inference variables or types, and U is a proper type. 
		 * For conciseness, a bound of the form α = T may also match a bound of the form T = α.)
		 * 
		 * When a bound set contains a pair of bounds that match one of the following rules, a new constraint formula is implied:
		 * 
		 * 1• α = S and α = T imply ‹S = T›
		 * 2• α = S and α <: T imply ‹S <: T›
		 * 3• α = S and T <: α imply ‹T <: S›
		 * 4• S <: α and α <: T imply ‹S <: T›
		 * 5• α = U and S = T imply ‹S[α:=U] = T[α:=U]›
		 * 6• α = U and S <: T imply ‹S[α:=U] <: T[α:=U]›
		 * 
		 * ... see below
		 */ 
		private void incorporateSubtypeBound(BoundSet bounds, SubtypeBound sb) {
			if (sb.s instanceof InferenceVariable) {
				final InferenceVariable alpha = (InferenceVariable) sb.s;
				for(EqualityBound b : equalities) {
					// case 2
					if (alpha == b.s) {
						reduceSubtypingConstraints(bounds, b.t, sb.t);
					}
				}
				for(SubtypeBound b : subtypeBounds) {
					if (b.t instanceof InferenceVariable) {
						// case 4a: S <: α and α <: T imply ‹S <: T›
						reduceSubtypingConstraints(bounds, b.s, sb.t);
					}
					if (b.s instanceof InferenceVariable) {
						/*
						 * When a bound set contains a pair of bounds α <: S and α <: T, and there exists a supertype of S
						 * of the form G<S1, ..., Sn> and a supertype of T of the form G<T1, ..., Tn> (for some generic class
						 * or interface, G), then for all i (1 ≤ i ≤ n), if Si and Ti are types (not wildcards), 
						 * the constraint formula ‹Si = Ti› is implied.
						 */
						// TODO handle supertype case
					}
				}
			}
			if (sb.t instanceof InferenceVariable) {
				final InferenceVariable alpha = (InferenceVariable) sb.t;
				for(EqualityBound b : equalities) {
					// case 3
					if (alpha == b.s) {
						reduceSubtypingConstraints(bounds, sb.s, b.t);
					}
				}	
				for(SubtypeBound b : subtypeBounds) {
					// case 4b
					if (alpha == b.s) {
						reduceSubtypingConstraints(bounds, sb.s, b.t);
					}
				}
			}
			for(EqualityBound b : equalities) {
				if (b.s instanceof InferenceVariable && b.t.isProperType()) {
					// case 6
					final InferenceVariable alpha = (InferenceVariable) b.s;
					final IJavaTypeSubstitution s = new TypeSubstitution(tEnv.getBinder(), Collections.singletonMap(alpha, b.t));
					if (sb.t.subst(s) == null) {
						sb.t.subst(s);
					}
					reduceSubtypingConstraints(bounds, sb.s.subst(s), sb.t.subst(s)); // TODO check if the same?
				}				
			}
		}

		/**
		 * 18.3.2 Bounds Involving Capture Conversion
		 * 
		 * When a bound set contains a bound of the form G<α1, ..., αn> = capture(G<A1, ..., An>), 
		 * new bounds are implied and new constraint formulas may be implied, as follows.
		 * 
		 * Let P1, ..., Pn represent the type parameters of G and let B1, ..., Bn represent the bounds
		 * of these type parameters. Let θ represent the substitution [P1:=α1, ..., Pn:=αn]. 
		 * Let R be a type that is not an inference variable (but is not necessarily a proper type).
		 * 
		 * A set of bounds on α1, ..., αn is implied, constructed from the declared bounds of P1, ..., Pn as specified in §18.1.3.
		 * In addition, for all i (1 ≤ i ≤ n):
		 * 
		 * • If Ai is not a wildcard, then the bound αi = Ai is implied.
		 * 
		 * • If Ai is a wildcard of the form ?:
		 *   – αi = R implies the bound false
		 *   – αi <: R implies the constraint formula ‹Bi θ <: R›
		 *   – R <: αi implies the bound false
		 * 
		 * • If Ai is a wildcard of the form ? extends T:
		 *   – αi = R implies the bound false
		 *   – If Bi is Object, then αi <: R implies the constraint formula ‹T <: R›
		 *   – If T is Object, then αi <: R implies the constraint formula ‹Bi θ <: R›
		 *   – R <: αi implies the bound false
		 *   
		 * • If Ai is a wildcard of the form ? super T:
		 *   – αi = R implies the bound false
		 *   – αi <: R implies the constraint formula ‹Bi θ <: R›
		 *   – R <: αi implies the constraint formula ‹R <: T›
		 */
		private void incorporateCaptureBound(BoundSet bounds, CaptureBound cb) {
			// TODO Auto-generated method stub
			constructInitialSet(null);
			
			int i=0;
			for(IJavaType a_i : cb.t.getTypeParameters()) {
				if (a_i instanceof IJavaWildcardType) {
					IJavaWildcardType wt = (IJavaWildcardType) a_i;
					if (wt.getUpperBound() != null) {
						// case 3
						throw new NotImplemented();
					}
					else if (wt.getLowerBound() != null) {
						// case 4
						throw new NotImplemented();
					}
					else {
						// case 2
						throw new NotImplemented();
					}
				} else {
					// case 1
					addEqualityBound(cb.s.getTypeParameters().get(i), a_i);
				}
				i++;
			}
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
			if (uninstantiated.size() < 1) {
				return uninstantiated;
			}
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
		 *   - If α i has one or more proper lower bounds, L 1 , ..., L k , then T i = lub( L 1 , ..., L k ) (Â§4.10.4).
		 *   
		 *   - Otherwise, if the bound set contains throws α i , and the proper upper
		 *     bounds of α i are, at most, Exception , Throwable , and Object , then T i = RuntimeException .
		 *   
		 *   - Otherwise, where α i has proper upper bounds U 1 , ..., U k , T i = glb( U 1 , ..., U k ) (Â§5.1.10).
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
		 *   - For all i (1 <= i <= n), if α i has one or more proper lower bounds L 1 , ..., L k , then
		 *     let the lower bound of Y i be lub( L 1 , ..., L k ); if not, then Y i has no lower bound.
		 *   
		 *   - For all i (1 <= i <= n), where α i has upper bounds U 1 , ..., U k , let the upper bound
		 *     of Y i be glb( U 1 θ, ..., U k θ), where θ is the substitution [ α 1 := Y 1 , ..., α n := Y n ] .
		 *   
		 *   If the type variables Y 1 , ..., Y n do not have well-formed bounds (that is, a lower
		 *   bound is not a subtype of an upper bound, or an intersection type is inconsistent), then resolution fails.
		 *  
		 *   Otherwise, for all i (1 <= i <= n), all bounds of the form G< ..., α i , ... > =
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
			final Map<InferenceVariable,InferenceVariable> y_subst = new HashMap<InferenceVariable,InferenceVariable>(subset.size());
			for(InferenceVariable a_i : subset) {
				final InferenceVariable y_i = new InferenceVariable(null); // TODO unique?
				y_subst.put(a_i, y_i);
			}
			final TypeSubstitution theta = new TypeSubstitution(tEnv.getBinder(), y_subst);
			
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
				List<IJavaType> upper = new ArrayList<IJavaType>(bounds.upperBounds.get(a_i));
				IJavaType u_i = null;
				if (upper != null && !upper.isEmpty()) {
					u_i = utils.getGreatestLowerBound(toArray(theta.substTypes(null, upper))); 
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
		
		// a = T, T = a, a <: T, T <: a
		BoundSubset findAssociatedBounds(InferenceVariable a) {
			BoundSubset rv = new BoundSubset();
			for(EqualityBound bound : equalities) {
				if (bound.s == a || bound.t == a) {
					rv.equalities.add(bound);
				}
			}
			for(SubtypeBound b : subtypeBounds) {
				if (b.s == a) {
					rv.upperBounds.add(b);
				}
				else if (b.t == a) {
					rv.lowerBounds.add(b);
				}
			}
			return rv;
		}

		void usedUncheckedConversion() {
			usedUncheckedConversion = true;
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
	 * V such that i) for all i (1 <= i <= n), if α i depends on the resolution of a variable Î²,
	 * then either Î² has an instantiation or there is some j such that Î² = α j ; and ii) there
	 * exists no non-empty proper subset of { α 1 , ..., α n } with this property. Resolution
	 * proceeds by generating an instantiation for each of α 1 , ..., α n based on the bounds in the bound set:
	 * 
	 * - If the bound set does not contain a bound of the form G< ..., α i , ... > =
	 *   capture( G< ... > ) for all i (1 <= i <= n), then a candidate instantiation T i is defined for each α i :
	 * 
	 *   ...
	 * 
	 * - If the bound set contains a bound of the form G< ..., α i , ... > = capture( G< ... > ) for some i (1 <= i <= n), or;
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
		if (subset.isEmpty()) {
			return bounds; // All instantiated
		}
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
		 * - Given a bound of one of the following forms, where T is either an inference variable Î² or a type that mentions Î²:
		 *   - α = T
		 *   - α <: T
		 *   - T = α
		 *   - T <: α
		 * 
		 *   If α appears on the left-hand side of another bound of the form G< ..., α, ... > =
		 *   capture( G< ... > ), then Î² depends on the resolution of α. Otherwise, α depends on the resolution of Î².
		 * 
		 * - An inference variable α appearing on the left-hand side of a bound of the form
		 *   G< ..., α, ... > = capture( G< ... > ) depends on the resolution of every other inference
		 *   variable mentioned in this bound (on both sides of the = sign).
		 * 
		 * - An inference variable α depends on the resolution of an inference variable Î² if
		 *   there exists an inference variable Î³ such that α depends on the resolution of Î³ and
		 *   Î³ depends on the resolution of Î².
		 * 
		 * - An inference variable α depends on the resolution of itself.
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
					markDependsOn(alpha, alpha);
				}
				else if (b.t instanceof InferenceVariable) {
					InferenceVariable alpha = (InferenceVariable) b.t;
					b.s.getReferencedInferenceVariables(temp);
					for(InferenceVariable beta : temp) {
						markDependsOn(alpha, beta);
					}
					markDependsOn(alpha, alpha);
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
			L â†� Empty list that will contain the sorted nodes
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
					if (m != n && components.containsKey(m)) { // filter to the component roots
						visitForSort(l, toVisit, m);
					}
				}
				n.index = 100;
				l.add(0, n);
			}
		}
	}
 	
	static boolean isInferenceVariable(IJavaType t) {
		return t instanceof InferenceVariable;
	}
	
	IJavaReferenceType box(IJavaPrimitiveType formalP) {
		IJavaDeclaredType boxedEquivalent = JavaTypeFactory.getCorrespondingDeclType(tEnv, formalP);
		return boxedEquivalent;
	}
	
	/**
	 * @return true if T is a parameterized type of the form G<T 1 , ..., T n > , and there exists
	 * no type of the form G< ... > that is a supertype of S , but the raw type G is a supertype
	 * of S
	 */
	boolean hasRawSuperTypeOf(IJavaType s, IJavaType t) {
		if (t instanceof IJavaDeclaredType) {
			IJavaDeclaredType g = (IJavaDeclaredType) t;
			if (g.getTypeParameters().size() > 0) { 
				// g is parameterized
				return onlyHasRawSuperTypeOf(s, g.getDeclaration());
			}
		}
		return false;
	}
	
	boolean onlyHasRawSuperTypeOf(IJavaType s, IRNode g) {
		if (s instanceof IJavaDeclaredType) {
			IJavaDeclaredType gs = (IJavaDeclaredType) s;
			if (gs.getDeclaration().equals(g) && gs.isRawType(tEnv)) {
				return true;
			}
		}
		for(IJavaType st : s.getSupertypes(tEnv)) {
			if (onlyHasRawSuperTypeOf(st, g)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 18.2 Reduction
	 * 
     * Reduction is the process by which a set of constraint formulas (Â§18.1.2) is
     * simplified to produce a bound set (Â§18.1.3).
     * 
     * Each constraint formula is considered in turn. The rules in this section specify how
     * the formula is reduced to one or both of:
     * 
     * - A bound or bound set, which is to be incorporated with the "current" bound set.
     *   Initially, the current bound set is empty.
     *   
     * - Further constraint formulas, which are to be reduced recursively.
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
	 * A constraint formula of the form <Expression -> T > is reduced as follows:
	 * 
	 * - If T is a proper type, the constraint reduces to true if the expression is compatible
	 *   in a loose invocation context with T (Â§5.3), and false otherwise.
	 *   
	 * - Otherwise, if the expression is a standalone expression (Â§15.2) of type S , the
	 *   constraint reduces to < S -> T >.
	 *   
	 * - Otherwise, the expression is a poly expression (Â§15.2). The result depends on
	 *   the form of the expression:
	 * 
	 *   - If the expression is a parenthesized expression of the form ( Expression' ) , the
	 *     constraint reduces to <Expression' -> T >.
	 *     
	 *   - If the expression is a class instance creation expression or a method invocation
	 *     expression, the constraint reduces to the bound set B 3 which would be used
	 *     to determine the expression's invocation type when targeting T , as defined in
	 *     Â§18.5.2. (For a class instance creation expression, the corresponding "method"
	 *     used for inference is defined in Â§15.9.3).
	 *     This bound set may contain new inference variables, as well as dependencies
	 *     between these new variables and the inference variables in T .
	 *     
	 *   - If the expression is a conditional expression of the form e 1 ? e 2 : e 3 , the
	 *     constraint reduces to two constraint formulas, < e 2 -> T > and < e 3 -> T >.
	 *     
	 *   - If the expression is a lambda expression or a method reference expression, the
	 *     result is specified below.
	 * @param bounds 
	 */
	void reduceExpressionCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
		if (t.isProperType()) {
			if (mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, t, e, tEnv.getBinder().getJavaType(e))) {
				bounds.addTrue();
			} else {
				//mb.LOOSE_INVOCATION_CONTEXT.isCompatible(null, t, e, tEnv.getBinder().getJavaType(e));				
				bounds.addFalse();
			}
		}
		else if (!mb.isPolyExpression(e)) {
			IJavaType s = tEnv.getBinder().getJavaType(e);
			reduceTypeCompatibilityConstraints(bounds, s, t);
		} else {
			Operator op = JJNode.tree.getOperator(e);
			if (ParenExpression.prototype.includes(op)) {
				reduceExpressionCompatibilityConstraints(bounds, ParenExpression.getOp(e), t);
			}
			else if (NewExpression.prototype.includes(op) || MethodCall.prototype.includes(op)) {
				try {
					bounds.merge(computeInvocationBounds((CallInterface) op, e, t));
				} catch (NoArgs e1) {
					throw new IllegalStateException("No arguments for "+DebugUnparser.toString(e));
				}
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

	private BoundSet computeInvocationBounds(CallInterface c, final IRNode e, final IJavaType t) throws NoArgs {
		final MethodBinding m = new MethodBinding(tEnv.getBinder().getIBinding(e));
        final CallState call = new CallState(tEnv.getBinder(), e, c.get_TypeArgs(e), c.get_Args(e), m.bind.getReceiverType());
		final BoundSet b_2 = inferForInvocationApplicability(call, m, null);
		return computeB_3(call, m, b_2, t); // TODO
	}

	/**
	 * A constraint formula of the form <LambdaExpression -> T >, where T mentions at
	 * least one inference variable, is reduced as follows:
	 * 
	 * - If T is not a functional interface type (Â§9.8), the constraint reduces to false.
	 * 
	 * - Otherwise, let T' be the ground target type derived from T , as specified in
	 *   Â§15.27.3. If Â§18.5.3 is used to derive a functional interface type which is
	 *   parameterized, then the test that F<A' 1 , ..., A' m > is a subtype of F<A 1 , ..., A m > is
	 *   not performed (instead, it is asserted with a constraint formula below). Let the
	 *   target function type for the lambda expression be the function type of T' . Then:
	 *   
	 *   - If no valid function type can be found, the constraint reduces to false.
	 *   - Otherwise, the congruence of LambdaExpression with the target function type
	 *     is asserted as follows:
	 *     
	 *     > If the number of lambda parameters differs from the number of parameter
	 *       types of the function type, the constraint reduces to false.
	 *       
	 *     > If the lambda expression is implicitly typed and one or more of the function
	 *       type's parameter types is not a proper type, the constraint reduces to false.
	 *       
	 *     > If the function type's result is void and the lambda body is neither a
	 *       statement expression nor a void-compatible block, the constraint reduces to false.
	 *       
	 *     > If the function type's result is not void and the lambda body is a block that
	 *       is not value-compatible, the constraint reduces to false.
	 *       
	 *     > Otherwise, the constraint reduces to all of the following constraint formulas:
	 *     
	 *       Â» If the lambda parameters have explicitly declared types F 1 , ..., F n and the
	 *         function type has parameter types G 1 , ..., G n , then i) for all i (1 <= i <= n),
	 *         < F i = G i >, and ii) < T' <: T >.
	 *         
	 *       Â» If the function type's return type is a (non- void ) type R , assume the
	 *         lambda's parameter types are the same as the function type's parameter
	 *         types. Then:
	 *         
	 *         - If R is a proper type, and if the lambda body or some result expression
	 *           in the lambda body is not compatible in an assignment context with R ,
	 *           then false.
	 *           
	 *         - Otherwise, if R is not a proper type, then where the lambda body has the
	 *           form Expression, the constraint <Expression -> R >; or where the lambda
	 *           body is a block with result expressions e 1 , ..., e m , for all i (1 <= i <= m),
	 *           < e i -> R >.
	 */
	void reduceLambdaCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
		throw new NotImplemented(); // TODO
	}
	
	/** 
	 * A constraint formula of the form <MethodReference -> T >, where T mentions at	 
	 * least one inference variable, is reduced as follows:
	 * 
	 * - If T is not a functional interface type, or if T is a functional interface type that
	 *   does not have a function type (Â§9.9), the constraint reduces to false.
	 *   
	 * - Otherwise, if there does not exist a potentially applicable method for the method
	 *   reference when targeting T , the constraint reduces to false.
	 *   
	 * - Otherwise, if the method reference is exact (Â§15.13.1), then let P 1 , ..., P n be the
	 *   parameter types of the function type of T , and let F 1 , ..., F k be the parameter
	 *   types of the potentially applicable method. The constraint reduces to a new set
	 *   of constraints, as follows:
	 * 	
	 *   - In the special case where n = k+1, the parameter of type P 1 is to act as the target
	 *     reference of the invocation. The method reference expression necessarily
	 *     has the form ReferenceType :: [TypeArguments] Identifier. The constraint
	 *     reduces to < P 1 <: ReferenceType> and, for all i (2 <= i <= n), < P i -> F i-1 >.
	 *     In all other cases, n = k, and the constraint reduces to, for all i (1 <= i <= n),
	 *     < P i -> F i >.
	 *     
	 *   - If the function type's result is not void , let R be its return type. Then, if the result
	 *     of the potentially applicable compile-time declaration is void , the constraint
	 *     reduces to false. Otherwise, the constraint reduces to < R ' -> R >, where R ' is
	 *     the result of applying capture conversion (Â§5.1.10) to the return type of the
	 *     potentially applicable compile-time declaration.
	 *     
	 * - Otherwise, the method reference is inexact, and:
	 *   - If one or more of the function type's parameter types is not a proper type, the
	 *     constraint reduces to false.
	 *     
	 *   - Otherwise, a search for a compile-time declaration is performed, as specified
	 *     in Â§15.13.1. If there is no compile-time declaration for the method reference,
	 *     the constraint reduces to false. Otherwise, there is a compile-time declaration,
	 *     and:
	 *     > If the result of the function type is void , the constraint reduces to true.
	 *     
	 *     > Otherwise, if the method reference expression elides TypeArguments, and
	 *       the compile-time declaration is a generic method, and the return type of
	 *       the compile-time declaration mentions at least one of the method's type
	 *       parameters, then the constraint reduces to the bound set B 3 which would be
	 *       used to determine the method reference's invocation type when targeting the
	 *       return type of the function type, as defined in Â§18.5.2. B 3 may contain new
	 *       inference variables, as well as dependencies between these new variables
	 *       and the inference variables in T .
	 *     
	 *     > Otherwise, let R be the return type of the function type, and let R ' be the result
	 *       of applying capture conversion (Â§5.1.10) to the return type of the invocation
	 *       type (Â§15.12.2.6) of the compile-time declaration. If R ' is void , the constraint
	 *       reduces to false; otherwise, the constraint reduces to < R ' -> R >.
	*/
	void reduceMethodReferenceCompatibilityConstraints(BoundSet bounds, IRNode e, IJavaType t) {
		throw new NotImplemented(); // TODO
	}
	
	/**
	 * 18.2.2 Type Compatibility Constraints
	 * 
	 * A constraint formula of the form < S -> T > is reduced as follows:
	 * 
	 * - If S and T are proper types, the constraint reduces to true if S is compatible in a
	 *   loose invocation context with T (Â§5.3), and false otherwise.
	 * 
	 * - Otherwise, if S is a primitive type, let S' be the result of applying boxing
	 *   conversion (Â§5.1.7) to S . Then the constraint reduces to < S' -> T >.
	 * 
	 * - Otherwise, if T is a primitive type, let T' be the result of applying boxing
	 *   conversion (Â§5.1.7) to T . Then the constraint reduces to < S = T' >.
	 * 
	 * - Otherwise, if T is a parameterized type of the form G<T 1 , ..., T n > , and there exists
	 *   no type of the form G< ... > that is a supertype of S , but the raw type G is a supertype
	 *   of S , then the constraint reduces to true.
	 * 
	 * - Otherwise, if T is an array type of the form G<T 1 , ..., T n >[] k , and there exists no
	 *   type of the form G< ... >[] k that is a supertype of S , but the raw type G[] k is a
	 *   supertype of S , then the constraint reduces to true. (The notation [] k indicates
	 *   an array type of k dimensions.)
	 *   
	 * - Otherwise, the constraint reduces to < S <: T >.
	 *   The fourth and fifth cases are implicit uses of unchecked conversion (Â§5.1.9).
	 *   These, along with any use of unchecked conversion in the first case, may result in
	 *   compile-time unchecked warnings, and may influence a method's invocation type
	 *   (Â§15.12.2.6).
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
			reduceTypeCompatibilityConstraints(bounds, box((IJavaPrimitiveType) s), t);
		}
		else if (t instanceof IJavaPrimitiveType) {
			reduceTypeCompatibilityConstraints(bounds, s, box((IJavaPrimitiveType) t));
		}
		else if (hasRawSuperTypeOf(s, t)) {
			bounds.addTrue();
			bounds.usedUncheckedConversion();
		}
		else if (t instanceof IJavaArrayType && s instanceof IJavaArrayType) {
			IJavaArrayType ta = (IJavaArrayType) t;
			IJavaArrayType sa = (IJavaArrayType) s;
			if (ta.getDimensions() == sa.getDimensions() && hasRawSuperTypeOf(sa.getBaseType(), ta.getBaseType())) {
				bounds.addTrue();
				bounds.usedUncheckedConversion();
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
	 * A constraint formula of the form < S <: T > is reduced as follows:
	 * - If S and T are proper types, the constraint reduces to true if S is a subtype of T
	 *   (Â§4.10), and false otherwise.
	 * - Otherwise, if S is the null type, the constraint reduces to true.
	 * - Otherwise, if T is the null type, the constraint reduces to false.
	 * - Otherwise, if S is an inference variable, α, the constraint reduces to the bound α <: T .
	 * - Otherwise, if T is an inference variable, α, the constraint reduces to the bound S <: α.
	 * - Otherwise, the constraint is reduced according to the form of T :
	 * 
	 *   - If T is a parameterized class or interface type, or an inner class type of a
	 *     parameterized class or interface type (directly or indirectly), let A 1 , ..., A n be
	 *     the type arguments of T . Among the supertypes of S , a corresponding class
	 *     or interface type is identified, with type arguments B 1 , ..., B n . If no such type
	 *     exists, the constraint reduces to false. Otherwise, the constraint reduces to the
	 *     following new constraints: for all i (1 <= i <= n), < B i <= A i >.
	 *     
	 *   - If T is any other class or interface type, then the constraint reduces to true if T
	 *     is among the supertypes of S , and false otherwise.
	 *     
	 *   - If T is an array type, T'[] , then among the supertypes of S that are array types,
	 *     a most specific type is identified, S'[] (this may be S itself). If no such array
	 *     type exists, the constraint reduces to false. Otherwise:
	 *     
	 *     > If neither S' nor T' is a primitive type, the constraint reduces to < S' <: T' >.
	 *     > Otherwise, the constraint reduces to true if S' and T' are the same primitive
	 *       type, and false otherwise.
	 *     
	 *   - If T is a type variable, there are three cases:
	 *     > If S is an intersection type of which T is an element, the constraint reduces to true.
	 *     > Otherwise, if T has a lower bound, B , the constraint reduces to < S <: B >.
	 *     > Otherwise, the constraint reduces to false.
	 *     
	 *   - If T is an intersection type, I 1 & ... & I n , the constraint reduces to the following
	 *     new constraints: for all i (1 <= i <= n), < S <: I i >.
	 */
	void reduceSubtypingConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (s.isProperType() && t.isProperType()) {
			if (tEnv.isSubType(s, t)) {
				bounds.addTrue();
			} else {
				tEnv.isSubType(s, t);
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
		
		else if (t instanceof IJavaDeclaredType) {
			final IJavaDeclaredType dt = (IJavaDeclaredType) t;
			// TODO subcase 1
			if (dt.getTypeParameters().size() > 0/* TODO || dt.getOuterType().getTypeParameters().size() > 0*/) {
				final IJavaDeclaredType ds = findCorrespondingSuperType(dt.getDeclaration(), s);
				if (ds != null && ds.getTypeParameters().size() > 0) {
					final int n = dt.getTypeParameters().size();
					for(int i=0; i<n; i++) {
						final IJavaType a_i = dt.getTypeParameters().get(i);
						final IJavaType b_i = ds.getTypeParameters().get(i);
						reduceTypeArgContainmentConstraints(bounds, b_i, a_i);
					}
				} else {
					bounds.addFalse();
				}
			}
			else if (tEnv.isSubType(s, t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		else if (t instanceof IJavaArrayType) {
			final IJavaArrayType s_primeArray = findMostSpecificArraySuperType(s);
			if (s_primeArray == null) {
				bounds.addFalse();
			} else {
				final IJavaArrayType t_primeArray = (IJavaArrayType) t;
				final IJavaType s_prime = s_primeArray.getElementType();
				final IJavaType t_prime = t_primeArray.getElementType();
				if (s_prime instanceof IJavaPrimitiveType || t_prime instanceof IJavaPrimitiveType) {
					if (!s_prime.equals(t_prime)) {
						bounds.addFalse();
					}
				} else {
					reduceSubtypingConstraints(bounds, s_prime, t_prime);	
				}
			}
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
	
	private IJavaDeclaredType findCorrespondingSuperType(final IRNode decl, IJavaType s) {
		if (s instanceof IJavaDeclaredType) {
			IJavaDeclaredType ds = (IJavaDeclaredType) s;
			if (ds.getDeclaration().equals(decl)) {
				return ds;
			}
		}
		for(IJavaType st : s.getSupertypes(tEnv)) {
			IJavaDeclaredType rv = findCorrespondingSuperType(decl, st);
			if (rv != null) {
				return rv;
			}
		}
		return null;
	}

	private IJavaArrayType findMostSpecificArraySuperType(IJavaType s) {
		if (s instanceof IJavaArrayType) {
			return (IJavaArrayType) s;
		}
		else if (s instanceof IJavaDeclaredType) {
			return null; // TODO right?
		}
		// What other cases are there?
		throw new NotImplemented(); // TODO
	}

	/**
	 * A constraint formula of the form < S <= T >, where S and T are type arguments
	 * (Â§4.5.1), is reduced as follows:
	 * - If T is a type:
	 *   - If S is a type, the constraint reduces to < S = T >.
	 *   - If S is a wildcard, the constraint reduces to false.
	 *   
	 * - If T is a wildcard of the form ? , the constraint reduces to true.
	 * 
	 * - If T is a wildcard of the form ? extends T' :
	 *   - If S is a type, the constraint reduces to < S <: T' >.
	 *   - If S is a wildcard of the form ? , the constraint reduces to < Object <: T' >.
	 *   - If S is a wildcard of the form ? extends S' , the constraint reduces to < S' <: T' >.
	 *   - If S is a wildcard of the form ? super S' , the constraint reduces to < Object = T' >.
	 *   
	 * - If T is a wildcard of the form ? super T' :
	 *   - If S is a type, the constraint reduces to < T' <: S >.
	 *   - If S is a wildcard of the form ? super S' , the constraint reduces to < T' <: S' >.
	 *   - Otherwise, the constraint reduces to false.
	 */
	private void reduceTypeArgContainmentConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (t instanceof IJavaWildcardType) {
			IJavaWildcardType wt = (IJavaWildcardType) t;
			if (wt.getUpperBound() != null) { 
				// Case 3: ? extends X
				if (s instanceof IJavaWildcardType) {
					IJavaWildcardType ws = (IJavaWildcardType) s;
					if (ws.getUpperBound() != null) {
						reduceSubtypingConstraints(bounds, ws.getUpperBound(), wt.getUpperBound());
					}
					else if (ws.getLowerBound() != null) {
						reduceTypeEqualityConstraints(bounds, tEnv.getObjectType(), wt.getUpperBound());
					} else {
						reduceSubtypingConstraints(bounds, tEnv.getObjectType(), wt.getUpperBound());
					}
				} else {
					reduceSubtypingConstraints(bounds, s, wt.getUpperBound());
				}
			}
			else if (wt.getLowerBound() != null) { 
				// Case 4: ? super X
				if (s instanceof IJavaWildcardType) {
					IJavaWildcardType ws = (IJavaWildcardType) s;
					if (ws.getLowerBound() != null) {
						reduceSubtypingConstraints(bounds, wt.getLowerBound(), ws.getLowerBound());
					} else {
						bounds.addFalse();
					}
				} else {
					reduceSubtypingConstraints(bounds, wt.getLowerBound(), s);
					
				}
			}
			else {
				// Case 2: ?			
				bounds.addTrue();
			}
		}
		else if (t instanceof InferenceVariable) {
			// TODO HACK?
			if (s instanceof IJavaWildcardType) {
				// ? <= ?
				// ? extends U <= ? extends (? super U)
				// ? super U <= ? super (? extends U)
				/*
				IJavaWildcardType ws = (IJavaWildcardType) s;
				if (ws.getLowerBound() != null) {
					
				}
				*/
				reduceTypeEqualityConstraints(bounds, s, t);
			} else {
				reduceTypeEqualityConstraints(bounds, s, t);
			}
		}
		// Case 1
		else if (s instanceof IJavaWildcardType) {
			bounds.addFalse();
		} 
		else {
			reduceTypeEqualityConstraints(bounds, s, t);
		}
	}
	
	/**
	 * 18.2.4 Type Equality Constraints
	 * 
	 * A constraint formula of the form < S = T >, where S and T are types, is reduced as
	 * follows:
	 * 
	 * - If S and T are proper types, the constraint reduces to true if S is the same as T
	 *   (Â§4.3.4), and false otherwise.
	 *   
	 * - Otherwise, if S is an inference variable, α, the constraint reduces to the bound α = T .
	 * - Otherwise, if T is an inference variable, α, the constraint reduces to the bound S = α.
	 * 
	 * - Otherwise, if S and T are class or interface types with the same erasure, where S
	 *   has type arguments B 1 , ..., B n and T has type arguments A 1 , ..., A n , the constraint
	 *   reduces to the following new constraints: for all i (1 <= i <= n), < B i = A i >.
	 *   
	 * - Otherwise, if S and T are array types, S'[] and T'[] , the constraint reduces to < S' = T' >.
	 * - Otherwise, the constraint reduces to false.
	 */
	void reduceTypeEqualityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (s.isProperType() && t.isProperType()) {
			if (s.equals(t)) {
				bounds.addTrue();
			} else {
				bounds.addFalse();
			}
		}
		else if (isInferenceVariable(s) || isInferenceVariable(t)) {
			bounds.addEqualityBound(s, t);
		}
		else if (s instanceof IJavaDeclaredType && t instanceof IJavaDeclaredType) {
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
		else if (s instanceof IJavaArrayType && t instanceof IJavaArrayType) {
			IJavaArrayType sa = (IJavaArrayType) s;
			IJavaArrayType ta = (IJavaArrayType) t;
			reduceTypeEqualityConstraints(bounds, sa.getElementType(), ta.getElementType());
		}
		else {
			bounds.addFalse();
		}
	}
	
	/**
	 * A constraint formula of the form < S = T >, where S and T are type arguments (Â§4.5.1),
	 * is reduced as follows:
	 * 
	 * - If S and T are types, the constraint is reduced as described above.
	 * - If S has the form ? and T has the form ? , the constraint reduces to true.
	 * - If S has the form ? and T has the form ? extends T' , the constraint reduces to < Object = T' >.
	 * - If S has the form ? extends S' and T has the form ? , the constraint reduces to < S' = Object >.
	 * 
	 * - If S has the form ? extends S' and T has the form ? extends T' , the constraint
	 *   reduces to < S' = T' >.
	 *   
	 * - If S has the form ? super S' and T has the form ? super T' , the constraint reduces
	 *   to < S' = T' >.
	 *   
	 * - Otherwise, the constraint reduces to false.
	 * @param bounds 
	 */
	private void reduceTypeArgumentEqualityConstraints(BoundSet bounds, IJavaType s, IJavaType t) {
		if (!(s instanceof IJavaWildcardType) && !(t instanceof IJavaWildcardType)) {
			reduceTypeEqualityConstraints(bounds, s, t);
		}
		else if (s instanceof IJavaWildcardType && t instanceof IJavaWildcardType) {
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
	 * A constraint formula of the form <LambdaExpression -> throws T > is reduced as
	 * follows:
	 * 
	 * - If T is not a functional interface type (Â§9.8), the constraint reduces to false.
	 * 
	 * - Otherwise, let the target function type for the lambda expression be determined
	 *   as specified in Â§15.27.3. If no valid function type can be found, the constraint
	 *   reduces to false.
	 *   
	 * - Otherwise, if the lambda expression is implicitly typed, and one or more of the
	 *   function type's parameter types is not a proper type, the constraint reduces to
	 *   false.
	 *   
	 * - Otherwise, if the function type's return type is neither void nor a proper type,
	 *   the constraint reduces to false.
	 * 
	 * - Otherwise, let E 1 , ..., E n be the types in the function type's throws clause that are
	 *   not proper types. If the lambda expression is implicitly typed, let its parameter
	 *   types be the function type's parameter types. If the lambda body is a poly
	 *   expression or a block containing a poly result expression, let the targeted return
	 *   type be the function type's return type. Let X 1 , ..., X m be the checked exception
	 *   types that the lambda body can throw (Â§11.2). Then there are two cases:
	 *   
	 *   - If n = 0 (the function type's throws clause consists only of proper types), then
	 *     if there exists some i (1 <= i <= m) such that X i is not a subtype of any proper type
	 *     in the throws clause, the constraint reduces to false; otherwise, the constraint
	 *     reduces to true.
	 *     
	 *   - If n > 0 , the constraint reduces to a set of subtyping constraints: for all i (1 <=
	 *     i <= m), if X i is not a subtype of any proper type in the throws clause, then the
	 *     constraints include, for all j (1 <= j <= n), < X i <: E j >. In addition, for all j (1 <= j
	 *     <= n), the constraint reduces to the bound throws E j .
	 */
	private void reduceLambdaCheckedExceptionConstraints(BoundSet bounds, IRNode lambda, IJavaType t) {
		if (tEnv.isFunctionalType(t) == null) {
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
	 * A constraint formula of the form <MethodReference -> throws T > is reduced as
	 * follows:
	 * 
	 * - If T is not a functional interface type, or if T is a functional interface type but
	 *   does not have a function type (Â§9.9), the constraint reduces to false.
	 * 
	 * - Otherwise, let the target function type for the method reference expression be
	 *   the function type of T . If the method reference is inexact (Â§15.13.1) and one or
	 *   more of the function type's parameter types is not a proper type, the constraint
	 *   reduces to false.
	 *   
	 * - Otherwise, if the method reference is inexact and the function type's result is
	 *   neither void nor a proper type, the constraint reduces to false.
	 * 
	 * - Otherwise, let E 1 , ..., E n be the types in the function type's throws clause that
	 *   are not proper types. Let X 1 , ..., X m be the checked exceptions in the throws
	 *   clause of the invocation type of the method reference's compile-time declaration
	 *   (Â§15.13.2) (as derived from the function type's parameter types and return type).
	 *   Then there are two cases:
	 *   
	 *   - If n = 0 (the function type's throws clause consists only of proper types), then
	 *     if there exists some i (1 <= i <= m) such that X i is not a subtype of any proper type
	 *     in the throws clause, the constraint reduces to false; otherwise, the constraint
	 *     reduces to true.
	 *     
	 *   - If n > 0 , the constraint reduces to a set of subtyping constraints: for all i (1 <=
	 *     i <= m), if X i is not a subtype of any proper type in the throws clause, then the
	 *     constraints include, for all j (1 <= j <= n), < X i <: E j >. In addition, for all j (1 <= j
	 *     <= n), the constraint reduces to the bound throws E j .
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
	
	static class TypeSubstitution extends AbstractTypeSubstitution {
		final Map<? extends IJavaTypeFormal, ? extends IJavaType> subst;
		
		<T extends IJavaType> TypeSubstitution(IBinder b, Map<? extends IJavaTypeFormal, T> s) {
			super(b);
			subst = s;
		}
		
		@Override
		public IJavaType get(IJavaTypeFormal jtf) {
			IJavaType rv = subst.get(jtf);
			if (rv != null) {
				return rv;
			}
			return jtf;
		}
		@Override
		protected <V> V process(IJavaTypeFormal jtf, Process<V> processor) {
			throw new UnsupportedOperationException();
		}		
	}
}
