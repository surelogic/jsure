package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.common.Pair;
import com.surelogic.common.util.EmptyIterator;
import com.surelogic.common.util.Iteratable;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinding.Util;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.bind.IJavaType.BooleanVisitor;
import edu.cmu.cs.fluid.java.bind.MethodInfo.Kind;
import edu.cmu.cs.fluid.java.bind.TypeInference8.BoundSet;
import edu.cmu.cs.fluid.java.bind.TypeInference8.LambdaCache;
import edu.cmu.cs.fluid.java.bind.TypeInference8.TypeFormalCollector;
import edu.cmu.cs.fluid.java.bind.TypeInference8.TypeVariable;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Implements overload resolution according to JLS sec 15.12.2 for Java 8
 * 
 * @author edwin
 */
public class MethodBinder8 implements IMethodBinder {
	private final boolean debug;
	final IPrivateBinder binder;
	final ITypeEnvironment tEnv;
	final TypeInference8 typeInfer;
	
	MethodBinder8(IPrivateBinder b, boolean debug) {
		tEnv = b.getTypeEnvironment();
		binder = b;
		this.debug = debug;
		typeInfer = new TypeInference8(this);
	}	
	
    public BindingInfo findBestMethod(final IJavaScope scope, final LookupContext context, final boolean needMethod, final IRNode from, final CallState call) {
        final Iterable<IBinding> methods = findMethods(scope, context, needMethod, from);
        final Set<MethodBinding> applicable = new HashSet<MethodBinding>();
        for(IBinding mb : methods) {
        	if (isPotentiallyApplicable(call, from, mb)) {
        		applicable.add(new MethodBinding(mb));
        	}
        }
    	// Eliminate A.foo() when subclass B.foo() overrides it (assumed by JLS)
    	final Collection<MethodBinding> processedMethods = new ArrayList<MethodBinding>(applicable);
    	for(MethodBinding mb : applicable) {
    		for(IBinding b : binder.findOverriddenMethods(mb.getNode())) {
    			Iterator<MethodBinding> it = processedMethods.iterator();
    			while (it.hasNext()) {
    				MethodBinding mb2 = it.next();
    				if (mb2.getNode().equals(b.getNode())) {
    					it.remove();
    				}
    			}
    		}
    	}
        IMethodBinding8 rv = tryToFindMostSpecific(call, processedMethods);
        if (rv == null) {
        	return null;
        }
        // TODO is this right?
        return new BindingInfo(rv, 0, false, 0);
    }

	private MethodBinding8 tryToFindMostSpecific(final ICallState call, final Collection<MethodBinding> applicable) {
		MethodBinding8 rv = findMostSpecific(call, applicable, STRICT_INVOCATION);
        if (rv == null && !call.needsExactInvocation()) {
        	rv = findMostSpecific(call, applicable, LOOSE_INVOCATION);
        	if (rv == null) {
        		rv = findMostSpecific(call, applicable, VARIABLE_ARITY_INVOCATION);
        	}
        }
		return rv;
	}    
    
