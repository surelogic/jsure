package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.common.util.Iteratable;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.bind.MethodBinder.CallState;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Implements overload resolution according to JLS sec 15.12.2 for Java 8
 * 
 * @author edwin
 */
public class MethodBinder8 {
	private final boolean debug;
	private final AbstractJavaBinder binder;
	private final ITypeEnvironment typeEnvironment;
	
	MethodBinder8(AbstractJavaBinder b, boolean debug) {
		binder = b;
		typeEnvironment = b.getTypeEnvironment();
		this.debug = debug;
	}	

    BindingInfo findBestMethod(final IJavaScope scope, final LookupContext context, final boolean needMethod, final IRNode from, final CallState call) {
        final IJavaScope.Selector isAccessible = MethodBinder.makeAccessSelector(typeEnvironment, from);
        final Iterable<IBinding> methods = new Iterable<IBinding>() {
  			public Iterator<IBinding> iterator() {
  				return IJavaScope.Util.lookupCallable(scope, context, isAccessible, needMethod);
  			}
        };
        final Set<IBinding> applicable = new HashSet<IBinding>();
        for(IBinding mb : methods) {
        	if (isPotentiallyApplicable(call, from, mb)) {
        		applicable.add(mb);
        	}
        }
        IBinding rv = findMostSpecific(call, applicable, STRICT_INVOCATION);
        if (rv == null) {
        	rv = findMostSpecific(call, methods, LOOSE_INVOCATION);
        	if (rv == null) {
        		rv = findMostSpecific(call, methods, VARIABLE_ARITY_INVOCATION);
        	}
        }
        // TODO
        return null;
    }

	private static int numChildren(IRNode n) {
    	return JJNode.tree.numChildren(n);
    }
    
    private Iteratable<IRNode> children(IRNode n) {
    	return JJNode.tree.children(n);
    }
    
    private IRNode getChild(IRNode n, int i) {
    	return JJNode.tree.getChild(n, i);
    }
    
    /*
    15.12.2.1 Identify Potentially Applicable Methods [Modified]

 	A member method is potentially applicable to a method invocation if and only if all of the following are true:

      * The name of the member is identical to the name of the method in the method invocation.
      * The member is accessible (6.6) to the class or interface in which the method invocation appears.

   		    Whether a member method is accessible at a method invocation depends on the access modifier 
   		    (public, none, protected, or private) in the member's declaration and on where the method invocation appears.
   		    
 	  * If the member is a fixed arity method with arity n, the arity of the method invocation is equal to n, and 
 	    for all i, 1 ≤ i ≤ n, the ith argument of the method invocation is potentially compatible, as defined below, 
 	    with the type of the ith parameter of the method.
 	    
      * If the member is a variable arity method with arity n, then for all i, 1 ≤ i ≤ n-1, the ith argument 
        of the method invocation is potentially compatible with the type of the ith parameter of the method; 
        and, where the nth parameter of the method has type T[], one of the following is true:
  
    	- The arity of the method invocation is equal to n-1.
    	- The arity of the method invocation is equal to n, and the nth argument of the method invocation is 
    	  potentially compatible with either T or T[].
    	- The arity of the method invocation is m, where m > n, and for all i, n ≤ i ≤ m, the ith argument 
    	  of the method invocation is potentially compatible with T. 
    	  
      * If the method invocation includes explicit type arguments, and the member is a generic method, 
        then the number of type arguments is equal to the number of type parameters of the method.
    */
    private boolean isPotentiallyApplicable(CallState call, IRNode from, IBinding mb) {
    	final IRNode params;
		final IRNode typeParams;
    	// Check name -- probably redundant
    	if (call.constructorType != null) {
    		if (!ConstructorDeclaration.prototype.includes(mb.getNode())) {
    			return false;
    		}
    		params = ConstructorDeclaration.getParams(mb.getNode());
    		typeParams = ConstructorDeclaration.getTypes(mb.getNode());
    	} else {
    		final String callName = MethodCall.getMethod(call.call);
    		final String methodName = MethodDeclaration.getId(mb.getNode());
    		if (!callName.equals(methodName)) {
    			return false;
    		}
    		params = MethodDeclaration.getParams(mb.getNode());
    		typeParams = MethodDeclaration.getTypes(mb.getNode());
    	}
    	// Check accessibility -- probably redundant
    	if (!BindUtil.isAccessible(typeEnvironment, mb.getNode(), from)) {
    		return false;
    	}
    	// Check varargs/arity -> parameters
    	final int numParams = numChildren(params);
    	final int numArgs = call.args.length;
    	final IRNode lastParam;
    	if (numParams > 0 && VarArgsType.prototype.includes(ParameterDeclaration.getType(lastParam = getChild(params, numParams-1)))) {
    		// varargs
    		if (numArgs < numParams-1 || 
    			!arePotentiallyCompatible(mb, numParams-1, children(params), call.args)) {
    			return false;
    		}
    		// check the varargs
    		if (numArgs >= numParams) {
    			final IJavaArrayType at = (IJavaArrayType) getParamType(lastParam);
    			final IJavaType t = at.getElementType();
    			if (numArgs == numParams) {
    				final IRNode e = call.args[numParams-1];
    				if (!isPotentiallyCompatible(mb, e, at) && !isPotentiallyCompatible(mb, e, t)) {
    					return false;
    				}
    			} else if (!areTheRestPotentiallyCompatible(mb, numParams, t, call.args)) {
    				return false;    				
    			}
    		}
    	} else { // no varargs
    		if (numParams != numArgs || 
    			!arePotentiallyCompatible(mb, numParams, children(params), call.args)) {
    			return false;
    		}
    	}
    	
    	// Check type arguments (if any)
    	final int numTypeArgs = call.getNumTypeArgs();
    	final int numTypeParams = numChildren(typeParams);
    	if (numTypeArgs != numTypeParams) {
    		return false;
    	}    	
    	return true;
    }
    
