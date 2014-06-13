package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.common.util.EmptyIterator;
import com.surelogic.common.util.Iteratable;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Implements overload resolution according to JLS sec 15.12.2 for Java 8
 * 
 * @author edwin
 */
public class MethodBinder8 implements IMethodBinder {
	private final boolean debug;
	final AbstractJavaBinder binder;
	final ITypeEnvironment tEnv;
	final TypeInference8 typeInfer;
	
	MethodBinder8(AbstractJavaBinder b, boolean debug) {
		tEnv = b.getTypeEnvironment();
		binder = b;
		this.debug = debug;
		typeInfer = new TypeInference8(this);
	}	
	
    public BindingInfo findBestMethod(final IJavaScope scope, final LookupContext context, final boolean needMethod, final IRNode from, final CallState call) {
        final IJavaScope.Selector isAccessible = MethodBinder.makeAccessSelector(tEnv, from);
        final Iterable<IBinding> methods = new Iterable<IBinding>() {
  			public Iterator<IBinding> iterator() {
  				return IJavaScope.Util.lookupCallable(scope, context, isAccessible, needMethod);
  			}
        };
        final Set<MethodBinding> applicable = new HashSet<MethodBinding>();
        for(IBinding mb : methods) {
        	if (isPotentiallyApplicable(call, from, mb)) {
        		applicable.add(new MethodBinding(mb));
        	}
        }
        IBinding rv = findMostSpecific(call, applicable, STRICT_INVOCATION);
        if (rv == null) {
        	rv = findMostSpecific(call, applicable, LOOSE_INVOCATION);
        	if (rv == null) {
        		rv = findMostSpecific(call, applicable, VARIABLE_ARITY_INVOCATION);
        	}
        }
        // TODO is this right?
        return new BindingInfo(rv, 0, false);
    }

	private static int numChildren(IRNode n) {
		if (n == null) {
			return 0;
		}
    	return JJNode.tree.numChildren(n);
    }
    
