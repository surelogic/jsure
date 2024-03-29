package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinding.Util;
import edu.cmu.cs.fluid.java.bind.IMethodBinder.CallState;
import edu.cmu.cs.fluid.java.bind.MethodBinder8.MethodBinding8;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.surelogic.ThreadSafe;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.*;

/**
 * Class for computing the type of an AST node.
 * @author yangzhao
 */
@ThreadSafe
public class JavaTypeVisitor extends Visitor<IJavaType> {
  
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  
  static public final JavaTypeVisitor prototype = new JavaTypeVisitor();
  
  protected JavaTypeVisitor() {
	  // Nothing to do
  }
  
  static Operator getOperator(IRNode node) {
    return JJNode.tree.getOperator(node);
  }
  
  // method called for any operator without a visit method overridden.
  @Override
  public IJavaType visit(IRNode node) {
    Operator op = getOperator(node);
    if (op instanceof IHasType) {
      LOG.log(Level.SEVERE, "no type visitor type for node with operator " + op, new Throwable());
    }
    return null;
  }
  
  @Override
  public IJavaType visitAddExpression(IRNode node) {
    //The Binop code should handle string concatenation that is using
    //AddExpression.  It's supposed to be converted to StringConcat,
    //but the parser generates AddExpression nodes first, and we have to
    //handle these cases.
    IRNode node1 = BinopExpression.getOp1( node );
    IJavaType type1 = doAccept( node1 );
    
    IRNode node2 = BinopExpression.getOp2( node );
    IJavaType type2 =  doAccept( node2 );
    
    IJavaType stringType = binder.getTypeEnvironment().getStringType();
    if( type1 == stringType || type2 == stringType ) {
      return stringType;
    } else {
      return visitArithBinopExpression( node );
    }
  }
  
  @Override
  public IJavaType visitAnnotatedType(IRNode node) {
	return doAccept(AnnotatedType.getBase(node));
  }
  
  @Override
  public IJavaType visitAnnotation(IRNode node) {      
	  /*
	  if ("Override".equals(Annotation.getId(node))) {
		  IRNode type = VisitUtil.getEnclosingType(node);
		  if (AnonClassExpression.prototype.includes(type)) {
			  String unparse = DebugUnparser.toString(type);
			  if (unparse.startsWith("new edu.cmu.cs.fluid.java.bind.ModulePromises . Rule <edu.cmu.cs.fluid.ir.IRNode>")) {
				  System.out.println("Getting type for: "+DebugUnparser.toString(node));
				  System.out.println();
				  if (AbstractJavaBinder.issueCount.get() == 0) {
					  AbstractJavaBinder.foundIssue = true;
				  } else {
					  AbstractJavaBinder.issueCount.decrementAndGet();
				  }
			  }
		  }
	  }	
	  */  
	  IBinding b = binder.getIBinding(node);
	  if (b == null) {
		  //System.out.println("Got null binding for "+DebugUnparser.toString(node));
		  //binder.getBinding(node);
		  return null;
	  }
	  IJavaType rv = binder.getTypeEnvironment().convertNodeTypeToIJavaType( b.getNode() );
	  if (rv == null) {
		  System.out.println("No type for "+DebugUnparser.toString(node));
	  }
	  return rv;
  }
  
  @Override
  public IJavaType visitAnnotationDeclaration(IRNode node) {
    return JavaTypeFactory.getDeclaredType(node, Collections.<IJavaType>emptyList(), null);
  }
  
  @Override
  public IJavaType visitAnnotationElement(IRNode node) {
	return doAccept(AnnotationElement.getType(node));
  }
  
  @Override
  public IJavaType visitAnonClassExpression(IRNode node) {
    return JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
	// return doAccept(AnonClassExpression.getType(node));
  }
  
  @Override
  public IJavaType visitArithBinopExpression(IRNode node) {
    IRNode node1 = BinopExpression.getOp1( node );
    IJavaType type1 = binder.getJavaType( node1 );//doAccept( node1 );
    
    IRNode node2 = BinopExpression.getOp2( node );
    IJavaType type2 = binder.getJavaType( node2 );//doAccept( node2 );
    return typeInference2( type1, type2 );
  }
  
  @Override
  public IJavaType visitArrayCreationExpression(IRNode node) {
    IRNode n           = ArrayCreationExpression.getBase( node );
    IJavaType baseType = binder.getTypeEnvironment().convertNodeTypeToIJavaType( n );
    IRNode dimEs       = ArrayCreationExpression.getAllocated( node );
    int dims           = ArrayCreationExpression.getUnallocated(node) +
                         JJNode.tree.numChildren(dimEs);
    IJavaType capture = captureWildcards(binder, baseType);
    return JavaTypeFactory.getArrayType( capture, dims );
  }
  
  @Override
  public IJavaType visitArrayInitializer(IRNode node) {
	IRNode parent = JJNode.tree.getParent(node);
	if (JJNode.tree.getOperator(parent) instanceof ArrayCreationExpression) {
      return visitArrayCreationExpression(parent);
	}  	  
    IJavaType current = null;
    for(IRNode n : ArrayInitializer.getInitIterator(node)) {
      IJavaType temp = doAccept(n);
      if (current == null || 
          !binder.getTypeEnvironment().isAssignmentCompatible(current, temp, n)) {
        current = temp;
      }
    }      
    if (current != null) {
      return JavaTypeFactory.getArrayType(current, 1);
    }
    return JavaTypeFactory.getArrayType(binder.getTypeEnvironment().getObjectType(), 1);
  }
  
  @Override
  public IJavaType visitArrayLength(IRNode node) {
    return JavaTypeFactory.intType;
  }
  
  @Override
  public IJavaType visitArrayRefExpression(IRNode node) {
    IRNode n = ArrayRefExpression.getArray( node );
    IJavaType type1 = doAccept( n );
    if( type1 instanceof IJavaArrayType ) {
      IJavaType ty = ((IJavaArrayType)type1).getElementType();
      return captureWildcards(binder, ty);
    }else {
      return null;
    }
  }
  
  @Override
  public IJavaType visitArrayType(IRNode node) {
    return binder.getTypeEnvironment().convertNodeTypeToIJavaType( node );
  }
  
  @Override
  public IJavaType visitAssignExpression(IRNode node) {
    IRNode lvalue = AssignExpression.getOp1( node );
    IJavaType type = doAccept(lvalue);
    //Not for the lvalue
    //return captureWildcards(binder, type);
    return type;
  }
  
  @Override
  public IJavaType visitBooleanLiteral(IRNode node) {
    return JavaTypeFactory.booleanType;
  }
  
  @Override
  public IJavaType visitBooleanType(IRNode node) {
    return JavaTypeFactory.booleanType;
  }
  
  @Override
  public IJavaType visitBoxExpression(IRNode node) {
	IJavaType t           = doAccept(BoxExpression.getOp(node));
	if (!(t instanceof IJavaPrimitiveType)) {
		doAccept(BoxExpression.getOp(node));
		return null;
	}
    IJavaPrimitiveType pt = (IJavaPrimitiveType) t;
    String qname          = pt.getCorrespondingTypeName();
    IRNode nt             = binder.getTypeEnvironment().findNamedType(qname);
    return JavaTypeFactory.convertNodeTypeToIJavaType(nt, binder);
  }

