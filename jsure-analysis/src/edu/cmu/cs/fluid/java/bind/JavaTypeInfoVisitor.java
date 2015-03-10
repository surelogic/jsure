package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.surelogic.ThreadSafe;
import com.surelogic.common.logging.SLLogger;

/**
 * Class for computing the type of an AST node.
 * Also computes the value if it's a constant expression
 * 
 * @author Edwin
 */
@Deprecated
@ThreadSafe
public class JavaTypeInfoVisitor extends Visitor<JavaTypeInfoVisitor.TypeInfo> {
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  
  public static class TypeInfo {
	  final IJavaType type;
	  final Object value;	  
	  
	  // null could be a valid value only if of IJavaNullType
	  TypeInfo(IJavaType t, Object v) {
		  type = t;
		  value = v;
	  }
	  
	  /**
	   * No constant value
	   */
	  TypeInfo(IJavaType t) {
		  this(t, null);
	  }
  }
  
  static final TypeInfo NONE = new TypeInfo(null);
  static final TypeInfo BOOLEAN = new TypeInfo(JavaTypeFactory.booleanType);
  static final TypeInfo BYTE = new TypeInfo(JavaTypeFactory.byteType);
  static final TypeInfo CHAR = new TypeInfo(JavaTypeFactory.charType);
  static final TypeInfo DOUBLE = new TypeInfo(JavaTypeFactory.doubleType);
  static final TypeInfo FLOAT = new TypeInfo(JavaTypeFactory.floatType);
  static final TypeInfo INT = new TypeInfo(JavaTypeFactory.intType);
  static final TypeInfo LONG = new TypeInfo(JavaTypeFactory.longType);
  static final TypeInfo NULL = new TypeInfo(JavaTypeFactory.nullType, null);
  static final TypeInfo SHORT = new TypeInfo(JavaTypeFactory.shortType);
  static final TypeInfo VOID = new TypeInfo(JavaTypeFactory.voidType);
  static final TypeInfo WILDCARD = new TypeInfo(JavaTypeFactory.wildcardType);
  static final Map<IJavaType,TypeInfo> infoMap = new HashMap<IJavaType, TypeInfo>();
  static {
	  infoMap.put(JavaTypeFactory.booleanType, BOOLEAN);
	  infoMap.put(JavaTypeFactory.byteType, BYTE);
	  infoMap.put(JavaTypeFactory.charType, CHAR);
	  infoMap.put(JavaTypeFactory.doubleType, DOUBLE);
	  infoMap.put(JavaTypeFactory.floatType, FLOAT);
	  infoMap.put(JavaTypeFactory.intType, INT);
	  infoMap.put(JavaTypeFactory.longType, LONG);
	  infoMap.put(JavaTypeFactory.nullType, NULL);
	  infoMap.put(JavaTypeFactory.shortType, SHORT);
	  infoMap.put(JavaTypeFactory.voidType, VOID);
	  infoMap.put(JavaTypeFactory.wildcardType, WILDCARD);	  
  }
  
  /**
   * Only used if the type might be cached already
   */
  static TypeInfo getTypeInfo(IJavaType t) {
	  TypeInfo rv = infoMap.get(t);
	  if (rv != null) {
		  return rv;
	  }
	  // TODO cache?
	  return new TypeInfo(t);
  }
  
  protected JavaTypeInfoVisitor(IBinder b) {
	  binder = b;
  }
  
  static Operator getOperator(IRNode node) {
    return JJNode.tree.getOperator(node);
  }
  
  // method called for any operator without a visit method overridden.
  @Override
  public TypeInfo visit(IRNode node) {
    Operator op = getOperator(node);
    if (op instanceof IHasType) {
      LOG.log(Level.SEVERE, "no type visitor type for node with operator " + op, new Throwable());
    }
    return NONE;
  }
  