    private IJavaType getParamType(IRNode param) {
		final IRNode type = ParameterDeclaration.getType(param);
		final IJavaType t = binder.getJavaType(type);
		return t;
    }
    
    /**
     * check the first N parameters/arguments
     */
    private boolean arePotentiallyCompatible(IBinding mb, final int limit, final Iterable<IRNode> params, final IRNode[] args) {
    	int i = 0;
    	for(IRNode param : params) {
    		if (i >= args.length) {
    			return false;
    		}
    		final IJavaType t = getParamType(param);
    		if (!isPotentiallyCompatible(mb, args[i], t)) {
    			return false;
    		}
    		i++;
    		if (i >= limit) {
    			return true;
    		}
    	}
		
    	return false;
	}

    /**
     * check the arguments against T after the first N
     */
    private boolean areTheRestPotentiallyCompatible(IBinding mb, final int start, final IJavaType t, final IRNode[] args) {
    	int i = 0;
    	for(IRNode arg : args) { 		
    		if (i++ < start) {
    			continue;
    		}
    		if (!isPotentiallyCompatible(mb, arg, t)) {
    			return false;
    		}
    	}
		return true;
	}
    
	/*
      An expression is potentially compatible with a target type according to the following rules:

    * A lambda expression (15.27) is potentially compatible with a function type (9.8) if all of the following are true:
      - The arity of the targeted functional interface's function descriptor is the same as the arity of the lambda expression.
      - If the functional interface's function descriptor has a void return, then the lambda body is either a statement expression (14.8) 
        or a void-compatible block (15.27.2).
      - If the functional interface's function descriptor has a (non-void) return type, then the lambda body is either an expression 
        or a value-compatible block (15.27.2). 
        
    * A method reference (15.28) is potentially compatible with a function type if, based on the method searches described in 15.28.1, 
      there exists at least one potentially-applicable compile-time declaration for the method reference.

    Where the method reference has the form ReferenceType :: NonWildTypeArgumentsopt Identifier and the target functional interface 
    has one or more parameters, two searches are performed, regardless of the type of the first parameter.

    * A lambda expression or a method reference is potentially compatible with a type variable if the type variable is a type parameter of the candidate method.
    * A parenthesized expression (15.8.5) is potentially compatible with a type if its contained expression is potentially compatible with that type.
    * A poly conditional expression (15.25.3) is potentially compatible with a type if each of its second and third operand expressions 
      are potentially compatible with that type.
    * A class instance creation expression (15.9), a method invocation expression, or an expression of a standalone form (15.2) is 
      potentially compatible with any type. 
     */
    private boolean isPotentiallyCompatible(IBinding mb, IRNode e, IJavaType t) {
    	final Operator op = JJNode.tree.getOperator(e);
    	if (LambdaExpression.prototype.includes(op)) {
    		if (t instanceof IJavaTypeFormal) {
    			return declaresTypeParam(MethodDeclaration.getTypes(mb.getNode()), (IJavaTypeFormal) t);
    		}
    		else if (t instanceof IJavaFunctionType) {
    			final IJavaFunctionType ft = (IJavaFunctionType) t;
    			if (ft.getParameterTypes().size() == numChildren(LambdaExpression.getParams(e))) {
    				if (ft.getReturnType() == JavaTypeFactory.voidType) {
    					return isVoidCompatible(LambdaExpression.getBody(e));
    				} else {
    					return isReturnCompatible(LambdaExpression.getBody(e));
    				}
    			}    		
    		}
    		return false;
    	}
    	else if (MethodReference.prototype.includes(op)) {
    		if (t instanceof IJavaTypeFormal) {
    			return declaresTypeParam(MethodDeclaration.getTypes(mb.getNode()), (IJavaTypeFormal) t);
    		}
    		// TODO how to figure out if there are decls?
    		return false;
    	}
    	else if (ConstructorReference.prototype.includes(op)) {
    		if (t instanceof IJavaTypeFormal) {
    			return declaresTypeParam(ConstructorDeclaration.getTypes(mb.getNode()), (IJavaTypeFormal) t);
    		}
    		// TODO how to figure out if there are decls?  		
    		return false;
    	}  
    	else if (ParenExpression.prototype.includes(op)) {
    		return isPotentiallyCompatible(mb, ParenExpression.getOp(e), t);
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		return !isPolyExpression(e) || 
    		       (isPotentiallyCompatible(mb, ConditionalExpression.getIftrue(e), t) && 
    		        isPotentiallyCompatible(mb, ConditionalExpression.getIffalse(e), t));
    	}
    	else if (MethodCall.prototype.includes(op)) {    			  
    		return true;
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		return true;
    	}
    	return !isPolyExpression(e);
    }    