  @Override
  public IJavaType visitByteType(IRNode node) {
    return JavaTypeFactory.byteType;
  }
  
  @Override
  public IJavaType visitCastExpression(IRNode node) {
    IRNode decl = CastExpression.getType( node );
    return captureWildcards(binder, doAccept( decl ));
  }
  
  @Override
  public IJavaType visitCharLiteral(IRNode node) {
    return JavaTypeFactory.charType;
  }
  
  @Override
  public IJavaType visitCharType(IRNode node) {
	return JavaTypeFactory.charType;
  }
  
  @Override
  public IJavaType visitClassDeclaration(IRNode node) {
    //return JavaTypeFactory.getDeclaredType( node, null, null );
    return JavaTypeFactory.getMyThisType( node );
  }
  
  @Override
  public IJavaType visitConstructorReference(IRNode node) {
	  TypeUtils utils = new TypeUtils(binder.getTypeEnvironment());
	  return utils.getPolyExpressionTargetType(node);	    
  }
  
  @Override
  public IJavaType visitElementValueArrayInitializer(IRNode node) {
	  if (!JJNode.tree.hasChildren(node)) {
		  return JavaTypeFactory.getArrayType(JavaTypeFactory.anyType, 1);
	  }
	  IRNode first = ElementValueArrayInitializer.getValue(node, 0);
	  return JavaTypeFactory.getArrayType(doAccept(first), 1);
  }
  
  @Override
  public IJavaType visitElementValuePair(IRNode node) {
	IBinding b = binder.getIBinding(node);
	return visitAnnotationElement(b.getNode());
  }
  
  @Override
  public IJavaType visitEnumDeclaration(IRNode node) {
    return binder.getTypeEnvironment().getMyThisType( node );
  }
  
  @Override
  public IJavaType visitEnumConstantDeclaration(IRNode node) {
    IRNode decl = VisitUtil.getEnclosingType(node);
    return visitEnumDeclaration(decl);
  }
  
  @Override
  public IJavaType visitClassExpression(IRNode node) {
    IJavaType base    = doAccept( ClassExpression.getType(node) );
    IRNode nodeType   = binder.getTypeEnvironment().findNamedType("java.lang.Class");
    List<IJavaType> p = new ArrayList<IJavaType>(1);
    /*
    if (base instanceof IJavaPrimitiveType) {
    	base = JavaTypeFactory.getCorrespondingDeclType(binder.getTypeEnvironment(), (IJavaPrimitiveType) base);
    }
    */
    p.add(base);
    return JavaTypeFactory.getDeclaredType(nodeType, p, null);
  }
  
  private IJavaPrimitiveType convertToPrim(IJavaType t) {
	  if (t instanceof IJavaDeclaredType) {
		  return JavaTypeFactory.getCorrespondingPrimType((IJavaDeclaredType) t);
	  } else {
		  return (IJavaPrimitiveType) t;
	  }
  }
  
  @Override
  public IJavaType visitComplementExpression(IRNode node) {
    IRNode n = ComplementExpression.getOp( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( n ));    
    return doUnaryNumericPromotion(t);
  }

  @Override
  public IJavaType visitConstructorCall(IRNode node) {
    if (JJNode.tree.hasChildren(ConstructorCall.getArgs(node))) {
      return JavaTypeFactory.getVoidType();
    }
    return doAccept(ConstructorCall.getObject(node));
  }
  
  @Override 
  public IJavaType visitConstructorDeclaration(IRNode node) {
	IRNode type = VisitUtil.getEnclosingType(node);
	return JavaTypeFactory.convertNodeTypeToIJavaType(type, binder);
  }
  
  @Override
  public IJavaType visitConditionalAndExpression(IRNode node) {
    return JavaTypeFactory.booleanType;
  }
  @Override
  public IJavaType visitConditionalExpression(IRNode node) {
    IRNode node1 = ConditionalExpression.getIftrue( node );
    IJavaType type1 = doAccept( node1 );
    
    IRNode node2 = ConditionalExpression.getIffalse( node );
    IJavaType type2 = doAccept( node2 );
    
    IJavaType type = typeInference3( type1, node1, type2, node2 );
    return captureWildcards(binder, type);
  }
  @Override
  public IJavaType visitConditionalOrExpression(IRNode node) {
    return JavaTypeFactory.booleanType;
  }
  
  @Override
  public IJavaType visitDefaultValue(IRNode node) {
    return doAccept(DefaultValue.getValue(node));
  }
  
  @Override
  public IJavaType visitDoubleType(IRNode node) {
    return JavaTypeFactory.doubleType;
  }
  
  @Override
  public IJavaType visitFieldDeclaration(IRNode node) {
    IJavaType rv = doAccept(FieldDeclaration.getType(node));
    return rv;
  }
  
  @Override
  public IJavaType visitFieldRef(IRNode node) {
    IBinding b = binder.getIBinding( node );
    if (b == null) return null;
    IRNode n = b.getNode();
    if (n == null) {
    	return null;
    }
	IJavaType rv = b.convertType(binder, doAccept( n ));
    rv = JavaTypeVisitor.captureWildcards(getBinder(), rv);
    return rv;
  }
  
  @Override
  public IJavaType visitFloatLiteral(IRNode node) {
    String floatStr = FloatLiteral.getToken( node );
    char last       = floatStr.charAt( floatStr.length()-1 );
    if( last == 'F' || last == 'f' ) {
      return JavaTypeFactory.floatType;
    } else {
      return JavaTypeFactory.doubleType;
    }
  }
  
  @Override
  public IJavaType visitFloatType(IRNode node) {
    return JavaTypeFactory.floatType;
  }
  
  @Override
  public IJavaType visitImplicitReceiver(IRNode node) {
    return JavaTypeFactory.getThisType(node);
  }
  
  @Override
  public IJavaType visitInitialization(IRNode node) {
    return doAccept(Initialization.getValue(node));
  }
  
  @Override
  public IJavaType visitInstanceOfExpression(IRNode node) {
    return JavaTypeFactory.booleanType;
  }
  
  @Override
  public IJavaType visitIntLiteral(IRNode node) {
    String intStr = IntLiteral.getToken( node );
    //For a Integer Literal in java Language, if the end of it is 'L' or 'l', then it is long type, 
    //otherwise it is int type(not short or byte type). 
    char last = intStr.charAt( intStr.length()-1 );
    if( last == 'L' || last == 'l' ) {
      return JavaTypeFactory.longType;
    }else {
      return JavaTypeFactory.intType;
    }
  }
  
  @Override
  public IJavaType visitIntType(IRNode node) {
    return JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
  }
  
  @Override
  public IJavaType visitInterfaceDeclaration(IRNode node) {
    //return JavaTypeFactory.getDeclaredType( node, null, null );
    return JavaTypeFactory.getMyThisType( node, true, true );
  }
  