    private Iteratable<IRNode> children(IRNode n) {
    	if (n == null) {
    		return EmptyIterator.prototype();
    	}
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
    	if (call.constructorType != null || ConstructorCall.prototype.includes(call.call)) {
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
    	if (!BindUtil.isAccessible(tEnv, mb.getNode(), from)) {
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
    	if (numTypeArgs != numTypeParams && numTypeArgs > 0) {
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
    	if (limit == 0) {
    		return true;
    	}
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
		
    	return (i >= limit);
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
			throw new NotImplemented();
    	}
    	else if (ConstructorReference.prototype.includes(op)) {
    		if (t instanceof IJavaTypeFormal) {
    			return declaresTypeParam(ConstructorDeclaration.getTypes(mb.getNode()), (IJavaTypeFormal) t);
    		}
    		// TODO how to figure out if there are decls?  		
			throw new NotImplemented();
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
    		/*
    		if (isInAssignmentOrInvocationContext(e) &&
    			numChildren(MethodCall.getTypeArgs(e)) == 0) {
    			
    			final IBinding mb = binder.getIBinding(e);
    			IRNode typeParams = MethodDeclaration.getTypes(mb.getNode());
    			if (numChildren(typeParams) > 0) {
    				return refersToTypeParams(MethodDeclaration.getReturnType(mb.getNode()), typeParams);
    			}    			
    		}    		
    		*/
    		return false; // Not relevant to lambda purposes
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return couldBePolyExpression(ParenExpression.getOp(e));
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		// A class instance creation expression is a poly expression (15.2) 
    		// if i) it uses a diamond '<>' in place of type arguments, and 
    		// ii) it appears in an assignment context (5.2) or an invocation context (5.3). 
    		// Otherwise, it is a standalone expression.  	
    		/*
    		IRNode typeArgs = NewExpression.getTypeArgs(e);    		
    		return typeArgs != null && numChildren(typeArgs) == 0 && isInAssignmentOrInvocationContext(e);
    		*/
    		return false; // Not relevant to lambda purposes
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
    	boolean isApplicable(CallState call, MethodBinding mb);
    	boolean usesVarargs();
    	InvocationKind getKind();
    }
    
    interface ArgCompatibilityContext {
    	boolean isCompatible(IRNode param, IJavaType pType, IRNode arg, IJavaType argType);
    }
	
    final ArgCompatibilityContext STRICT_INVOCATION_CONTEXT = new ArgCompatibilityContext() {
    	public boolean isCompatible(IRNode param, IJavaType pType, IRNode arg, IJavaType argType) {
    		// TODO
    		return tEnv.isCallCompatible(pType, argType);
    	}
	}; 
	
    final ArgCompatibilityContext LOOSE_INVOCATION_CONTEXT = new ArgCompatibilityContext() {
    	public boolean isCompatible(IRNode param, IJavaType pType, IRNode arg, IJavaType argType) {
    		// TODO handle boxing and unboxing
    		return tEnv.isCallCompatible(pType, argType);
    	}
	}; 
    
    /*
     *  15.12.2.2 Phase 1: Identify Matching Arity Methods Applicable by Strict Invocation 
     *  
     * Let m be a potentially applicable method (§15.12.2.1) with arity n and formal
     * parameter types F 1 ... F n , and let e 1 , ..., e n be the actual argument expressions of
     * the method invocation. Then:
     * • If m is a generic method and the method invocation does not provide explicit type
     *   arguments, then the applicability of the method is inferred as specified in §18.5.1.
     *   
     * • If m is a generic method and the method invocation provides explicit type
     *   arguments, then let R 1 ... R p (p ≥ 1) be the type parameters of m , let B l be the
     *   declared bound of R l (1 ≤ l ≤ p), and let U 1 , ..., U p be the explicit type arguments
     *   given in the method invocation. Then m is applicable by strict invocation if both
     *   of the following are true:
     *   – For 1 ≤ i ≤ n, if e i is pertinent to applicability then e i is compatible in a strict
     *     invocation context with F i [R 1 := U 1 , ..., R p := U p ] .
     *   – For 1 ≤ l ≤ p, U l <: B l [R 1 := U 1 , ..., R p := U p ] .
     *   
     * • If m is not a generic method, then m is applicable by strict invocation if, for 1 ≤
     *   i ≤ n, either e i is compatible in a strict invocation context with F i or e i is not
     *   pertinent to applicability.
     *  
     *  If no method applicable by strict invocation is found, the search for applicable methods continues with phase 2 (15.12.2.3).
     *  
     *  Otherwise, the most specific method (15.12.2.5) is chosen among the methods that are applicable by strict invocation.
     */    
    class InvocationFilter implements ApplicableMethodFilter {
    	final InvocationKind kind;
    	final ArgCompatibilityContext context;
    	
    	InvocationFilter(InvocationKind k, ArgCompatibilityContext c) {
    		kind = k;
    		context = c;
    	}
    	
    	public final InvocationKind getKind() {
    		return kind;
    	}
    	
    	public final boolean usesVarargs() {
    		return getKind() == InvocationKind.VARARGS;
    	}
    	
		public boolean isApplicable(CallState call, MethodBinding m) {
			if (call.args.length != m.getNumFormals()) {
				return false;
			}
			if (m.isGeneric()) {
				if (call.getNumTypeArgs() == 0) {				
					return typeInfer.inferForInvocationApplicability(call, m, getKind());
				} else {							
					if (call.getNumTypeArgs() != m.numTypeFormals) {
						return false;
					}
					final IJavaTypeSubstitution methodTypeSubst = FunctionParameterSubstitution.create(binder, m.bind, call.targs);
					if (!isApplicableAndCompatible(call, m, methodTypeSubst, context, usesVarargs())) {
						return false;
					}
					int i=0;
					for(IRNode tf : TypeFormals.getTypeIterator(m.typeFormals)) {
						IJavaType u_l = binder.getJavaType(call.targs[i]);
						IJavaType b_l = JavaTypeFactory.getTypeFormal(tf).getExtendsBound(tEnv);
						IJavaType b_subst = b_l.subst(methodTypeSubst);
						if (!tEnv.isSubType(u_l, b_subst)) {
							return false;
						}
						i++;
					}
					return true;
				}
			}
			return isApplicableAndCompatible(call, m, IJavaTypeSubstitution.NULL, context, usesVarargs());
		}
    }
    
    // Assumes that #formals == #args
	// Check each arg for applicability / compatibility
	boolean isApplicableAndCompatible(CallState call, MethodBinding m, IJavaTypeSubstitution substForParams, ArgCompatibilityContext context, boolean varArity) {
		final IJavaType[] argTypes = call.getArgTypes(); // TODO factor out?
		int i=0;			
		//for(IRNode param : Parameters.getFormalIterator(m.formals)) {
		for(IJavaType pType : m.getParamTypes(binder, argTypes.length, varArity)) {//Parameters.getFormalIterator(m.formals)) {
			final IRNode arg = call.args[i];
			final IJavaType argType = argTypes[i];
			i++;
			if (!isPertinentToApplicability(m, call.getNumTypeArgs() > 0, arg)) {
				continue; // Ignore this one
			}
			//IJavaType pType = binder.getJavaType(ParameterDeclaration.getType(param));
	    	IJavaType substType = pType.subst(substForParams);
			if (!context.isCompatible(null, substType, arg, argType)) {
				return false;										
			}
		}	
		return true;
	}
    
    private final ApplicableMethodFilter STRICT_INVOCATION = new InvocationFilter(InvocationKind.STRICT, STRICT_INVOCATION_CONTEXT);
	
	/*
	 * As above, but applicable by loose invocation
	 * 
	 * 15.12.2.3 Phase 2: Identify Matching Arity Methods Applicable by Loose Invocation
	 * 
	 * Let m be a potentially applicable method (§15.12.2.1) with arity n and formal
	 * parameter types F 1 , ..., F n , and let e 1 , ..., e n be the actual argument expressions of
	 * the method invocation. Then:
	 * 
	 * • If m is a generic method and the method invocation does not provide explicit type
	 *   arguments, then the applicability of the method is inferred as specified in §18.5.1.
	 *   
	 * • If m is a generic method and the method invocation provides explicit type
	 *   arguments, then let R 1 ... R p (p ≥ 1) be the type parameters of m , let B l be the
	 *   declared bound of R l (1 ≤ l ≤ p), and let U 1 ... U p be the explicit type arguments]
	 *   given in the method invocation. Then m is applicable by loose invocation if both
	 *   of the following are true:
	 *   – For 1 ≤ i ≤ n, if e i is pertinent to applicability (§15.12.2.2) then e i is compatible
	 *     in a loose invocation context with F i [R 1 := U 1 , ..., R p := U p ] .
	 *   – For 1 ≤ l ≤ p, U l <: B l [R 1 := U 1 , ..., R p := U p ] .
	 *   
	 * • If m is not a generic method, then m is applicable by loose invocation if, for 1 ≤
	 *   i ≤ n, either e i is compatible in a loose invocation context with F i or e i is not
	 *   pertinent to applicability.
	 */
	private final ApplicableMethodFilter LOOSE_INVOCATION = new InvocationFilter(InvocationKind.LOOSE, LOOSE_INVOCATION_CONTEXT);

	/*
	 * 15.12.2.4 Phase 3: Identify Methods Applicable by Variable Arity Invocation
	 * 
	 * Where a variable-arity method has formal parameter types F1, ..., Fn-1, Fn[], 
	 * let the ith variable-arity parameter type of the method as follows:
	 * 
	 *   For i ≤ n-1, the ith variable-arity parameter type is Fi.
	 *   For i ≥ n, the ith variable-arity parameter type is Fn. 
	 *   
	 * Let m be a potentially applicable method (§15.12.2.1) with variable arity, let T 1 , ...,
	 * T k be the first k variable arity parameter types of m , and let e 1 , ..., e k be the actual
	 * argument expressions of the method invocation. Then:
	 * 
	 * • If m is a generic method and the method invocation does not provide explicit type
	 *   arguments, then the applicability of the method is inferred as specified in §18.5.1.
	 *   
	 * • If m is a generic method and the method invocation provides explicit type
	 *   arguments, then let R 1 ... R p (p ≥ 1) be the type parameters of m , let B l be the
	 *   declared bound of R l (1 ≤ l ≤ p), and let U 1 ... U p be the explicit type arguments
	 *   given in the method invocation. Then m is applicable by variable arity invocation
	 *   if:
	 *   
	 *   – For 1 ≤ i ≤ k, if e i is pertinent to applicability (§15.12.2.2) then e i is compatible
	 *     in a loose invocation context with T i [R 1 := U 1 , ..., R p := U p ] .
	 *     
	 *   – For 1 ≤ l ≤ p, U l <: B l [R 1 := U 1 , ..., R p := U p ] .
	 *   
	 * • If m is not a generic method, then m is applicable by variable arity invocation if,
	 *   for 1 ≤ i ≤ k, either e i is compatible in a loose invocation context with T i or e i
     *   is not pertinent to applicability (§15.12.2.2).
	 */
	private final ApplicableMethodFilter VARIABLE_ARITY_INVOCATION = new InvocationFilter(InvocationKind.VARARGS, LOOSE_INVOCATION_CONTEXT) {
    	@Override
		public boolean isApplicable(CallState call, MethodBinding m) {
    		if (!m.isVariableArity()) {
    			return false;
    		}
    		return super.isApplicable(call, m);
    	}
    };
	
	/*
	 * 15.12.2.5 Choosing the Most Specific Method
	 * 
	 * If more than one member method is both accessible and applicable to a method
	 * invocation, it is necessary to choose one to provide the descriptor for the run-
	 * time method dispatch. The Java programming language uses the rule that the most
	 * specific method is chosen.
	 * 
	 * The informal intuition is that one method is more specific than another if any invocation handled by the first method
	 * could be passed on to the other one without a compile-time error. In cases such as a lambda expression argument (15.27) 
	 * or a variable-arity invocation (15.12.2.4), some flexibility is allowed to adapt one signature to the other. 
	 * 
	 * (see below)
	 * 
	 * A method m 1 is strictly more specific than another method m 2 if and only if m 1 is
	 * more specific than m 2 and m 2 is not more specific than m 1 .
	 * 
	 * A method is said to be maximally specific for a method invocation if it is accessible
	 * and applicable and there is no other method that is applicable and accessible that
	 * is strictly more specific.
	 * 
	 * If there is exactly one maximally specific method, then that method is in fact
	 * the most specific method; it is necessarily more specific than any other accessible
	 * method that is applicable. It is then subjected to some further compile-time checks
	 * as specified in §15.12.3.
	 * 
	 * It is possible that no method is the most specific, because there are two or more
	 * methods that are maximally specific. In this case:
	 * • If all the maximally specific methods have override-equivalent signatures
	 * (§8.4.2), then:
	 *   – If exactly one of the maximally specific methods is concrete (that is, non-
	 *     abstract or default), it is the most specific method.
	 *  
	 *   – Otherwise, if all the maximally specific methods are abstract or default, and
	 *     the signatures of all of the maximally specific methods have the same erasure
	 *     (§4.6), then the most specific method is chosen arbitrarily among the subset
	 *     of the maximally specific methods that have the most specific return type.
	 *     In this case, the most specific method is considered to be abstract . Also, the
	 *     most specific method is considered to throw a checked exception if and only
	 *     if that exception or its erasure is declared in the throws clauses of each of the
	 *     maximally specific methods.
	 * 
	 * • Otherwise, the method invocation is ambiguous, and a compile-time error occurs.
	 */
    private IBinding findMostSpecific(final CallState call, Iterable<MethodBinding> methods, ApplicableMethodFilter filter) {
    	final Set<MethodBinding> applicable = new HashSet<MethodBinding>();
    	for(MethodBinding mb : methods) {
    		if (filter.isApplicable(call, mb)) {
    			applicable.add(mb);
    		}
    	}
    	if (applicable.isEmpty()) {
    		return null;
    	}
    	MethodBinding rv = null;
    	for(MethodBinding mb : applicable) {
    		if (rv == null) {
    			rv = mb;
    		} 
    		// TODO is this transitive?
    		else if (isMoreSpecific(call, mb, rv, filter.usesVarargs())) {
    			rv = mb;
    		}
    	}
    	return rv == null ? null : rv.bind;
	}

    /**
	 * One applicable method m1 is more specific than another applicable method m2, for an invocation with argument expressions 
	 * e 1 , ..., e k , if any of the following are true:
	 * 
	 * • m 2 is generic and m 1 is inferred to be more specific than m 2 for argument
	 *   expressions e 1 , ..., e k by §18.5.4.
	 *   
	 * • m 2 is not generic, m 1 and m 2 are applicable by strict or loose invocation, and where
	 *   m 1 has formal parameter types S 1 , ..., S n and m 2 has formal parameter types T 1 , ...,
	 *   T n , the type S i is more specific than T i for argument e i for all i (1 ≤ i ≤ n, n = k).
	 *   
	 * • m 2 is not generic, m 1 and m 2 are applicable by variable arity invocation, and where
	 *   the first k variable arity parameter types of m 1 are S 1 , ..., S k and the first k variable
	 *   arity parameter types of m 2 are T 1 , ..., T k , the type S i is more specific than T i for
	 *   argument e i for all i (1 ≤ i ≤ k). Additionally, if m 2 has k+1 parameters, then the
	 *   k+1'th variable arity parameter type of m 1 is a subtype of the k+1'th variable arity
	 *   parameter type of m 2 .
     *     
     * The above conditions are the only circumstances under which one method may be more specific than another.
     */
    private boolean isMoreSpecific(final CallState call, MethodBinding m1, MethodBinding m2, boolean varArity) {
    	if (m2.isGeneric()) {
    		// Case 1
    		throw new NotImplemented(); // TODO JLS 18.5.4
    	}    	
    	final int k = call.getArgTypes().length;
    	IJavaType[] m1Types = m1.getParamTypes(binder, k+1, varArity);
    	IJavaType[] m2Types = m2.getParamTypes(binder, k+1, varArity);
		// Case 2 + 3
		for(int i=0; i<k; i++) {
			if (!isMoreSpecific(m1Types[i], m2Types[i], call.args[i])) {
				return false;
			}
		}    	
    	if (varArity && m2.getNumFormals() == k+1) {
    		// Case 3
    		return m1Types[k].isSubtype(tEnv, m2Types[k]);
    	}
    	return true;
    }
    
	/*
	 * A type S is more specific than a type T for any expression if S <: T (§4.10).
	 * 
	 * A functional interface type S is more specific than a functional interface type T for
	 * an expression e if T is not a subtype of S and one of the following is true (where
	 * U 1 ... U k and R 1 are the parameter types and return type of the function type of the
	 * capture of S , and V 1 ... V k and R 2 are the parameter types and return type of the
	 * function type of T ):
	 * 
	 * • If e is an explicitly typed lambda expression (§15.27.1), then one of the following
	 *   is true:
	 *   – R 2 is void .
	 *   – R 1 <: R 2 .
	 *   – R 1 and R 2 are functional interface types, and R 1 is more specific than R 2 for
	 *     each result expression of e .
	 *     
	 *     The result expression of a lambda expression with a block body is defined
	 *     in §15.27.2; the result expression of a lambda expression with an expression
	 *     body is simply the body itself.
	 *    
	 *   – R 1 is a primitive type, R 2 is a reference type, and each result expression of e is
	 *     a standalone expression (§15.2) of a primitive type.
	 *   – R 1 is a reference type, R 2 is a primitive type, and each result expression of e is
	 *     either a standalone expression of a reference type or a poly expression.
	 *  
	 *  • If e is an exact method reference expression (§15.13.1), then i) for all i (1 ≤ i ≤
	 *  k), U i is the same as V i , and ii) one of the following is true:
	 *  
	 *    – R 2 is void .
	 *    – R 1 <: R 2 .
	 *    – R 1 is a primitive type, R 2 is a reference type, and the compile-time declaration
	 *      for the method reference has a return type which is a primitive type.
	 *    – R 1 is a reference type, R 2 is a primitive type, and the compile-time declaration
	 *      for the method reference has a return type which is a reference type.
	 *      
	 *  • If e is a parenthesized expression, then one of these conditions applies recursively
	 *    to the contained expression.
	 *  • If e is a conditional expression, then for each of the second and third operands,
	 *    one of these conditions applies recursively.
	 *******************************************************************************************
	 * 
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
    	if (t.isSubtype(tEnv, s)) {
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
			throw new NotImplemented();
    	}
    	else if (MethodReference.prototype.includes(op)) {
    		// TODO
			throw new NotImplemented();
    	}
    	else if (ConstructorReference.prototype.includes(op)) {
    		// TODO
			throw new NotImplemented();
    	}
    	return false;
    }
    
    /**
     * From JLS 15.12.2.2 

     * An argument expression is considered pertinent to applicability for a potentially
     * applicable method m unless it has one of the following forms:
     * • An implicitly typed lambda expression (§15.27.1).
     * • An inexact method reference expression (§15.13.1).
     * • If m is a generic method and the method invocation does not provide explicit type
     *   arguments, an explicitly typed lambda expression or an exact method reference
     *   expression for which the corresponding target type (as derived from the signature
     *   of m ) is a type parameter of m .
     * • An explicitly typed lambda expression whose body is an expression that is not
     *   pertinent to applicability.
     * • An explicitly typed lambda expression whose body is a block, where at least one
     *   result expression is not pertinent to applicability.
     * • A parenthesized expression (§15.8.5) whose contained expression is not
     *   pertinent to applicability.
     * • A conditional expression (§15.25) whose second or third operand is not pertinent
     *   to applicability
     */
    private boolean isPertinentToApplicability(final MethodBinding m, boolean callHasTypeArgs, final IRNode arg) {
    	Operator op = JJNode.tree.getOperator(arg);
    	if (LambdaExpression.prototype.includes(op)) {
    		if (isImplicitlyTypedLambda(arg)) {
    			return true;
    		} 
    		// Explicitly typed
    		if (m.isGeneric() && !callHasTypeArgs && m.hasTypeParameterAsReturnType(binder)) {
    			return false;
    		}    		
    		IRNode body = LambdaExpression.getBody(arg);
    		if (Expression.prototype.includes(body)) {
    			return isPertinentToApplicability(m, callHasTypeArgs, body);
    		} else {
    			throw new NotImplemented(); // TODO check return exprs
    		}
    	}
    	else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
    		return isExactMethodReference(arg) && (!m.isGeneric() || callHasTypeArgs || !m.hasTypeParameterAsReturnType(binder));
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return isPertinentToApplicability(m, callHasTypeArgs, ParenExpression.getOp(arg));
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		return isPertinentToApplicability(m, callHasTypeArgs, ConditionalExpression.getIftrue(arg)) &&
    			   isPertinentToApplicability(m, callHasTypeArgs, ConditionalExpression.getIffalse(arg));
    	}
    	return true;
    }
    
    static boolean isImplicitlyTypedLambda(IRNode lambda) {
    	IRNode params = LambdaExpression.getParams(lambda);
    	for(IRNode param : Parameters.getFormalIterator(params)) {
    		IRNode type = ParameterDeclaration.getType(param);
    		return Type.prototype == JJNode.tree.getOperator(type);
    	}
    	return false;
    }
    
    /**
     * From JLS 15.13.1.
     * 
     * A method reference expression ending with Identifier is exact if it satisfies all of
     * the following:
     * • If the method reference expression has the form ReferenceType ::
     *   [TypeArguments] Identifier, then ReferenceType does not denote a raw type.
     * • The type to search has exactly one member method with the name Identifier that
     *   is accessible to the class or interface in which the method reference expression
     *   appears.
     * • This method is not variable arity (§8.4.1).
     * • If this method is generic (§8.4.4), then the method reference expression provides TypeArguments.
     * 
     * A method reference expression of the form ClassType :: [TypeArguments] new is
     * exact if it satisfies all of the following:
     * • The type denoted by ClassType is not raw, or is a non- static member type of a raw type.
     * • The type denoted by ClassType has exactly one constructor that is accessible to
     *   the class or interface in which the method reference expression appears.
     * • This constructor is not variable arity.
     * • If this constructor is generic, then the method reference expression provides TypeArguments.
     * 
     * A method reference expression of the form ArrayType :: new is always exact.
     */
    boolean isExactMethodReference(IRNode ref) {
    	Operator op = JJNode.tree.getOperator(ref);
    	if (MethodReference.prototype.includes(op)) {
    		throw new NotImplemented(); // TODO
    	} else {
    		IRNode recv = ConstructorReference.getReceiver(ref);
    		IRNode type = TypeExpression.getType(recv);
    		if (ArrayType.prototype.includes(type)) {
    			return true;
    		}
    		throw new NotImplemented(); // TODO
    	}
    }
}