	/**
     * Assumes that the code compiles
     */
    private boolean isVoidCompatible(IRNode lambdaBody) {
    	if (StatementExpression.prototype.includes(lambdaBody)) {
    		return true;
    	}
    	// Look for a return statement
    	for(IRNode n : JJNode.tree.topDown(lambdaBody)) {
        	final Operator op = JJNode.tree.getOperator(n);
        	if (ReturnStatement.prototype.includes(op)) {
        		return false;
        	}
          	else if (VoidReturnStatement.prototype.includes(op)) {
        		return true;
        	}
    	}
		return true;
	}
    
	/**
     * Assumes that the code compiles
     */
    private boolean isReturnCompatible(IRNode lambdaBody) {
    	if (Expression.prototype.includes(lambdaBody)) {
    		return true;
    	}
    	// Look for a void return statement
    	for(IRNode n : JJNode.tree.topDown(lambdaBody)) {
        	final Operator op = JJNode.tree.getOperator(n);
        	if (ReturnStatement.prototype.includes(op)) {
        		return true;
        	}
        	else if (VoidReturnStatement.prototype.includes(op)) {
        		return false;
        	}
    	}
		return false;
	}

	private boolean declaresTypeParam(IRNode types, IJavaTypeFormal t) {
		for(IRNode f : children(types)) {
			if (f.equals(t.getDeclaration())) {
				return true;
			}
		}
		return false;
	}