  @Override
  public IJavaType visitIntersectionType(IRNode node) {
	IJavaReferenceType result = null;
	for(IRNode t : IntersectionType.getTypeIterator(node)) {
		if (result == null) {
		  result = (IJavaReferenceType) doAccept(t);
		} else {
		  IJavaReferenceType temp = (IJavaReferenceType) doAccept(t);
		  result = JavaTypeFactory.getIntersectionType(result, temp);
		}
	}
	return result;
  }
  
  /**
   *  From 15.27.3 Type of a Lambda Expression
   *  
   *  If a lambda expression is compatible with a target type T , then the type of the
   *  expression, U , is the ground target type derived from T .
   */
  @Override
  public IJavaType visitLambdaExpression(final IRNode node) {
	  //String unparse = DebugUnparser.toString(node);
	  TypeUtils utils = new TypeUtils(binder.getTypeEnvironment());
	  IJavaType targetType = utils.getPolyExpressionTargetType(node, true);
	  MethodBinder8 mb = new MethodBinder8((IPrivateBinder) binder, false);
	  if (MethodBinder8.containsTypeVariables(targetType)) {
		  utils.getPolyExpressionTargetType(node, true); // java.util.function.Function<? super java.lang.String,? extends &2k903v[..., java.lang.String] & java.lang.String>
	  }
	  return mb.computeGroundTargetType(node, targetType);
  }
  
  @Override
  public IJavaType visitLogBinopExpression(IRNode node) {
    IRNode node1 = BinopExpression.getOp1( node );
    IJavaType node1_type = doAccept( node1 );
    IRNode node2 = BinopExpression.getOp2( node );
    IJavaType node2_type = doAccept( node2 );
    return typeInference1( node1_type, node2_type );
  }
  
  @Override
  public IJavaType visitLongType(IRNode node) {
    return JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
  }
  
  @Override
  public IJavaType visitMethodCall(IRNode node) {
    IBinding b = binder.getIBinding( node );
    IRNode targs = MethodCall.getTypeArgs(node);
	IRNode args = MethodCall.getArgs(node);
	CallState call = new CallState(binder, node, targs, args , b.getReceiverType(), MethodCall.getObject(node));
    IJavaType type = computeReturnType(call, b);    
    //return captureWildcards(binder, type);
    return type;
  }
  
  public static IJavaType computeReturnType(IBinder binder, IBinding mb, CallState call) {
	  if (ConstructorDeclaration.prototype.includes(mb.getNode())) {
		  if (mb.getContextType() == null) {
			  throw new IllegalStateException();
		  }
		  return mb.getContextType();
	  }	  
	  JavaTypeVisitor jtv = JavaTypeVisitor.prototype;
	  synchronized ( jtv ) {
		  final IBinder preBinder = jtv.binder;
		  jtv.setBinder(binder);
		  try {
			  return jtv.computeReturnType(call, mb);
		  } finally {
			  jtv.setBinder(preBinder);
		  }
	  }
  }
  
  private IJavaType computeReturnType(CallState call, IBinding b) {
    if (b == null) return null;
    IRNode n = b.getNode();
    Operator op = JJNode.tree.getOperator( n );
    if( op instanceof MethodDeclaration ) {
      if (processJava8) {
    	 MethodBinder8 mb = new MethodBinder8((IPrivateBinder) binder, false);
    	 IJavaFunctionType ft = mb.computeInvocationType(call, (MethodBinding8) b, false);
    	 if (ft == null) {
    		 ft = mb.computeInvocationType(call, (MethodBinding8) b, false);
    	 }
    	 return ft.getReturnType();
      }
    	
      // Check if Object.getClass()
      final IJavaDeclaredType objectT = binder.getTypeEnvironment().getObjectType();
      if (b.getContextType() != null &&
    	  b.getContextType().equals(objectT) && 
          MethodDeclaration.getId(n).equals("getClass") &&
          JJNode.tree.numChildren(MethodDeclaration.getParams(n)) == 0) {
        IJavaReferenceType upper = (IJavaReferenceType) binder.getTypeEnvironment().computeErasure(b.getReceiverType());
        IRNode classDecl        = binder.getTypeEnvironment().findNamedType("java.lang.Class");
        List<IJavaType> params = new ArrayList<IJavaType>(1);
        params.add(JavaTypeFactory.getWildcardType(upper, null));
        /*        
        if (upper.equals(objectT)) {
          params.add(JavaTypeFactory.getWildcardType(null, null));
        } else {
          params.add(JavaTypeFactory.getWildcardType(upper, null)); 
        }
        */
        return JavaTypeFactory.getDeclaredType(classDecl, params, null);
      }
      // FIX IRNode typeParams = MethodDeclaration.getTypes( n );
      /*
      if () {
        
      }
      */
      /*
      if (MethodDeclaration.getId(n).equals("toArray")) {
    	  System.out.println("Getting return type for call: "+DebugUnparser.toString(node));
      }
      */
      IRNode returnType = MethodDeclaration.getReturnType( n );      
      return b.convertType(binder, binder.getTypeEnvironment().convertNodeTypeToIJavaType( returnType ));
    } else if (op instanceof AnnotationElement) {
      IRNode returnType = AnnotationElement.getType(n);
      return b.convertType(binder, binder.getTypeEnvironment().convertNodeTypeToIJavaType( returnType ));
    } else {
      return null;
    }
  }
  
  static IJavaDeclaredType computeErasure(IJavaDeclaredType t) {
	if (t == null) {
	  return null;
	}
    if (t.getTypeParameters().size() > 0) {
      return JavaTypeFactory.getDeclaredType(t.getDeclaration(), 
                                             Collections.<IJavaType>emptyList(), 
                                             t.getOuterType());
    }
    return t;
  }

  @Override
  public IJavaType visitMethodDeclaration(IRNode node) {
	  return binder.getJavaType(MethodDeclaration.getReturnType(node));
  }
  
  @Override
  public IJavaType visitMethodReference(IRNode node) {
	  TypeUtils utils = new TypeUtils(binder.getTypeEnvironment());
	  return utils.getPolyExpressionTargetType(node);	    
  }
  
  @Override
  public IJavaType visitMinusExpression(IRNode node) {
    IRNode n             = MinusExpression.getOp( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( n ));    
    return doUnaryNumericPromotion(t);
  }

  
  @Override
  public IJavaType visitNameType(IRNode node) {
	/*
    String unparse = DebugUnparser.toString(node);
	if (unparse.contains("DelegationTokenRenewer.Renewable")) {
		System.out.println("Computing type for DelegationTokenRenewer.Renewable");
	}
	
	try {
	*/
		return binder.getTypeEnvironment().convertNodeTypeToIJavaType( node );
	/*
	} catch(StackOverflowError e) {
		System.out.println("Stack overflow on "+unparse);
		throw e;
	}
	*/
  }
  
  @Override
  public IJavaType visitNamedSuperExpression(IRNode node) {
    //IJavaSourceRefType dt = JavaTypeFactory.getThisType(node);
    //return dt.getSuperclass(binder.getTypeEnvironment());
	IRNode type = NamedSuperExpression.getType( node );
	return doAccept( type );
  }
  