  @Override
  public TypeInfo visitAddExpression(IRNode node) {
    //The Binop code should handle string concatenation that is using
    //AddExpression.  It's supposed to be converted to StringConcat,
    //but the parser generates AddExpression nodes first, and we have to
    //handle these cases.
    IRNode node1 = BinopExpression.getOp1( node );
    TypeInfo type1 = doAccept( node1 );
    
    IRNode node2 = BinopExpression.getOp2( node );
    TypeInfo type2 =  doAccept( node2 );
    
    IJavaType stringType = binder.getTypeEnvironment().getStringType();
    if( type1 == stringType || type2 == stringType ) {
      return new TypeInfo(stringType);
    } else {
      return visitArithBinopExpression( node );
    }
  }
  
  @Override
  public TypeInfo visitAnnotation(IRNode node) {      
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
		  return NONE;
	  }
	  IJavaType rv = binder.getTypeEnvironment().convertNodeTypeToIJavaType( b.getNode() );
	  if (rv == null) {
		  System.out.println("No type for "+DebugUnparser.toString(node));
	  }
	  return new TypeInfo(rv);
  }
  
  @Override
  public TypeInfo visitAnnotationDeclaration(IRNode node) {
    return new TypeInfo(JavaTypeFactory.getDeclaredType(node, Collections.<IJavaType>emptyList(), null));
  }
  
  @Override
  public TypeInfo visitAnnotationElement(IRNode node) {
	return doAccept(AnnotationElement.getType(node));
  }
  
  @Override
  public TypeInfo visitAnonClassExpression(IRNode node) {
    return new TypeInfo(JavaTypeFactory.convertNodeTypeToIJavaType( node, binder ));
	// return doAccept(AnonClassExpression.getType(node));
  }
  
  @Override
  public TypeInfo visitArithBinopExpression(IRNode node) {
    IRNode node1 = BinopExpression.getOp1( node );
    TypeInfo type1 = doAccept( node1 );
    
    IRNode node2 = BinopExpression.getOp2( node );
    TypeInfo type2 = doAccept( node2 );
    return getTypeInfo(typeInference2( type1.type, type2.type ));
  }
  
  @Override
  public TypeInfo visitArrayCreationExpression(IRNode node) {
    IRNode n           = ArrayCreationExpression.getBase( node );
    IJavaType baseType = binder.getTypeEnvironment().convertNodeTypeToIJavaType( n );
    IRNode dimEs       = ArrayCreationExpression.getAllocated( node );
    int dims           = ArrayCreationExpression.getUnallocated(node) +
                         JJNode.tree.numChildren(dimEs);
    return new TypeInfo(JavaTypeFactory.getArrayType( baseType, dims ));
  }
  
  @Override
  public TypeInfo visitArrayInitializer(IRNode node) {
	IRNode parent = JJNode.tree.getParent(node);
	if (JJNode.tree.getOperator(parent) instanceof ArrayCreationExpression) {
      return visitArrayCreationExpression(parent);
	}  	  
    IJavaType current = null;
    for(IRNode n : ArrayInitializer.getInitIterator(node)) {
      IJavaType temp = doAccept(n).type;
      if (current == null || 
          !binder.getTypeEnvironment().isAssignmentCompatible(current, temp, n)) {
        current = temp;
      }
    }      
    if (current != null) {
      return new TypeInfo(JavaTypeFactory.getArrayType(current, 1));
    }
    return new TypeInfo(JavaTypeFactory.getArrayType(binder.getTypeEnvironment().getObjectType(), 1));
  }
  
  @Override
  public TypeInfo visitArrayLength(IRNode node) {
    return INT;
  }
  
  @Override
  public TypeInfo visitArrayRefExpression(IRNode node) {
    IRNode n = ArrayRefExpression.getArray( node );
    TypeInfo type1 = doAccept( n );
    if( type1 instanceof IJavaArrayType ) {
      return getTypeInfo(((IJavaArrayType)type1).getElementType());
    }else {
      return NONE;
    }
  }
  
  @Override
  public TypeInfo visitArrayType(IRNode node) {
    return new TypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( node ));
  }
  
  @Override
  public TypeInfo visitAssignExpression(IRNode node) {
    IRNode lvalue = AssignExpression.getOp1( node );
    return doAccept(lvalue);
  }
  
  @Override
  public TypeInfo visitBooleanLiteral(IRNode node) {
    return computeTypeInfo(JavaTypeFactory.booleanType);
  }
  
  @Override
  public TypeInfo visitBooleanType(IRNode node) {
    return BOOLEAN;
  }
  
  @Override
  public TypeInfo visitBoxExpression(IRNode node) {
	TypeInfo t           = doAccept(BoxExpression.getOp(node));
	if (!(t instanceof IJavaPrimitiveType)) {
		doAccept(BoxExpression.getOp(node));
		return NONE;
	}
    IJavaPrimitiveType pt = (IJavaPrimitiveType) t;
    String qname          = pt.getCorrespondingTypeName();
    IRNode nt             = binder.getTypeEnvironment().findNamedType(qname);
    return new TypeInfo(JavaTypeFactory.convertNodeTypeToIJavaType(nt, binder));
  }

  @Override
  public TypeInfo visitByteType(IRNode node) {
    return BYTE;
  }
  
  @Override
  public TypeInfo visitCastExpression(IRNode node) {
    IRNode decl = CastExpression.getType( node );
    return doAccept( decl );
  }
  
  @Override
  public TypeInfo visitCharLiteral(IRNode node) {
    return computeTypeInfo(JavaTypeFactory.charType);
  }
  
  @Override
  public TypeInfo visitCharType(IRNode node) {
	return CHAR;
  }
  
  @Override
  public TypeInfo visitClassDeclaration(IRNode node) {
    //return JavaTypeFactory.getDeclaredType( node, null, null );
    return new TypeInfo(JavaTypeFactory.getMyThisType( node ));
  }
  
  @Override
  public TypeInfo visitElementValuePair(IRNode node) {
	IBinding b = binder.getIBinding(node);
	return visitAnnotationElement(b.getNode());
  }
  
  @Override
  public TypeInfo visitEnumDeclaration(IRNode node) {
    return new TypeInfo(binder.getTypeEnvironment().getMyThisType( node ));
  }
  
  @Override
  public TypeInfo visitEnumConstantDeclaration(IRNode node) {
    IRNode decl = VisitUtil.getEnclosingType(node);
    return visitEnumDeclaration(decl);
  }
  
  @Override
  public TypeInfo visitClassExpression(IRNode node) {
    TypeInfo base    = doAccept( ClassExpression.getType(node) );
    IRNode nodeType   = binder.getTypeEnvironment().findNamedType("java.lang.Class");
    List<IJavaType> p = new ArrayList<IJavaType>(1);
    /*
    if (base instanceof IJavaPrimitiveType) {
    	base = JavaTypeFactory.getCorrespondingDeclType(binder.getTypeEnvironment(), (IJavaPrimitiveType) base);
    }
    */
    p.add(base.type);
    return new TypeInfo(JavaTypeFactory.getDeclaredType(nodeType, p, null));
  }
  
  private IJavaPrimitiveType convertToPrim(TypeInfo t) {
	  if (t instanceof IJavaDeclaredType) {
		  return JavaTypeFactory.getCorrespondingPrimType((IJavaDeclaredType) t);
	  } else {
		  return (IJavaPrimitiveType) t;
	  }
  }
  
  @Override
  public TypeInfo visitComplementExpression(IRNode node) {
    IRNode n = ComplementExpression.getOp( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( n ));    
    return computeTypeInfo(doUnaryNumericPromotion(t));
  }

  @Override
  public TypeInfo visitConstructorCall(IRNode node) {
    if (JJNode.tree.hasChildren(ConstructorCall.getArgs(node))) {
      return VOID;
    }
    return doAccept(ConstructorCall.getObject(node));
  }
  
  @Override 
  public TypeInfo visitConstructorDeclaration(IRNode node) {
	IRNode type = VisitUtil.getEnclosingType(node);
	return new TypeInfo(JavaTypeFactory.convertNodeTypeToIJavaType(type, binder));
  }
  
  @Override
  public TypeInfo visitConditionalAndExpression(IRNode node) {
    return computeTypeInfo(JavaTypeFactory.booleanType);
  }
  @Override
  public TypeInfo visitConditionalExpression(IRNode node) {
    IRNode node1 = ConditionalExpression.getIftrue( node );
    TypeInfo type1 = doAccept( node1 );
    
    IRNode node2 = ConditionalExpression.getIffalse( node );
    TypeInfo type2 = doAccept( node2 );
    
    IJavaType type = typeInference3( type1.type, node1, type2.type, node2 );
    return computeTypeInfo(type);
  }
  @Override
  public TypeInfo visitConditionalOrExpression(IRNode node) {
    return computeTypeInfo(JavaTypeFactory.booleanType);
  }
  
  @Override
  public TypeInfo visitDefaultValue(IRNode node) {
    return doAccept(DefaultValue.getValue(node));
  }
  
  @Override
  public TypeInfo visitDoubleType(IRNode node) {
    return DOUBLE;
  }
  
  @Override
  public TypeInfo visitFieldDeclaration(IRNode node) {
    return doAccept(FieldDeclaration.getType(node));
  }
  
  @Override
  public TypeInfo visitFieldRef(IRNode node) {
    IBinding b = binder.getIBinding( node );
    if (b == null) return NONE;
    return getJavaType(b);
  }
  
  @Override
  public TypeInfo visitFloatLiteral(IRNode node) {
    String floatStr = FloatLiteral.getToken( node );
    char last       = floatStr.charAt( floatStr.length()-1 );
    if( last == 'F' || last == 'f' ) {
      return computeTypeInfo(JavaTypeFactory.floatType);
    } else {
      return computeTypeInfo(JavaTypeFactory.doubleType);
    }
  }
  
  @Override
  public TypeInfo visitFloatType(IRNode node) {
    return FLOAT;
  }
  
  @Override
  public TypeInfo visitImplicitReceiver(IRNode node) {
    return new TypeInfo(JavaTypeFactory.getThisType(node));
  }
  
  @Override
  public TypeInfo visitInitialization(IRNode node) {
    return doAccept(Initialization.getValue(node));
  }
  
  @Override
  public TypeInfo visitInstanceOfExpression(IRNode node) {
    return BOOLEAN;
  }
  
  @Override
  public TypeInfo visitIntLiteral(IRNode node) {
    String intStr = IntLiteral.getToken( node );
    //For a Integer Literal in java Language, if the end of it is 'L' or 'l', then it is long type, 
    //otherwise it is int type(not short or byte type). 
    char last = intStr.charAt( intStr.length()-1 );
    if( last == 'L' || last == 'l' ) {
      return computeTypeInfo(JavaTypeFactory.longType);
    }else {
      return computeTypeInfo(JavaTypeFactory.intType);
    }
  }
  
  @Override
  public TypeInfo visitIntType(IRNode node) {
    return INT;//JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
  }
  
  @Override
  public TypeInfo visitInterfaceDeclaration(IRNode node) {
    //return JavaTypeFactory.getDeclaredType( node, null, null );
    return new TypeInfo(JavaTypeFactory.getMyThisType( node ));
  }
  
  @Override
  public TypeInfo visitLambdaExpression(IRNode node) {
	// TODO do we have to redo this whole thing?
	return NONE;
  }
  
  @Override
  public TypeInfo visitLogBinopExpression(IRNode node) {
    IRNode node1 = BinopExpression.getOp1( node );
    TypeInfo node1_type = doAccept( node1 );
    IRNode node2 = BinopExpression.getOp2( node );
    TypeInfo node2_type = doAccept( node2 );
    return computeTypeInfo(typeInference1( node1_type.type, node2_type.type ));
  }
  
  @Override
  public TypeInfo visitLongType(IRNode node) {
    return LONG;//JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
  }
  
  @Override
  public TypeInfo visitMethodCall(IRNode node) {
    IBinding b = binder.getIBinding( node );
    return getTypeInfo(computeReturnType(b));    
  }
  
  public IJavaType computeReturnType(IBinding mb) {
	  if (ConstructorDeclaration.prototype.includes(mb.getNode())) {
		  if (mb.getContextType() == null) {
			  throw new IllegalStateException();
		  }
		  return mb.getContextType();
	  }	  
	  return computeReturnType_private(mb);
  }
  
  private IJavaType computeReturnType_private(IBinding b) {
    if (b == null) return null;
    IRNode n = b.getNode();
    Operator op = JJNode.tree.getOperator( n );
    if( op instanceof MethodDeclaration ) {
    	
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
    if (t.getTypeParameters().size() > 0) {
      return JavaTypeFactory.getDeclaredType(t.getDeclaration(), 
                                             Collections.<IJavaType>emptyList(), 
                                             t.getOuterType());
    }
    return t;
  }

  @Override
  public TypeInfo visitMethodDeclaration(IRNode node) {
	  return getTypeInfo(binder.getJavaType(MethodDeclaration.getReturnType(node)));
  }
  
  @Override
  public TypeInfo visitMinusExpression(IRNode node) {
    IRNode n             = MinusExpression.getOp( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( n ));    
    return computeTypeInfo(doUnaryNumericPromotion(t));
  }

  
  @Override
  public TypeInfo visitNameType(IRNode node) {
	/*
    String unparse = DebugUnparser.toString(node);
	if (unparse.contains("DelegationTokenRenewer.Renewable")) {
		System.out.println("Computing type for DelegationTokenRenewer.Renewable");
	}
	
	try {
	*/
		return new TypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( node ));
	/*
	} catch(StackOverflowError e) {
		System.out.println("Stack overflow on "+unparse);
		throw e;
	}
	*/
  }
  
  @Override
  public TypeInfo visitNamedType(IRNode node) {
    /*
	if (DebugUnparser.toString(node).contains("Context")) {
		System.out.println("Computing type for NamedType Context");
	}
    */
    return new TypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( node ));
  }
  
  @Override
  public TypeInfo visitNameExpression(IRNode node) {
    IBinding b = binder.getIBinding( node );
    if (b == null) return NONE;
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
    return getJavaType(b);
  }
  
  @Override
  public TypeInfo visitNewExpression(IRNode node) {
    NewExpression newE = (NewExpression) getOperator(node);
    IRNode nodeType = newE.get_Type( node );
    return new TypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( nodeType ));
  }
  
  @Override
  public TypeInfo visitNotExpression(IRNode node) {
    return computeTypeInfo(JavaTypeFactory.booleanType);
  }

  @Override
  public TypeInfo visitNullLiteral(IRNode node) {
    return NULL;
  }
  
  @Override
  public TypeInfo visitOpAssignExpression(IRNode node) {
    IRNode lvalue = OpAssignExpression.getOp1( node );
    return doAccept( lvalue );
  }
  
  @Override
  public TypeInfo visitParameterDeclaration(IRNode node) {
    return getTypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( ParameterDeclaration.getType( node ) ));
  }
  
  @Override
  public TypeInfo visitParameterizedType(IRNode node) {
    return new TypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( node ));
  }
  
  @Override
  public TypeInfo visitParenExpression(IRNode node) {
    IRNode n = ParenExpression.getOp( node );
    return doAccept( n );
  }

  @Override
  public TypeInfo visitPlusExpression(IRNode node) {
    IRNode n = PlusExpression.getOp( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( n ));    
    return computeTypeInfo(doUnaryNumericPromotion(t));
  }

  @Override
  public TypeInfo visitCrementExpression(IRNode node) {
    IRNode n = UnopExpression.getOp( node );
    if (UnboxExpression.prototype.includes(n)) {
      return doAccept( UnboxExpression.getOp(n) );
    }
    return doAccept( n );
  }

  @Override
  public TypeInfo visitOuterObjectSpecifier(IRNode node) {
    IRNode n = OuterObjectSpecifier.getCall(node);
    return doAccept( n );
  }
  
  @Override
  public TypeInfo visitQualifiedReceiverDeclaration(IRNode node) {
	  return new TypeInfo(QualifiedReceiverDeclaration.getJavaType(this.binder, node));
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
  public TypeInfo visitQualifiedSuperExpression(IRNode node) {
    IJavaDeclaredType dt = (IJavaDeclaredType) binder.getJavaType(QualifiedSuperExpression.getType( node ));
    return new TypeInfo(dt.getSuperclass(binder.getTypeEnvironment()));
  }
  @Override
  public TypeInfo visitQualifiedThisExpression(IRNode node) {
    IRNode n = QualifiedThisExpression.getType( node );
    return doAccept( n );
  }
  
  @Override
  public TypeInfo visitReceiverDeclaration(IRNode node) {
    return new TypeInfo(JavaTypeFactory.getThisType(node));
  }

  @Override
  public TypeInfo visitReturnValueDeclaration(IRNode rvd) {
	  IRNode n = JavaPromise.getPromisedForOrNull(rvd);
	  Operator op = JJNode.tree.getOperator(n);
	  if (MethodDeclaration.prototype.includes(op)) {
		  return getJavaType(MethodDeclaration.getReturnType(n));
	  }
	  else if (ConstructorDeclaration.prototype.includes(op)) {
		  return new TypeInfo(JavaTypeFactory.getThisType(n));
	  }
	  else if (AnnotationElement.prototype.includes(op)) {
		  return getJavaType(AnnotationElement.getType(n));
	  }
	  throw new UnsupportedOperationException("No return value for "+DebugUnparser.toString(n));
  }
  
  @Override
  public TypeInfo visitRelopExpression(IRNode node) {
    return computeTypeInfo(JavaTypeFactory.booleanType);
  }

  @Override
  public TypeInfo visitStatementExpression(IRNode node) {
    return VOID;
  }
  
  @Override
  public TypeInfo visitShiftExpression(IRNode node) {
    IRNode lvalue        = BinopExpression.getOp1( node );
    IJavaPrimitiveType t = convertToPrim(doAccept( lvalue ));    
    return computeTypeInfo(doUnaryNumericPromotion(t));
  }
  
  @Override
  public TypeInfo visitShortType(IRNode node) {
    return SHORT;//JavaTypeFactory.convertNodeTypeToIJavaType( node, binder );
  }
  
  @Override
  public TypeInfo visitStringConcat(IRNode node) {
    return computeTypeInfo(binder.getTypeEnvironment().getStringType());
  }
  @Override
  public TypeInfo visitStringLiteral(IRNode node) {
    return new TypeInfo(binder.getTypeEnvironment().getStringType());
  }
  
  @Override
  public TypeInfo visitSuperExpression(IRNode node) {
    IJavaSourceRefType dt = JavaTypeFactory.getThisType(node);
    return new TypeInfo(dt.getSuperclass(binder.getTypeEnvironment()));
  }
  
  @Override
  public TypeInfo visitThisExpression(IRNode node) {
    return new TypeInfo(JavaTypeFactory.getThisType(node));
  }
  
  @Override
  public TypeInfo visitTypeExpression(IRNode node) {
    IRNode decl = TypeExpression.getType( node );
    return doAccept( decl );
  }
  
  @Override 
  public TypeInfo visitTypeFormal(IRNode node) {
    return new TypeInfo(JavaTypeFactory.getTypeFormal(node));
  }
  
  @Override
  public TypeInfo visitTypeRef(IRNode node) {
    return new TypeInfo(binder.getTypeEnvironment().convertNodeTypeToIJavaType( node ));
  }
  
  @Override
  public TypeInfo visitUnboxExpression(IRNode node) {
    TypeInfo ty = doAccept(UnboxExpression.getOp(node));
    if (ty instanceof IJavaDeclaredType) {
      return getTypeInfo(JavaTypeFactory.getCorrespondingPrimType((IJavaDeclaredType) ty));
    } else {
      return NONE; // oh well
    }
  }
  
  @Override
  public TypeInfo visitUnionType(IRNode node) {
	List<IJavaType> types = new ArrayList<IJavaType>();
	for(IRNode type : UnionType.getTypeIterator(node)) {
		types.add(doAccept(type).type);	
	}	
	return new TypeInfo(JavaTypeFactory.getUnionType(types));
  }
  
  @Override
  public TypeInfo visitVarArgsExpression(IRNode node) {
    int num = JJNode.tree.numChildren(node);
    if (num > 0) {
      //TypeInfo t = doAccept( VarArgsExpression.getArg(node, 0) );        	
      IJavaType t = null;
      // Look at all the exprs and combine
      for(IRNode e : VarArgsExpression.getArgIterator(node)) {
    	  final IJavaType eT = doAccept(e).type;
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
      return new TypeInfo(JavaTypeFactory.getArrayType(t, 1));
    }
    //  FIX what should this be?
    return new TypeInfo(JavaTypeFactory.getArrayType(JavaTypeFactory.anyType, 1)); 
  }
  
  @Override
  public TypeInfo visitVarArgsType(IRNode node) {
    IJavaType baseT = doAccept(VarArgsType.getBase(node)).type; 
    return new TypeInfo(JavaTypeFactory.getArrayType(baseT, 1));    
  }
  
  @Override
  public TypeInfo visitVariableUseExpression(IRNode node) {
    IBinding b = binder.getIBinding( node );
    if (b == null) return NONE;

    if (b.getContextType() != null) {
      LOG.info("expected binding of local to have empty content: " + b);
    }
    return getJavaType(b);
  }
  
  private TypeInfo getJavaType(IBinding b) {
	  IRNode n = b.getNode();
	  if (n == null) {
		  return NONE;
	  }
	  TypeInfo rv = doAccept( n );
	  IJavaType temp = b.convertType(binder, rv.type); // probably a NOP
	  if (temp != rv.type) {
		  rv = getTypeInfo(temp);
	  }
	  return rv;
  }
  
  @Override
  public TypeInfo visitVariableDeclarator(IRNode node) {
    IJavaType jt = binder.getTypeEnvironment().convertNodeTypeToIJavaType( VariableDeclarator.getType( node ) );
    int dims = VariableDeclarator.getDims(node);
    if (dims > 0) jt = JavaTypeFactory.getArrayType(jt,dims);
    return getTypeInfo(jt);
  }
  
  @Override
  public TypeInfo visitVoidType(IRNode node) {
    return VOID;
  }
  
  @Override
  public TypeInfo visitWildcardType(IRNode node) {
    return WILDCARD;
  }
  
  @Override
  public TypeInfo visitWildcardSuperType(IRNode node) {
    IJavaReferenceType lower = (IJavaReferenceType) doAccept(WildcardSuperType.getLower(node));
    return new TypeInfo(JavaTypeFactory.getWildcardType(null, lower));
  }
  
  @Override
  public TypeInfo visitWildcardExtendsType(IRNode node) {
    IJavaReferenceType upper = (IJavaReferenceType) doAccept(WildcardExtendsType.getUpper(node));
    return new TypeInfo(JavaTypeFactory.getWildcardType(upper, null));
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
    }else {
      //final TypeInfo lub1 = getLowestUpperBound(type1, type2);
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

  protected final IBinder binder;
  
  /**
   * Return the type of a node 
   * 
   * @param node expression node
   * @return type of expression node
   */  
  public TypeInfo getJavaType(IRNode node) {
	  TypeInfo result = doAccept(node);  
	  if (result == null) {
		  final String unparse = DebugUnparser.toString(node);
		  if (AbstractJavaBinder.isBinary(node)) {
			  if (!unparse.contains(" . 1")) {
				  System.err.println("Cannot get type for " + unparse+" in "+binder.getTypeEnvironment());
			  }
		  } else {
			  LOG.log( Level.WARNING, "Cannot get type for " + unparse+" in "+binder.getTypeEnvironment());
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
		  for(TypeInfo t : jdt.getTypeParameters()) {
			  if (t instanceof IJavaCaptureType) {
				  System.out.println("Found capture type ("+t+") for "+DebugUnparser.toString(node));
			  }
		  }
	  }
	  */
	  return result;
  }
  
  // TODO temporary to get things to compile
  private TypeInfo computeTypeInfo(IJavaType i) {
	  return new TypeInfo(i);
  }
}