	/**
     * according to JLS sec 15.2 for Java 8
     * 
       The following forms of expressions may be poly expressions:

       Parenthesized expressions (15.8.5)
       Class instance creation expressions (15.9)
       Method invocation expressions (15.12)
       Conditional operator expressions (15.25)
       Lambda expressions (15.27)
       Method references (15.28)
     */
    private boolean isPolyExpression(final IRNode e) {
    	final Operator op = JJNode.tree.getOperator(e);
    	if (MethodCall.prototype.includes(op)) {
    		//  A method invocation expression is a poly expression if all of the following are true:

    		// The invocation appears in an assignment context (5.2) or an invocation context (5.3).
    	    // The invocation elides NonWildTypeArguments.
    	    // Per the following sections, the method to be invoked is a generic method (8.4.4).
    	    // The return type of the method to be invoked mentions at least one of the method's type parameters. 

    		// Otherwise, the method invocation expression is a standalone expression.    		
    		if (isInAssignmentOrInvocationContext(e) &&
    			numChildren(MethodCall.getTypeArgs(e)) == 0) {
    			final IBinding mb = binder.getIBinding(e);
    			IRNode typeParams = MethodDeclaration.getTypes(mb.getNode());
    			if (numChildren(typeParams) > 0) {
    				return refersToTypeParams(MethodDeclaration.getReturnType(mb.getNode()), typeParams);
    			}
    		}    		
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return isPolyExpression(ParenExpression.getOp(e));
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		// A class instance creation expression is a poly expression (15.2) 
    		// if i) it uses a diamond '<>' in place of type arguments, and 
    		// ii) it appears in an assignment context (5.2) or an invocation context (5.3). 
    		// Otherwise, it is a standalone expression.  		
    		IRNode typeArgs = NewExpression.getTypeArgs(e);
    		return numChildren(typeArgs) == 0 && isInAssignmentOrInvocationContext(e);
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		// 15.25.1 Boolean Conditional Expressions [New]
    		//
    		//   Boolean conditional expressions are standalone expressions (15.2).
    		//
    		// 15.25.2 Numeric Conditional Expressions [New]
    		//
    		//   Numeric conditional expressions are standalone expressions (15.2).
    		// 
    		// 15.25.3 Reference Conditional Expressions [New]
    		// 
    		//   A reference conditional expression is a poly expression if it appears in an 
    		//   assignment context (5.2) or an invocation context (5.3). Otherwise, it is a standalone expression.
    		if (classifyCondExpr(e) == ExpressionKind.REF) {
    			return isInAssignmentOrInvocationContext(e);
    		}
    	}
    	else if (LambdaExpression.prototype.includes(op) || 
    			 MethodReference.prototype.includes(op) ||
    			 ConstructorReference.prototype.includes(op)) {
    		return true;
    	}    	
    	return false;
    }

    /**
     * An conservative approximation of JLS 15.2 for the purposes of determining granule boundaries
     */
    public static boolean couldBePolyExpression(IRNode e) {
    	final Operator op = JJNode.tree.getOperator(e);
    	if (MethodCall.prototype.includes(op)) {
    		//  A method invocation expression is a poly expression if all of the following are true:

    		// The invocation appears in an assignment context (5.2) or an invocation context (5.3).
    	    // The invocation elides NonWildTypeArguments.
    	    // Per the following sections, the method to be invoked is a generic method (8.4.4).
    	    // The return type of the method to be invoked mentions at least one of the method's type parameters. 

    		// Otherwise, the method invocation expression is a standalone expression.    		
    		if (isInAssignmentOrInvocationContext(e) &&
    			numChildren(MethodCall.getTypeArgs(e)) == 0) {
    			return true;
    			/*
    			final IBinding mb = binder.getIBinding(e);
    			IRNode typeParams = MethodDeclaration.getTypes(mb.getNode());
    			if (numChildren(typeParams) > 0) {
    				return refersToTypeParams(MethodDeclaration.getReturnType(mb.getNode()), typeParams);
    			}
    			*/
    		}    		
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return couldBePolyExpression(ParenExpression.getOp(e));
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		// A class instance creation expression is a poly expression (15.2) 
    		// if i) it uses a diamond '<>' in place of type arguments, and 
    		// ii) it appears in an assignment context (5.2) or an invocation context (5.3). 
    		// Otherwise, it is a standalone expression.  		
    		IRNode typeArgs = NewExpression.getTypeArgs(e);    		
    		return typeArgs != null && numChildren(typeArgs) == 0 && isInAssignmentOrInvocationContext(e);
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		// 15.25.1 Boolean Conditional Expressions [New]
    		//
    		//   Boolean conditional expressions are standalone expressions (15.2).
    		//
    		// 15.25.2 Numeric Conditional Expressions [New]
    		//
    		//   Numeric conditional expressions are standalone expressions (15.2).
    		// 
    		// 15.25.3 Reference Conditional Expressions [New]
    		// 
    		//   A reference conditional expression is a poly expression if it appears in an 
    		//   assignment context (5.2) or an invocation context (5.3). Otherwise, it is a standalone expression.
    		if (true) { //classifyCondExpr(e) == ExpressionKind.REF) {
    			return isInAssignmentOrInvocationContext(e);
    		}
    	}
    	else if (LambdaExpression.prototype.includes(op) || 
    			 MethodReference.prototype.includes(op) ||
    			 ConstructorReference.prototype.includes(op)) {
    		return true;
    	}    	
    	return false;    	
    }
    