  @Override
  public IJavaType visitNamedType(IRNode node) {
    /*
	if (DebugUnparser.toString(node).contains("Context")) {
		System.out.println("Computing type for NamedType Context");
	}
    */
    return binder.getTypeEnvironment().convertNodeTypeToIJavaType( node );
  }
  
  @Override
  public IJavaType visitNameExpression(IRNode node) {
    IBinding b = binder.getIBinding( node );
    if (b == null) return null;
    IRNode n = b.getNode();
    if (n == null) return null;
    /*
    String unparse = DebugUnparser.toString(node);
    if (unparse.contains("context")) {
    	System.out.println("Getting type for context");
    }
  	  IRNode eT  = VisitUtil.getEnclosingType(node);
  	  if ("MustHoldTransfer".equals(JavaNames.getTypeName(eT))) {
  		if (VariableDeclarator.prototype.includes(n)) {
  			IRNode decl  = VisitUtil.getEnclosingClassBodyDecl(n);
  			System.out.println(node);
  		}  
  	  }
    }
    */
    IJavaType rv = b.convertType(binder, doAccept( n ));
    rv = JavaTypeVisitor.captureWildcards(getBinder(), rv);
    return rv;
  }
  
  @Override
  public IJavaType visitNewExpression(IRNode node) {
    NewExpression newE = (NewExpression) getOperator(node);
    IRNode nodeType = newE.get_Type( node );
    return binder.getTypeEnvironment().convertNodeTypeToIJavaType( nodeType );
  }
  
  @Override
  public IJavaType visitNotExpression(IRNode node) {
    return JavaTypeFactory.booleanType;
  }

  @Override
  public IJavaType visitNullLiteral(IRNode node) {
    return JavaTypeFactory.getNullType();
  }
  
  @Override
  public IJavaType visitOpAssignExpression(IRNode node) {
    IRNode lvalue = OpAssignExpression.getOp1( node );
    return doAccept( lvalue );
  }
  
  @Override
  public IJavaType visitParameterDeclaration(IRNode node) {
    IJavaType rv = binder.getTypeEnvironment().convertNodeTypeToIJavaType( ParameterDeclaration.getType( node ) );
    return rv;//Not for formals --- captureWildcards(binder, rv);
  }
  
  @Override
  public IJavaType visitParameterizedType(IRNode node) {
    return binder.getTypeEnvironment().convertNodeTypeToIJavaType( node );
  }
  
  @Override
  public IJavaType visitParenExpression(IRNode node) {
    IRNode n = ParenExpression.getOp( node );
    return doAccept( n );
  }

  @Override
  public IJavaType visitPlusExpression(IRNode node) {
    IRNode n = PlusExpression.getOp( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( n ));    
    return doUnaryNumericPromotion(t);
  }

  @Override
  public IJavaType visitCrementExpression(IRNode node) {
    IRNode n = UnopExpression.getOp( node );
    if (UnboxExpression.prototype.includes(n)) {
      return doAccept( UnboxExpression.getOp(n) );
    }
    return doAccept( n );
  }

  @Override
  public IJavaType visitOuterObjectSpecifier(IRNode node) {
    IRNode n = OuterObjectSpecifier.getCall(node);
    return doAccept( n );
  }
  
  @Override
  public IJavaType visitQualifiedReceiverDeclaration(IRNode node) {
	  return QualifiedReceiverDeclaration.getJavaType(this.binder, node);
  }
  /*
  private IJavaDeclaredType handleTypeAST(IRNode n) {
	  Operator op = JJNode.tree.getOperator(n);
	  IJavaDeclaredType outer = null;
	  String name;
	  if (op instanceof TypeRef) {
		  IRNode base = TypeRef.getBase(n);
		  outer       = handleTypeAST(base);
		  name        = outer.getName()+'.'+TypeRef.getId(n);
	  } 
	  else { // Assume to be NamedType
		  name = NamedType.getType(n);
	  }
	  IRNode decl = binder.getTypeEnvironment().findNamedType(name);
	  return JavaTypeFactory.getDeclaredType(decl, null, outer);	  
  }
  */
  @Override
  public IJavaType visitQualifiedSuperExpression(IRNode node) {	
	IJavaDeclaredType dt = (IJavaDeclaredType) binder.getJavaType(QualifiedSuperExpression.getType( node ));
	if (dt.isRawType(binder.getTypeEnvironment())) {
		// See if I can get type args
		IBinding b = binder.getIBinding(node);
		dt = (IJavaDeclaredType) b.convertType(binder, doAccept(b.getNode()));
	}
	if (InterfaceDeclaration.prototype.includes(dt.getDeclaration())) {
		// Special case to handle diamond inheritance of default methods?
		return dt;
	}
    return dt.getSuperclass(binder.getTypeEnvironment());
  }
  @Override
  public IJavaType visitQualifiedThisExpression(IRNode node) {
    IRNode n = QualifiedThisExpression.getType( node );
    IJavaDeclaredType rv = (IJavaDeclaredType) doAccept( n );
	if (rv.isRawType(binder.getTypeEnvironment())) {
		// See if I can get type args
		IBinding b = binder.getIBinding(node);
		rv = (IJavaDeclaredType) b.convertType(binder, doAccept(b.getNode()));
	}
	return rv;
  }
  
  @Override
  public IJavaType visitReceiverDeclaration(IRNode node) {
    return JavaTypeFactory.getThisType(node);
  }

  @Override
  public IJavaType visitReturnStatement(IRNode node) {
	IRNode rnode = binder.getBinding(node);
	IRNode decl = JavaPromise.getPromisedFor(rnode);
	if (MethodDeclaration.prototype.includes(decl)) {
	  return getJavaType(MethodDeclaration.getReturnType(decl));
	}
	// Assumed to be in a lambda
	return new TypeUtils(binder.getTypeEnvironment()).getPolyExpressionTargetType(decl);
  }
  
  @Override
  public IJavaType visitReturnValueDeclaration(IRNode rvd) {
	  IRNode n = JavaPromise.getPromisedForOrNull(rvd);
	  Operator op = JJNode.tree.getOperator(n);
	  if (MethodDeclaration.prototype.includes(op)) {
		  return getJavaType(MethodDeclaration.getReturnType(n));
	  }
	  else if (ConstructorDeclaration.prototype.includes(op)) {
		  return JavaTypeFactory.getThisType(n);
	  }
	  else if (AnnotationElement.prototype.includes(op)) {
		  return getJavaType(AnnotationElement.getType(n));
	  }
	  throw new UnsupportedOperationException("No return value for "+DebugUnparser.toString(n));
  }
  
  @Override
  public IJavaType visitRelopExpression(IRNode node) {
    return JavaTypeFactory.booleanType;
  }

  @Override
  public IJavaType visitStatementExpression(IRNode node) {
    return JavaTypeFactory.getVoidType();
  }
  
