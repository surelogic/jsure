package com.surelogic.analysis.type.checker;

import com.surelogic.common.Pair;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ArrayCreationExpression;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.ComplementExpression;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.DimExprs;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.MinusExpression;
import edu.cmu.cs.fluid.java.operator.NotExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.PlusExpression;
import edu.cmu.cs.fluid.java.operator.PostDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PostIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PreDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PreIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VisitorWithException;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

// TODO: Need to do error handling when getBinding() fails

/**
 * Visitor that computes the type of each expression, checking the types along
 * the way.  We assume that we only run on Java code that already compiles.
 * Thus we don't have to check the types of any normal Java constructs, only
 * types that have to do with any type-qualifying annotations that may be
 * present on the code (e.g., &#64;NonNull or &#64;Raw).  To this end,
 * we ignore any special checks that have to do with primitive types, but
 * insert special hooks via <code>typeCheck</code> methods for cases
 * where reference types need additional checking.  In this implementation,
 * the <code>typeCheck</code> methods do not check anything.
 */
public class TypeChecker extends VisitorWithException<IType, TypeCheckingFailed> {
  private static final String JAVA_LANG_DOUBLE = "java.lang.Double";
  private static final String JAVA_LANG_FLOAT = "java.lang.Float";
  private static final String JAVA_LANG_LONG = "java.lang.Long";
  private static final String JAVA_LANG_INTEGER = "java.lang.Integer";
  private static final String JAVA_LANG_CHARACTER = "java.lang.Character";
  private static final String JAVA_LANG_SHORT = "java.lang.Short";
  private static final String JAVA_LANG_BYTE = "java.lang.Byte";
  private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
  
  private final IBinder binder;
  private final ITypeFactory typeFactory;
  private final IConversionEngine conversionEngine;
  
  public TypeChecker(final IBinder b,
      final ITypeFactory tf, final IConversionEngine ce) {
    binder = b;
    typeFactory = tf;
    conversionEngine = ce;
  }

  
  
  // ======================================================================
  // == Deal with type checking errors
  // ======================================================================

  // Protected: allow subclasses to fail in new ways
  protected final void error(
      final IRNode expr, final IType type, final ITypeError error)
  throws TypeCheckingFailed {
    handleError(expr, type, error);
    throw new TypeCheckingFailed(expr, error);
  }
  
  protected void handleError(
      final IRNode expr, final IType type, final ITypeError error) {
    // nothing to do by default
  }

  
  
  // ======================================================================
  // == Bind names
  // ======================================================================

  /*
   * This method should be used instead of calling 
   * binder.getBinding() directly because it handles errors.
   */
  
  protected final IRNode getBinding(final IRNode expr)
  throws TypeCheckingFailed {
    final IRNode binding = binder.getBinding(expr);
    if (binding == null) {
      error(expr, null, JavaError.NAME_NOT_RESOLVABLE);
      // really dead code: error always throws an exception
      return null;
    } else {
      return binding;
    }
  }
  
  
  
  // ======================================================================
  // == Base checks
  // ======================================================================

  /* ¤4.2 Primitive types and values */
  
  protected final boolean isBooleanType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isByteType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isShortType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isIntType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isLongType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isCharType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isFloatType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isDoubleType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isNullType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  protected final boolean isIntegralType(final IType type) {
    return isByteType(type) ||
        isShortType(type) ||
        isIntType(type) ||
        isLongType(type) ||
        isCharType(type);
  }
  
  protected final boolean isFloatingPointType(final IType type) {
    return isFloatType(type) || isDoubleType(type);
  }
  
  protected final boolean isNumericType(final IType type) {
    return isIntegralType(type) || isFloatingPointType(type);
  }

  protected final boolean isPrimitiveType(final IType type) {
    return isBooleanType(type) || isNumericType(type);
  }
  
  protected final boolean isReferenceType(final IType type) {
    // TODO: May be there will be a cheaper way to test this
    return !isPrimitiveType(type);
  }
  