	enum ExpressionKind {
    	BOOLEAN, NUMERIC, REF 
    }
    
	/*
	 * 15.25 Conditional Operator ? :
	 * 
	 * If both the second and the third operand expressions are boolean expressions, the conditional expression is a boolean conditional. 
     * Similarly, if both the second and the third operand expressions are numeric expressions, the conditional expression is a numeric conditional. 
     * Otherwise, the conditional expression is a reference conditional.
	 */
    private ExpressionKind classifyCondExpr(IRNode e) {
    	ExpressionKind second = classifyExpression(ConditionalExpression.getIftrue(e));
    	ExpressionKind third = classifyExpression(ConditionalExpression.getIffalse(e));
    	if (second == third) {
    		return second;
    	}
		return ExpressionKind.REF; // Mixed
	}
    
    /*
     * For the purpose of classifying a conditional, the following expressions are boolean expressions:
     * 
     *   An expression of a standalone form (15.2) that has type boolean or Boolean.
     *   A parenthesized (15.8.5) boolean expression.
     *   A class instance creation expression (15.9) for class Boolean.
     *   A method invocation expression (15.12) for which the chosen most-specific method (15.12.2.5) has return type boolean or Boolean.
     *   (Note that, for a generic method, this is the type before instantiating the method's type arguments.)
     *   A boolean conditional expression. 
     * 
     * For the purpose of classifying a conditional, the following expressions are numeric expressions:
     * 
     *   An expression of a standalone form (15.2) with a type that is convertible to a numeric type (4.2, 5.1.8).
     *   A parenthesized (15.8.5) numeric expression.
     *   A class instance creation expression (15.9) for a class that is convertible to a numeric type.
     *   A method invocation expression (15.12) for which the chosen most-specific method (15.12.2.5) has a return type that is convertible to a numeric type.
     *   (Note that, for a generic method, this is the type before instantiating the method's type arguments.)
     *   A numeric conditional expression. 
     */
    private ExpressionKind classifyExpression(IRNode e) {
    	final Operator op = JJNode.tree.getOperator(e);    	
    	if (MethodCall.prototype.includes(op)) {
			final IBinding mb = binder.getIBinding(e);
			IRNode rtype = MethodDeclaration.getReturnType(mb.getNode());
			return classifyType(binder.getJavaType(rtype));
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return classifyExpression(ParenExpression.getOp(e));
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		IRNode t = NewExpression.getType(e);
    		return classifyType(binder.getJavaType(t));
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		return classifyCondExpr(e);
    	}
    	if (!isPolyExpression(e)) {
    		return classifyType(binder.getJavaType(e));
    	}
		return ExpressionKind.REF;
	}
	   