  @Override
  public IJavaType visitShiftExpression(IRNode node) {
    IRNode lvalue        = BinopExpression.getOp1( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( lvalue ));    
    return doUnaryNumericPromotion(t);
  }
  
  @Override
  public IJavaType visitShortType(IRNode node) {
    return JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
  }
  
  @Override
  public IJavaType visitStringConcat(IRNode node) {
    return binder.getTypeEnvironment().getStringType();
  }
  @Override
  public IJavaType visitStringLiteral(IRNode node) {
    return binder.getTypeEnvironment().getStringType();
  }
  
  @Override
  public IJavaType visitSuperExpression(IRNode node) {
    IJavaSourceRefType dt = JavaTypeFactory.getThisType(node);
    return dt.getSuperclass(binder.getTypeEnvironment());
  }
  
  @Override
  public IJavaType visitThisExpression(IRNode node) {
    return JavaTypeFactory.getThisType(node);
  }
  
  @Override
  public IJavaType visitType(final IRNode here) {
	// This should only appear in param decls for lambdas
	final IRNode parent = JJNode.tree.getParent(here);
	final Operator pop = JJNode.tree.getOperator(parent);
	IRNode node = parent;
	if (ParameterDeclaration.prototype.includes(pop)) {
		node = JJNode.tree.getParent(parent);
    	// Figure out which parameter this is
		final int pIndex = JJNode.tree.childLocationIndex(node, JJNode.tree.getLocation(parent));		
		node = JJNode.tree.getParent(node);
		
		final Operator op = JJNode.tree.getOperator(node);
		if (LambdaExpression.prototype.includes(op)) {
			// Match up the param with the method in the interface
			IJavaReferenceType targetType = (IJavaReferenceType) visitLambdaExpression(node);
	    	IJavaTypeSubstitution subst = JavaTypeSubstitution.createSubst(binder.getTypeEnvironment(), targetType);
	    	IJavaFunctionType ftype = binder.getTypeEnvironment().isFunctionalType(targetType);
	    	if (ftype == null) {
	    	  binder.getTypeEnvironment().isFunctionalType(targetType);
	    	  throw new IllegalStateException("No function type for "+DebugUnparser.toString(node)+" : "+targetType); 
	    	}
	    	// TODO must have a receiver?
	    	IJavaType type = ftype.getParameterTypes().get(pIndex);
	    	return Util.subst(type, subst);
	    	/*
	    	// TODO where else do I need to know what the method is?
	    	
	    	// TODO could be in a supertype of the target type
	    	for(IRNode m : VisitUtil.getClassMethodsOnly(targetType.getDeclaration())) {
		    	// TODO filter out abstract methods originally from Object
	    		IRNode params = MethodDeclaration.getParams(m);
	    		IRNode param = Parameters.getFormal(params, pIndex);
	    		IJavaType type = visitParameterDeclaration(param);
	    		return Util.subst(type, subst);
	    	}
	    	*/
		} else {
			System.out.println("Not a lambda");
		}
	}
	throw new IllegalStateException("Type not in a lambda: "+DebugUnparser.toString(node));
  }
  
  @Override
  public IJavaType visitTypeExpression(IRNode node) {
    IRNode decl = TypeExpression.getType( node );
    return doAccept( decl );
  }
  
  @Override 
  public IJavaType visitTypeFormal(IRNode node) {
    return JavaTypeFactory.getTypeFormal(node);
  }
  
  @Override
  public IJavaType visitTypeRef(IRNode node) {
    return binder.getTypeEnvironment().convertNodeTypeToIJavaType( node );
  }
  
  @Override
  public IJavaType visitUnboxExpression(IRNode node) {
    IJavaType ty = doAccept(UnboxExpression.getOp(node));
    if (ty instanceof IJavaDeclaredType) {
      return JavaTypeFactory.getCorrespondingPrimType((IJavaDeclaredType) ty);
    } else {
      return null; // oh well
    }
  }
  
  @Override
  public IJavaType visitUnionType(IRNode node) {
	List<IJavaType> types = new ArrayList<IJavaType>();
	for(IRNode type : UnionType.getTypeIterator(node)) {
		types.add(doAccept(type));	
	}	
	return JavaTypeFactory.getUnionType(types);
  }
  
  @Override
  public IJavaType visitVarArgsExpression(IRNode node) {
    int num = JJNode.tree.numChildren(node);
    if (num > 0) {
      //IJavaType t = doAccept( VarArgsExpression.getArg(node, 0) );        	
      IJavaType t = null;
      // Look at all the exprs and combine
      for(IRNode e : VarArgsExpression.getArgIterator(node)) {
    	  final IJavaType eT = doAccept(e);
    	  if (t instanceof IJavaReferenceType) {
    		  // TODO is this right?
    		  final IJavaType t2 = eT instanceof IJavaPrimitiveType ? forceBoxed(eT) : eT; 
    		  t = typeInference3(t, null, t2, e);
    	  } else if (t == JavaTypeFactory.booleanType) {
    		  if (eT != JavaTypeFactory.booleanType) {
        		  throw new IllegalStateException("Not boolean: "+eT);
    		  }
    		  // continue
    	  } else if (t == eT) {
    		  continue; 
    	  } else if (t instanceof IJavaPrimitiveType) {
    		  t = typeInference2(t, eT); // TODO not right for byte, char, short
    	  } else if (t != null) {
    		  throw new IllegalStateException("Unknown type: "+eT);
    	  } else {
    		  t = eT;
    	  }
      }
      return JavaTypeFactory.getArrayType(t, 1);
    }
    //  FIX what should this be?
    return JavaTypeFactory.getArrayType(JavaTypeFactory.anyType, 1); 
  }
  
  @Override
  public IJavaType visitVarArgsType(IRNode node) {
    IJavaType baseT = doAccept(VarArgsType.getBase(node)); 
    return JavaTypeFactory.getArrayType(baseT, 1);    
  }
  
  @Override
  public IJavaType visitVariableUseExpression(IRNode node) {
    IBinding b = binder.getIBinding( node );
    if (b == null) return null;
    IRNode n = b.getNode();
    if (b.getContextType() != null) {
      LOG.info("expected binding of local to have empty content: " + b);
    }
    //return b.convertType(binder, doAccept( n )); // probably a NOP
    IJavaType rv = b.convertType(binder, doAccept( n ));
    rv = JavaTypeVisitor.captureWildcards(getBinder(), rv);
    return rv;
  }
  
  @Override
  public IJavaType visitVariableDeclarator(IRNode node) {
    IJavaType jt = binder.getTypeEnvironment().convertNodeTypeToIJavaType( VariableDeclarator.getType( node ) );
    int dims = VariableDeclarator.getDims(node);
    if (dims > 0) jt = JavaTypeFactory.getArrayType(jt,dims);
    return jt;
  }
  
  @Override
  public IJavaType visitVoidType(IRNode node) {
    return JavaTypeFactory.voidType;
  }
  
  @Override
  public IJavaType visitWildcardType(IRNode node) {
    return JavaTypeFactory.getWildcardType(null, null);
  }
  