	private Iterable<IBinding> findMethods(final IJavaScope scope, final LookupContext context, final boolean needMethod, final IRNode from) {
		final IJavaScope.Selector isAccessible = MethodBinder.makeAccessSelector(tEnv, from);
        final Iterable<IBinding> methods = new Iterable<IBinding>() {
  			public Iterator<IBinding> iterator() {
  				return IJavaScope.Util.lookupCallable(scope, context, isAccessible, needMethod);
  			}
        };
		return methods;
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
    		if (call.call != null) {
    			final String callName = MethodCall.getMethod(call.call);
    			final String methodName = MethodDeclaration.getId(mb.getNode());
    			if (!callName.equals(methodName)) {
    				return false;
    			}
    		} // otherwise, assumed to be correct
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
    			final IJavaArrayType at = (IJavaArrayType) getParamType(mb, lastParam);
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
    
    private IJavaType getParamType(IBinding mb, IRNode param) {
    	IJavaType rv = binder.getJavaType(param);    	
    	return mb.convertType(binder, rv);
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
    		final IJavaType t = getParamType(mb, param);
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
    
	/**
     * An expression is potentially compatible with a target type according to the following rules:
     *   
     * • A lambda expression (§15.27) is potentially compatible with a functional interface type (§9.8) 
     *   if all of the following are true:
     * 
     *   – The arity of the target type's function type is the same as the arity of the lambda expression.
     *   – If the target type's function type has a void return, then the lambda body is
     *     either a statement expression (§14.8) or a void-compatible block (§15.27.2).
     *   – If the target type's function type has a (non- void ) return type, then the lambda
     *     body is either an expression or a value-compatible block (§15.27.2).
     *     
     * • A method reference expression (§15.13) is potentially compatible with a
     *   functional interface type if ... (see below)
     *      
     * • A lambda expression or a method reference expression is potentially compatible
     *   with a type variable if the type variable is a type parameter of the candidate method.
     *   
     * • A parenthesized expression (§15.8.5) is potentially compatible with a type if its
     *   contained expression is potentially compatible with that type.
     * 
     * • A conditional expression (§15.25) is potentially compatible with a type if each
     *   of its second and third operand expressions are potentially compatible with that type.
     *   
     * • A class instance creation expression, a method invocation expression, or an
     *   expression of a standalone form (§15.2) is potentially compatible with any type.
     */
    private boolean isPotentiallyCompatible(IBinding mb, IRNode e, IJavaType t) {
    	final Operator op = JJNode.tree.getOperator(e);
    	if (LambdaExpression.prototype.includes(op)) {
    		if (t instanceof IJavaTypeFormal) {
    			return declaresTypeParam(MethodDeclaration.getTypes(mb.getNode()), (IJavaTypeFormal) t);
    		}
    		IJavaFunctionType ft = tEnv.isFunctionalType(t);
    		if (ft != null) {
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
    		return methodRefHasPotentiallyApplicableMethods(t, MethodReference.getReceiver(e), MethodReference.getMethod(e)) != null;
    	}
    	else if (ConstructorReference.prototype.includes(op)) {
    		if (t instanceof IJavaTypeFormal) {
    			return declaresTypeParam(ConstructorDeclaration.getTypes(mb.getNode()), (IJavaTypeFormal) t);
    		}
    		return methodRefHasPotentiallyApplicableMethods(t, ConstructorReference.getType(e), "new") != null;
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
     * • A method reference expression (§15.13) is potentially compatible with a
     *   functional interface type if, where the type's function type arity is n, there exists
     *   at least one potentially applicable method for the method reference expression
     *   with arity n (§15.13.1), and one of the following is true:
     *   
     *   – The method reference expression has the form ReferenceType :: [TypeArguments] Identifier 
     *     and at least one potentially applicable method is
     *     i) static and supports arity n, or ii) not static and supports arity n-1.
     *     
     *   – The method reference expression has some other form and at least one
     *     potentially applicable method is not static .
     */
    IJavaFunctionType methodRefHasPotentiallyApplicableMethods(IJavaType t, IRNode base, String name) {
    	final IJavaType ct = computeGroundTargetTypeForMethodRef(t);
    	//JavaTypeVisitor.captureWildcards(binder, t);
    	final IJavaFunctionType ft = tEnv.isFunctionalType(ct);
    	if (methodRefHasPotentiallyApplicableMethods(ft, base, name)) {
    		return ft;
    	}
    	return null;
    }
    
    boolean methodRefHasPotentiallyApplicableMethods(IJavaFunctionType ft, IRNode base, String name) {    
    	if (ft != null) {    	
    		final int n = ft.getParameterTypes().size();
    		final boolean isConstructor = "new".equals(name);
    		if (!isConstructor && identifyReceiverKind(base) == ReceiverKind.REF_TYPE) {
    			for(IBinding m : findPotentiallyApplicableForMethodRef(base, name, n, ft.getTypeFormals().size())) {
    				final int p = getArity(m.getNode(), isConstructor);
    				if (TypeUtil.isStatic(m.getNode())) {
    					if (p == n) {
    						return true;
    					}
    					MethodBinding mb = new MethodBinding(m);
    					if (mb.isVariableArity() && p-1 == n) {
    						return true;
    					}
    				} else {
    					if (p == n-1) {
    						return true;
    					}
    					MethodBinding mb = new MethodBinding(m);
    					if (mb.isVariableArity() && p == n) {
    						return true;
    					}
    				}
    			}
    		} else {
      			for(IBinding m : findPotentiallyApplicableForMethodRef(base, name, n, ft.getTypeFormals().size())) {
      				if (!TypeUtil.isStatic(m.getNode())) {
      					return true;
      				}
      			}
    		}
    	}
    	return false;
    }
    
    enum ReceiverKind {
    	REF_TYPE, ARRAY_TYPE, EXPR
    }
    
    private ReceiverKind identifyReceiverKind(IRNode receiver) {
    	final Operator op = JJNode.tree.getOperator(receiver);
    	if (TypeExpression.prototype.includes(op)) {
    		IRNode t = TypeExpression.getType(receiver);
    		Operator top =  JJNode.tree.getOperator(t);    		
    		if (ArrayType.prototype.includes(top)) {
    			return ReceiverKind.ARRAY_TYPE;
    		}    		
    		return ReceiverKind.REF_TYPE;
    	}
    	else if (NameExpression.prototype.includes(op)) {
    		// Arrays should show up above
    		IRNode name = NameExpression.getName(receiver);
    		IBinding b = binder.getIBinding(name);
    		if (TypeDeclaration.prototype.includes(b.getNode())) {
    			return ReceiverKind.REF_TYPE;
    		}
    	}
    	return ReceiverKind.EXPR;
    }
    
	private int getArity(IRNode node, boolean isConstructor) {
		IRNode params;
		if (isConstructor) {
			params = ConstructorDeclaration.getParams(node);
		} else {
			params = MethodDeclaration.getParams(node);
		}
		return JJNode.tree.numChildren(params);
	}
	
	private int getNumTypeParams(IRNode node, boolean isConstructor) {
		IRNode params;
		if (isConstructor) {
			params = ConstructorDeclaration.getTypes(node);
		} else {
			params = MethodDeclaration.getTypes(node);
		}
		return JJNode.tree.numChildren(params);
	}

	/**
     * From 15.13.1 Compile-Time Declaration of a Method Reference
     * 
     * • First, a type to search is determined:
     * 
     * – If the method reference expression has the form ExpressionName :: [TypeArguments] Identifier
     *   or Primary :: [TypeArguments] Identifier, the type to search is the type of the expression 
     *   preceding the :: token.
     *   
     * – If the method reference expression has the form ReferenceType :: [TypeArguments] Identifier, 
     *   the type to search is the result of capture conversion (§5.1.10) applied to ReferenceType.
     *   
     * – If the method reference expression has the form super :: [TypeArguments] Identifier, 
     *   the type to search is the superclass type of the class whose declaration contains the 
     *   method reference.
     *   
     * – If the method reference expression has the form TypeName . super :: [TypeArguments] Identifier, 
     *   then if TypeName denotes a class, the type to search is the superclass type of the named class; 
     *   otherwise, TypeName denotes an interface, and the corresponding superinterface type of the 
     *   class or interface whose declaration contains the method reference is the type to search.
     *   
     * – For the two other forms (involving :: new ), the referenced method is notional 
     *   and there is no type to search.
     */
    IJavaType findTypeToSearchForMethodRef(IRNode receiver, ReceiverKind kind, boolean isConstructor) {
    	if (!isConstructor && kind == ReceiverKind.REF_TYPE) {
    		IJavaType t = binder.getJavaType(receiver); 
    		return JavaTypeVisitor.captureWildcards(binder, t);
    	}
    	IJavaType rv = binder.getJavaType(receiver);
       	if (isConstructor) {
       		if (rv instanceof IJavaDeclaredType) {
       			IJavaDeclaredType dt = (IJavaDeclaredType) rv;
       			if (dt.getTypeParameters().isEmpty() && dt.isRawType(tEnv)) {
       				return tEnv.convertNodeTypeToIJavaType(dt.getDeclaration());
       			}
       		}
       		else if (rv instanceof IJavaArrayType) {
       			IJavaArrayType at = (IJavaArrayType) rv;
    			List<IJavaType> params = Collections.singletonList(at.getElementType());
    			return JavaTypeFactory.getDeclaredType(tEnv.getArrayClassDeclaration(), params, null);       			
       		}
       	}
       	return rv;
    }
        
    /**
     * From 15.13.1 Compile-Time Declaration of a Method Reference
     * 
     * • Second, given a targeted function type with n parameters, a set of potentially
     *   applicable methods is identified:
     *   
     *   – If the method reference expression has the form ReferenceType :: [TypeArguments] Identifier, 
     *     the potentially applicable methods are the member methods of the type to search that have an
     *     appropriate name (given by Identifier), accessibility, arity (n or n-1), and type argument 
     *     arity (derived from [TypeArguments]), as specified in §15.12.2.1.
     *     
     *       Two different arities, n and n-1, are considered, to account for the possibility that this
     *       form refers to either a static method or an instance method.
     *       
     *   – If the method reference expression has the form ClassType :: [TypeArguments] new , the 
     *     potentially applicable methods are a set of notional methods corresponding to the 
     *     constructors of ClassType.
     *     
     *     If ClassType is a raw type, but is not a non- static member type of a raw type,
     *     the candidate notional member methods are those specified in §15.9.3 for a
     *     class instance creation expression that uses <> to elide the type arguments to a class.
     *     
     *     Otherwise, the candidate notional member methods are the constructors of ClassType, 
     *     treated as if they were methods with return type ClassType. Among these candidates, 
     *     the methods with appropriate accessibility, arity (n), and type argument arity 
     *     (derived from [TypeArguments]) are selected, as specified in §15.12.2.1.
     *     
     *   – If the method reference expression has the form ArrayType :: new , a single notional method
     *     is considered. The method has a single parameter of type int, returns the ArrayType, and has 
     *     no throws clause. If n = 1, this is the only potentially applicable method; otherwise, there 
     *     are no potentially applicable methods.
     *   
     *   – For all other forms, the potentially applicable methods are the member methods of the type
     *     to search that have an appropriate name (given by Identifier), accessibility, arity (n), and 
     *     type argument arity (derived from [TypeArguments]), as specified in §15.12.2.1.
     */
    Set<IBinding> findPotentiallyApplicableForMethodRef(IRNode base, String name, int numParams, int numTypeArgs) {
		final boolean isConstructor = "new".equals(name);
		final ReceiverKind kind = identifyReceiverKind(base);
		if (isConstructor && kind == ReceiverKind.ARRAY_TYPE) {
			if (numParams != 1 || numTypeArgs > 0) {
				// This can't match an array
				return Collections.emptySet();
			}
		}
		
		final IJavaType t = findTypeToSearchForMethodRef(base, kind, isConstructor);
    	final LookupContext context = new LookupContext();
    	final IJavaScope scope;
    	final boolean isRefType;
    	
    	if (isConstructor) {
  			isRefType = !isConstructor;
    		if (t instanceof IJavaArrayType) {
       			IJavaArrayType at = (IJavaArrayType) t;
    			List<IJavaType> params = Collections.singletonList(at.getElementType());
    			IJavaDeclaredType ast  = JavaTypeFactory.getDeclaredType(tEnv.getArrayClassDeclaration(), params, null);
    			scope = binder.typeMemberTable(ast).asLocalScope(tEnv);
    			context.use(PromiseConstants.ARRAY_CLASS_NAME, base);
    		} else {
    			// Only needs to look at the specified type
    			IJavaSourceRefType tdecl = (IJavaSourceRefType) t;
    			scope = binder.typeMemberTable(tdecl).asLocalScope(tEnv);
    			context.use(JJNode.getInfo(tdecl.getDeclaration()), base);
    		}
    	} else {
    		scope = binder.typeScope(t);
    		isRefType = (kind == ReceiverKind.REF_TYPE);
    		context.use(name, base);
    	}
    	
    	final Set<IBinding> rv = new HashSet<IBinding>();
    	for(IBinding m : findMethods(scope, context, !isConstructor, base)) {
    		// Check num parameters/type args
    		final int p = getArity(m.getNode(), isConstructor);
    		if (p != numParams) {
    			// Check for receiver/varargs
    			if (p-1 == numParams) {
    				// could be varargs
    				final MethodBinding mb = new MethodBinding(m);
    				if (!mb.isVariableArity()) {
    					continue;
    				} 
    			}
    			else if (!isRefType || p != numParams-1) {
    				continue;
    			}
    		}
    		if (numTypeArgs >= 0) {
    			int numTypes = getNumTypeParams(m.getNode(), isConstructor);    			
    			if (numTypes != numTypeArgs) {
    				continue;
    			}
    		}
    		rv.add(m);    		
    	}
    	return rv;
	}
    
	/**
     * Assumes that the code compiles
     */
    boolean isVoidCompatible(IRNode lambdaBody) {
    	if (StatementExpression.prototype.includes(lambdaBody)) {
    		return true;
    	}
    	// Look for a void return statement
    	return rsv.doAccept(lambdaBody) != Boolean.TRUE;
	}
    
	/**
     * Assumes that the code compiles
     */
    boolean isReturnCompatible(IRNode lambdaBody) {
    	if (Expression.prototype.includes(lambdaBody)) {
    		return true;
    	}
    	// Look for a return statement
    	return rsv.doAccept(lambdaBody) == Boolean.TRUE;
	}

    private final ReturnStatementVisitor rsv = new ReturnStatementVisitor();
    
    /**
     * Looks for a return statement
     * @author edwin
     */
    private class ReturnStatementVisitor extends AbstractLambdaVisitor<Boolean> {
    	ReturnStatementVisitor() {
    		super(null);
    	}
		@Override
		Boolean merge(Boolean v1, Boolean v2) {
			if (v1 == null) {
				return v2;
			}
			if (v2 == null) {
				return v1;
			}
			return v1 || v2;
		}
		@Override
		protected Boolean mergeResults(List<Boolean> results) {
			Boolean rv = null;
			for(Boolean v : results) {
				rv = merge(rv, v);
			}
			return rv;
		}
    	@Override
    	public Boolean visitReturnStatement(IRNode s) {
    		return Boolean.TRUE;
    	}
    	@Override
    	public Boolean visitVoidReturnStatement(IRNode s) {
    		return Boolean.FALSE;
    	}
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
    boolean isPolyExpression(final IRNode e) {
    	if (e == null) {
    		return false;
    	}
    	final Operator op = JJNode.tree.getOperator(e);
    	return isPolyExpression(e, op);
    }
    
    boolean isPolyCall(final IRNode e, final Operator op, IBinding m) {
    	if (MethodCall.prototype.includes(op)) {
    		//  A method invocation expression is a poly expression if all of the following are true:

    		// The invocation appears in an assignment context (5.2) or an invocation context (5.3).
    	    // The invocation elides NonWildTypeArguments.
    	    // Per the following sections, the method to be invoked is a generic method (8.4.4).
    	    // The return type of the method to be invoked mentions at least one of the method's type parameters. 

    		// Otherwise, the method invocation expression is a standalone expression.    		
    		if (isInAssignmentOrInvocationContext(e) &&
    			numChildren(MethodCall.getTypeArgs(e)) == 0) {
    			final IBinding mb = m != null ? m : binder.getIBinding(e);
    			if (AnnotationElement.prototype.includes(mb.getNode())) {
    				return false;
    			}
    			IRNode typeParams = MethodDeclaration.getTypes(mb.getNode());
    			if (numChildren(typeParams) > 0) {
    				return refersToTypeParams(binder, MethodDeclaration.getReturnType(mb.getNode()), typeParams);
    			}
    		}    		
    		return false;
    	}
    	else if (ConstructorCall.prototype.includes(op)) {
    		return false;
    	}
    	throw new IllegalStateException("Unexpected call: "+op.name());
    }
    
    boolean isPolyExpression(final IRNode e, final Operator op) {
    	if (SomeFunctionCall.prototype.includes(op)) {
    		return isPolyCall(e, op, null);
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		// A class instance creation expression is a poly expression (15.2) 
    		// if i) it uses a diamond '<>' in place of type arguments, and 
    		// ii) it appears in an assignment context (5.2) or an invocation context (5.3). 
    		// Otherwise, it is a standalone expression.  		
    		IRNode typeArgs = NewExpression.getTypeArgs(e);
    		return numChildren(typeArgs) == 0 && isInAssignmentOrInvocationContext(e);
    	}    	
    	else if (ParenExpression.prototype.includes(op)) {
    		return isPolyExpression(ParenExpression.getOp(e));
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
			return classifyType(rtype);
    	}
    	else if (ParenExpression.prototype.includes(op)) {
    		return classifyExpression(ParenExpression.getOp(e));
    	}
    	else if (NewExpression.prototype.includes(op)) {
    		IRNode t = NewExpression.getType(e);
    		return classifyType(t);
    	}
    	else if (ConditionalExpression.prototype.includes(op)) {
    		return classifyCondExpr(e);
    	}
    	if (!isPolyExpression(e)) {
    		return classifyType(binder.getJavaType(e));
    	}
		return ExpressionKind.REF;
	}
	   
    private ExpressionKind classifyType(final IRNode t) {
    	final Operator op = JJNode.tree.getOperator(t);
    	if (BooleanType.prototype.includes(op)) {
    		return ExpressionKind.BOOLEAN;
    	}
    	else if (NumericType.prototype.includes(op)) {
    		return ExpressionKind.NUMERIC;
    	}
    	else if (ParameterizedType.prototype.includes(op)) {
    		return ExpressionKind.REF;
    	}
    	else if (ArrayType.prototype.includes(op)) {
    		return ExpressionKind.REF;
    	}
    	return classifyType(binder.getJavaType(t));    	
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
    private static boolean refersToTypeParams(IBinder b, IRNode type, IRNode typeParams) {
    	final Set<IRNode> formals = new HashSet<IRNode>();
    	for(final IRNode f : TypeFormals.getTypeIterator(typeParams)) {
    		formals.add(f);
    	}
    	for(final IRNode n : JJNode.tree.bottomUp(type)) {
    		final Operator op = JJNode.tree.getOperator(n);
    		if (NamedType.prototype.includes(op) || NameType.prototype.includes(op)) {
    			final IBinding tb = b.getIBinding(n);
    			if (formals.contains(tb.getNode())) {
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
		if (Arguments.prototype.includes(pop) || MethodCall.prototype.includes(pop)) {
			return ConversionContextKind.INVOCATION;
		}
		else if (AssignmentExpression.prototype.includes(pop) || Initialization.prototype.includes(pop)) {
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
		else if (ConditionalExpression.prototype.includes(pop)) {
			return getConversionContext(parent);
		}
		// TODO what cases am I missing?
    	return ConversionContextKind.UNKNOWN;
    }
        
    private static boolean isInAssignmentOrInvocationContext(IRNode e) {    	
    	return getConversionContext(e).ordinal() <= ConversionContextKind.INVOCATION.ordinal();
    }    
        
    interface ApplicableMethodFilter {
    	/**
    	 * @return non-null if applicable
    	 */
    	MethodBinding8 isApplicable(ICallState call, MethodBinding mb);
    	InvocationKind getKind();
    }
    
    interface ArgCompatibilityContext {
    	boolean isCompatible(IRNode param, IJavaType pType, IRNode arg, IJavaType argType);
    }
	
    final ArgCompatibilityContext STRICT_INVOCATION_CONTEXT = new ArgCompatibilityContext() {
    	public boolean isCompatible(IRNode param, IJavaType pType, IRNode arg, IJavaType argType) {    		 
    		argType = getArgTypeIfPossible(arg, argType);
    		if (argType != null) {
    			pType = JavaTypeVisitor.captureWildcards(binder, pType);
    		}
     		return isCallCompatible(pType, arg, argType, false);
    	}
	}; 
	
    final ArgCompatibilityContext LOOSE_INVOCATION_CONTEXT = new ArgCompatibilityContext() {
    	public boolean isCompatible(IRNode param, IJavaType pType, IRNode arg, IJavaType argType) {
    		if (pType == null) {
    			return false;
    		}
    		argType = getArgTypeIfPossible(arg, argType);
    		if (argType != null) {
    			pType = JavaTypeVisitor.captureWildcards(binder, pType);
    		}
    		
    		// TODO switch these two for efficiency?
    		if (isCallCompatible(pType, arg, argType, true)) {
    			return true;
    		}
    		return onlyNeedsBoxing(pType, arg, argType); // TODO anything else to do?
    	}
	}; 
    
	protected IJavaType getArgTypeIfPossible(IRNode arg, IJavaType argType) {
		if (argType == null && arg != null && !isPolyExpression(arg)) {
			argType = tEnv.getBinder().getJavaType(arg);
		}
		return argType;
	}
	
	protected boolean isCallCompatible(IJavaType pType, IRNode arg, IJavaType argType, boolean tryUnBoxingForPoly) {  
		if (argType == null) {			
			final Operator op = JJNode.tree.getOperator(arg);				
			if (!isPolyExpression(arg, op)) {
				argType = binder.getJavaType(arg);

				if (MethodBinder.captureTypes) {    		
					IJavaType temp = JavaTypeVisitor.captureWildcards(binder, argType);
					if (argType != temp) {
						argType = temp;
					}
				}
			}
			else if (LambdaExpression.prototype.includes(op)) {
				/*
				 * 15.27.3 Type of a Lambda Expression
				 * 
				 * A lambda expression is compatible in an assignment context, invocation context,
				 * or casting context with a target type T if T is a functional interface type (§9.8) and
				 * the expression is congruent with the function type of the ground target type derived from T .
				 */
				// Note: no need to check for boxing 
				if (tEnv.isFunctionalType(pType) == null) {
					return false;
				}
				IJavaType groundTargetType = computeGroundTargetType(arg, pType);
				IJavaFunctionType ft = tEnv.isFunctionalType(groundTargetType);
				return isLambdaCongruentWith(arg, groundTargetType, ft);
			}
			else if (MethodCall.prototype.includes(op) || NonPolymorphicNewExpression.prototype.includes(op)) {
				MethodBinding8 b = (MethodBinding8) binder.getIBinding(arg);
				CallState call = CallState.create(binder, arg, b);								
				if (tryUnBoxingForPoly) {
					boolean result = false;
					if (pType instanceof IJavaPrimitiveType) {
						IJavaPrimitiveType formalP = (IJavaPrimitiveType) pType;
		    			IJavaType boxedEquivalent = JavaTypeFactory.getCorrespondingDeclType(tEnv, formalP);
						if (boxedEquivalent != null) {
							result = isCallCompatible_call(boxedEquivalent, b, call);
						}
					}
					else if (pType instanceof IJavaDeclaredType) {		
						IJavaDeclaredType formalD = (IJavaDeclaredType) pType;
		    			IJavaType unboxedEquivalent = JavaTypeFactory.getCorrespondingPrimType(formalD);
						if (unboxedEquivalent != null) {
							result = isCallCompatible_call(unboxedEquivalent, b, call);						
						}
					}
					if (result) {
						return true;
					}
				}
				return isCallCompatible_call(pType, b, call);
			}
			else if (MethodReference.prototype.includes(op) || ConstructorReference.prototype.includes(op)) {
				// Note: no need to check for boxing 
				return isCompatibleWithRef(pType, arg); 
			} 
			else if (ConditionalExpression.prototype.includes(op)) {
				return isCallCompatible(pType, ConditionalExpression.getIftrue(arg), argType, tryUnBoxingForPoly) && 
					   isCallCompatible(pType, ConditionalExpression.getIffalse(arg), argType, tryUnBoxingForPoly);
			} 
			else if (ParenExpression.prototype.includes(op)) {
				return isCallCompatible(pType, ParenExpression.getOp(arg), argType, tryUnBoxingForPoly);
			}
 			else {
 				String msg = "Need code for "+op.name()+" : "+DebugUnparser.toString(arg);
 				throw new NotImplemented(msg);		
 			}
		}
		if (tryUnBoxingForPoly && onlyNeedsBoxing(pType, arg, argType)) {
			return true;
		}
		return tEnv.isCallCompatible(pType, argType);
	}

	private boolean isCallCompatible_call(IJavaType pType, MethodBinding8 b, CallState call) {
		IJavaFunctionType ftype = computeInvocationType(call, b, false, pType);
		if (ftype == null) {
			computeInvocationType(call, b, false, pType);
			return false;
		}
		boolean result = tEnv.isCallCompatible(pType, ftype.getReturnType());
		if (result) {
			return true;
		}
		final IJavaType rtype_subst = TypeInference8.eliminateReboundedTypeFormals(tEnv, ftype.getReturnType());
		if (rtype_subst != ftype.getReturnType()) {
			return tEnv.isCallCompatible(pType, rtype_subst);
		} else {
			final IJavaTypeSubstitution s = b.getBoundSetSubst();
			if (s != IJavaTypeSubstitution.NULL) {
				final IJavaType pType_subst = pType.subst(s);
				if (pType != pType_subst) {
					return tEnv.isCallCompatible(pType_subst, rtype_subst);
				}
			}
		}
		return false;
	}

	
	
	/**
	 * 15.13.2 Type of a Method Reference
	 * 
	 * A method reference expression is compatible in an assignment context, invocation
	 * context, or casting context with a target type T if T is a functional interface type
	 * (§9.8) and the expression is congruent with the function type of the ground target
	 * type derived from T .
	 */
	boolean isCompatibleWithRef(IJavaType type, IRNode ref) {
		if (tEnv.isFunctionalType(type) == null) {
			return false;
		}
		final IJavaType groundTarget = computeGroundTargetTypeForMethodRef(type);
		final IJavaFunctionType ftype = tEnv.isFunctionalType(groundTarget);
		if (ftype == null) {
			return false;
		}
		/*
		 * A method reference expression is congruent with a function type if both of the following are true:
		 * 
		 * • The function type identifies a single compile-time declaration corresponding to the reference.				 
		 * • One of the following is true:
		 * – The result of the function type is void .
		 * – The result of the function type is R , and the result of applying capture conversion (§5.1.10) 
		 *   to the return type of the invocation type (§15.12.2.6) of the chosen compile-time declaration is 
		 *   R ' (where R is the target type that may be used to infer R '), and neither R nor R ' is void , 
		 *   and R ' is compatible with R in an assignment context.
		 */
		final RefState state = new RefState(ftype, ref);	
		MethodBinding8 mb = findCompileTimeDeclForRef(ftype, state);
		if (mb == null) {
			return false;
		}
		final IJavaType r = ftype.getReturnType();
		if (r instanceof IJavaVoidType) {
			return true;
		}
		final IJavaFunctionType itype = computeInvocationType(state, mb, false, r);
		final IJavaType r_prime = JavaTypeVisitor.captureWildcards(binder, itype.getReturnType());
		if (r_prime instanceof IJavaVoidType) {
			return false;
		}
		boolean rv = tEnv.isAssignmentCompatible(r, r_prime, ref);
		if (!rv) {
			// Check for (un)boxing
			IJavaType t = null;
			if (r_prime instanceof IJavaPrimitiveType) {
				t = JavaTypeFactory.getCorrespondingDeclType(tEnv, (IJavaPrimitiveType) r_prime);
			} 
			else if (r_prime instanceof IJavaDeclaredType) {
				t = JavaTypeFactory.getCorrespondingPrimType((IJavaDeclaredType) r_prime);
			}
			if (t != null) {
				rv = tEnv.isAssignmentCompatible(r, t, ref);
			}
		}
		return rv;
	}
		
	IJavaType getCompileTimeResultType(IRNode call, IBinding b, IJavaType targetType) {
	  if (b instanceof MethodBinding8) {
		CallState state = CallState.create(binder, call, b);
		IJavaFunctionType ftype = getCompileTimeFunctionType(state, (MethodBinding8) b, targetType);  
		if (ftype == null) {
			ftype = getCompileTimeFunctionType(state, (MethodBinding8) b, targetType);  
			if (ftype == null) {
				return null;
			}
		}
		return ftype.getReturnType();
	  }
	  // Only for annotation elements?
	  return binder.getJavaType(b.getNode());
	}
	
	/**
	 * The compile-time parameter types and compile-time result are determined as
     * follows:
     * 
     * • If the compile-time declaration for the method invocation is not a signature
     *   polymorphic method, then the compile-time parameter types are the types of the
     *   formal parameters of the compile-time declaration, and the compile-time result
     *   is the result chosen for the compile-time declaration (§15.12.2.6).
     *   
     * • If the compile-time declaration for the method invocation is a signature
     *   polymorphic method, then:
     *   – The compile-time parameter types are the static types of the actual argument
     *     expressions. An argument expression which is the null literal null (§3.10.7)
     *     is treated as having the static type Void .
     *   – The compile-time result is determined as follows:
     *     › If the method invocation expression is an expression statement, the compile-
     *       time result is void .
     *     › Otherwise, if the method invocation expression is the operand of a cast
     *       expression (§15.16), the compile-time result is the erasure of the type of the
     *       cast expression (§4.6).
     *     › Otherwise, the compile-time result is the signature polymorphic method's
     *       declared return type, Object .
	 */
	private IJavaFunctionType getCompileTimeFunctionType(ICallState call, MethodBinding8 mb, IJavaType targetType) {		
		IJavaFunctionType ft = computeInvocationType(call, mb, false, targetType);
		//JavaTypeFactory.getMemberFunctionType(mb.getNode(), call.getReceiverType(), binder);
		if (!isSignaturePolymorphic(mb)) {
			return ft;
		}
		IRNode callParent = JJNode.tree.getParent(call.getNode());
		Operator pop = JJNode.tree.getOperator(callParent);
		IJavaType resultType;
		if (ExprStatement.prototype.includes(pop)) {
			resultType = JavaTypeFactory.voidType;
		}
		else if (CastExpression.prototype.includes(pop)) {
			IJavaType cast = binder.getJavaType(CastExpression.getType(callParent));
			resultType = tEnv.computeErasure(cast);
		}
		else {
			resultType = tEnv.getObjectType();
		}
		List<IJavaType> paramTypes = new ArrayList<>();
		for(int i=0; i<call.numArgs(); i++) {
			IJavaType t = call.getArgType(i);
			paramTypes.add(t == JavaTypeFactory.nullType ? tEnv.findJavaTypeByName("java.lang.Void") : t);
		}
		return JavaTypeFactory.getFunctionType(ft.getTypeFormals(), resultType, paramTypes, ft.isVariable(), ft.getExceptions());
	}
	
	/**
	 * A method is signature polymorphic if all of the following are true:
	 * 
	 * • It is declared in the java.lang.invoke.MethodHandle class.
	 * • It takes a single variable arity parameter (§8.4.1) whose declared type is Object[] .
	 * • It has a return type of Object .
	 * • It is native .
	 */
	private boolean isSignaturePolymorphic(IBinding b) {
		if (!"java.lang.invoke.MethodHandle".equals(b.getContextType().getName())) {
			return false;
		}
		if (!JavaNode.getModifier(b.getNode(), JavaNode.NATIVE)) {
			return false;
		}
		IJavaType rtype = binder.getJavaType(b.getNode());
		if (!tEnv.getObjectType().equals(rtype)) {
			return false;
		}		
		MethodBinding mb = new MethodBinding(b);
		if (!mb.isVariableArity()) {
			return false;
		}
		final IJavaType objArray = JavaTypeFactory.getArrayType(tEnv.getObjectType(), 1);
		return objArray.equals(mb.getParamTypes(binder, 1, false));
	}
	
	/**
	 * A lambda expression is congruent with a function type if all of the following are true:
	 * 
	 * • The function type has no type parameters.
	 * 
	 * • The number of lambda parameters is the same as the number of parameter types of the function type.
	 * 
	 * • If the lambda expression is explicitly typed, its formal parameter types are the
	 *   same as the parameter types of the function type.
	 * 
	 */
	private boolean isLambdaCongruentWith(IRNode lambda, IJavaType t, IJavaFunctionType ft) {
		if (!ft.getTypeFormals().isEmpty()) {
			return false;
		}
		IRNode params = LambdaExpression.getParams(lambda);
		if (JJNode.tree.numChildren(params) != ft.getParameterTypes().size()) {
			return false;
		}
		if (isImplicitlyTypedLambda(lambda)) {
			/*
			 *  • If the lambda parameters are assumed to have the same types as the function
			 *    type's parameter types, then:
			 *   
			 *    – If the function type's result is void , the lambda body is either a statement
			 *      expression or a void -compatible block.
			 *   
			 *    – If the function type's result is a (non- void ) type R , then either 
			 *   
			 *      i)  the lambda body is an expression that is compatible with R in an assignment context, or
			 *      ii) the lambda body is a value-compatible block, and each result expression
			 *          (§15.27.2) is compatible with R in an assignment context.
			 */
			final IRNode body = LambdaExpression.getBody(lambda);
			final IJavaType r = ft.getReturnType();
			if (r instanceof IJavaVoidType) {
				return isVoidCompatible(body);
			}
			return checkResultExprCompatibility(body, r, new LambdaCache(tEnv, null, lambda, t, ft));
		} else {
			int i=0;
			for(final IRNode pd : Parameters.getFormalIterator(params)) {
				final IJavaType fpt = ft.getParameterTypes().get(i);
				final IJavaType lt = tEnv.getBinder().getJavaType(ParameterDeclaration.getType(pd));
				if (!fpt.isEqualTo(tEnv, lt)) {
					return false;
				}
				i++;
			}
			return true;
		}
	}

	private boolean checkResultExprCompatibility(IRNode body, IJavaType r, LambdaCache cache) {
		final UnversionedJavaBinder ujb;
		if (tEnv.getBinder() instanceof UnversionedJavaBinder) {
			ujb = (UnversionedJavaBinder) tEnv.getBinder();
		} else {
			throw new NotImplemented();
		}
		final JavaCanonicalizer.IBinderCache old = ujb.setBinderCache(cache);
		try {
			for(IRNode expr : TypeInference8.findResultExprs(body)) {
				if (!isAssignCompatible(r, expr)) {
					return false;
				}
			}			
			return true;
		} finally {
			ujb.setBinderCache(old);
		}
	}
	
	boolean isAssignCompatible(IJavaType varType, IRNode expr) {
		IJavaType type = getArgTypeIfPossible(expr, null);
		// TODO what should this be?
		if (varType instanceof TypeVariable) {
			TypeVariable v = (TypeVariable) varType;
			return type.isSubtype(tEnv, v.getUpperBound(tEnv)); // TODO HACK
		}
		return isCallCompatible(varType, expr, type, false);
	}
	
    /*
    TODO fix calls to isCallCompatible
    Math.min(...)
    
    TODO what about poly expressions?
    */
	private boolean onlyNeedsBoxing(IJavaType formal, IRNode a, IJavaType arg) {
    	if (formal instanceof IJavaPrimitiveType) {
       		// Could unbox arg?
    		if (arg instanceof IJavaDeclaredType) {        	
    			IJavaDeclaredType argD = (IJavaDeclaredType) arg;
    			IJavaType unboxed = JavaTypeFactory.getCorrespondingPrimType(argD);
    			return unboxed != null && isCallCompatible(formal, a, unboxed, false);  
    		} 
    		else if (arg instanceof IJavaReferenceType) {
    			IJavaPrimitiveType formalP = (IJavaPrimitiveType) formal;
    			IJavaType boxedEquivalent = JavaTypeFactory.getCorrespondingDeclType(tEnv, formalP);
    			return boxedEquivalent != null && isCallCompatible(boxedEquivalent, a, arg, false);
    		}
    	}
    	else if (formal instanceof IJavaReferenceType && arg instanceof IJavaPrimitiveType) {
    		// Could box arg?
    		IJavaPrimitiveType argP = (IJavaPrimitiveType) arg;
    		IJavaType boxed         = JavaTypeFactory.getCorrespondingDeclType(tEnv, argP);
    		return boxed != null && isCallCompatible(formal, a, boxed, false); 
    	}
    	else if (formal instanceof IJavaDeclaredType && arg instanceof IJavaDeclaredType) {
    		IJavaDeclaredType fdt = (IJavaDeclaredType) formal;
    		IJavaDeclaredType adt = (IJavaDeclaredType) arg;    		
    		// Hack since Class can take primitive types
    		final IRNode cls = tEnv.findNamedType("java.lang.Class");
    		if (fdt.getDeclaration().equals(cls) && adt.getDeclaration().equals(cls)) {
    			if (fdt.getTypeParameters().isEmpty() || adt.getTypeParameters().isEmpty()) {
    				return false;
    			}
    			return onlyNeedsBoxing(fdt.getTypeParameters().get(0), a, adt.getTypeParameters().get(0));
    		}
    	}
    	return false;
    }
	
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
    	
		public MethodBinding8 isApplicable(ICallState call, MethodBinding m) {			
			if (kind != InvocationKind.VARARGS && 
				(call.numArgs() != m.getNumFormals() ||	call.needsVarArgs() && !m.isVariableArity())) {
				return null;
			}
			if (m.isGeneric()) {
				if (call.getNumTypeArgs() == 0) {				
					final boolean debug = false && call.toString().equals("crlPaths.stream.map((#) -> #.getAbsolutePath#).map(#.fileSystem:: <> readFileBlocking)");
		    		if (debug) {		
		    			System.out.println("Checking if applicable: "+m);
		    		}
					BoundSet bounds = typeInfer.inferForInvocationApplicability(call, m, getKind(), false);
					return bounds == null ? null : MethodBinding8WithBoundSet.create(MethodBinder8.this, call, m, tEnv, bounds, getKind());
				} else {							
					if (call.getNumTypeArgs() != m.numTypeFormals) {
						return null;
					}
					final IJavaTypeSubstitution methodTypeSubst = FunctionParameterSubstitution.create(binder, m.bind, call.getTypeArgs());
					if (!isApplicableAndCompatible(call, m, methodTypeSubst, context, usesVarargs())) {
						return null;
					}
					int i=0;
					for(IRNode tf : TypeFormals.getTypeIterator(m.typeFormals)) {
						IJavaType u_l = call.getTypeArg(i);
						IJavaType b_l = JavaTypeFactory.getTypeFormal(tf).getExtendsBound(tEnv);
						IJavaType b_subst = Util.subst(m.convertType(binder, b_l), methodTypeSubst);
						if (!tEnv.isSubType(u_l, b_subst)) {
							return null;
						}
						i++;
					}
					return MethodBinding8.create(call, m, tEnv, methodTypeSubst, getKind());
				}
			}
			// subst for rebounded formals?
			final IJavaTypeSubstitution subst;
			if (true) {
				final TypeFormalCollector v = new TypeFormalCollector(false);
				// copied from isApplicableAndCompatible()
				for(IJavaType pType : m.getParamTypes(binder, call.numArgs(), usesVarargs())) {
					if (pType == null) {
						m.getParamTypes(binder, call.numArgs(), usesVarargs());
					}
					pType.visit(v);
				}		
				subst = v.getSubst(tEnv);
			} else {
				subst = IJavaTypeSubstitution.NULL;
			}
			/*
			// WORKING
			if (subst != IJavaTypeSubstitution.NULL) {
				System.out.println("Using nonnull subst");
			}
			*/
			final boolean usesVarargs = usesVarargs();
			if (isApplicableAndCompatible(call, m, subst, context, usesVarargs)) {
				return MethodBinding8.create(call, m, tEnv, subst, getKind());
			}
			return null;
		}
    }
    
    // Assumes that #formals == #args
	// Check each arg for applicability / compatibility
	boolean isApplicableAndCompatible(ICallState call, MethodBinding m, IJavaTypeSubstitution substForParams, ArgCompatibilityContext context, boolean varArity) {
		final int numArgs = call.numArgs();
		int i=0;
		for(IJavaType pType : m.getParamTypes(binder, numArgs, varArity)) {
			final IRNode arg = call.getArgOrNull(i);		
			if (!isPertinentToApplicability(m, call.getNumTypeArgs() > 0, arg)) {
				i++;
				continue; // Ignore this one
			}
			//IJavaType pType = binder.getJavaType(ParameterDeclaration.getType(param));
			final IJavaType substType = Util.subst(pType, substForParams);
			// Try to get the arg type if the arg is null
			final IJavaType argType = arg != null ? null : call.getArgType(i);
			if (!context.isCompatible(null, substType, arg, argType)) {
				m.getParamTypes(binder, numArgs, varArity);
				context.isCompatible(null, substType, arg, argType);
				return false;										
			}
			i++;
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
		public MethodBinding8 isApplicable(ICallState call, MethodBinding m) {
    		if (!m.isVariableArity()) {
    			return null;
    		}
    		// TODO what else to do?
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
    private MethodBinding8 findMostSpecific(final ICallState call, Collection<MethodBinding> methods, ApplicableMethodFilter filter) {
    	/*
    	if (call.getNode() != null && "of".equals(JJNode.getInfoOrNull(call.getNode()))) {
    		System.out.println("Trying to find method for allOf");
    	}
    	if ("ugi.doAs(new # # { # public # # run# # # })".equals(call.toString())) {
    		System.out.println("Found call to doAs()");
    	}    	
    	*/
    	if (call instanceof RefState && methods.size() == 1) {
    		// HACK
    		MethodBinding temp = methods.iterator().next();
    		if (temp instanceof MethodBinding8) {
    			return (MethodBinding8) temp;
    		}
    		return MethodBinding8.create(call, temp, tEnv, null, filter.getKind());
    	}
    	
    	// Arrays.stream(#.readLine#.split(#)).map(String:: <> trim)
    	final Set<MethodBinding8> applicable = new HashSet<MethodBinding8>();
    	for(MethodBinding mb : methods) {
    		MethodBinding8 result = filter.isApplicable(call, mb);
    		if (result != null) {
    			applicable.add(result);
    		}
    	}
    	if (applicable.isEmpty()) {
    		return null;
    	}
    	else if (applicable.size() == 1) {
    		return applicable.iterator().next();
    	}
    	// Kept as more specific to each other
    	final Set<MethodBinding8> maxSpecific = new HashSet<MethodBinding8>();
    	for(MethodBinding8 mb : applicable) {
    		if (maxSpecific.isEmpty()) {
    			maxSpecific.add(mb);
    			continue;
    		} 
    		final MethodBinding8 first = maxSpecific.iterator().next();
    		if (isMoreSpecific(call, mb, first, filter.getKind())) {
    			if (!isMoreSpecific(call, first, mb, filter.getKind())) {
    				// A new most specific so far
    				maxSpecific.clear(); 
    			}
    			maxSpecific.add(mb);
    		}
    	}
    	if (maxSpecific.isEmpty()) {
    		return null;
    	}
    	else if (maxSpecific.size() == 1) {
    		return maxSpecific.iterator().next();
    	}
    	MethodBinding8 concrete = null;    	
    	for(MethodBinding8 mb : maxSpecific) {
    		if (mb.isConcrete()) {
    			if (concrete == null) {
    				concrete = mb;
    			}
    			// TODO is this in the right place?
    			else if (mb.bind.getContextType().isSubtype(tEnv, concrete.bind.getContextType())) {
    				concrete = mb;
    			}
    			else if (!concrete.bind.getContextType().isSubtype(tEnv, mb.bind.getContextType())) {
    				throw new IllegalStateException("Ambiguous call to "+mb+" or "+concrete);
    			}
    		}
    		// Otherwise abstract or default
    	}
    	if (concrete != null) {
        	return concrete;
    	}
    	// Return one arbitrarily
    	//return maxSpecific.iterator().next().getFinalResult();
    	//
    	// Deal with possible overrides 
    	MethodBinding8 rv = null;   
    	for(MethodBinding8 mb : maxSpecific) {
			if (rv == null) {
				rv = mb;
			}
			// TODO is this in the right place?
			// TODO check if they have the same signature?
			else if (tEnv.isRawSubType(mb.bind.getContextType(), rv.bind.getContextType())) {
				rv = mb;
			}
			/* Just use the first one, since they're all abstract (concrete ones are handled above)
			 * 
			else if (!tEnv.isRawSubType(rv.bind.bind.getContextType(), mb.bind.bind.getContextType())) {
				throw new IllegalStateException("Ambiguous call to "+mb+" or "+concrete);
			}
			*/
    	}
    	return rv;
	}

    static class MethodBinding8 extends MethodBinding implements IMethodBinding8 {
    	final InvocationKind kind;
    	//final String call;
    	final ICallState state;
    	
    	MethodBinding8(ITypeEnvironment tEnv, ICallState call, IBinding b, InvocationKind invocationKind) {
    		super(b);
    		/*
    		String unparse = call.toString();
    		if (unparse.startsWith("<implicit>.expect(#.targetMethod).andReturn(")) {
    			System.out.println("Found <implicit>.expect(#.targetMethod).andReturn(...)");
    		}
    		*/
    		if (call.getReceiverType() != null && call.getReceiverType() != b.getReceiverType()) {
    			if (call.getReceiverType().isSubtype(tEnv, b.getReceiverType())) {
    				// TODO Update the binding? (too late!)
    			} else {
    				throw new IllegalStateException();
    			}
    		}
    		kind = invocationKind;
    		//this.call = call.toString(); 
    		state = call;
    	}
    	
    	public boolean isConcrete() {
			final int mods = JavaNode.getModifiers(mdecl);
			return !JavaNode.isSet(mods, JavaNode.ABSTRACT | JavaNode.DEFAULT);
		}

		static MethodBinding8 create(ICallState call, MethodBinding m, ITypeEnvironment tEnv, 
				                  IJavaTypeSubstitution newTypeSubst, InvocationKind kind) {
			final IBinding newB = reworkBinding(call, m.bind, tEnv, newTypeSubst);
			// TODO check for same binding?
    		return new MethodBinding8(tEnv, call, newB, kind); 
		}
		
		static IBinding reworkBinding(ICallState call, IBinding b, ITypeEnvironment tEnv, IJavaTypeSubstitution newTypeSubst) {
			//System.out.println("Receiver for "+call+" : "+call.getReceiverType());
			final boolean sameReceiver = call.getReceiverType() == b.getReceiverType();
			final boolean sameSubst = newTypeSubst == null || b.getSubst() == newTypeSubst;
			if (sameReceiver && sameSubst) {
				return b;
			}			
			return IBinding.Util.makeMethodBinding(b, b.getContextType(), // TODO is this right? (for diamond op)?
					                               newTypeSubst, call.getReceiverType(), tEnv);
		}
		
		@Override
		public String toString() {
			//return bind.toString();
			//
			//return call;
			return state.toString();
		}

		@Override
		public InvocationKind getInvocationKind() {
			return kind;
		}

		@Override
		public BoundSet getInitialBoundSet() {
			return null;
		}

		/**
		 * Apply the substitution to the receiver/context type and 
		 */
		final MethodBinding8 substReceiver(ITypeEnvironment tEnv, IJavaTypeSubstitution s) {
			if (s == IJavaTypeSubstitution.NULL) {
				return this;
			}
			final ICallState origCall  = state;
			final IJavaType newRec     = getReceiverType().subst(s);
			final ICallState newCall   = origCall.replaceReceiverType(newRec);
			final IJavaDeclaredType newContext = getContextType().subst(s);
			// TODO do I need to combine the subst with the original one?
			final IBinding newB        = IBinding.Util.makeMethodBinding(this, newContext, getSubst()/*s*/, newRec, tEnv);
    		return create(tEnv, newCall, newB); 
		}

		MethodBinding8 create(ITypeEnvironment tEnv, ICallState call, IBinding b) {
			return new MethodBinding8(tEnv, call, b, kind);
		}
		
		IJavaTypeSubstitution getBoundSetSubst() {
			return IJavaTypeSubstitution.NULL;
		}
    }
    
    static class MethodBinding8WithBoundSet extends MethodBinding8 {
    	final BoundSet bounds;
    	final IJavaTypeSubstitution contextSubst;
    	private MethodBinding8WithBoundSet(ITypeEnvironment tEnv, ICallState call, IBinding m, BoundSet b, InvocationKind kind) {
    		super(tEnv, call, m, kind);
    		bounds = b;
    		contextSubst = JavaTypeSubstitution.create(tEnv, m.getContextType());
    	}
    	
    	static MethodBinding8 create(MethodBinder8 mb, ICallState c, MethodBinding m, ITypeEnvironment te, BoundSet b, InvocationKind kind) {
    		final boolean debug = false && c.toString().equals("crlPaths.stream.map((#) -> #.getAbsolutePath#).map(#.fileSystem:: <> readFileBlocking)");
    		if (debug) {		
    			System.out.println("Creating boundset: "+b);
    		}
    		IBinding newB;
    		/*
    		final boolean isPolyCall = mb.isPolyCall(c.getNode(), JJNode.tree.getOperator(c.getNode()), m);
    		if (isPolyCall) {
    			// stExps.stream.flatMap((# # exp) -> #.get#.stream)
    			// stExps.stream.flatMap((#) -> #.stream#).map((# # pmid) -> #.get(#))
    			System.out.println("Got poly call for "+c);
    		}
    		*/
    		// Need to check if this is Java 8+ code
    		//if (false && mb.tEnv.getMajorJavaVersion() >= 8 && mb.isPolyCall(c.getNode(), JJNode.tree.getOperator(c.getNode()), m)) {
    		if (mb.typeInfer.isGenericMethodRef(m)) {
        		// Don't include instantiations from computing applicability since they may not be right
    			// TODO what if the return type is a type formal, and needs some kind of substitution?
    			
    			//System.out.println("Omitting type substitution since source level is Java 8+: "+mb.tEnv);
    			newB = m.bind;
    		} else {
        		final BoundSet result = TypeInference8.resolve(b, null, debug);    		
        		/*
        		if ("br.lines.collect(Collectors.groupingBy(# -> #, #.toCollection#))".equals(c.toString())) {
        			boolean gotObject = false;
        			final Map<IJavaTypeFormal,IJavaType> map = result.computeTypeSubst(true);
        			for(Map.Entry<IJavaTypeFormal,IJavaType> e : map.entrySet()) {
        				if (e.getValue() == te.getObjectType()) {
        					System.out.println("Mapped to Object: "+e.getKey());
        					if ("R extends java.lang.Object in java.util.stream.Stream.collect(java.util.stream.Collector <? super T, A, R>)".equals(e.getKey().toString())) {
        						gotObject = true;
        					}
        				}
        			}
        			if (gotObject) {
        				System.out.println("Done with mappings");
        				result.computeTypeSubst(true);
        			}
        		}
        		*/
    			newB = reworkBinding(c, m.bind, te, result.getFinalTypeSubst(true, true));
    		}
    		return new MethodBinding8WithBoundSet(te, c, newB, b, kind);
    	}
  
    	@Override
		MethodBinding8 create(ITypeEnvironment tEnv, ICallState call, IBinding b) {
			return new MethodBinding8WithBoundSet(tEnv, call, b, bounds, kind);
		}
    	
    	@Override
		IJavaTypeSubstitution getBoundSetSubst() {
      		final BoundSet result = TypeInference8.resolve(bounds, null, false);   
      		return result.getFinalTypeSubst(true, true);			
		}
    	
		@Override
		public BoundSet getInitialBoundSet() {
			return bounds;
		}
		
		@Override
    	IJavaType getJavaType(IBinder b, IRNode formal, boolean withSubst) {
			IJavaType t = super.getJavaType(b, formal, withSubst);
			// TODO Need to use the right subst!
			if (!withSubst) {			
				// Using alternate substitution?
				return Util.subst(t, contextSubst);
			}
			return t;
		}
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
    private boolean isMoreSpecific(final ICallState call, MethodBinding8 m1, MethodBinding8 m2, InvocationKind kind) {
    	final boolean varArity = kind == InvocationKind.VARARGS;
    	if (m2.isGeneric()) {
    		// Case 1
    		return typeInfer.inferToBeMoreSpecificMethod(call, m1, kind, m2);
    	}    	
    	final int k = call.numArgs();
    	IJavaType[] m1Types = m1.getParamTypes(binder, k+1, varArity); // TODO is this right?
    	IJavaType[] m2Types = m2.getParamTypes(binder, k+1, varArity);
		// Case 2 + 3
		for(int i=0; i<k; i++) {
			if (!isMoreSpecific(m1Types[i], m2Types[i], call.getArgOrNull(i))) {
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
    boolean isMoreSpecific(IJavaType t, IJavaType s, IRNode contextExpr) {
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
    boolean isPertinentToApplicability(final MethodBinding m, boolean callHasTypeArgs, final IRNode arg) {
    	if (arg == null) {
    		return true; // Type is already computed
    	}
    	Operator op = JJNode.tree.getOperator(arg);
    	if (LambdaExpression.prototype.includes(op)) {
    		if (isImplicitlyTypedLambda(arg)) {
    			return false;
    		} 
    		// Explicitly typed
    		if (m.isGeneric() && !callHasTypeArgs && m.hasTypeParameterAsReturnType(binder)) {
    			return false;
    		}    		
    		IRNode body = LambdaExpression.getBody(arg);
    		for(IRNode expr : TypeInference8.findResultExprs(body)) {
    			if (!isPertinentToApplicability(m, callHasTypeArgs, expr)) {
    				return false;
    			}
    		}
    		return true;
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
    	return getExactMethodReference(ref) != null;
    }
    
    MethodBinding getExactMethodReference(IRNode ref) {
    	final Operator op = JJNode.tree.getOperator(ref);
    	final IRNode recv;
    	final boolean isMethod;
    	if (MethodReference.prototype.includes(op)) {
       		recv = MethodReference.getReceiver(ref);
    		isMethod = true;
    	} else {
    		recv = ConstructorReference.getType(ref);
    		isMethod = false;
    	}
    	final ReceiverKind kind = identifyReceiverKind(recv);
      	final IJavaType t = findTypeToSearchForMethodRef(recv, kind, !isMethod);
		if (!isMethod || kind == ReceiverKind.REF_TYPE) {
			if (t instanceof IJavaDeclaredType) {
				final IJavaDeclaredType dt = (IJavaDeclaredType) t;
				if (dt.isRawType(tEnv)) {
					return null;
				}
			}
		}
	  	final LookupContext context = new LookupContext();
	  	final IJavaSourceRefType st;
	  	if (isMethod) {
	  		context.use(JJNode.getInfoOrNull(ref), ref);
	  		st = (IJavaSourceRefType) t;
	  	} else {	  	
	  		// Decl type or array
	  		if (t instanceof IJavaArrayType) {
       			IJavaArrayType at = (IJavaArrayType) t;
    			List<IJavaType> params = Collections.singletonList(at.getElementType());
    			st = JavaTypeFactory.getDeclaredType(tEnv.getArrayClassDeclaration(), params, null); 
	  		} else {
	  			st = (IJavaDeclaredType) t;
	  		}
	  		context.use(JJNode.getInfoOrNull(st.getDeclaration()), ref);	
	  	}
	
    	final IJavaScope scope = isMethod ? binder.typeMemberTable(st).asScope(binder) : binder.typeMemberTable(st).asLocalScope(tEnv);
    	IBinding result = null;
    	for(IBinding m : findMethods(scope, context, isMethod, ref)) {
    		if (result == null) {
    			result = m;
    		} else {
    			return null; // More than one
    		}
    	}
    	if (result == null) {
    		return null;
    	}
    	// Check if varargs, generic w/o type args
    	MethodBinding mb = new MethodBinding(result);
    	if (mb.isVariableArity()) {
    		return null;
    	}
    	if (!mb.isGeneric() || getNumTypeParams(ref, !isMethod) > 0) {
    		return mb;
    	}
    	return null;
    }

    public static boolean containsTypeVariables(IJavaType t) {
    	BooleanVisitor v = new BooleanVisitor(false) {
			@Override
			public boolean accept(IJavaType t) {
				result |= t instanceof TypeVariable;
				return !result;
			}    		
    	};
    	t.visit(v);
    	return v.result;
    }
    
    public static boolean containsTypeVariables(IJavaFunctionType ft) {
    	if (containsTypeVariables(ft.getReturnType())) {
    		return true;
    	}
    	for(IJavaType p : ft.getParameterTypes()) {
    		if (containsTypeVariables(p)) {
    			return true;
    		}
    	}
    	return false;
    }
    
	/**
	 * The ground target type is derived from T as follows:
	 * � If T is a wildcard-parameterized functional interface type and the lambda expression 
	 *   is explicitly typed, then the ground target type is inferred as described in �18.5.3.
	 *   
	 * � If T is a wildcard-parameterized functional interface type and the lambda expression 
	 *   is implicitly typed, then the ground target type is the non-wildcard parameterization (�9.9) of T.
     * � Otherwise, the ground target type is T.
	 */
	public IJavaType computeGroundTargetType(IRNode lambda, IJavaType t) {
		IJavaType rv = computeGroundTargetType(lambda, t, true);
		/*
		if (containsTypeVariables(rv)) {
			System.out.println("Found type variable");
			computeGroundTargetType(lambda, t, true);
		}
		*/
		return rv;
	}

	public IJavaType computeGroundTargetType(IRNode lambda, IJavaType t, boolean checkForSubtype) {
		IJavaDeclaredType wpt = typeInfer.isWildcardParameterizedType(t);
		if (wpt != null) {
			if (isImplicitlyTypedLambda(lambda)) {
				return typeInfer.computeNonWildcardParameterization(wpt);
			} else {
				return typeInfer.inferForFunctionalInterfaceParameterization(t, lambda, checkForSubtype);
			}
		}
		return t;
	}
	
	/**
	 * 15.13.2 Type of a Method Reference
	 * 
	 * A method reference expression is compatible in an assignment context, invocation context, 
	 * or casting context with a target type T if T is a functional interface type (�9.8) and the
	 * expression is congruent with the function type of the ground target type derived from T.
	 * 
	 * The ground target type is derived from T as follows:
	 * � If T is a wildcard-parameterized functional interface type, then the ground target type 
	 *   is the non-wildcard parameterization (�9.9) of T.
	 * � Otherwise, the ground target type is T
	 */
	public IJavaType computeGroundTargetTypeForMethodRef(IJavaType t) {
		IJavaDeclaredType wpt = typeInfer.isWildcardParameterizedType(t);
		if (wpt != null) {			
			return typeInfer.computeNonWildcardParameterization(wpt);
		}
		return t;
	}
	
	/**
	 * 15.12.2.6 Method Invocation Type
	 * 
	 * The invocation type of a most specific accessible and applicable method is a method
	 * type (§8.2) expressing the target types of the invocation arguments, the result
	 * (return type or void ) of the invocation, and the exception types of the invocation.
	 * 
	 * It is determined as follows:
	 * 
	 * (see below)
	 */	
	public IJavaFunctionType computeInvocationType(final ICallState call, final MethodBinding8 b, boolean eliminateTypeVars) {
		return computeInvocationType(call, b, eliminateTypeVars, null);
	}
	
	public IJavaFunctionType computeInvocationType(final ICallState call, final MethodBinding8 b, boolean eliminateTypeVars, IJavaType targetType) {
		Pair<MethodBinding,BoundSet> result = typeInfer.recomputeB_2(call, b);
		final MethodBinding mb = result.first();
		final BoundSet b_2 = result.second();
		
		if (typeInfer.isGenericMethodRef(mb)) {
			/*
			 * • If the chosen method is generic and the method invocation does not provide
			 *   explicit type arguments, the invocation type is inferred as specified in §18.5.2.
			 */
			if (call.getNumTypeArgs() == 0) {
				//System.out.println("Inferring invocation type for "+call);
				if (false && call.toString().equals("ss.collect(<implicit>.toList)")) {//"Arrays.stream(#, #, #).map(#:: <> get).flatMap(Grep:: <> getPathStream)")) {
					System.out.println("Got br.lines.collect(Collectors.groupingBy(# -> #, #.toCollection#))");
					BoundSet temp = TypeInference8.resolve(b_2, null, true);
					temp.getFinalTypeSubst(eliminateTypeVars, false); 
				}
				IJavaFunctionType rv = typeInfer.inferForInvocationType(call, (MethodBinding8) mb, b_2, eliminateTypeVars, targetType);
				if (rv == null) {
					return null;
				}
				if (containsTypeVariables(rv)) {
					typeInfer.inferForInvocationType(call, (MethodBinding8) mb, b_2, eliminateTypeVars, targetType);
				}
				return rv;
			} else {
				/*
				 * • If the chosen method is generic and the method invocation provides explicit type
				 *   arguments, let P i be the type parameters of the method and let T i be the explicit
				 *   type arguments provided for the method invocation (1 ≤ i ≤ p). Then:
				 *   
				 *   – If unchecked conversion was necessary for the method to be applicable, then
				 *     the invocation type's parameter types are obtained by applying the substitution
				 *     [P 1 := T 1 , ..., P p := T p ] to the parameter types of the method's type, and the
				 *     invocation type's return type and thrown types are given by the erasure of the
				 *     return type and thrown types of the method's type.
				 *     
				 *   – If unchecked conversion was not necessary for the method to be applicable,
				 *     then the invocation type is obtained by applying the substitution 
				 *     [P 1 := T 1 , ..., P p := T p ] to the method's type.
				 */
				final Map<IJavaTypeFormal, IJavaType> map = new HashMap<IJavaTypeFormal, IJavaType>(mb.numTypeFormals);				
				final List<IJavaTypeFormal> newFormals = new ArrayList<>();
				int i=0;
				for(IRNode tf : JJNode.tree.children(mb.typeFormals)) {
					IJavaTypeFormal p_i = JavaTypeFactory.getTypeFormal(tf);
					IJavaType t_i = call.getTypeArg(i);
					map.put(p_i, t_i);
					newFormals.add(p_i);
					i++;
				}
				IJavaTypeSubstitution subst = TypeInference8.TypeSubstitution.create(tEnv.getBinder(), map);
				IJavaFunctionType mtype = computeMethodType(mb);
				
				if (b_2 == null) {
					typeInfer.recomputeB_2(call, b);
				}
				if (b_2.usedUncheckedConversion()) {
					return substParams_eraseReturn(mtype, subst);
				} else {
					return mtype.instantiate(newFormals, subst);
				}
			}
		} else {
			/*
			 * • If the chosen method is not generic, then:
			 * 
			 *   – If unchecked conversion was necessary for the method to be applicable, the
			 *     parameter types of the invocation type are the parameter types of the method's
			 *     type, and the return type and thrown types are given by the erasures of the
			 *     return type and thrown types of the method's type.
			 *     
			 *   – Otherwise, if the chosen method is the getClass method of the class Object
			 *     (§4.3.2), the invocation type is the same as the method's type, except that the
			 *     [return type is Class<? extends T> , where T is the type that was searched, as
			 *     determined by §15.12.1.
			 *   
			 *   – Otherwise, the invocation type is the same as the method's type.
			 */
			if (false && call.toString().startsWith("spy:: <> get")) {
				System.out.println("What's the problem here?");
			}
			IJavaFunctionType mtype = computeMethodType(/*m*/b, call);
			// TODO any other way to know unchecked conversion was used?
			if (b_2 != null && b_2.usedUncheckedConversion()) {
				return substParams_eraseReturn(mtype, null);
			} 
			if ("getClass".equals(JJNode.getInfo(b.getNode())) &&
				tEnv.getObjectType().equals(b.getContextType())) {
				IRNode decl = tEnv.findNamedType("java.lang.Class");
				IJavaType param = JavaTypeFactory.getWildcardType(b.getReceiverType(), null);
				return replaceReturn(mtype, JavaTypeFactory.getDeclaredType(decl, Collections.singletonList(param), null));
			}			
			return mtype;
		}
	}

	IJavaFunctionType computeMethodType(MethodBinding m) {		
		return computeMethodType(m, null);
	}

	IJavaFunctionType computeMethodType(MethodBinding m, ICallState call) {
		//IJavaType receiver = TypeUtil.isStatic(m.bind.getNode()) ? null : m.bind.getReceiverType();
		//
		// The subst below should convert the receiver to the right type
		boolean skipFirstParameter = false;
		IJavaTypeSubstitution subst = m.getSubst();
		final IJavaType receiver;
		if (TypeUtil.isStatic(m.bind.getNode())) {
			receiver = null;
		}
		else if (m.mkind == Kind.CONSTRUCTOR) {
			receiver = m.bind.getContextType();
			//skipFirstParameter = true;
		}		
		else { // A method!
			if (call != null && MethodCall.prototype.includes(call.getNode())) {		
				IRNode rec = MethodCall.getObject(call.getNode());
				if (MethodCall.prototype.includes(rec)) {
					IBinding b = binder.getIBinding(rec);
					IJavaType targetType = call.getReceiverType();
					if (targetType instanceof IJavaDeclaredType) {
						/*
						if ("<implicit>.setSelector(<implicit>.q, selector)._(cdata)".equals(call.toString())) {
							System.out.println("Looking at problematic call");
						}
						*/						
						// Try with wildcardified, and otherwise use the one without
						// TODO when shouldn't I do a subst?
						IJavaType targetType2 = wildcardify(targetType);
						IJavaType rec2 = getCompileTimeResultType(rec, b, targetType2);
						if (rec2 == null) {
							rec2 = getCompileTimeResultType(rec, b, targetType);
						}
						if (rec2 == null) {
							rec2 = getCompileTimeResultType(rec, b, targetType);
							throw new IllegalStateException("Couldn't compute result type of method: "+b+" for "+targetType);
						}
						receiver = rec2;
					} else {
						receiver = getCompileTimeResultType(rec, b, targetType); // TODO is this right?
					}
					if (receiver instanceof IJavaDeclaredType) {
						subst = JavaTypeSubstitution.create(tEnv, (IJavaDeclaredType) receiver).combine(subst);
					}
					skipFirstParameter = true;
				}
				// Same as below
				else if (m.getReceiverType() != null) {
					// Note that the receiver used already has the substitution applied?
					receiver = m.getReceiverType(); // TODO is this right?
					skipFirstParameter = true;
				}
				else {
					receiver = JavaTypeFactory.getMyThisType(m.bind.getContextType().getDeclaration());
				}
			}
			else if (m.getReceiverType() != null) {
				// Note that the receiver used already has the substitution applied?
				receiver = m.getReceiverType(); // TODO is this right?
				skipFirstParameter = true;
			}
			else {
				receiver = JavaTypeFactory.getMyThisType(m.bind.getContextType().getDeclaration());
			}
		}
		IJavaFunctionType t = JavaTypeFactory.getMemberFunctionType(m.bind.getNode(), receiver, tEnv.getBinder());
		String unparse = null;//t.toString();
		if (unparse != null && m.toString().contains("c.asSubclass(clazz)") && unparse.contains("<null-type,")) {
		  System.out.println("Introducing capture here?");
		}
		//return t;
		//return t.instantiate(t.getTypeFormals(), JavaTypeSubstitution.create(tEnv, (IJavaDeclaredType) m.bind.getContextType()));
		return t.instantiate(t.getTypeFormals(), subst, skipFirstParameter);
	}
	
	/**
	 * Replace type formals with wildcards
	 */
	private IJavaDeclaredType wildcardify(IJavaType presumedDeclaredT) {
		final IJavaDeclaredType dt = (IJavaDeclaredType) presumedDeclaredT;
		final int num = dt.getTypeParameters().size(); 
		if (num > 0) {
			// Replace any formals with wildcards?
			List<IJavaType> types = new ArrayList<>(num);
			for(int i=0; i<num; i++) {
				IJavaType temp = dt.getTypeParameters().get(i);
				if (temp instanceof IJavaTypeFormal) {
					IJavaTypeFormal f = (IJavaTypeFormal) temp;
					try {
					IJavaType bound = f.getExtendsBound(tEnv);
					if (refersTo(bound, f)) {
						// Skip these due to self-reference (SOE)
						// Ex: T extends javax.validation.Configuration <T>
					}
					else if (bound != tEnv.getObjectType()) {
						//System.out.println("Wildcardifying "+presumedDeclaredT);
						IJavaReferenceType lower = bound instanceof IJavaDeclaredType ? wildcardify(bound) : (IJavaReferenceType) bound;
						temp = JavaTypeFactory.getWildcardType(null, lower);
					} else {
						// TODO is this right?
						temp = JavaTypeFactory.wildcardTargetType;
					}	
					} catch(StackOverflowError e) {
						System.out.println("SOE: "+DebugUnparser.toString(f.getDeclaration()));
						throw e;
					}
				}
				else if (temp instanceof IJavaDeclaredType) {
					temp = wildcardify(temp);
				}
				// TODO what other cases are interesting?
				types.add(temp);
			}
			return JavaTypeFactory.getDeclaredType(dt.getDeclaration(), types, dt.getOuterType(/*TODO*/));
		}
		return dt;		
	}
	
	private boolean refersTo(IJavaType bound, IJavaTypeFormal f) {
		TypeFormalChecker c = new TypeFormalChecker();
		bound.visit(c);
		return c.formals.contains(f);
	}

	private IJavaFunctionType replaceReturn(IJavaFunctionType orig, IJavaType newReturn) {
		return JavaTypeFactory.getFunctionType(orig.getTypeFormals(), newReturn, orig.getParameterTypes(), orig.isVariable(), orig.getExceptions());
	}
	
	IJavaFunctionType substParams_eraseReturn(IJavaFunctionType orig, IJavaTypeSubstitution subst) {
		final List<IJavaType> paramTypes;
		if (subst != null) {
			paramTypes = new ArrayList<IJavaType>();
			for(IJavaType pt : orig.getParameterTypes()) {
				paramTypes.add(Util.subst(pt, subst));
			}
		} else {
			paramTypes = orig.getParameterTypes();
		}
		final IJavaType returnType = tEnv.computeErasure(orig.getReturnType());
		final Set<IJavaType> throwTypes;
		if (orig.getExceptions().isEmpty()) {
			throwTypes = Collections.emptySet();
		} else {
			throwTypes = new HashSet<IJavaType>();
			for(IJavaType e : orig.getExceptions()) {
				throwTypes.add(tEnv.computeErasure(e));
			}
		}		
		return JavaTypeFactory.getFunctionType(orig.getTypeFormals(), returnType, paramTypes, orig.isVariable(), throwTypes);
	}
	
	/**
	 * 15.13.1 Compile-Time Declaration of a Method Reference
	 * 
	 * The compile-time declaration of a method reference is the method to which the expression refers. 
	 * In special cases, the compile-time declaration does not actually exist, but is a notional method 
	 * that represents a class instance creation or an array creation. The choice of compile-time declaration 
	 * depends on a function type targeted by the expression, just as the compile-time declaration of a method
	 * invocation depends on the invocation's arguments (�15.12).
	 * 
	 * The search for a compile-time declaration mirrors the process for method invocations in �15.12.1 and 
	 * �15.12.2, as follows:
	 * 
	 * � First, a type to search is determined ...
	 * � Second, given a targeted function type with n parameters, a set of potentially applicable methods 
	 *   is identified ...
	 *   
	 *   (see findPotentiallyApplicableForMethodRef)
	 *   
	 * � Finally, if there are no potentially applicable methods, then there is no compile- time declaration.
	 * 
	 *   Otherwise, given a targeted function type with parameter types P1, ..., Pn and a set of potentially 
	 *   applicable methods, the compile-time declaration is selected as follows:
	 *   
	 *   (see below)
	 */	
	MethodBinding8 findCompileTimeDeclForRef(IJavaFunctionType ft, RefState ref) {
		final List<IJavaType> p = ft.getParameterTypes();
		final Set<IBinding> potentiallyApplicable = findPotentiallyApplicableForMethodRef(ref.base, ref.name, p.size(), ft.getTypeFormals().size());
		if (potentiallyApplicable.isEmpty()) {
			return null;
		}
	
		final Set<MethodBinding> methods = new HashSet<MethodBinding>();
		for(IBinding b : potentiallyApplicable) {
			methods.add(new MethodBinding(b));
		}
		final MethodBinding8 rv;
		if (ref.name != "new" && identifyReceiverKind(ref.base) == ReceiverKind.REF_TYPE) {
			/*   � If the method reference expression has the form ReferenceType :: [TypeArguments] Identifier, then 
			 *     two searches for a most specific applicable method are performed. Each search is as specified in 
			 *     �15.12.2.2 through �15.12.2.5, with the clarifications below. Each search may produce a method or, 
			 *     in the case of an error as specified in �15.12.2.2 through �15.12.2.5, no result.
			 *     
			 *     In the first search, the method reference is treated as if it were an invocation with argument 
			 *     expressions of types P1, ..., Pn; the type arguments, if any, are given by the method reference 
			 *     expression.
			 *     
			 *     In the second search, if P1, ..., Pn is not empty and P1 is a subtype of ReferenceType, then the 
			 *     method reference expression is treated as if it were a method invocation expression with 
			 *     argument expressions of types P2, ..., Pn. If ReferenceType is a raw type, and there exists a 
			 *     parameterization of this type, G<...>, that is a supertype of P1, the type to search is the 
			 *     result of capture conversion (�5.1.10) applied to G<...>; otherwise, the type to search is the 
			 *     same as the type of the first search. Again, the type arguments, if any, are given by the 
			 *     method reference expression.
			 *     
			 *     If the first search produces a static method, and no non-static method is applicable by �15.12.2.2, 
			 *     �15.12.2.3, or �15.12.2.4 during the second search, then the compile-time declaration is the result 
			 *     of the first search.
			 *     
			 *     Otherwise, if no static method is applicable by �15.12.2.2, �15.12.2.3, or �15.12.2.4 during the 
			 *     first search, and the second search produces a non- static method, then the compile-time declaration
			 *     is the result of the second search.
			 *     
			 *     Otherwise, there is no compile-time declaration.
			 */
			final MethodBinding8 first = tryToFindMostSpecific(ref, methods);
			final MethodBinding8 second;
			if (ft.getParameterTypes().isEmpty()) {
				second = null;
			} else {
				if (ref.prepForSecond()) {	
					second = tryToFindMostSpecific(ref, methods);					
				} else {
					second = null;
				}
			}
			boolean firstWorks = first != null && TypeUtil.isStatic(first.getNode());
			boolean secondWorks = second != null && !TypeUtil.isStatic(second.getNode());
			if (firstWorks && !secondWorks) {
				return first;
			}
			else if (!firstWorks && secondWorks) {
				return second;
			}
			rv = null;
		} else {
			/*   � For all other forms of method reference expression, one search for a most specific applicable method
			 *     is performed. The search is as specified in �15.12.2.2 through �15.12.2.5, with the clarifications below
			 *     
			 *     The method reference is treated as if it were an invocation with argument expressions of types 
			 *     P1, ..., Pn; the type arguments, if any, are given by the method reference expression.
			 *     
			 *     If the search results in an error as specified in �15.12.2.2 through �15.12.2.5, or if the 
			 *     most specific applicable method is static, there is no compile-time declaration.
			 *     
			 *     Otherwise, the compile-time declaration is the most specific applicable method
			 */
			rv = tryToFindMostSpecific(ref, methods);

			if (TypeUtil.isStatic(rv.getNode())) {
				return null;
			}
		}
		return rv;
	}
	
	public class RefState implements ICallState {
		final IJavaFunctionType type;
		final IRNode ref;
		final IRNode base;
		final String name;
		private boolean doSecond = false;
		private IJavaType[] secondArgTypes;
		private IJavaType secondReceiver;
		
		public RefState(IJavaFunctionType ft, IRNode r) {
			type = ft;
			ref = r;
			
			if (MethodReference.prototype.includes(ref)) {
				base = MethodReference.getReceiver(ref);
				name = MethodReference.getMethod(ref);
			} else {
				base = ConstructorReference.getType(ref);
				name = "new";
			}
		}

		public IRNode getNode() {
			return ref;
		}

		public int numArgs() {
			if (doSecond) {
				return type.getParameterTypes().size() - 1; 
			}
			return type.getParameterTypes().size();
		}

		public IRNode getArgOrNull(int i) {
			return null;
		}

		public IJavaType getArgType(int i) {
			if (doSecond) {
				return secondArgTypes[i];
			}
			return type.getParameterTypes().get(i);
		}

		public int getNumTypeArgs() {
			return type.getTypeFormals().size();
		}

		public IJavaType getTypeArg(int i) {
			return type.getTypeFormals().get(i);
		}

		public IJavaType[] getTypeArgs() {
			return type.getTypeFormals().toArray(JavaGlobals.noTypes);
		}

		public IRNode getReceiverOrNull() {
			return null; // TODO what else could I return?
		}
		
		public IJavaType getReceiverType() {
			if (doSecond) {
				return secondReceiver;
			}
			return binder.getJavaType(base);
		}		
		
		/**
		 *     In the second search, if P1, ..., Pn is not empty and P1 is a subtype of ReferenceType, then the 
		 *     method reference expression is treated as if it were a method invocation expression with 
		 *     argument expressions of types P2, ..., Pn. If ReferenceType is a raw type, and there exists a 
		 *     parameterization of this type, G<...>, that is a supertype of P1, the type to search is the 
		 *     result of capture conversion (�5.1.10) applied to G<...>; otherwise, the type to search is the 
		 *     same as the type of the first search. Again, the type arguments, if any, are given by the 
		 *     method reference expression.
		 */
		boolean prepForSecond() {
			if (doSecond) {
				doSecond = false;
			}
			final IJavaType refType = getReceiverType();
			doSecond = true;
			
			List<IJavaType> ptypes = type.getParameterTypes();
			if (ptypes.isEmpty()) {
				return false;
			}
			final IJavaType p_1 = ptypes.get(0);
			if (p_1.isSubtype(tEnv, refType)) {
			//if (refType.isSubtype(tEnv, p_1)) { // HACK?
				secondArgTypes = new IJavaType[ptypes.size()-1];
				for(int i=1; i<ptypes.size(); i++) {
					secondArgTypes[i-1] = ptypes.get(i);
				}
			} else {
				return false;
			}
			secondReceiver = refType;
			if (refType instanceof IJavaDeclaredType) {
				IJavaDeclaredType refDType = (IJavaDeclaredType) refType;
				if (refDType.isRawType(tEnv)) {
					IJavaType paramd_g = findParameterizedTypeAsSuperTypeOf(refDType.getDeclaration(), p_1);
					if (paramd_g != null) {
						secondReceiver = JavaTypeVisitor.captureWildcards(binder, paramd_g);
					}
				}
			}
			return true; // Ok to do second search
		}

		@Override
		public boolean needsVarArgs() {
			// TODO is this ever true?
			return false;
		}
		
        @Override 
        public String toFullyQualifiedText() {
        	return toSourceText(); // TODO
        }
        
		@Override
    	public String toString() {
			return toSourceText();
		}
	     
		@Override
		public String toSourceText() {
			return DebugUnparser.toString(ref)+" ("+type+")";
		}

		void reset() {
			doSecond = false;
		}

		@Override
		public boolean needsExactInvocation() {
			return false;
		}

		@Override
		public ICallState replaceReceiverType(IJavaType newRec) {
			throw new IllegalStateException("Not possible!?!  Have to change the function type");
		}
	}

	/**
	 * @return nonnull if there exists a parameterization of this type, G<...>, that is a supertype of P1
	 */
	public IJavaType findParameterizedTypeAsSuperTypeOf(final IRNode decl, IJavaType t) {
		if (t instanceof IJavaDeclaredType) {
			IJavaDeclaredType dt = (IJavaDeclaredType) t;
			if (decl.equals(dt.getDeclaration())) {
				return dt;
			}
		}
		for(IJavaType st : t.getSupertypes(tEnv)) {
			IJavaType rv = findParameterizedTypeAsSuperTypeOf(decl, st);
			if (rv != null) {
				return rv;
			}
		}
		return null;
	}

	// Originally to replace inferForDiamond()
	public static IJavaDeclaredType computeDiamondType(IPrivateBinder b, IRNode paramdType) {
		MethodBinder8 mb = new MethodBinder8(b, false);
		IRNode call = JJNode.tree.getParentOrNull(paramdType);
		MethodBinding8 bi = (MethodBinding8) b.getIBinding(call);
		CallState state = CallState.create(mb.binder, call, bi);
		IJavaFunctionType ft = mb.computeInvocationType(state, bi, false);
		return (IJavaDeclaredType) ft.getReturnType();
	}
}