    private ExpressionKind classifyType(final IJavaType t) {
    	if (t instanceof IJavaPrimitiveType) {
    		return (t == JavaTypeFactory.booleanType) ? ExpressionKind.BOOLEAN : ExpressionKind.NUMERIC;
    	}
    	if (t instanceof IJavaDeclaredType) {
    		IJavaDeclaredType dt = (IJavaDeclaredType) t;
    		IJavaPrimitiveType pt = JavaTypeFactory.getCorrespondingPrimType(dt);
    		if (pt != null) {
    			// Same as above
    			return (pt == JavaTypeFactory.booleanType) ? ExpressionKind.BOOLEAN : ExpressionKind.NUMERIC; 
    		}
    	}
    	return ExpressionKind.REF;
    }
    
    /**
     * @return true if type refers to one of the type parameters
     */
    private static boolean refersToTypeParams(IRNode type, IRNode typeParams) {
    	//final IJavaType t = binder.getJavaType(type);
    	//HACK to lookup the names myself?
    	final Set<String> formals = new HashSet<String>();
    	for(final IRNode f : TypeFormals.getTypeIterator(typeParams)) {
    		formals.add(TypeFormal.getId(f));
    	}
    	for(final IRNode n : JJNode.tree.bottomUp(type)) {
    		final Operator op = JJNode.tree.getOperator(n);
    		if (NamedType.prototype.includes(op) || NameType.prototype.includes(op)) {
    			final String name = JJNode.getInfo(n);
    			if (formals.contains(name)) {
    				return true;
    			}
    		}
    	}
		return false;
	}

    enum ConversionContextKind {
    	ASSIGNMENT, INVOCATION, STRING, CASTING, NUMERIC, UNKNOWN;
    }
    
    // JLS 5
    private static ConversionContextKind getConversionContext(IRNode e) {
		final IRNode parent = JJNode.tree.getParent(e);
		final Operator pop = JJNode.tree.getOperator(parent);
		if (Arguments.prototype.includes(pop)) {
			return ConversionContextKind.INVOCATION;
		}
		else if (AssignmentExpression.prototype.includes(pop)) {
			return ConversionContextKind.ASSIGNMENT;
		}
		else if (StringConcat.prototype.includes(pop)) {
			return ConversionContextKind.STRING;
		}
		else if (CastExpression.prototype.includes(pop)) {
			return ConversionContextKind.CASTING;			
		}
		else if (pop instanceof ArithExpression) {
			return ConversionContextKind.NUMERIC;
		}
		// TODO what cases am I missing?
    	return ConversionContextKind.UNKNOWN;
    }
        
    private static boolean isInAssignmentOrInvocationContext(IRNode e) {    	
    	return getConversionContext(e).ordinal() <= ConversionContextKind.INVOCATION.ordinal();
    }    
        
    interface ApplicableMethodFilter {
    	boolean isApplicable(CallState call, IBinding mb);
    }
    /*
     *  15.12.2.2 Phase 1: Identify Matching Arity Methods Applicable by Strict Invocation [Modified]
     *  
     *  Let m be a potentially applicable method (15.12.2.1), let e1, ..., en be the actual argument expressions of 
     *  the method invocation, and let F1, ..., Fn be the types of the formal parameters of m. Then:
     *  
     *  - If m is a generic method and the method invocation does not provide explicit type arguments, then the 
     *    applicability of the method is inferred as described in 18.5.1.
     *  
     *  - Otherwise, if m is a generic method, then let R1, ..., Rp (p ≥ 1) be the type parameters of m, 
     *    let Bl be the declared bound of Rl (1 ≤ l ≤ p), and let U1, ..., Up be the explicit type arguments given in the method invocation. 
     *    Then m is applicable by strict invocation if:
     *  
     *      For 1 ≤ i ≤ n, ei is compatible in a strict invocation context with Fi[R1:=U1, ..., Rp:=Up].
     *      For 1 ≤ l ≤ p, Ul <: Bl[R1:=U1, ..., Rp:=Up]. 
     *    
     *  - Otherwise, m is applicable by strict invocation if, for 1 ≤ i ≤ n, ei is compatible in a strict invocation context with Fi. 
     *  
     *  If no method applicable by strict invocation is found, the search for applicable methods continues with phase 2 (15.12.2.3).
     *  
     *  Otherwise, the most specific method (15.12.2.5) is chosen among the methods that are applicable by strict invocation.
     */    
    private final ApplicableMethodFilter STRICT_INVOCATION = new ApplicableMethodFilter() {
		public boolean isApplicable(CallState call, IBinding mb) {
			// TODO Auto-generated method stub
			return false;
		}
	};
    