  @Override
  public IJavaType visitWildcardSuperType(IRNode node) {
    IJavaReferenceType lower = (IJavaReferenceType) doAccept(WildcardSuperType.getLower(node));
    return JavaTypeFactory.getWildcardType(null, lower);
  }
  
  @Override
  public IJavaType visitWildcardExtendsType(IRNode node) {
    IJavaReferenceType upper = (IJavaReferenceType) doAccept(WildcardExtendsType.getUpper(node));
    return JavaTypeFactory.getWildcardType(upper, null);
  }
  
  protected IJavaPrimitiveType forceUnboxed(IJavaType ty) {
    if (ty instanceof IJavaPrimitiveType) {
      return (IJavaPrimitiveType) ty;
    } else if (ty instanceof IJavaDeclaredType) {
      return JavaTypeFactory.getCorrespondingPrimType((IJavaDeclaredType) ty);
    }
    return null;
  }
  
  /**
   * Used in the LogBinopExpression
   * @param type1
   * @param type2
   * @return
   */
  protected IJavaType typeInference1( IJavaType type1, IJavaType type2 ) {
    IJavaPrimitiveType t1 = forceUnboxed(type1);
    IJavaPrimitiveType t2 = forceUnboxed(type2);
    if( t1 == null || t2 == null ) return null;
    PrimitiveType pt1 = t1.getOp();
    PrimitiveType pt2 = t2.getOp();
    if (pt1 instanceof FloatingPointType || pt2 instanceof FloatingPointType) {
      throw new IllegalArgumentException("got floating point: "+type1+", "+type2);
    }
    return doBinaryNumericPromotion(pt1, pt2);
  } 
  
  /**
   * Used in the ArithBinopExpression.
   * @param t1
   * @param t2
   * @return
   */
  protected IJavaType typeInference2( IJavaType type1, IJavaType type2 ) {
    IJavaPrimitiveType t1 = forceUnboxed(type1);
    IJavaPrimitiveType t2 = forceUnboxed(type2);
    if( t1 != null && t2 != null ) {
      if (t1.getOp() instanceof BooleanType || t2.getOp() instanceof BooleanType) {
        throw new IllegalArgumentException("got boolean: "+t1+", "+t2);
      }
      return doBinaryNumericPromotion(t1.getOp(), t2.getOp());
    }
    return null;
  }
  
  /**
   * Used in the ConditionalExpression
   * @param type1
   * @param type2
   * @return
   */
  protected IJavaType typeInference3( IJavaType type1, IRNode node1, IJavaType type2, IRNode node2 ) {
    if( ( type1 instanceof IJavaPrimitiveType ) && ( type2 instanceof IJavaPrimitiveType ) ) {
      //If both are all primitive type:
      PrimitiveType pt1 = ((IJavaPrimitiveType)type1).getOp();
      PrimitiveType pt2 = ((IJavaPrimitiveType)type2).getOp();
      
      if( pt1 instanceof BooleanType && pt2 instanceof BooleanType ) {
        //If both are BooleanType, then return BooleanType:
        return type1;
      }else {
        if( pt1 instanceof BooleanType || pt2 instanceof BooleanType ) {
          //If one of them is BooleanType, and the other is not -> try boxing 
          IJavaType bt1 = forceBoxed(type1);
          IJavaType bt2 = forceBoxed(type2);
          return typeInference3(bt1, node1, bt2, node2);
        }
        boolean assignableTo2 = pt1 instanceof IntegralType && 
                                binder.getTypeEnvironment().isAssignmentCompatible(type2, type1, node1);
        boolean assignableTo1 = pt2 instanceof IntegralType && 
                                binder.getTypeEnvironment().isAssignmentCompatible(type1, type2, node2);    
        if (assignableTo1 != assignableTo2) {
        	// take whichever was true
        	return assignableTo1 ? type1 : type2;
        }
        else if (assignableTo1) { // both true {
        	// both true, so take the smaller one
        	return getWidthOfPrimitiveType( pt1 ) < getWidthOfPrimitiveType( pt2 ) ? type1 : type2;
        }
        else {
        	return getWidthOfPrimitiveType( pt1 ) > getWidthOfPrimitiveType( pt2 ) ? type1 : type2;        
        }
      }
    }else if ( ( type1 instanceof IJavaPrimitiveType ) || ( type2 instanceof IJavaPrimitiveType ) ){
      // FIX compensate for boxing?
      IJavaPrimitiveType pt1 = forceUnboxed(type1);
      IJavaPrimitiveType pt2 = forceUnboxed(type2); 
      if (pt1 != null && pt2 != null) {
    	return typeInference3(pt1, node1, pt2, node2);
      }      
      IJavaType bt1 = forceBoxed(type1);
      IJavaType bt2 = forceBoxed(type2);
      return typeInference3(bt1, node1, bt2, node2);
      //If one of them is primitive type, the other is not.
      //return null;
    }else if( ( type1 instanceof IJavaVoidType ) || ( type2 instanceof IJavaVoidType ) ) {
      //If either of them is VoidType
      return null;
    }else if( type1 instanceof IJavaNullType  ) {
      //If one is NullType, then it's the other
      return type2;
    }else if( type2 instanceof IJavaNullType ) {
      //If the other is NullType, then it's the one
      return type1;      
    }else if( ( type1 instanceof IJavaArrayType ) && ( type2 instanceof IJavaArrayType ) ) {
      //If both are ArrayType
      return compareIJavaArrayType( type1, type2 );
    //}else if( ( type1 instanceof IJavaDeclaredType ) && ( type2 instanceof IJavaDeclaredType ) ) {
    } else {
      if (type1.isSubtype(binder.getTypeEnvironment(), type2)) {
    	  return type2;
      }
      if (type2.isSubtype(binder.getTypeEnvironment(), type1)) {
    	  return type1;
      }
      //final IJavaType lub1 = getLowestUpperBound(type1, type2);
      TypeUtils helper = new TypeUtils(binder.getTypeEnvironment());
      IJavaReferenceType lub2 = helper.getLowestUpperBound((IJavaReferenceType) type1, (IJavaReferenceType) type2);
      /*
      if (!binder.getTypeEnvironment().isSubType(lub2, lub1)) {
    	  String unparse = lub2.toString();
    	  if (!"java.lang.Class<?>".equals(unparse)) {
        	  try {
        		  System.out.println("LUB not the same: "+lub2);
        	  } catch(StackOverflowError e) {
        		  System.out.println("LUB not the same as orig: "+lub1);
        	  }  
    	  }
      } // otherwise it's ok, since lub2 subsumes lub1
      */
      return lub2;
    }
  } 
  