  protected final boolean isNamedType(final IType type, final String typeName) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }

  protected final boolean isArrayType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  
  
  // ======================================================================
  // == Conversions
  // ======================================================================

  /**
   * Widen the given primitive type to the given primitive type according to
   * ¤5.1.2.  Includes identity conversion.
   */
  protected final IType widenPrimitive(final IType type, final IPrimitiveType widenTo) {
    if (!widenTo.canWiden(type)) {
      throw new IllegalArgumentException(
          type + " cannot be widened to " + widenTo);
    } else {
      return widenTo;
    }
  }
  
  /**
   * Box the given type according to ¤5.1.7.
   */
  protected final IType box(final IType type) {
    if (isBooleanType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_BOOLEAN);
    } else if (isByteType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_BYTE);
    } else if (isShortType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_SHORT);
    } else if (isCharType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_CHARACTER);
    } else if (isIntType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_INTEGER);
    } else if (isLongType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_LONG);
    } else if (isFloatType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_FLOAT);
    } else if (isDoubleType(type)) {
      return typeFactory.getReferenceTypeFromName(JAVA_LANG_DOUBLE);
    } else if (isNullType(type)) {
      return type;
    } else {
      throw new IllegalArgumentException(type + " cannot be boxed");
    }
  }
  
  /**
   * Unbox the given type according to ¤5.1.8.
   */
  protected final IType unbox(final IType type) {
    /*
     * N.B. I purposely don't call preProcessUnbox() here, because I don't 
     * want to preprocess types that will cause an exception.
     */
    if (isNamedType(type, JAVA_LANG_BOOLEAN)) {
      preProcessUnbox(type);
      return typeFactory.getBooleanType();
    } else if (isNamedType(type, JAVA_LANG_BYTE)) {
      preProcessUnbox(type);
      return typeFactory.getByteType();
    } else if (isNamedType(type, JAVA_LANG_SHORT)) {
      preProcessUnbox(type);
      return typeFactory.getShortType();
    } else if (isNamedType(type, JAVA_LANG_CHARACTER)) {
      preProcessUnbox(type);
      return typeFactory.getCharType();
    } else if (isNamedType(type, JAVA_LANG_INTEGER)) {
      preProcessUnbox(type);
      return typeFactory.getIntType();
    } else if (isNamedType(type, JAVA_LANG_LONG)) {
      preProcessUnbox(type);
      return typeFactory.getLongType();
    } else if (isNamedType(type, JAVA_LANG_FLOAT)) {
      preProcessUnbox(type);
      return typeFactory.getFloatType();
    } else if (isNamedType(type, JAVA_LANG_DOUBLE)) {
      preProcessUnbox(type);
      return typeFactory.getDoubleType();
    } else {
      throw new IllegalArgumentException(type + " cannot be unboxed");
    }
  }

  protected void preProcessUnbox(final IType type) {
    // TODO: Figure out how to get the source expression here.  Don't yet 
    // TODO: want to commit to passing it directly.
    // TYPECHECK: Check for @NonNull here
  }
  
  /**
   * Is the type "convertible to a numeric type" as defined in ¤5.1.8.
   */
  protected final boolean isConvertibleToNumericType(final IType type) {
    return isNumericType(type) ||
        isNamedType(type, JAVA_LANG_BYTE) ||
        isNamedType(type, JAVA_LANG_SHORT) ||
        isNamedType(type, JAVA_LANG_CHARACTER) ||
        isNamedType(type, JAVA_LANG_INTEGER) ||
        isNamedType(type, JAVA_LANG_LONG) ||
        isNamedType(type, JAVA_LANG_FLOAT) ||
        isNamedType(type, JAVA_LANG_DOUBLE);
  }

  /**
   * Is the type "convertible to a integral type" as defined in ¤5.1.8.
   */
  protected final boolean isConvertibleToIntegralType(final IType type) {
    return isIntegralType(type) ||
        isNamedType(type, JAVA_LANG_BYTE) ||
        isNamedType(type, JAVA_LANG_SHORT) ||
        isNamedType(type, JAVA_LANG_CHARACTER) ||
        isNamedType(type, JAVA_LANG_INTEGER) ||
        isNamedType(type, JAVA_LANG_LONG);
  }
  
  
  
  // ======================================================================
  // == Promotions
  // ======================================================================

  /* Here we always assume that the promotion is possible: that is, that a 
   * previous check has been made to weed out bad expressions, such as not
   * trying to promote an expression that is not convertible to an integral type 
   * using unary numeric promotion.
   */

  // TODO: Do I want pre/post processing calls here?  What use are they?
  
  /**
   * Promote the given type using unary numeric promotion as defined 
   * in ¤5.6.1.
   */
  protected final IType unaryNumericPromotion(final IType type) {
    if (isNamedType(type, JAVA_LANG_BYTE) ||
        isNamedType(type, JAVA_LANG_SHORT) ||
        isNamedType(type, JAVA_LANG_CHARACTER) ||
        isNamedType(type, JAVA_LANG_INTEGER)) {
      return widenPrimitive(unbox(type), typeFactory.getIntType());
    } else if (isNamedType(type, JAVA_LANG_LONG) ||
        isNamedType(type, JAVA_LANG_FLOAT) ||
        isNamedType(type, JAVA_LANG_DOUBLE)) {
      return unbox(type);
    } else if (isByteType(type) ||
        isShortType(type) ||
        isCharType(type)) {
      return widenPrimitive(type, typeFactory.getIntType());
    } else {
      return type;
    }
  }
  
  /**
   * Promote the given types using binary numeric promotion as defined
   * in ¤5.6.2.
   */
  protected final Pair<IType, IType> binaryNumericPromotion(
      IType type1, IType type2) {
    if (isReferenceType(type1)) type1 = unbox(type1);
    if (isReferenceType(type2)) type2 = unbox(type2);

    if (isDoubleType(type1)) {
      type2 = typeFactory.getDoubleType();
    } else if (isDoubleType(type2)) {
      type1 = typeFactory.getDoubleType();
    } else if (isFloatType(type1)) {
      type2 = typeFactory.getFloatType();
    } else if (isFloatType(type2)) {
      type1 = typeFactory.getFloatType();
    } else if (isLongType(type1)) {
      type2 = typeFactory.getLongType();
    } else if (isLongType(type2)) {
      type1 = typeFactory.getLongType();
    } else {
      type1 = typeFactory.getIntType();
      type2 = typeFactory.getIntType();
    }
    return new Pair<IType, IType>(type1, type2);
  }
  