	/*
	 * As above, but applicable by loose invocation
	 */
	private final ApplicableMethodFilter LOOSE_INVOCATION = new ApplicableMethodFilter() {
		public boolean isApplicable(CallState call, IBinding mb) {
			// TODO Auto-generated method stub
			return false;
		}
	};

	/*
	 * 15.12.2.4 Phase 3: Identify Methods Applicable by Variable Arity Invocation [Modified]
	 * 
	 * Where a variable-arity method has formal parameter types F1, ..., Fn-1, Fn[], 
	 * define the ith variable-arity parameter type of the method as follows:
	 * 
	 *   For i ≤ n-1, the ith variable-arity parameter type is Fi.
	 *   For i ≥ n, the ith variable-arity parameter type is Fn. 
	 *   
	 * Let m be a potentially applicable method (15.12.2.1) with variable arity, let e1, ..., ek be 
	 * the actual argument expressions of the method invocation and let T1, ..., Tk be first k 
	 * variable-arity parameter types of m. Then:
	 * 
	 * - If m is a generic method and the method invocation does not provide explicit type arguments, 
	 *   then the applicability of the method is inferred as described in 18.5.1.
	 *   
	 * - Otherwise, if m is a generic method, then let R1, ..., Rp (p ≥ 1) be the type parameters of m, 
	 *   let Bl be the declared bound of Rl (1 ≤ l ≤ p), and let U1, ..., Up be the explicit type arguments given in the method invocation. 
	 *   Then m is an applicable variable-arity method if:
	 *     For 1 ≤ i ≤ k, ei is compatible in a loose invocation context with Ti[R1:=U1, ..., Rp:=Up].
	 *     Where the last formal parameter type of m is Fn[], the type which is the erasure of Fn[R1:=U1, ..., Rp:=Up] is accessible at the point of invocation.
	 *     For 1 ≤ l ≤ p, Ul <: Bl[R1:=U1, ..., Rp:=Up]. 
	 *     
     * - Otherwise, m is applicable by variable-arity invocation if:
     *     For 1 ≤ i ≤ k, ei is compatible in a loose invocation context with Ti.
     *     Where the last formal parameter type of m is Fn[], the type which is the erasure of Fn is accessible at the point of invocation. 
     *     
     * ...
	 */
	private final ApplicableMethodFilter VARIABLE_ARITY_INVOCATION = new ApplicableMethodFilter() {
		public boolean isApplicable(CallState call, IBinding mb) {
			// TODO Auto-generated method stub
			return false;
		}
	};
	
	/*
	 * 15.12.2.5 Choosing the Most Specific Method [Modified]
	 * 
	 * The informal intuition is that one method is more specific than another if any invocation handled by the first method
	 * could be passed on to the other one without a compile-time error. In cases such as a lambda expression argument (15.27) 
	 * or a variable-arity invocation (15.12.2.4), some flexibility is allowed to adapt one signature to the other. 
	 * 
	 * One applicable method m1 is more specific than another applicable method m2, for an invocation with argument 
	 * expressions exp1, ..., expk, if both of the following are true:
	 * 
	 * - If the invocation does not provide type arguments, it is not the case that either m1 or m2 is generic and was 
	 *   determined to be only provisionally applicable by 18.5.1.
	 * - Either:
     *   - m2 is generic and m1 is inferred to be more specific than m2 for argument expressions exp1, ..., expk by 18.5.4.
     *   - m2 is not generic, m1 and m2 are applicable by strict or loose invocation, and where m1 has parameter types T1, ..., Tn 
     *     and m2 has parameter types S1, ..., Sn, for all i (1 ≤ i ≤ n), the type Ti is more specific than Si for argument expi.
     *   - m2 is not generic, m1 and m2 are applicable by variable arity invocation, and where the first k variable-arity parameter
     *     types of m1 are T1, ..., Tk and the first k variable-arity parameter types of m2 are S1, ..., Sk, for all i (1 ≤ i ≤ k), 
     *     the type Ti is more specific than Si for argument expi. 
     *     
     * The above conditions are the only circumstances under which one method may be more specific than another.
	 */
    private IBinding findMostSpecific(final CallState call, Iterable<IBinding> methods, ApplicableMethodFilter filter) {
    	final Set<IBinding> applicable = new HashSet<IBinding>();
    	for(IBinding mb : methods) {
    		if (filter.isApplicable(call, mb)) {
    			applicable.add(mb);
    		}
    	}
    	if (applicable.isEmpty()) {
    		return null;
    	}
    	IBinding rv = null;    	
		return rv;
	}
    