  protected IJavaType forceBoxed(IJavaType ty) {
	    if (ty instanceof IJavaPrimitiveType) {	    
	      return JavaTypeFactory.getCorrespondingDeclType(binder.getTypeEnvironment(), 
	        		                                      (IJavaPrimitiveType) ty);
	    }
	    return ty;
	  }
  

  
  private IJavaType getLowestUpperBound(IJavaType t1, IJavaType t2) {
    if (binder.getTypeEnvironment().isSubType(t2, t1)) {
      return t1;
    }
    if (t1 instanceof IJavaDeclaredType && t2 instanceof IJavaDeclaredType) {
    	IJavaDeclaredType d1 = (IJavaDeclaredType) t1;
    	IJavaDeclaredType d2 = (IJavaDeclaredType) t2;
    	if (d1.getDeclaration().equals(d2.getDeclaration()) && 
    	    d1.getTypeParameters().size() == d2.getTypeParameters().size() &&
    	    ((d1.getOuterType() == null && d1.getOuterType() == d2.getOuterType()) || 
    	      d1.getOuterType() != null && d1.getOuterType().equals(d2.getOuterType()))) {
    		// Check if type parameters line up
    		final List<IJavaType> params1 = d1.getTypeParameters();
    		final List<IJavaType> params2 = d2.getTypeParameters();
    		final List<IJavaType> lubs    = new ArrayList<IJavaType>();
    		final int size = params1.size();
    		for(int i=0; i<size; i++) {
    			lubs.add(getLowestUpperBound(params1.get(i), params2.get(i)));
    		}
    		return JavaTypeFactory.getDeclaredType(d1.getDeclaration(), lubs, d1.getOuterType());
    	}
    } 
    /*
    else if (t1 instanceof IJavaWildcardType && t2 instanceof IJavaWildcardType) {
    	IJavaWildcardType w1 = (IJavaWildcardType) t1;
    	IJavaWildcardType w2 = (IJavaWildcardType) t2;
    	if (w1.getLowerBound() == w2.getLowerBound() && ) {
    		
    	}
    }   
    */
    IJavaType lub = null;
    for(IJavaType superT : binder.getTypeEnvironment().getSuperTypes(t1)) {
      IJavaType current = getLowestUpperBound(superT, t2);
      if (lub == null || binder.getTypeEnvironment().isSubType(current, lub)) { 
        lub = current;
      }
    }
    if (lub != null) {
      return lub;
    }
    return binder.getTypeEnvironment().getObjectType();    
  }
    
  /**
   * Unary numeric promotion is performed on expressions in the following situations:
   * Each dimension expression in an array creation expression (15.10)
   * The index expression in an array access expression (15.13)
   * The operand of a unary plus operator + (15.15.3)
   * The operand of a unary minus operator - (15.15.4)
   * The operand of a bitwise complement operator ~ (15.15.5)
   * Each operand, separately, of a shift operator >>, >>>, or << (15.19); 
   * therefore a long shift distance (right operand) does not promote the value 
   * being shifted (left operand) to long 
   */
  protected IJavaType doUnaryNumericPromotion(IJavaPrimitiveType t) {
    int width = getWidthOfPrimitiveType(t.getOp());
    if (width < 4) {
      return JavaTypeFactory.intType;
    }
    return t;
  }
  
  /**
   * If any of the operands is of a reference type, unboxing conversion (5.1.8) is performed. Then:
   * If either operand is of type double, the other is converted to double.
   * Otherwise, if either operand is of type float, the other is converted to float.
   * Otherwise, if either operand is of type long, the other is converted to long.
   * Otherwise, both operands are converted to type int. 
   *
   * After the type conversion, if any, value set conversion (5.1.13) is applied to each operand.
   *
   * Binary numeric promotion is performed on the operands of certain operators:
   * 
   * The multiplicative operators *, / and % (15.17)
   * The addition and subtraction operators for numeric types + and - (15.18.2)
   * The numerical comparison operators <, <=, >, and >= (15.20.1)
   * The numerical equality operators == and != (15.21.1)
   * The integer bitwise operators &, ^, and | (15.22.1)
   * In certain cases, the conditional operator ? : (15.25) 
   */
  protected IJavaType doBinaryNumericPromotion(PrimitiveType pt1, PrimitiveType pt2) {
    if (pt1 instanceof DoubleType || pt2 instanceof DoubleType) {
      return JavaTypeFactory.doubleType;
    }
    if (pt1 instanceof FloatType || pt2 instanceof FloatType) {
      return JavaTypeFactory.floatType;
    }
    if (pt1 instanceof LongType || pt2 instanceof LongType) {
      return JavaTypeFactory.longType;
    }
    if( pt1 instanceof BooleanType || pt2 instanceof BooleanType ) {
      return JavaTypeFactory.booleanType;
    }
    return JavaTypeFactory.intType;
  }
  
  /**
   * Compare two Array Type recursively.
   * @param at1
   * @param at2
   * @return
   */
  protected IJavaType compareIJavaArrayType( IJavaType at1, IJavaType at2 ) {
    if( at1 instanceof IJavaNullType || at2 instanceof IJavaNullType ) {
      return null;
    }else if( at1 instanceof IJavaVoidType || at2 instanceof IJavaVoidType ) {
      return null;
    }else {
      //Normal condition.
    }
    
    if( ( at1 instanceof IJavaArrayType ) && ( at2 instanceof IJavaArrayType ) ) {
      //Both are IJavaArrayType.
      IJavaType temp = compareIJavaArrayType( ((IJavaArrayType)at1).getElementType(), ((IJavaArrayType)at2).getElementType() ); 
      if( temp == null ) {
        return null;
      }else {
        return temp.equals( ((IJavaArrayType)at1).getElementType() ) ? at1 : at2;
      }
    }else if( !(( at1 instanceof IJavaArrayType ) || ( at2 instanceof IJavaArrayType )) ) {
      if (at1.equals(at2)) {
    	  return at1;
      }
    	
      //Both are not IJavaArrayType.
      if( ( at1 instanceof IJavaPrimitiveType ) && ( at2 instanceof IJavaPrimitiveType ) ) {
        //Both are IJavaPrimitiveType
        return at1.equals( at2 ) ? at1 : null;
      }else if( ( at1 instanceof IJavaDeclaredType ) && ( at2 instanceof IJavaDeclaredType ) ) {
        //Both are IJavaDeclaredType
        if( binder.getTypeEnvironment().isSubType( at2, at1 ) ) {
          return at1;
        }else if( binder.getTypeEnvironment().isSubType( at1, at2 ) ) {
          return at2;
        }else {
          return binder.getTypeEnvironment().getObjectType();
        }
      }else {
        return null;
      }
    }else {
      //One is IJavaArrayType and the other is not.
      return null;
    }
  }
  
  protected int getWidthOfPrimitiveType( PrimitiveType pt ) {
    if( pt instanceof IntType ) {
      return 4;
    }else if( pt instanceof LongType ) {
      return 8;
    }else if( pt instanceof ShortType || pt instanceof CharType ) {
      return 2;
    }else if( pt instanceof ByteType ) {
      return 1;
    }else if( pt instanceof FloatType ) {
      return 16;  //Float should be 32bit, but here I make it longer than LongType.
    }else if( pt instanceof DoubleType ) {
      return 32;  //Double should be 64bit, but here I make it longer than FloatType.
    }else {
      //BooleanType
      return 0;
    }
  }

  private IBinder binder;
  private boolean processJava8;
  