//  /**
//   * Give a subclass a chance to refine the qualifiers on the type being
//   * returned after unaryNumericPromotion.
//   * 
//   * @param originalType
//   *          The type before promotion
//   * @param type
//   *          The type after promotion
//   * @return The type after further processing.
//   */
//  protected IType postProcessUnuaryNumericPromotion(
//      final IType originalType, final IType type) {
//    return type;
//  }


  
  // ----------------------------------------------------------------------
  // -- Compound promotion operations
  // ----------------------------------------------------------------------

  /*
   * Unlike the above, this operations perform the necessary check of
   * applicability before performing promotion, and report type checking errors
   * if the promotion cannot be applied, or if the result after
   * promotion/conversion is not what is desired.
   */
  
  /**
   * First checks if type is convertible to an integral type (¤5.1.8), and if 
   * so, tests if the type is <code>int</code> after unary numeric promotion
   * (¤5.6.1).
   */
  protected final void unaryNumericPromotionToIntIfConvertible(
      final IRNode expr, IType type)
  throws TypeCheckingFailed {
    if (!isConvertibleToIntegralType(type)) {
      error(expr, type, JavaError.NOT_CONVERTIBLE_TO_INTEGRAL_TYPE);
    } else {
      type = unaryNumericPromotion(type);
      if (!isIntType(type)) {
        error(expr, type, JavaError.NOT_INT);
      }
    }
  }
  
  /**
   * First checks if type is convertible to a numeric type (¤5.1.8), and if 
   * so, performs unary numeric promotion (¤5.6.1).
   */
  protected final IType unaryNumericPromotionIfConvertibleToNumeric(
      final IRNode expr, final IType type)
  throws TypeCheckingFailed {
    if (!isConvertibleToNumericType(type)) {
      error(expr, type, JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE);
      return null; // DEAD CODE: error() always throws an exception
    } else {
      // Force the promotion so we can toggle an unbox if necessary; don't care about the resulting type
      return unaryNumericPromotion(type);
    }
  }
  
  /**
   * First checks if type is convertible to an integral type (¤5.1.8), and if 
   * so, performs unary numeric promotion (¤5.6.1).
   */
  protected final IType unaryNumericPromotionIfConvertibleToIntegral(
      final IRNode expr, final IType type)
  throws TypeCheckingFailed {
    if (!isConvertibleToIntegralType(type)) {
      error(expr, type, JavaError.NOT_CONVERTIBLE_TO_INTEGRAL_TYPE);
      return null; // DEAD CODE: error() always throws an exception
    } else {
      // Force the promotion so we can toggle an unbox if necessary; don't care about the resulting type
      return unaryNumericPromotion(type);
    }
  }

  
  
  // ======================================================================
  // == Hooks for further processing
  // ======================================================================

  protected IType postProcessType(final IType type) {
    return type;
  }
  
  
   
  // ======================================================================
  // == ¤6.5.6 Expression Names
  // ======================================================================

  /*
   * Qualified expression names refer to fields, and are handled by
   * visitFieldRef() below. Here we only handle simple expression names that
   * refer to local variables or parameters. Simple expressions that refer to
   * fields are converted by the parse tree to have explicit "this" references
   * and thus are also handled by visitFieldRef() below.
   */
  
  @Override
  public final IType visitVariableUseExpression(final IRNode varUseExpr)
  throws TypeCheckingFailed {
    /*
     * If the expression name appears in a context where it is subject to
     * assignment conversion or method invocation conversion or casting
     * conversion, then the type of the expression name is the declared type of
     * the field, local variable, or parameter after capture conversion
     * (¤5.1.10).
     * 
     * That is, if the expression name appears "on the right hand side", its
     * type is subject to capture conversion. If the expression name is a
     * variable that appears "on the left hand side", its type is not subject to
     * capture conversion.
     */
    final IRNode parent = JJNode.tree.getParent(varUseExpr);
    final Operator parentOp = JJNode.tree.getOperator(parent);
    boolean isRValue = true;
    if (parentOp instanceof AssignmentInterface) {
      if (((AssignmentInterface) parentOp).getTarget(parent) == varUseExpr) {
        isRValue = false;
      }
    }
    
    final IRNode nameDecl = getBinding(varUseExpr);
    final IRNode typeExpr;
    if (ParameterDeclaration.prototype.includes(nameDecl)) {
      typeExpr = ParameterDeclaration.getType(nameDecl);
    } else { // VariableDeclarator
      typeExpr = DeclStatement.getType(
          JJNode.tree.getParent(JJNode.tree.getParent(nameDecl)));
    }
    IType type = typeFactory.getTypeFromExpression(typeExpr);
    if (isRValue) type = conversionEngine.capture(type);    
    return postProcessVariableUseExpression(varUseExpr, nameDecl, isRValue, type);
  }
  
  protected IType postProcessVariableUseExpression(
      final IRNode varUseExpr, final IRNode nameDecl,
      final boolean isRValue, final IType type) {
    // TYPECHECK: Need to check the flow analyses to get the @NonNull/@Raw state
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == ¤8.3 Field Declarations
  // ======================================================================
  
  // TODO
  
  
  
  // ======================================================================
  // == ¤9.3 Field (Constant) Declarations
  // ======================================================================
  
  // TODO
  
  
  
  // ======================================================================
  // == ¤10.6 Array Initializers
  // ======================================================================
  
  /*
   * Each variable initializer must be assignment-compatible (¤5.2) with the
   * array's component type, or a compile-time error occurs.
   */
  
  /*
   * Problem: How to keep track of the array type as we go into nested
   * initializers, e.g.,
   * "new int[][][] { { { 1, 2, 3 }, { 4, 5, 6 } }, { { 7 }, { 8 } } }"
   */
  
  // TODO

  
  
  // ======================================================================
  // == ¤14.4 Local Variable Declaration Statements
  // ======================================================================
  
  // TODO

  
  
  // ======================================================================
  // == ¤15.8.1 Lexical Literals
  // ======================================================================

  /**
   * Determine the type of the expression based on the last character of the
   * given token.
   * 
   * @param token
   *          The token whose last character is to be tested.
   * @param tag
   *          The character to test against; must be uppercase!
   * @param trueType
   *          The type to return if the case-insensitive comparison of the last
   *          character against <code>tag</code> is <code>true</code>.
   * @param falseType
   *          The type to return otherwise.
   * @return The type of the literal represented by the given token.
   */
  private IType getTypeBasedOnEndTag(
      final String token, final char tag,
      final IType trueType, final IType falseType) {
    final char endsWith = token.charAt(token.length() - 1);
    return (Character.toUpperCase(endsWith) == tag) ? trueType : falseType;
  }

  @Override
  public final IType visitIntLiteral(final IRNode intLiteral) {
    /*
     * The type of an integer literal (¤3.10.1) that ends with L or l is long
     * (¤4.2.1). The type of any other integer literal is int (¤4.2.1).
     */
    final IType type = getTypeBasedOnEndTag(
        IntLiteral.getToken(intLiteral), 'L', 
        typeFactory.getLongType(), typeFactory.getIntType());
    return postProcessIntLiteral(intLiteral, type);
  }

  protected IType postProcessIntLiteral(final IRNode intLiteral, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitFloatLiteral(final IRNode floatLiteral) {
    /*
     * The type of a floating-point literal (¤3.10.2) that ends with F or f is
     * float and its value must be an element of the float value set (¤4.2.3).
     * The type of any other floating-point literal is double and its value must
     * be an element of the double value set (¤4.2.3).
     */
    final IType type = getTypeBasedOnEndTag(
        FloatLiteral.getToken(floatLiteral), 'F', 
        typeFactory.getFloatType(), typeFactory.getDoubleType());
    return postProcessFloatLiteral(floatLiteral, type);
  }

  protected IType postProcessFloatLiteral(final IRNode floatLiteral, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitBooleanLiteral(final IRNode booleanLiteral) {
    /*
     * The type of a boolean literal (¤3.10.3) is boolean (¤4.2.5).
     */
    return postProcessBooleanLiteral(
        booleanLiteral, typeFactory.getBooleanType());
  }

  protected IType postProcessBooleanLiteral(final IRNode booleanLiteral, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitCharLiteral(final IRNode charLiteral) {
    /*
     * The type of a character literal (¤3.10.4) is char (¤4.2.1).
     */
    return postProcessCharLiteral(charLiteral, typeFactory.getCharType());
  }

  protected IType postProcessCharLiteral(final IRNode charLiteral, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitStringLiteral(final IRNode stringLiteral) {
    /*
     * The type of a string literal (¤3.10.5) is String (¤4.3.3).
     */
    return postProcessStringLiteral(stringLiteral, typeFactory.getStringType());
  }

  protected IType postProcessStringLiteral(final IRNode stringLiteral, final IType type) {
    // TYPECHECK: When null checking, a String literal is a "@NonNull String"
    return postProcessType(type);
  }
  
  @Override
  public final IType visitNullLiteral(final IRNode nullLiteral) {
    /*
     * The type of the null literal null (¤3.10.7) is the null type (¤4.1); its
     * value is the null reference.
     */
    return postProcessNullLiteral(nullLiteral, typeFactory.getNullType());
  }

  protected IType postProcessNullLiteral(final IRNode nullLiteral, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.8.2 Class Literals
  // ======================================================================

  @Override
  // XXX: throws TypeCheckingFailed?  Depends if needs binding or not
  public final IType visitClassExpression(final IRNode classExpr) {
    /*
     * The type of C.class, where C is the name of a class, interface, or array
     * type (¤4.3), is Class<C>.
     * 
     * The type of p.class, where p is the name of a primitive type (¤4.2), is
     * Class<B>, where B is the type of an expression of type p after boxing
     * conversion (¤5.1.7).
     * 
     * The type of void.class (¤8.4.5) is Class<Void>.
     */
    final IRNode typeExpr = ClassExpression.getType(classExpr);
    final Operator typeExprOp = JJNode.tree.getOperator(typeExpr);
    final IType type;
    if (PrimitiveType.prototype.includes(typeExprOp)) { // case #2
      type = typeFactory.getClassType(
          box(typeFactory.getPrimitiveType(typeExprOp)));
    } else if (VoidType.prototype.includes(typeExprOp)) { // case #3
      type = typeFactory.getClassType(typeFactory.getVoidType());
    } else { // case #1
      /* 
       * Expression is a ReferenceType, more specifically it must be a
       * NamedType or an ArrayType.
       */
      // TODO: Does this involve binding?  If so, error if binding fails.
      type = typeFactory.getClassType(
          typeFactory.getReferenceTypeFromExpression(typeExpr));
    }
    return postProcessClassExpression(classExpr, type);
  }

  protected IType postProcessClassExpression(final IRNode classExpr, final IType type) {
    // TYPECHECK: When null checking the returned type is actually @NonNull!
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == ¤15.8.3 this
  // ======================================================================
  
  @Override
  public final IType visitThisExpression(final IRNode thisExpr) {
    /*
     * The type of this is the class C within which the keyword this occurs.
     */
    final IType type = typeFactory.getReferenceTypeFromDeclaration(
        VisitUtil.getEnclosingType(thisExpr));
    return postProcessThisExpression(thisExpr, type);
  }

  protected IType postProcessThisExpression(final IRNode thisExpr, final IType type) {
    // TYPECHECK: When null checking the type of the receiver is @NonNull!
    // TYPECHECK: Need to check the flow analysis to get the @Raw state
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == ¤15.8.4 Qualified this
  // ======================================================================
  
  @Override
  public final IType visitQualifiedThisExpression(final IRNode qualifiedThisExpr)
  throws TypeCheckingFailed {
    /*
     * Let C be the class denoted by ClassName. Let n be an integer such that C
     * is the n'th lexically enclosing class of the class in which the qualified
     * this expression appears.
     * 
     * The type of the expression is C.
     */
    final IRNode decl = getBinding(
        QualifiedThisExpression.getType(qualifiedThisExpr));
    final IType type = typeFactory.getReferenceTypeFromDeclaration(decl);
    return postProcessQualifiedThisExpression(qualifiedThisExpr, type);
  }

  protected IType postProcessQualifiedThisExpression(final IRNode qualifiedThisExpr, final IType type) {
    // TYPECHECK: The qualified receiver is always @NonNull!
    // TYPECHECK: Need to check the flow analysis to get the @Raw state
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == ¤15.8.5 Parenthesized Expressions
  // ======================================================================
  
  @Override
  public final IType visitParenExpression(final IRNode parenExpr)
  throws TypeCheckingFailed {
    /*
     * A parenthesized expression is a primary expression whose type is the type
     * of the contained expression and whose value at run-time is the value of
     * the contained expression.
     */
    return postProcessParenExpresion(parenExpr, 
        doAccept(ParenExpression.getOp(parenExpr))); 
  }
  
  protected IType postProcessParenExpresion(final IRNode parenExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.9 Class Instance Creation Expressions
  // ======================================================================

  // TODO

  
  
  // ======================================================================
  // == ¤15.10 Array Creation Expressions
  // ======================================================================
  
  @Override
  public final IType visitArrayCreationExpression(final IRNode arrayExpr) {
    /*
     * The type of each dimension expression within a DimExpr must be a type
     * that is convertible (¤5.1.8) to an integral type, or a compile-time error
     * occurs.
     * 
     * Each dimension expression undergoes unary numeric promotion (¤5.6.1). The
     * promoted type must be int, or a compile-time error occurs.
     */
    final IRNode dimExprs = ArrayCreationExpression.getAllocated(arrayExpr);
    for (final IRNode sizeExpr : DimExprs.getSizeIterator(dimExprs)) {
      try {
        IType type = doAccept(sizeExpr);
        unaryNumericPromotionToIntIfConvertible(sizeExpr, type);
      } catch (final TypeCheckingFailed e) {
        /* Eat the exception here, because we can still give a type to the
         * overall expression even if the index type is wrong.
         */
      }
    }
    
    // Don't forget the initializer
    try {
      doAccept(ArrayCreationExpression.getInit(arrayExpr));
    } catch (final TypeCheckingFailed e) {
      /* Eat the exception here, because we can still give a type to the
       * overall expression even if the initializer is messed up.
       */
    }
      
    /*
     * The type of the array creation expression is an array type that can be
     * denoted by a copy of the array creation expression from which the new
     * keyword and every DimExpr expression and array initializer have been
     * deleted.
     */
    
    /* Problem: When we have "new int[a][b][c][][]" the type overall type
     * should be "int[][][][][]", where the base type is "int".  But the 
     * parse tree switches things around so that the base type is 
     * "int[][]", and the new expression only has three dimensions.  This is
     * semantically more pure, but not in line with the JLS description of
     * things.  So I have to check to see if the base type of the 
     * ArrayCreationExpression is an ArrayType.
     */
    IRNode baseType = ArrayCreationExpression.getBase(arrayExpr);
    int numDims = JJNode.tree.numChildren(
        ArrayCreationExpression.getAllocated(arrayExpr));
    if (ArrayType.prototype.includes(baseType)) {
      numDims += ArrayType.getDims(baseType); // must go first
      baseType = ArrayType.getBase(baseType); // resets baseType
    }
    return postProcessArrayCreationExpression(arrayExpr,
        typeFactory.getArrayType(
            typeFactory.getTypeFromExpression(baseType), numDims));
  }
  
  protected IType postProcessArrayCreationExpression(
      final IRNode arrayExpr, final IType type) {
    // TYPECHECK: (1) Created array is always non-null
    // TYPECHECK: (2) Array elements might be non-null
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.11 Field Access Expressions
  // ======================================================================
  
  @Override
  public final IType visitFieldRef(final IRNode fieldRefExpr) 
  throws TypeCheckingFailed {
    /*
     * This rule technically only applies when the object expression is a
     * Primary, "super", or "ClassName.super" expression: The type of the field
     * access expression is the type of the member field after capture
     * conversion (¤5.1.10).
     * 
     * But the rules in ¤6.5.6 for expression names, also say to use the
     * type of the field after capture conversion, except in the case where we
     * have a simple expression name "f" that refers to a non-final instance 
     * field.  In that case, we are only supposed to use capture conversion
     * if the expression appears as an rvalue.  That is, if it's on the left
     * hand side of an assignment, then no capture conversion is performed.
     * Two issues here: (1) it means that "f = ..." and "this.f = ..." aren't
     * handled the same way, which is strange; (2) Our parse tree does not
     * allow us to tell the difference between the two because we always add 
     * in the implicit "this".
     */
    
    /*
     * We could eat the error here, because the type of the expression depends
     * on the declared type of the field. But failure here definitely means that
     * the field cannot be bound properly below and that another error will
     * arise.
     */
    final IRNode objectExpr = FieldRef.getObject(fieldRefExpr);
    final IType objectType = doAccept(objectExpr);
    if (isPrimitiveType(objectType)) {
      error(objectExpr, objectType, JavaError.NOT_REFERENCE_TYPE);
    }
    
    /* Binding the field reference expression gets the VariableDeclarator
     * of the accessed field, or the EnumConstantDeclaration of the accessed
     * enumeration constant.
     * 
     * N.B. Per ¤8.9.2 the type of an enumeration constant is the enumeration
     * type E that contains the constant declaration.
     */
    final IRNode varDecl = getBinding(fieldRefExpr);
    final IType fieldType;
    if (VariableDeclarator.prototype.includes(varDecl)) {
      final IRNode fieldDecl = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
      final IRNode typeExpr = FieldDeclaration.getType(fieldDecl);
      fieldType = typeFactory.getTypeFromExpression(typeExpr);
      
      if (!TypeUtil.isStatic(varDecl)) {
        preProcessObjectReference(objectExpr, objectType);
      }
    } else {
      final IRNode enumDecl = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
      fieldType = typeFactory.getReferenceTypeFromDeclaration(enumDecl);
    }
    return postProcessFieldRefExpression(
        fieldRefExpr, varDecl, conversionEngine.capture(fieldType));
  }

  // Only called for instance fields
  protected void preProcessObjectReference(final IRNode objectExpr, final IType objectType) {
    // TYPECHECK: Check the type of the object expression for null
  }
  
  protected IType postProcessFieldRefExpression(
      final IRNode fieldRefExpr, final IRNode varDecl, final IType type) {
    // TYPECHECK: Need to check the field declaration to get the 
    // TYPECHECK: @NonNull/@Raw state.  EnumDeclarations are always 
    // TYPECHECK: NonNull?
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.12 Method Invocation Expressions
  // ======================================================================
  
  // TODO

  
  
  // ======================================================================
  // == ¤15.13 Array Access Expressions
  // ======================================================================
 
  @Override
  public final IType visitArrayRefExpression(final IRNode arrayRefExpr)
  throws TypeCheckingFailed {
    /*
     * The type of the array reference expression must be an array type (call it
     * T[], an array whose components are of type T).
     * 
     * The index expression undergoes unary numeric promotion (¤5.6.1). The
     * promoted type must be int, or a compile-time error occurs.
     * 
     * The type of the array access expression is the result of applying capture
     * conversion (¤5.1.10) to T.
     */
    
    /* If there is a type-error when visiting the reference expression, or the
     * type is not an array type, then we cannot recover here, so we pass
     * on the exception.
     */
    final IRNode refExpr = ArrayRefExpression.getArray(arrayRefExpr);
    final IType arrayType = doAccept(refExpr);
    if (!isArrayType(arrayType)) {
      error(refExpr, arrayType, JavaError.NOT_ARRAY_TYPE);
    }
    processArrayReference(refExpr, arrayType);
    
    final IRNode indexExpr = ArrayRefExpression.getIndex(arrayRefExpr);
    try {
      final IType indexType = doAccept(indexExpr);
      unaryNumericPromotionToIntIfConvertible(indexExpr, indexType);
    } catch (final TypeCheckingFailed e) {
      /* If the index expression has an error, or is not convertible to an int,
       * we catch the exception, because we can still give the array reference
       * expression a type based on the type of the array.
       */
    }
    final IType elementType = typeFactory.getArrayElementType(arrayType);
    return postProcessArrayRefExpression(
        arrayRefExpr, conversionEngine.capture(elementType));
  }
  
  protected void processArrayReference(final IRNode refExpr, final IType type) {
    // TYPECHECK: Need to check that the array reference expression isn't null.
  }
  
  protected IType postProcessArrayRefExpression(final IRNode arrayRefExpr, final IType type) {
    // TYPECHECK: The array element type might be @NonNull
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.14.2 Postfix Increment Operator
  // ======================================================================
  
  @Override
  public final IType visitPostIncrementExpression(final IRNode postIncExpr) 
  throws TypeCheckingFailed {
    /*
     * The result of the postfix expression must be a variable of a type that is
     * convertible (¤5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the postfix increment expression is the type of the variable.
     * 
     * Before the addition, binary numeric promotion (¤5.6.2) is performed on
     * the value 1 and the value of the variable.
     */
    
    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PostIncrementExpression.getOp(postIncExpr);
    final IType exprType = doAccept(opExpr);
    if (!isConvertibleToNumericType(exprType)) {
      error(opExpr, exprType, JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE);
    }
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPostIncrementExpression(postIncExpr, exprType);
  }
  
  protected IType postProcessPostIncrementExpression(final IRNode postIncExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.14.3 Postfix Decrement Operator
  // ======================================================================
  
  @Override
  public final IType visitPostDecrementExpression(final IRNode postDecExpr)
  throws TypeCheckingFailed {
    /*
     * The result of the postfix expression must be a variable of a type that is
     * convertible (¤5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the postfix decrement expression is the type of the variable.
     * 
     * Before the subtraction, binary numeric promotion (¤5.6.2) is performed on
     * the value 1 and the value of the variable.
     */

    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PostDecrementExpression.getOp(postDecExpr);
    final IType exprType = doAccept(opExpr);
    if (!isConvertibleToNumericType(exprType)) {
      error(opExpr, exprType, JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE);
    }
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPostDecrementExpression(postDecExpr, exprType);
  }
  
  protected IType postProcessPostDecrementExpression(final IRNode postDecExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.15.1 Prefix Increment Operator
  // ======================================================================
  
  @Override
  public final IType visitPreIncrementExpression(final IRNode preIncExpr)
  throws TypeCheckingFailed {
    /*
     * The result of the unary expression must be a variable of a type that is
     * convertible (¤5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the prefix increment expression is the type of the variable.
     * 
     * Before the addition, binary numeric promotion (¤5.6.2) is performed on
     * the value 1 and the value of the variable.
     */

    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PreIncrementExpression.getOp(preIncExpr);
    final IType exprType = doAccept(opExpr);
    if (!isConvertibleToNumericType(exprType)) {
      error(opExpr, exprType, JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE);
    }
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPreIncrementExpression(preIncExpr, exprType);
  }
  
  protected IType postProcessPreIncrementExpression(final IRNode preIncExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.15.2 Prefix Decrement Operator
  // ======================================================================
  
  @Override
  public final IType visitPreDecrementExpression(final IRNode preDecExpr) 
  throws TypeCheckingFailed {
    /*
     * The result of the unary expression must be a variable of a type that is
     * convertible (¤5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the prefix decrement expression is the type of the variable.
     * 
     * Before the subtraction, binary numeric promotion (¤5.6.2) is performed on
     * the value 1 and the value of the variable.
     */
    
    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PreDecrementExpression.getOp(preDecExpr);
    final IType exprType = doAccept(opExpr);
    if (!isConvertibleToNumericType(exprType)) {
      error(opExpr, exprType, JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE);
    }
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPreDecrementExpression(preDecExpr, exprType);
  }
  
  protected IType postProcessPreDecrementExpression(final IRNode preDecExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.15.3 Unary Plus Operator
  // ======================================================================
  
  @Override
  public final IType visitPlusExpression(final IRNode plusExpr) 
  throws TypeCheckingFailed {
    /*
     * The type of the operand expression of the unary + operator must be a type
     * that is convertible (¤5.1.8) to a primitive numeric type, or a
     * compile-time error occurs.
     * 
     * Unary numeric promotion (¤5.6.1) is performed on the operand. The type of
     * the unary plus expression is the promoted type of the operand.
     */
    
    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PlusExpression.getOp(plusExpr);
    final IType operandType = doAccept(opExpr);
    final IType convertedType = 
        unaryNumericPromotionIfConvertibleToNumeric(opExpr, operandType);
    return postProcessPlusExpresion(plusExpr, convertedType);
  }
  
  protected IType postProcessPlusExpresion(final IRNode plusExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.15.4 Unary Minus Operator
  // ======================================================================
  
  @Override
  public final IType visitMinusExpression(final IRNode minusExpr) 
  throws TypeCheckingFailed {
    /*
     * The type of the operand expression of the unary - operator must be a type
     * that is convertible (¤5.1.8) to a primitive numeric type, or a
     * compile-time error occurs.
     * 
     * Unary numeric promotion (¤5.6.1) is performed on the operand.
     * 
     * The type of the unary minus expression is the promoted type of the
     * operand.
     */
    
    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = MinusExpression.getOp(minusExpr);
    final IType operandType = doAccept(opExpr);
    final IType convertedType = 
        unaryNumericPromotionIfConvertibleToNumeric(opExpr, operandType);
    return postProcessMinusExpresion(minusExpr, convertedType);
 }
  
  protected IType postProcessMinusExpresion(final IRNode minusExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.15.5 Bitwise Complement Operator ~
  // ======================================================================
  
  @Override
  public final IType visitComplementExpression(final IRNode complementExpr) 
  throws TypeCheckingFailed {
    /*
     * The type of the operand expression of the unary ~ operator must be a type
     * that is convertible (¤5.1.8) to a primitive integral type, or a
     * compile-time error occurs.
     * 
     * Unary numeric promotion (¤5.6.1) is performed on the operand. The type of
     * the unary bitwise complement expression is the promoted type of the
     * operand.
     */

    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = ComplementExpression.getOp(complementExpr);
    final IType operandType = doAccept(opExpr);
    final IType convertedType = 
        unaryNumericPromotionIfConvertibleToIntegral(opExpr, operandType);
    return postProcessComplementExpression(complementExpr, convertedType);
  }
  
  protected IType postProcessComplementExpression(final IRNode complementExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == ¤15.15.6 Logical Complement Operator !
  // ======================================================================
  
  @Override
  public final IType visitNotExpression(final IRNode notExpr) {
    /*
     * The type of the operand expression of the unary ! operator must be
     * boolean or Boolean, or a compile-time error occurs.
     * 
     * The type of the unary logical complement expression is boolean.
     * 
     * At run-time, the operand is subject to unboxing conversion (¤5.1.8) if
     * necessary.
     */
    
    final IRNode opExpr = NotExpression.getOp(notExpr);
    try {
      final IType operandType = doAccept(opExpr);
      final boolean isBoxedBoolean = isNamedType(operandType, JAVA_LANG_BOOLEAN);
      if (!(isBooleanType(operandType) || isBoxedBoolean)) {
        error(opExpr, operandType, JavaError.NOT_BOOLEAN_TYPE);
      }
      if (isBoxedBoolean) {
        // Force an unbox to enable null-checking 
        unbox(operandType);
      }
    } catch (final TypeCheckingFailed e) {
      /*
       * Eat any type errors in the subexpression because the type of the
       * expression is always boolean.
       */
    }
    return typeFactory.getBooleanType();
  }
  
  protected IType postProcessNotExpression(final IRNode notExpr, final IType type) {
    return postProcessType(type);
  }
}