    /*
     * A type T is more specific than a type S for an expression exp according to the following rules:
     *  
     * - T is more specific than S for any expression if T <: S.
     * - T is more specific than S for a standalone expression (15.2) of a primitive type if T is a primitive type and S is a reference type.
     * - T is more specific than S for a standalone expression of a reference type if T is a reference type and S is a primitive type.
     * - T is more specific than S for a parenthesized expression (15.8.5) if T is more specific than S for the contained expression.
     * - T is more specific than S for a poly conditional expression (15.25.3) if T is more specific than S for each of the second and 
     *   third operand expressions.
     * - T is more specific than S for a lambda expression (15.27) if all of the following are true:
     *   - T and S are function types (9.8).
     *   - The functional interface named by T is neither a subinterface nor a superinterface of S.
     *   - If the lambda expression's parameters have inferred types, then the descriptor parameter types of T are the 
     *     same as the descriptor parameter types of S.
     *   - Either i) the descriptor return type of S is void, or 
     *           ii) for all result expressions in the lambda body (or for the body itself if the body is an expression), 
     *               the descriptor return type of the capture of T is more specific than the descriptor return type of S. 
     * - T is more specific than S for a method reference expression (15.28) if all of the following are true:
     *   - T and S are function types.
     *   - The functional interface named by T is neither a subinterface nor a superinterface of S.
     *   - The descriptor parameter types of T are the same as the descriptor parameter types of S.
     *   - Either i) the descriptor return type of S is void, or 
     *           ii) the descriptor return type of the capture of T is more specific than the descriptor return type of S 
     *               for an invocation expression of the same form as the method reference. 
     * - T is more specific than S for a poly method invocation expression (15.12) or a poly class instance creation expression (15.9) 
     *   if T is a reference type and S is a primitive type.
     */
    private boolean isMoreSpecific(IJavaType t, IJavaType s, IRNode contextExpr) {
    	if (t.isSubtype(typeEnvironment, s)) {
    		return true;
    	}
    	if (!isPolyExpression(contextExpr)) {
    		IJavaType exprType = binder.getJavaType(contextExpr);
    		if (exprType instanceof IJavaPrimitiveType) {
    			return (t instanceof IJavaPrimitiveType) && (s instanceof IJavaReferenceType);
    		} 
 			return (t instanceof IJavaReferenceType) && (s instanceof IJavaPrimitiveType); 
    	}
    	final Operator op = JJNode.tree.getOperator(contextExpr);
    	if (MethodCall.prototype.includes(op) || NewExpression.prototype.includes(op)) {
			return (t instanceof IJavaReferenceType) && (s instanceof IJavaPrimitiveType); 
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return isMoreSpecific(t, s, ParenExpression.getOp(contextExpr));
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		return isMoreSpecific(t, s, ConditionalExpression.getIftrue(contextExpr)) &&
   				   isMoreSpecific(t, s, ConditionalExpression.getIffalse(contextExpr));
    	}
    	else if (LambdaExpression.prototype.includes(op)) {
    		// TODO
    	}
    	else if (MethodReference.prototype.includes(op)) {
    		// TODO
    	}
    	else if (ConstructorReference.prototype.includes(op)) {
    		// TODO
    	}
    	return false;
    }
}