  protected IBinder getBinder() {
	  return binder;
  }
  
  protected void setBinder(IBinder b) {
	  if (b == null) {
		  return;
	  }
	  binder = b;
	  if (b instanceof AbstractBinder) {
		  AbstractBinder ajb = (AbstractBinder) b;
		  processJava8 = ajb.processJava8;
	  } else {
		  processJava8 = false;
	  }
  }
  
  /**
   * Return the type of a node using a binder.
   * This code is internally non-reentrant: only one caller in a thread
   * can usefully use this method at a time.
   * @param node expression node
   * @param binder use to find what names refer to
   * @return type of expression node
   */
  static public IJavaType getJavaType( IRNode node, IBinder binder ) {
    JavaTypeVisitor jtv = JavaTypeVisitor.prototype;
    synchronized ( jtv ) {
      IJavaType result = doBind(node, binder, jtv);
      return result;
    }
  }

  protected static IJavaType doBind(IRNode node, IBinder binder, JavaTypeVisitor jtv) {
    IBinder preBinder = jtv.binder;
    jtv.binder = binder;
    try { 	
    	return jtv.getJavaType(node);
    } finally {
        jtv.binder = preBinder;
    }
  }
  
  public IJavaType getJavaType(IRNode node) {
	  IJavaType result = doAccept(node);  
	  if (result == null) {
		  final String unparse = DebugUnparser.toString(node);
		  final IJavaRef ref = JavaNode.getTempJavaRef(node);
		  if (AbstractJavaBinder.isBinary(node)) {
			  if (!unparse.contains(" . 1")) {
				  System.err.println("Cannot get type for " + unparse+" in "+binder.getTypeEnvironment()+" : "+ref);
			  }
		  } else {
			  LOG.log( Level.WARNING, "Cannot get type for " + unparse+" in "+binder.getTypeEnvironment()+" : "+ref);
		  }
		  result = doAccept(node);  
	  /*
	  } else {
		  if ("com.fedex.ground.tms.common.ILocationManager".equals(result.toString())) {
			  IRNode parent  = JJNode.tree.getParentOrNull(node);
			  IRNode gparent = JJNode.tree.getParentOrNull(parent);
			  String gpUnparse = DebugUnparser.toString(gparent);
			  if (gpUnparse.startsWith("TimeZoneUtils.registerTimeZoneFetchable(")) {
				  System.out.println("Found: "+gpUnparse);
				  doAccept(node);
			  }
		  }
      */
	  }
	  /*
	  if (result instanceof IJavaDeclaredType) {
		  IJavaDeclaredType jdt = (IJavaDeclaredType) result;
		  for(IJavaType t : jdt.getTypeParameters()) {
			  if (t instanceof IJavaCaptureType) {
				  System.out.println("Found capture type ("+t+") for "+DebugUnparser.toString(node));
			  }
		  }
	  }
	  */
	  return result;
  }

  /**
   * Return a re-entrant visitor that uses the given binder.
   * @param b binder to use.
   * @return a type visitor that can be used to gett types of nodes
   * using {@link #doAccept(IRNode)}.
   */
  static public JavaTypeVisitor getTypeVisitor(IBinder b) {
    JavaTypeVisitor jtv = new JavaTypeVisitor();
    jtv.setBinder(b);
    return jtv;
  }
  
  // On assignment, calls, casts
  // Result type of method calls (15.12)
  // Array access (15.13)
  // Casts (15.16)
  // Class fields (6.5)
  public static IJavaType captureWildcards(IBinder binder, final IJavaType ty) {
	  // Check if it's parameterized
	  if (ty instanceof IJavaDeclaredType) {
		  final IJavaDeclaredType jdt = (IJavaDeclaredType) ty;			  
		  final List<IJavaType> oldTypes = jdt.getTypeParameters();
		  if (!oldTypes.isEmpty()) {
			  final List<IJavaType> newTypes = new ArrayList<IJavaType>(oldTypes.size());
			  final IRNode formals = getTypes(jdt.getDeclaration());
			  final IJavaTypeSubstitution jdtSubst = jdt.getTypeParameters().size() > 1 ?
				  JavaTypeSubstitution.create(binder.getTypeEnvironment(), jdt) : IJavaTypeSubstitution.NULL;		      
			  if (false && jdtSubst != IJavaTypeSubstitution.NULL && jdt.toString().contains("AbstractListAssert")) {
				  System.out.println("Got problematic type");
			  }			  
			  boolean different = false;
			  int i = 0;
			  for(final IRNode formal : TypeFormals.getTypeIterator(formals)) {
				  final IJavaTypeFormal jtf = JavaTypeFactory.getTypeFormal(formal);
				  final IJavaType old = oldTypes.get(i);
				  final IJavaType captured = captureWildcards(binder, old);
				  IJavaType t = AbstractTypeSubstitution.captureWildcardType(binder, jtf, captured, jdtSubst); 
				  newTypes.add(t);
				  i++;
				  if (old != t) {
					  different = true;
				  }
			  }
			  if (different) {					 				  
				  return JavaTypeFactory.getDeclaredType(jdt.getDeclaration(), newTypes, jdt.getOuterType());
			  }
		  }
	  }
	  else if (ty instanceof IJavaArrayType) {
		  final IJavaArrayType at = (IJavaArrayType) ty;
		  final IJavaType oldBase = at.getBaseType();
		  final IJavaType captured = captureWildcards(binder, oldBase);
		  if (captured != oldBase) {
			  return JavaTypeFactory.getArrayType(captured, at.getDimensions());
		  }
	  }	  
	  else if (ty instanceof IJavaIntersectionType) {
		  final IJavaIntersectionType it = (IJavaIntersectionType) ty;
		  final IJavaType old1 = it.getPrimarySupertype();
		  final IJavaType old2 = it.getSecondarySupertype();
		  final IJavaType capture1 = captureWildcards(binder, old1);
		  final IJavaType capture2 = captureWildcards(binder, old2);
		  if (capture1 != old1 || capture2 != old2) {
			  return JavaTypeFactory.getIntersectionType((IJavaReferenceType) capture1, (IJavaReferenceType) capture2);
		  }
	  }
	  else if (ty instanceof IJavaUnionType) {
		  final IJavaUnionType it = (IJavaUnionType) ty;
		  final IJavaType old1 = it.getFirstType();
		  final IJavaType old2 = it.getAlternateType();
		  final IJavaType capture1 = captureWildcards(binder, old1);
		  final IJavaType capture2 = captureWildcards(binder, old2);
		  if (capture1 != old1 || capture2 != old2) {
			  return JavaTypeFactory.getUnionType((JavaReferenceType) capture1, (JavaReferenceType) capture2);
		  }
	  }
	  // TODO deal with other types?
	  return ty;
  }
  
  private static IRNode getTypes(IRNode tdecl) {
	  if (ClassDeclaration.prototype.includes(tdecl)) {
		  return ClassDeclaration.getTypes(tdecl);
	  } else {
		  return InterfaceDeclaration.getTypes(tdecl);
	  }
  }
}
