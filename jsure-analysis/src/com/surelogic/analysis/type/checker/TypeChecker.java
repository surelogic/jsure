package com.surelogic.analysis.type.checker;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AddExpression;
import edu.cmu.cs.fluid.java.operator.AndExpression;
import edu.cmu.cs.fluid.java.operator.ArrayCreationExpression;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.AssertMessageStatement;
import edu.cmu.cs.fluid.java.operator.AssertStatement;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.CatchClauses;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.ComplementExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalAndExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalOrExpression;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.DimExprs;
import edu.cmu.cs.fluid.java.operator.DivExpression;
import edu.cmu.cs.fluid.java.operator.DoStatement;
import edu.cmu.cs.fluid.java.operator.ElseClause;
import edu.cmu.cs.fluid.java.operator.EqExpression;
import edu.cmu.cs.fluid.java.operator.ExprStatement;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Finally;
import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.GreaterThanEqualExpression;
import edu.cmu.cs.fluid.java.operator.GreaterThanExpression;
import edu.cmu.cs.fluid.java.operator.IfStatement;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.LabeledStatement;
import edu.cmu.cs.fluid.java.operator.LeftShiftExpression;
import edu.cmu.cs.fluid.java.operator.LessThanEqualExpression;
import edu.cmu.cs.fluid.java.operator.LessThanExpression;
import edu.cmu.cs.fluid.java.operator.LocalClassDeclaration;
import edu.cmu.cs.fluid.java.operator.MinusExpression;
import edu.cmu.cs.fluid.java.operator.MulExpression;
import edu.cmu.cs.fluid.java.operator.NotEqExpression;
import edu.cmu.cs.fluid.java.operator.NotExpression;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.operator.OrExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.PlusExpression;
import edu.cmu.cs.fluid.java.operator.PostDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PostIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PreDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PreIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.RemExpression;
import edu.cmu.cs.fluid.java.operator.Resources;
import edu.cmu.cs.fluid.java.operator.RightShiftExpression;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.SubExpression;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.TryResource;
import edu.cmu.cs.fluid.java.operator.TryStatement;
import edu.cmu.cs.fluid.java.operator.UnsignedRightShiftExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableResource;
import edu.cmu.cs.fluid.java.operator.VisitorWithException;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.operator.WhileStatement;
import edu.cmu.cs.fluid.java.operator.XorExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

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
  
  private static final String JAVA_LANG_STRING = "java.lang.String";
  
  private final IBinder binder;
  private final ITypeFactory typeFactory;
  
  public TypeChecker(final IBinder b, final ITypeFactory tf) {
    binder = b;
    typeFactory = tf;
  }
  
  
  
  // ======================================================================
  // == Deal with type checking errors
  // ======================================================================

  // Protected: allow subclasses to fail in new ways
  protected final void error(
      final ITypeError error, final IRNode expr, final IType... type)
  throws TypeCheckingFailed {
    handleError(error, expr, type);
    throw new TypeCheckingFailed(expr, error);
  }
  
  protected void handleError(
      final ITypeError error, final IRNode expr, final IType... type) {
    // nothing to do by default
  }



  // ======================================================================
  // == Default post processor
  // ======================================================================

  protected IType postProcessType(final IType type) {
    /* 
     * Override to record errors as a side effect, for example.
     */
    return type;
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
      error(JavaError.NAME_NOT_RESOLVABLE, expr);
      // really dead code: error always throws an exception
      return null;
    } else {
      return binding;
    }
  }
  
  
  
  // ======================================================================
  // == §4.2 Primitive types and values
  // ======================================================================
  
  protected final boolean isSameType(final IType type1, final IType type2) {
    // TODO: Make this real
    return false;
  }
  
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
  
  protected final boolean isVoidType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }
  
  /**
   * Is the type an integral type as defined in §4.2?
   */
  protected final boolean isIntegralType(final IType type) {
    return isByteType(type) ||
        isShortType(type) ||
        isIntType(type) ||
        isLongType(type) ||
        isCharType(type);
  }
  
  /**
   * Is the type a floating-point type as defined in §4.2?
   */
  protected final boolean isFloatingPointType(final IType type) {
    return isFloatType(type) || isDoubleType(type);
  }
  
  /**
   * Is the type a numeric type as defined in §4.2?
   */
  protected final boolean isNumericType(final IType type) {
    return isIntegralType(type) || isFloatingPointType(type);
  }

  /**
   * Is the type a primitive type as defined in §4.2?
   */
  protected final boolean isPrimitiveType(final IType type) {
    return isBooleanType(type) || isNumericType(type);
  }
  
  
  
  // ======================================================================
  // == §4.3 Reference types and values
  // ======================================================================
    
  /**
   * Is the type a reference type as defined in §4.3?
   */
  protected final boolean isReferenceType(final IType type) {
    // TODO: May be there will be a cheaper way to test this
    return !isPrimitiveType(type);
  }
  
  protected final boolean isNamedType(final IType type, final String typeName) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }

  /**
   * Is the type an array type as defined in §4.3?
   */
  protected final boolean isArrayType(final IType type) {
    // TODO: Make this real when I flesh out the ITypes
    return false;
  }

  
  
  /**
   * Is the type reifiable as defined in §4.7?
   */
  protected final boolean isReifiable(final IType type) {
    // TODO: Make this real
    return false;
  }
  
  
  
  // ======================================================================
  // == §5.1 Conversions
  // ======================================================================

  /**
   * Widen the given primitive type to the given primitive type according to
   * §5.1.2.  Includes identity conversion.
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
   * Box the given type according to §5.1.7.
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
      // identity conversion
      return type;
    }
  }
  
  /**
   * Unbox the given type according to §5.1.8.
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
   * Is the type "convertible to a numeric type" as defined in §5.1.8.
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
   * Is the type "convertible to a integral type" as defined in §5.1.8.
   */
  protected final boolean isConvertibleToIntegralType(final IType type) {
    return isIntegralType(type) ||
        isNamedType(type, JAVA_LANG_BYTE) ||
        isNamedType(type, JAVA_LANG_SHORT) ||
        isNamedType(type, JAVA_LANG_CHARACTER) ||
        isNamedType(type, JAVA_LANG_INTEGER) ||
        isNamedType(type, JAVA_LANG_LONG);
  }
  
  /**
   * Perform a capture conversion as defined in §5.1.10.
   */
  protected final IType capture(final IType type) {
    // TODO: Make this real
    return type;
  }
  
  /**
   * Perform a string conversion as defined in §5.1.11.
   */
  protected final IType string(final IRNode expr, final IType type) {
    // XXX: Probably want a preprocess method here
    return typeFactory.getStringType();
  }
  
  /* 5.2 Assignment conversion */
  protected final boolean assignment(final IType lhs, final IType rhs) {
    // TODO: Make this real
    
    // XXX: Needs to first determine how to convert the type, and then convert
    // XXX: it, to get the unbox/box effects, but we don't want to unbox if 
    // XXX: ultimately unboxing is not useful.
    return false;
  }
  
  /* §5.5 Casting conversion */

  protected final boolean isCastable(final IType from, final IType to) {
    // TODO: make this real
    return false;
  }

  protected final void cast(final IType from, final IType to)
  throws TypeCheckingFailed {
    // TODO: make this real
    
    // throw exception if type 'from' is may not be converted to type 'to'
  }

  
  
  // ======================================================================
  // == Least upper bound
  // ======================================================================

  /**
   * Compute the least upper bound of type types as defined in §15.12.2.7.
   */
  protected final IType lub(final IType type1, final IType type2) {
    // TODO: Make this real
    return null;
  }
  
  
  
  // ======================================================================
  // == §5.6 Numeric Promotions
  // ======================================================================

  /* Here we always assume that the promotion is possible: that is, that a 
   * previous check has been made to weed out bad expressions, such as not
   * trying to promote an expression that is not convertible to an integral type 
   * using unary numeric promotion.
   */

  // TODO: Do I want pre/post processing calls here?  What use are they?
  
  /**
   * Promote the given type using unary numeric promotion as defined 
   * in §5.6.1.
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
   * in §5.6.2.
   */
  protected final IType binaryNumericPromotion(
      IType type1, IType type2) {
    if (isReferenceType(type1)) type1 = unbox(type1);
    if (isReferenceType(type2)) type2 = unbox(type2);

    if (isDoubleType(type1) || isDoubleType(type2)) {
      return typeFactory.getDoubleType();
    } else if (isFloatType(type1) || isFloatType(type2)) {
      return typeFactory.getFloatType();
    } else if (isLongType(type1) || isLongType(type2)) {
      return typeFactory.getLongType();
    } else {
      return typeFactory.getIntType();
    }
  }


  
  // ----------------------------------------------------------------------
  // -- Compound promotion operations
  // ----------------------------------------------------------------------

  /*
   * Unlike the above, these operations perform the necessary check of
   * applicability before performing promotion, and report type checking errors
   * if the promotion cannot be applied, or if the result after
   * promotion/conversion is not what is desired.
   */
  
  /**
   * First checks if type is convertible to an integral type (§5.1.8), and if 
   * so, tests if the type is <code>int</code> after unary numeric promotion
   * (§5.6.1).
   */
  protected final void unaryNumericPromotionToIntIfConvertible(
      final IRNode expr, IType type)
  throws TypeCheckingFailed {
    assertConvertibleToIntegralType(type, expr);
    type = unaryNumericPromotion(type);
    if (!isIntType(type)) {
      error(JavaError.NOT_INT, expr, type);
    }
  }
  
  /**
   * First checks if type is convertible to a numeric type (§5.1.8), and if 
   * so, performs unary numeric promotion (§5.6.1).
   */
  protected final IType unaryNumericPromotionIfConvertibleToNumeric(
      final IRNode expr, final IType type)
  throws TypeCheckingFailed {
    assertConvertibleToNumericType(type, expr);
    // Force the promotion so we can toggle an unbox if necessary; don't care about the resulting type
    return unaryNumericPromotion(type);
  }
  
  /**
   * First checks if type is convertible to an integral type (§5.1.8), and if 
   * so, performs unary numeric promotion (§5.6.1).
   */
  protected final IType unaryNumericPromotionIfConvertibleToIntegral(
      final IRNode expr, final IType type)
  throws TypeCheckingFailed {
    assertConvertibleToIntegralType(type, expr);
    // Force the promotion so we can toggle an unbox if necessary; don't care about the resulting type
    return unaryNumericPromotion(type);
  }

  
  
  // ======================================================================
  // == Other helper methods
  // ======================================================================

  /**
   * Type check a conditional expression, and assert that its type is boolean or
   * java.lang.Boolean, performing an unbox conversion if necessary.  Eats 
   * any type-checking failed exception.
   */
  protected final void processCondition(final IRNode condExpr) {
    try {
      assertIsBooleanWithUnbox(doAccept(condExpr), condExpr);
    } catch (final TypeCheckingFailed e) {
      // Ignore, result type is always void
    }
  }
  
  protected final void assertIsBooleanWithUnbox(final IType type, final IRNode expr)
  throws TypeCheckingFailed {
    final boolean isBoxedBoolean = isNamedType(type, JAVA_LANG_BOOLEAN);
    if (!(isBooleanType(type) || isBoxedBoolean)) {
      error(JavaError.NOT_BOOLEAN_TYPE, expr, type);
    }
    if (isBoxedBoolean) unbox(type);
  }

  /**
   * Assert that the type is convertible to a numeric type: returns normally
   * if {@link #isConvertibleToNumericType} is <code>true</code>; throws
   * an exception if not.
   */
  protected final void assertConvertibleToIntegralType(
      final IType type, final IRNode expr)
  throws TypeCheckingFailed {
    if (!isConvertibleToIntegralType(type)) {
      error(JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE, expr, type);
    }
  }

  /**
   * Assert that the type is convertible to a numeric type: returns normally
   * if {@link #isConvertibleToNumericType} is <code>true</code>; throws
   * an exception if not.
   */
  protected final void assertConvertibleToNumericType(
      final IType type, final IRNode expr)
  throws TypeCheckingFailed {
    if (!isConvertibleToNumericType(type)) {
      error(JavaError.NOT_CONVERTIBLE_TO_NUMERIC_TYPE, expr, type);
    }
  }
  
  
  
  // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  // @@ Start of visitor methods
  // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  
  // ======================================================================
  // == Default visitor
  // ======================================================================

  /*
   * Throw an exception if we get here, because it means we forgot to handle
   * an operator.
   */
  @Override
  public final IType visit(final IRNode n) {
    throw new UnsupportedOperationException(
        "Uh oh, we need to do something for " +
            JJNode.tree.getOperator(n).name());
  }

  
  
  // ======================================================================
  // == §6.5.6 Expression Names
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
     * (§5.1.10).
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
    if (isRValue) type = capture(type);    
    return postProcessVariableUseExpression(varUseExpr, nameDecl, isRValue, type);
  }
  
  protected IType postProcessVariableUseExpression(
      final IRNode varUseExpr, final IRNode nameDecl,
      final boolean isRValue, final IType type) {
    // TYPECHECK: Need to check the flow analyses to get the @NonNull/@Raw state
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == §8.3 Field Declarations
  // ======================================================================
  
  // TODO
  
  
  
  // ======================================================================
  // == §9.3 Field (Constant) Declarations
  // ======================================================================
  
  // TODO
  
  
  
  // ======================================================================
  // == §10.6 Array Initializers
  // ======================================================================
  
  /*
   * Each variable initializer must be assignment-compatible (§5.2) with the
   * array's component type, or a compile-time error occurs.
   */
  
  /*
   * Problem: How to keep track of the array type as we go into nested
   * initializers, e.g.,
   * "new int[][][] { { { 1, 2, 3 }, { 4, 5, 6 } }, { { 7 }, { 8 } } }"
   */
  
  // TODO

  
  
  // ======================================================================
  // == §14.2 Blocks
  // ======================================================================

  @Override
  public final IType visitBlockStatement(final IRNode statement) {
    try {
      doAcceptForChildren(statement);
    } catch (final TypeCheckingFailed e) {
      /* Children of this node are also statements, and can never throw this
       * exception.
       */
    }
    return typeFactory.getVoidType();
  }

  
  
  // ======================================================================
  // == §14.3 Local Class Declarations
  // ======================================================================

  @Override
  public final IType visitLocalClassDeclaration(final IRNode decl) {
    try {
      doAccept(LocalClassDeclaration.getBody(decl));
    } catch (final TypeCheckingFailed e) {
      /* Class body eats exceptions, so we won't have any
       */
    }
    return typeFactory.getVoidType();
  }

  
  
  // ======================================================================
  // == §14.4 Local Variable Declaration Statements
  // ======================================================================
  
  // TODO

  // TODO Also VariableResource! But we currently skip over those
  
  
  // ======================================================================
  // == §14.6 Empty Statement
  // ======================================================================

  @Override
  public final IType visitEmptyStatement(final IRNode statement) {
    return typeFactory.getVoidType();
  }

  
  
  // ======================================================================
  // == §14.7 Labeled Statement
  // ======================================================================

  @Override
  public final IType visitLabeledStatement(final IRNode statement) {
    try {
      doAccept(LabeledStatement.getStmt(statement));
    } catch (final TypeCheckingFailed e) {
      // Statements cannot throw this, and we always return void type
    }
    return typeFactory.getVoidType();
  }

  
  
  // ======================================================================
  // == §14.8 Expression Statement
  // ======================================================================

  @Override
  public final IType visitExprStatement(final IRNode statement) {
    try {
      doAccept(ExprStatement.getExpr(statement));
    } catch (final TypeCheckingFailed e) {
      // Eat exception, return type is always void 
    }
    return typeFactory.getVoidType();
  }

  
  
  // ======================================================================
  // == §14.9 The if Statement
  // ======================================================================

  @Override
  public final IType visitIfStatement(final IRNode ifThenElse) {
    /*
     * The Expression must have type boolean or Boolean, or a compile-time error
     * occurs.
     * 
     * If the result is of type Boolean, it is subject to unboxing conversion
     * (§5.1.8).
     */
    
    // Eat exceptions: result type is always void
    processCondition(IfStatement.getCond(ifThenElse));

    try {
      doAccept(IfStatement.getThenPart(ifThenElse));
      
      final IRNode elsePart = IfStatement.getElsePart(ifThenElse);
      if (ElseClause.prototype.includes(elsePart)) {
        doAccept(ElseClause.getElseStmt(elsePart));
      }
    } catch (final TypeCheckingFailed e) {
      // Won't ever be thrown because the nested statements will eat it.
    }
    
    return typeFactory.getVoidType();
  }
  
  
  
  // ======================================================================
  // == §14.10 The assert Statement
  // ======================================================================

  @Override
  public final IType visitAssertStatement(final IRNode assertStmt) {
    /*
     * It is a compile-time error if Expression1 does not have type boolean or
     * Boolean.
     * 
     * If the result is of type Boolean, it is subject to unboxing conversion
     * (§5.1.8).
     */
    // Can eat the any failture because the return type is always void
    processCondition(AssertStatement.getAssertion(assertStmt));
    return typeFactory.getVoidType();
  }

  

  @Override
  public final IType visitAssertMessageStatement(final IRNode assertStmt) {
    /*
     * It is a compile-time error if Expression1 does not have type boolean or
     * Boolean.
     * 
     * If the result is of type Boolean, it is subject to unboxing conversion
     * (§5.1.8).
     * 
     * In the second form of the assert statement, it is a compile-time error if
     * Expression2 is void (§15.1).
     */
    // Can eat the any failture because the return type is always void
    processCondition(AssertMessageStatement.getAssertion(assertStmt));
    
    try {
      final IRNode expr = AssertMessageStatement.getMessage(assertStmt);
      final IType type = doAccept(expr);
      if (isVoidType(type)) {
        error(JavaError.VOID_NOT_ALLOWED, expr, type);
      }
    } catch (final TypeCheckingFailed e) {
      // Ignore, result type is always void
    }
    
    return typeFactory.getVoidType();
  }

  
  
  // ======================================================================
  // == §14.11 The switch Statement
  // ======================================================================

  // TODO
  
  
  
  // ======================================================================
  // == §14.11 The while Statement
  // ======================================================================

  @Override
  public final IType visitWhileStatement(final IRNode whileStmt) {
    /*
     * The Expression must have type boolean or Boolean, or a compile-time error
     * occurs.
     * 
     * If the result is of type Boolean, it is subject to unboxing conversion
     * (§5.1.8).
     */
    // Eat the exception: Return type is always void
    processCondition(WhileStatement.getCond(whileStmt));
    
    try {
      doAccept(WhileStatement.getLoop(whileStmt));
    } catch (final TypeCheckingFailed e) {
      // Should never be thrown because the nested statement will eat it.
    }
    
    return typeFactory.getVoidType();
  }
  
  
  
  // ======================================================================
  // == §14.13 The do Statement
  // ======================================================================

  @Override
  public final IType visitDoStatement(final IRNode doStmt) {
    /*
     * The Expression must have type boolean or Boolean, or a compile-time error
     * occurs.
     * 
     * If the result is of type Boolean, it is subject to unboxing conversion
     * (§5.1.8).
     */
    // Eat the exception: Return type is always void
    processCondition(DoStatement.getCond(doStmt));
    
    try {
      doAccept(DoStatement.getLoop(doStmt));
    } catch (final TypeCheckingFailed e) {
      // Should never be thrown because the nested statement will eat it.
    }
    
    return typeFactory.getVoidType();
  }
  
  
  
  // ======================================================================
  // == §14.14 The for Statement
  // ======================================================================

  // TODO
  
  
  
  // ======================================================================
  // == §14.15 The break Statement
  // ======================================================================

  @Override
  public final IType visitBreakStatement(final IRNode breakStmt) {
    // nothing to do
    return typeFactory.getVoidType();
  }
  
  @Override
  public final IType visitLabeledBreakStatement(final IRNode breakStmt) {
    // nothing to do
    return typeFactory.getVoidType();
  }
  
  
  
  // ======================================================================
  // == §14.16 The continue Statement
  // ======================================================================

  @Override
  public final IType visitContinueStatement(final IRNode continueStmt) {
    // nothing to do
    return typeFactory.getVoidType();
  }
  
  @Override
  public final IType visitLabeledContinueStatement(final IRNode continueStmt) {
    // nothing to do
    return typeFactory.getVoidType();
  }
  
  
  
  // ======================================================================
  // == §14.17 The return Statement
  // ======================================================================

  // TODO
  
  
  
  // ======================================================================
  // == §14.18 The throw Statement
  // ======================================================================

  // TODO
  
  
  
  // ======================================================================
  // == §14.19 The synchronized Statement
  // ======================================================================

  @Override
  public final IType visitSynchronizedStatement(final IRNode syncStmt) {
    /*
     * The type of Expression must be a reference type, or a compile-time error
     * occurs.
     */
    try {
      final IRNode lockExpr = SynchronizedStatement.getLock(syncStmt);
      final IType type = doAccept(lockExpr);
      if (!isReferenceType(type)) {
        error(JavaError.NOT_REFERENCE_TYPE, lockExpr, type);
      }
      preProcessLockExpression(lockExpr, type);
    } catch (final TypeCheckingFailed e) {
      // eat the error: our type is always void
    }
    
    try {
      doAccept(SynchronizedStatement.getBlock(syncStmt));
    } catch (final TypeCheckingFailed e) {
      // eat the error: our type is always void
    }
    
    return typeFactory.getVoidType();
  }
  
  protected void preProcessLockExpression(final IRNode expr, final IType type) {
    // Check for null
  }
  
  
  
  // ======================================================================
  // == §14.20 The try Statement
  // ======================================================================

  @Override
  public final IType visitTryStatement(final IRNode tryStmt) {
    try {
      doAccept(TryStatement.getBlock(tryStmt));
    } catch (final TypeCheckingFailed e) {
      // Should never be thrown, but handed by the nested statements
    }

    final IRNode catchClauses = TryStatement.getCatchPart(tryStmt);
    for (final IRNode catchClause : CatchClauses.getCatchClauseIterator(catchClauses)) {
      try {
        doAccept(CatchClause.getBody(catchClause));
      } catch (final TypeCheckingFailed e) {
        // Should never be thrown, but handed by the nested statements
      }
    }

    final IRNode finallyClause = TryStatement.getFinallyPart(tryStmt);
    if (Finally.prototype.includes(finallyClause)) {
      try {
        doAccept(Finally.getBody(finallyClause));
      } catch (final TypeCheckingFailed e) {
        // Should never be thrown, but handed by the nested statements
      }
    }
    
    return typeFactory.getVoidType();
  }

  @Override
  public final IType visitTryResource(final IRNode tryStmt) {
    final IRNode resources = TryResource.getResources(tryStmt);
    for (final IRNode varResource : Resources.getResourceIterator(resources)) {
      try {
        // XXX: This skips over the VariableResource, and goes straight to the VariableDeclarator.  Might not be a good idea.
        doAccept(VariableResource.getVar(varResource));
      } catch (final TypeCheckingFailed e) {
        // Should never be thrown, but handed by the nested statements
      }
    }
    
    try {
      doAccept(TryResource.getBlock(tryStmt));
    } catch (final TypeCheckingFailed e) {
      // Should never be thrown, but handed by the nested statements
    }

    final IRNode catchClauses = TryResource.getCatchPart(tryStmt);
    for (final IRNode catchClause : CatchClauses.getCatchClauseIterator(catchClauses)) {
      try {
        doAccept(CatchClause.getBody(catchClause));
      } catch (final TypeCheckingFailed e) {
        // Should never be thrown, but handed by the nested statements
      }
    }

    final IRNode finallyClause = TryResource.getFinallyPart(tryStmt);
    if (Finally.prototype.includes(finallyClause)) {
      try {
        doAccept(Finally.getBody(finallyClause));
      } catch (final TypeCheckingFailed e) {
        // Should never be thrown, but handed by the nested statements
      }
    }
    
    return typeFactory.getVoidType();
  }
  
  
  
  // ======================================================================
  // == §15.8.1 Lexical Literals
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
     * The type of an integer literal (§3.10.1) that ends with L or l is long
     * (§4.2.1). The type of any other integer literal is int (§4.2.1).
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
     * The type of a floating-point literal (§3.10.2) that ends with F or f is
     * float and its value must be an element of the float value set (§4.2.3).
     * The type of any other floating-point literal is double and its value must
     * be an element of the double value set (§4.2.3).
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
     * The type of a boolean literal (§3.10.3) is boolean (§4.2.5).
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
     * The type of a character literal (§3.10.4) is char (§4.2.1).
     */
    return postProcessCharLiteral(charLiteral, typeFactory.getCharType());
  }

  protected IType postProcessCharLiteral(final IRNode charLiteral, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitStringLiteral(final IRNode stringLiteral) {
    /*
     * The type of a string literal (§3.10.5) is String (§4.3.3).
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
     * The type of the null literal null (§3.10.7) is the null type (§4.1); its
     * value is the null reference.
     */
    return postProcessNullLiteral(nullLiteral, typeFactory.getNullType());
  }

  protected IType postProcessNullLiteral(final IRNode nullLiteral, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == §15.8.2 Class Literals
  // ======================================================================

  @Override
  // XXX: throws TypeCheckingFailed?  Depends if needs binding or not
  public final IType visitClassExpression(final IRNode classExpr) {
    /*
     * The type of C.class, where C is the name of a class, interface, or array
     * type (§4.3), is Class<C>.
     * 
     * The type of p.class, where p is the name of a primitive type (§4.2), is
     * Class<B>, where B is the type of an expression of type p after boxing
     * conversion (§5.1.7).
     * 
     * The type of void.class (§8.4.5) is Class<Void>.
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
  // == §15.8.3 this
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
  // == §15.8.4 Qualified this
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
  // == §15.8.5 Parenthesized Expressions
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
  // == §15.9 Class Instance Creation Expressions
  // ======================================================================

  // TODO

  
  
  // ======================================================================
  // == §15.10 Array Creation Expressions
  // ======================================================================
  
  @Override
  public final IType visitArrayCreationExpression(final IRNode arrayExpr) {
    /*
     * The type of each dimension expression within a DimExpr must be a type
     * that is convertible (§5.1.8) to an integral type, or a compile-time error
     * occurs.
     * 
     * Each dimension expression undergoes unary numeric promotion (§5.6.1). The
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
  // == §15.11 Field Access Expressions
  // ======================================================================
  
  @Override
  public final IType visitFieldRef(final IRNode fieldRefExpr) 
  throws TypeCheckingFailed {
    /*
     * This rule technically only applies when the object expression is a
     * Primary, "super", or "ClassName.super" expression: The type of the field
     * access expression is the type of the member field after capture
     * conversion (§5.1.10).
     * 
     * But the rules in §6.5.6 for expression names, also say to use the
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
      error(JavaError.NOT_REFERENCE_TYPE, objectExpr, objectType);
    }
    
    /* Binding the field reference expression gets the VariableDeclarator
     * of the accessed field, or the EnumConstantDeclaration of the accessed
     * enumeration constant.
     * 
     * N.B. Per §8.9.2 the type of an enumeration constant is the enumeration
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
        fieldRefExpr, varDecl, capture(fieldType));
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
  // == §15.12 Method Invocation Expressions
  // ======================================================================
  
  // TODO

  
  
  // ======================================================================
  // == §15.13 Array Access Expressions
  // ======================================================================
 
  @Override
  public final IType visitArrayRefExpression(final IRNode arrayRefExpr)
  throws TypeCheckingFailed {
    /*
     * The type of the array reference expression must be an array type (call it
     * T[], an array whose components are of type T).
     * 
     * The index expression undergoes unary numeric promotion (§5.6.1). The
     * promoted type must be int, or a compile-time error occurs.
     * 
     * The type of the array access expression is the result of applying capture
     * conversion (§5.1.10) to T.
     */
    
    /* If there is a type-error when visiting the reference expression, or the
     * type is not an array type, then we cannot recover here, so we pass
     * on the exception.
     */
    final IRNode refExpr = ArrayRefExpression.getArray(arrayRefExpr);
    final IType arrayType = doAccept(refExpr);
    if (!isArrayType(arrayType)) {
      error(JavaError.NOT_ARRAY_TYPE, refExpr, arrayType);
    }
    preProcessArrayReference(refExpr, arrayType);
    
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
        arrayRefExpr, capture(elementType));
  }
  
  protected void preProcessArrayReference(final IRNode refExpr, final IType type) {
    // TYPECHECK: Need to check that the array reference expression isn't null.
  }
  
  protected IType postProcessArrayRefExpression(final IRNode arrayRefExpr, final IType type) {
    // TYPECHECK: The array element type might be @NonNull
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == §15.14.2 Postfix Increment Operator
  // ======================================================================
  
  @Override
  public final IType visitPostIncrementExpression(final IRNode postIncExpr) 
  throws TypeCheckingFailed {
    /*
     * The result of the postfix expression must be a variable of a type that is
     * convertible (§5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the postfix increment expression is the type of the variable.
     * 
     * Before the addition, binary numeric promotion (§5.6.2) is performed on
     * the value 1 and the value of the variable.
     */
    
    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PostIncrementExpression.getOp(postIncExpr);
    final IType exprType = doAccept(opExpr);
    assertConvertibleToNumericType(exprType, opExpr);
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPostIncrementExpression(postIncExpr, exprType);
  }
  
  protected IType postProcessPostIncrementExpression(final IRNode postIncExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == §15.14.3 Postfix Decrement Operator
  // ======================================================================
  
  @Override
  public final IType visitPostDecrementExpression(final IRNode postDecExpr)
  throws TypeCheckingFailed {
    /*
     * The result of the postfix expression must be a variable of a type that is
     * convertible (§5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the postfix decrement expression is the type of the variable.
     * 
     * Before the subtraction, binary numeric promotion (§5.6.2) is performed on
     * the value 1 and the value of the variable.
     */

    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PostDecrementExpression.getOp(postDecExpr);
    final IType exprType = doAccept(opExpr);
    assertConvertibleToNumericType(exprType, opExpr);
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPostDecrementExpression(postDecExpr, exprType);
  }
  
  protected IType postProcessPostDecrementExpression(final IRNode postDecExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == §15.15.1 Prefix Increment Operator
  // ======================================================================
  
  @Override
  public final IType visitPreIncrementExpression(final IRNode preIncExpr)
  throws TypeCheckingFailed {
    /*
     * The result of the unary expression must be a variable of a type that is
     * convertible (§5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the prefix increment expression is the type of the variable.
     * 
     * Before the addition, binary numeric promotion (§5.6.2) is performed on
     * the value 1 and the value of the variable.
     */

    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PreIncrementExpression.getOp(preIncExpr);
    final IType exprType = doAccept(opExpr);
    assertConvertibleToNumericType(exprType, opExpr);
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPreIncrementExpression(preIncExpr, exprType);
  }
  
  protected IType postProcessPreIncrementExpression(final IRNode preIncExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == §15.15.2 Prefix Decrement Operator
  // ======================================================================
  
  @Override
  public final IType visitPreDecrementExpression(final IRNode preDecExpr) 
  throws TypeCheckingFailed {
    /*
     * The result of the unary expression must be a variable of a type that is
     * convertible (§5.1.8) to a numeric type, or a compile-time error occurs.
     * 
     * The type of the prefix decrement expression is the type of the variable.
     * 
     * Before the subtraction, binary numeric promotion (§5.6.2) is performed on
     * the value 1 and the value of the variable.
     */
    
    /*
     * Type errors on the subexpression cannot be recovered from because the
     * type of this expression is derived from the type of the subexpression.
     */
    final IRNode opExpr = PreDecrementExpression.getOp(preDecExpr);
    final IType exprType = doAccept(opExpr);
    assertConvertibleToNumericType(exprType, opExpr);
    binaryNumericPromotion(exprType, typeFactory.getIntType());
    return postProcessPreDecrementExpression(preDecExpr, exprType);
  }
  
  protected IType postProcessPreDecrementExpression(final IRNode preDecExpr, final IType type) {
    return postProcessType(type);
  }

  
  
  // ======================================================================
  // == §15.15.3 Unary Plus Operator
  // ======================================================================
  
  @Override
  public final IType visitPlusExpression(final IRNode plusExpr) 
  throws TypeCheckingFailed {
    /*
     * The type of the operand expression of the unary + operator must be a type
     * that is convertible (§5.1.8) to a primitive numeric type, or a
     * compile-time error occurs.
     * 
     * Unary numeric promotion (§5.6.1) is performed on the operand. The type of
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
  // == §15.15.4 Unary Minus Operator
  // ======================================================================
  
  @Override
  public final IType visitMinusExpression(final IRNode minusExpr) 
  throws TypeCheckingFailed {
    /*
     * The type of the operand expression of the unary - operator must be a type
     * that is convertible (§5.1.8) to a primitive numeric type, or a
     * compile-time error occurs.
     * 
     * Unary numeric promotion (§5.6.1) is performed on the operand.
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
  // == §15.15.5 Bitwise Complement Operator ~
  // ======================================================================
  
  @Override
  public final IType visitComplementExpression(final IRNode complementExpr) 
  throws TypeCheckingFailed {
    /*
     * The type of the operand expression of the unary ~ operator must be a type
     * that is convertible (§5.1.8) to a primitive integral type, or a
     * compile-time error occurs.
     * 
     * Unary numeric promotion (§5.6.1) is performed on the operand. The type of
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
  // == §15.15.6 Logical Complement Operator !
  // ======================================================================
  
  @Override
  public final IType visitNotExpression(final IRNode notExpr) {
    /*
     * The type of the operand expression of the unary ! operator must be
     * boolean or Boolean, or a compile-time error occurs.
     * 
     * The type of the unary logical complement expression is boolean.
     * 
     * At run-time, the operand is subject to unboxing conversion (§5.1.8) if
     * necessary.
     */
    
    /*
     * Eat any type errors in the subexpression because the type of the
     * expression is always boolean.
     */
    processCondition(NotExpression.getOp(notExpr));
    return typeFactory.getBooleanType();
  }
  
  protected IType postProcessNotExpression(final IRNode notExpr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.16 Cast Expressions
  // ======================================================================

  @Override
  public final IType visitCastExpression(final IRNode expr) {
    /*
     * The type of a cast expression is the result of applying capture
     * conversion (§5.1.10) to the type whose name appears within the
     * parentheses.
     * 
     * It is a compile-time error if the compile-time type of the operand may
     * never be cast to the type specified by the cast operator according to the
     * rules of casting conversion (§5.5).
     */
    final IType type = typeFactory.getTypeFromExpression(
        CastExpression.getType(expr));
    
    try {
      final IRNode subExpr = CastExpression.getExpr(expr);
      final IType subExprType = doAccept(subExpr);
      cast(subExprType, type);
    } catch (final TypeCheckingFailed e) {
      // Eat the exception: return type is determined by the type expression
    }
    
    return postProcessCastExpression(expr, capture(type));
  }
  
  protected IType postProcessCastExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  


  // ======================================================================
  // == §15.17 Multiplicative Operators
  // ======================================================================
  
  private IType visitMultiplicativeOperator(
      final IRNode expr, final IRNode op1, final IRNode op2) 
  throws TypeCheckingFailed {
    /*
     * The type of each of the operands of a multiplicative operator must be a
     * type that is convertible (§5.1.8) to a primitive numeric type, or a
     * compile-time error occurs.
     * 
     * Binary numeric promotion is performed on the operands (§5.6.2).
     * 
     * The type of a multiplicative expression is the promoted type of its
     * operands.
     */
    
    /*
     * Do not catch any type errors in the sub expressions, because we are
     * unable to type this expression if we cannot get the sub expression
     * types.
     */
    final IType type1 = doAccept(op1);
    assertConvertibleToNumericType(type1, op1);
    
    final IType type2 = doAccept(op2);
    assertConvertibleToNumericType(type2, op2);
    
    final IType resultType = binaryNumericPromotion(type1, type2);
    return resultType;
  }
  
  @Override
  public final IType visitMulExpression(final IRNode mulExpr) 
  throws TypeCheckingFailed {
    return postProcessMulExpression(mulExpr,
        visitMultiplicativeOperator(mulExpr,
            MulExpression.getOp1(mulExpr), MulExpression.getOp2(mulExpr)));
  }
  
  protected IType postProcessMulExpression(final IRNode mulExpr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitDivExpression(final IRNode divExpr) 
  throws TypeCheckingFailed {
    return postProcessDivExpression(divExpr,
        visitMultiplicativeOperator(divExpr,
            DivExpression.getOp1(divExpr), DivExpression.getOp2(divExpr)));
  }
  
  protected IType postProcessDivExpression(final IRNode divExpr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitRemExpression(final IRNode remExpr) 
  throws TypeCheckingFailed {
    return postProcessRemExpression(remExpr,
        visitMultiplicativeOperator(remExpr,
            RemExpression.getOp1(remExpr), RemExpression.getOp2(remExpr)));
  }
  
  protected IType postProcessRemExpression(final IRNode remExpr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.18.1 String Concatenation Operator +
  // ======================================================================

  @Override
  public IType visitStringConcat(final IRNode concat) {
    /*
     * If only one operand expression is of type String, then string conversion
     * (§5.1.11) is performed on the other operand to produce a string at
     * run-time.
     * 
     * The result of string concatenation is a reference to a String object that
     * is the concatenation of the two operand strings.
     */
    try {
      final IRNode op1 = StringConcat.getOp1(concat);
      IType type1 = doAccept(op1);
      if (!isNamedType(type1, JAVA_LANG_STRING)) {
        type1 = string(op1, type1);
      }
    } catch (final TypeCheckingFailed e) {
      /*
       * Can ignore this because the overall type of this expression is always
       * string.
       */
    }

    try {
      final IRNode op2 = StringConcat.getOp2(concat);
      IType type2 = doAccept(op2);
      if (!isNamedType(type2, JAVA_LANG_STRING)) {
        type2 = string(op2, type2);
      }
    } catch (final TypeCheckingFailed e) {
      /*
       * Can ignore this because the overall type of this expression is always
       * string.
       */
    }

    return postProcessStringConcat(concat, typeFactory.getStringType());
  }
  
  protected IType postProcessStringConcat(final IRNode concat, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.18.1 Additive Operators (+ and -) for Numeric Types
  // ======================================================================

  private IType visitAdditiveOperator(
      final IRNode expr, final IRNode op1, final IRNode op2) 
  throws TypeCheckingFailed {
    /*
     * The type of each of the operands of the + operator must be a type that is
     * convertible (§5.1.8) to a primitive numeric type, or a compile-time error
     * occurs.
     * 
     * Binary numeric promotion is performed on the operands (§5.6.2).
     * 
     * The type of an additive expression on numeric operands is the promoted
     * type of its operands.
     */

    /*
     * Do not catch any type errors in the sub expressions, because we are
     * unable to type this expression if we cannot get the sub expression
     * types.
     */
    final IType type1 = doAccept(op1);
    assertConvertibleToNumericType(type1, op1);
    
    final IType type2 = doAccept(op2);
    assertConvertibleToNumericType(type2, op2);
    
    final IType resultType = binaryNumericPromotion(type1, type2);
    return resultType;
  }
  
  @Override
  public final IType visitAddExpression(final IRNode addExpr) 
  throws TypeCheckingFailed {
    return postProcessAddExpression(addExpr,
        visitAdditiveOperator(addExpr,
            AddExpression.getOp1(addExpr),
            AddExpression.getOp2(addExpr)));
  }
  
  protected IType postProcessAddExpression(final IRNode addExpr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitSubExpression(final IRNode subExpr) 
  throws TypeCheckingFailed {
    return postProcessSubExpression(subExpr,
        visitAdditiveOperator(subExpr,
            SubExpression.getOp1(subExpr),
            SubExpression.getOp2(subExpr)));
  }
  
  protected IType postProcessSubExpression(final IRNode subExpr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.19 Shift Operators
  // ======================================================================

  private IType visitShiftOperator(
      final IRNode expr, final IRNode op1, final IRNode op2)
  throws TypeCheckingFailed {
    /*
     * Unary numeric promotion (§5.6.1) is performed on each operand separately.
     * (Binary numeric promotion (§5.6.2) is not performed on the operands.)
     * 
     * It is a compile-time error if the type of each of the operands of a shift
     * operator, after unary numeric promotion, is not a primitive integral
     * type.
     * 
     * The type of the shift expression is the promoted type of the left-hand
     * operand.
     */
    
    /* Do not catch this error because we need the type of the LHS to type
     * this expression.
     */
    final IType type1 = unaryNumericPromotion(doAccept(op1));
    if (!isIntegralType(type1)) {
      error(JavaError.NOT_INTEGRAL_TYPE, op1, type1); 
    }
    
    try {
      final IType type2 = unaryNumericPromotion(doAccept(op2));
      if (!isIntegralType(type2)) {
        error(JavaError.NOT_INTEGRAL_TYPE, op2, type2); 
      }
    } catch (final TypeCheckingFailed e) {
      /*
       * Can eat this error because we only need the type of LHS to type
       * the shift expression.
       */
    }
    
    return type1;
  }
  
  @Override
  public final IType visitLeftShiftExpression(final IRNode leftShift)
  throws TypeCheckingFailed {
    return postProcessLeftShiftExpression(leftShift,
        visitShiftOperator(leftShift,
            LeftShiftExpression.getOp1(leftShift),
            LeftShiftExpression.getOp2(leftShift)));
  }
  
  protected IType postProcessLeftShiftExpression(final IRNode leftShift, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitRightShiftExpression(final IRNode rightShift)
  throws TypeCheckingFailed {
    return postProcessRightShiftExpression(rightShift,
        visitShiftOperator(rightShift,
            RightShiftExpression.getOp1(rightShift),
            RightShiftExpression.getOp2(rightShift)));
  }
  
  protected IType postProcessRightShiftExpression(final IRNode rightShift, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitUnsignedRightShiftExpression(final IRNode unsignedShift)
  throws TypeCheckingFailed {
    return postProcesUnsignedRightShiftExpression(unsignedShift,
        visitShiftOperator(unsignedShift,
            UnsignedRightShiftExpression.getOp1(unsignedShift),
            UnsignedRightShiftExpression.getOp2(unsignedShift)));
  }
  
  protected IType postProcesUnsignedRightShiftExpression(final IRNode unsignedShift, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.20.1 Numerical Comparison Operators <, <=, >, and >=
  // ======================================================================

  private IType visitNumericalComparison(
      final IRNode expr, final IRNode op1, final IRNode op2) {
    /*
     * The type of a relational expression is always boolean.
     * 
     * The type of each of the operands of a numerical comparison operator must
     * be a type that is convertible (§5.1.8) to a primitive numeric type, or a
     * compile-time error occurs.
     * 
     * Binary numeric promotion is performed on the operands (§5.6.2).
     */
    try {
      final IType type1 = doAccept(op1);
      assertConvertibleToNumericType(type1, op1);

      final IType type2 = doAccept(op2);
      assertConvertibleToNumericType(type2, op2);
      
      binaryNumericPromotion(type1, type2);
    } catch (final TypeCheckingFailed e) {
      /* Can eat the exception because the expression type is always
       * boolean.
       */
    }

    return typeFactory.getBooleanType();
  }
  
  @Override
  public final IType visitLessThanExpression(final IRNode expr) {
    return postProcessLessThanExpression(expr,
        visitNumericalComparison(expr,
            LessThanExpression.getOp1(expr),
            LessThanExpression.getOp2(expr)));
  }
  
  protected IType postProcessLessThanExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitLessThanEqualExpression(final IRNode expr) {
    return postProcessLessThanEqualExpression(expr,
        visitNumericalComparison(expr,
            LessThanEqualExpression.getOp1(expr),
            LessThanEqualExpression.getOp2(expr)));
  }
  
  protected IType postProcessLessThanEqualExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitGreaterThanExpression(final IRNode expr) {
    return postProcessGreaterThanExpression(expr,
        visitNumericalComparison(expr,
            GreaterThanExpression.getOp1(expr),
            GreaterThanExpression.getOp2(expr)));
  }
  
  protected IType postProcessGreaterThanExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitGreaterThanEqualExpression(final IRNode expr) {
    return postProcessGreaterThanEqualExpression(expr,
        visitNumericalComparison(expr,
            GreaterThanEqualExpression.getOp1(expr),
            GreaterThanEqualExpression.getOp2(expr)));
  }
  
  protected IType postProcessGreaterThanEqualExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.20.2 Type Comparison Operator instanceof
  // ======================================================================

  @Override
  public final IType visitInstanceOfExpression(final IRNode expr) {
    /*
     * The type of the RelationalExpression operand of the instanceof operator
     * must be a reference type or the null type; otherwise, a compile-time
     * error occurs.
     * 
     * It is a compile-time error if the ReferenceType mentioned after the
     * instanceof operator does not denote a reference type that is reifiable
     * (§4.7).
     */
    
    try {
      final IRNode value = InstanceOfExpression.getValue(expr);
      final IType type = doAccept(value);
      if (!isReferenceType(type) && !isNullType(type)) {
        error(JavaError.NOT_REFERENCE_OR_NULL_TYPE, value, type);
      }
    } catch (final TypeCheckingFailed e) {
      // Eat error because return type is always boolean
    }
    
    try {
      final IRNode typeExpr = InstanceOfExpression.getType(expr);
      final IType type = doAccept(typeExpr);
      if (!isReifiable(type)) {
        error(JavaError.NOT_REIFIABLE, typeExpr, type);
      }
    } catch (final TypeCheckingFailed e) {
      // Eat error because return type is always boolean
    }
    
    // The return type is always boolean
    return postProcessInstanceOfExpression(expr, typeFactory.getBooleanType());
  }
  
  protected IType postProcessInstanceOfExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == §15.21 Equality Operators
  // ======================================================================

  protected final IType visitEqualityOperator(
      final IRNode expr, final IRNode op1, final IRNode op2) {
    /*
     * The type of an equality expression is always boolean.
     */
    
    IType type1 = null;   
    try {
      type1 = doAccept(op1);
    } catch (final TypeCheckingFailed e) {
      type1 = null;
    }
    
    IType type2 = null;
    try {
      type2 = doAccept(op2);
    } catch (final TypeCheckingFailed e) {
      type2 = null;
    }
    
    if (type1 != null && type2 != null) {
      /*
       * §15.21.1 Numerical Equality Operators == and !=
       * 
       * If the operands of an equality operator are both of numeric type, or
       * one is of numeric type and the other is convertible (§5.1.8) to numeric
       * type, binary numeric promotion is performed on the operands (§5.6.2).
       * 
       * 
       * §15.21.2 Numerical Equality Operators == and !=
       * 
       * If the operands of an equality operator are both of type boolean, or if
       * one operand is of type boolean and the other is of type Boolean, then
       * the operation is boolean equality.
       * 
       * If one of the operands is of type Boolean, it is subjected to unboxing
       * conversion (§5.1.8).
       * 
       * 
       * §15.21.3 Reference Equality Operators == and !=
       * 
       * If the operands of an equality operator are both of either reference
       * type or the null type, then the operation is object equality.
       * 
       * It is a compile-time error if it is impossible to convert the type of
       * either operand to the type of the other by a casting conversion (§5.5).
       * The run-time values of the two operands would necessarily be unequal.
       */
      final boolean isNumeric1 = isNumericType(type1);
      final boolean isNumeric2 = isNumericType(type2);
      final boolean isBoolean1 = isBooleanType(type1);
      final boolean isBoolean2 = isBooleanType(type2);
      final boolean isBoxedBoolean1 = isNamedType(type1, JAVA_LANG_BOOLEAN);
      final boolean isBoxedBoolean2 = isNamedType(type2, JAVA_LANG_BOOLEAN);
      final boolean isRef1 = isReferenceType(type1);
      final boolean isRef2 = isReferenceType(type2);
      final boolean isNull1 = isNullType(type1);
      final boolean isNull2 = isNullType(type2);
      
      if ((isNumeric1 && isNumeric2) || 
          (isNumeric1 && isConvertibleToNumericType(type2)) ||
          (isNumeric2 && isConvertibleToNumericType(type1))) {
        binaryNumericPromotion(type1, type2);
      } else if ((isBoolean1 && isBoolean2) ||
            (isBoolean1 && isBoxedBoolean2) ||
            (isBoolean2 && isBoxedBoolean1)) {
        if (isBoxedBoolean1) unbox(type1);
        if (isBoxedBoolean2) unbox(type2);
      } else if ((isRef1 || isNull1) && (isRef2 || isNull2)) {
        if (!isCastable(type1, type2) && !isCastable(type2, type1)) {
          // TODO: What about the side effects of casting?  Might unbox?
          try {
            error(JavaError.NOT_APPLICABLE, expr, type1, type2);
          } catch (final TypeCheckingFailed e) {
            // always return boolean
          }
        } else {
          /* 
           * Both types are already reference, so there is no need for boxing
           * or unboxing that may cause strange things to happen.
           */
        }
      } else {
        try {
          error(JavaError.NOT_APPLICABLE, expr, type1, type2);
        } catch (final TypeCheckingFailed e) {
          // always return boolean
        }
      }
    }
    
    return typeFactory.getBooleanType();
  }
  
  @Override
  public final IType visitEqExpression(final IRNode expr) {
    return postProcessEqExpression(expr,
        visitEqualityOperator(expr,
            EqExpression.getOp1(expr),
            EqExpression.getOp2(expr)));
  }
  
  protected IType postProcessEqExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitNotEqExpression(final IRNode expr) {
    return postProcessNotEqExpression(expr,
        visitEqualityOperator(expr,
            NotEqExpression.getOp1(expr),
            NotEqExpression.getOp2(expr)));
  }
  
  protected IType postProcessNotEqExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  
  
  // ======================================================================
  // == §15.22 Bitwise and Logical Operators
  // ======================================================================
  
  private IType visitBitwiseAndLogical(
      final IRNode expr, final IRNode op1, final IRNode op2)
  throws TypeCheckingFailed {
    /*
     * §15.22.1 Integer Bitwise Operators &, ^, and |
     * 
     * When both operands of an operator &, ^, or | are of a type that is
     * convertible (§5.1.8) to a primitive integral type, binary numeric
     * promotion is first performed on the operands (§5.6.2).
     * 
     * The type of the bitwise operator expression is the promoted type of the
     * operands.
     * 
     * 
     * §15.22.2 Boolean Logical Operators &, ^, and |
     * 
     * When both operands of a &, ^, or | operator are of type boolean or
     * Boolean, then the type of the bitwise operator expression is boolean. In
     * all cases, the operands are subject to unboxing conversion (§5.1.8) as
     * necessary.
     */
    
    /* Cannot catch the exceptions here because without the types of the
     * operands we cannot determine the type of this expression.
     */
    final IType type1 = doAccept(op1);
    final IType type2 = doAccept(op2);
    
    if (isConvertibleToIntegralType(type1) && isConvertibleToIntegralType(type2)) {
      return binaryNumericPromotion(type1, type2);
    } else {
      final boolean isBoxedBoolean1 = isNamedType(type1, JAVA_LANG_BOOLEAN);
      final boolean isBoxedBoolean2 = isNamedType(type2, JAVA_LANG_BOOLEAN);
      if ((isBooleanType(type1) || isBoxedBoolean1) &&
          (isBooleanType(type2) || isBoxedBoolean2)) {
        if (isBoxedBoolean1) unbox(type1);
        if (isBoxedBoolean2) unbox(type2);
        return typeFactory.getBooleanType();
      }
    }
    error(JavaError.NOT_APPLICABLE, expr, type1, type2);
    return null; // DEAD CODE: error() always throws an exception
  }
  
  @Override
  public final IType visitAndExpression(final IRNode expr)
  throws TypeCheckingFailed {
    return postProcessAndExpression(expr,
        visitBitwiseAndLogical(expr,
            AndExpression.getOp1(expr),
            AndExpression.getOp2(expr)));
  }
  
  protected IType postProcessAndExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitOrExpression(final IRNode expr)
  throws TypeCheckingFailed {
    return postProcessOrExpression(expr,
        visitBitwiseAndLogical(expr,
            OrExpression.getOp1(expr),
            OrExpression.getOp2(expr)));
  }
  
  protected IType postProcessOrExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitXorExpression(final IRNode expr)
  throws TypeCheckingFailed {
    return postProcessXorExpression(expr,
        visitBitwiseAndLogical(expr,
            XorExpression.getOp1(expr),
            XorExpression.getOp2(expr)));
  }
  
  protected IType postProcessXorExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.23 Conditional-And Operator
  // ======================================================================
  
  @Override
  public final IType visitConditionalAndExpression(final IRNode expr) {
    /*
     * Each operand of the conditional-and operator must be of type boolean or
     * Boolean, or a compile-time error occurs.
     * 
     * The type of a conditional-and expression is always boolean.
     * 
     * At run-time, the left-hand operand expression is evaluated first; if the
     * result has type Boolean, it is subjected to unboxing conversion (§5.1.8).
     * 
     * If the value of the left-hand operand is true, then the right-hand
     * expression is evaluated; if the result has type Boolean, it is subjected
     * to unboxing conversion (§5.1.8).
     */

    /* Can eat the exception because the result type is always boolean. */
    processCondition(ConditionalAndExpression.getOp1(expr));

    /* Can eat the exception because the result type is always boolean. */
    processCondition(ConditionalAndExpression.getOp2(expr));

    return postProcessConditionalAndExpression(
        expr, typeFactory.getBooleanType());
  }
    
  protected IType postProcessConditionalAndExpression(
      final IRNode expr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.24 Conditional-Or Operator
  // ======================================================================
  
  @Override
  public final IType visitConditionalOrExpression(final IRNode expr) {
    /*
     * Each operand of the conditional-or operator must be of type boolean or
     * Boolean, or a compile-time error occurs.
     * 
     * The type of a conditional-or expression is always boolean.
     * 
     * At run-time, the left-hand operand expression is evaluated first; if the
     * result has type Boolean, it is subjected to unboxing conversion (§5.1.8).
     * 
     * If the value of the left-hand operand is false, then the right-hand
     * expression is evaluated; if the result has type Boolean, it is subjected
     * to unboxing conversion (§5.1.8).
     */

    /* Can eat the exception because the result type is always boolean. */
    processCondition(ConditionalOrExpression.getOp1(expr));

    /* Can eat the exception because the result type is always boolean. */
    processCondition(ConditionalOrExpression.getOp2(expr));
    
    return postProcessConditionalOrExpression(
        expr, typeFactory.getBooleanType());
  }
    
  protected IType postProcessConditionalOrExpression(
      final IRNode expr, final IType type) {
    return postProcessType(type);
  }



  // ======================================================================
  // == §15.25 Conditional Operator ?:
  // ======================================================================

  @Override
  public final IType visitConditionalExpression(final IRNode expr) 
  throws TypeCheckingFailed {
    /*
     * The first expression must be of type boolean or Boolean, or a
     * compile-time error occurs.
     * 
     * It is a compile-time error for either the second or the third operand
     * expression to be an invocation of a void method.
     * 
     * The type of a conditional expression is determined as follows:
     * 
     * If the second and third operands have the same type (which may be the
     * null type), then that is the type of the conditional expression.
     * 
     * If one of the second and third operands is of primitive type T, and the
     * type of the other is the result of applying boxing conversion (§5.1.7) to
     * T, then the type of the conditional expression is T.
     * 
     * If one of the second and third operands is of the null type and the type
     * of the other is a reference type, then the type of the conditional
     * expression is that reference type.
     * 
     * Otherwise, if the second and third operands have types that are
     * convertible (§5.1.8) to numeric types, then there are several cases:
     * 
     * If one of the operands is of type byte or Byte and the other is of type
     * short or Short, then the type of the conditional expression is short.
     * 
     * If one of the operands is of type T where T is byte, short, or char, and
     * the other operand is a constant expression (§15.28) of type int whose
     * value is representable in type T, then the type of the conditional
     * expression is T.
     * 
     * If one of the operands is of type T, where T is Byte, Short, or
     * Character, and the other operand is a constant expression (§15.28) of
     * type int whose value is representable in the type U which is the result
     * of applying unboxing conversion to T, then the type of the conditional
     * expression is U.
     * 
     * Otherwise, binary numeric promotion (§5.6.2) is applied to the operand
     * types, and the type of the conditional expression is the promoted type of
     * the second and third operands.
     * 
     * Otherwise, the second and third operands are of types S1 and S2
     * respectively. Let T1 be the type that results from applying boxing
     * conversion to S1, and let T2 be the type that results from applying
     * boxing conversion to S2.
     * 
     * The type of the conditional expression is the result of applying capture
     * conversion (§5.1.10) to lub(T1, T2) (§15.12.2.7).
     * 
     * At run-time, the first operand expression of the conditional expression
     * is evaluated first. If necessary, unboxing conversion is performed on the
     * result.
     */
    
    try {
      final IRNode cond = ConditionalExpression.getCond(expr);
      final IType type1 = doAccept(cond);
      assertIsBooleanWithUnbox(type1, cond);
    } catch (final TypeCheckingFailed e) {
      // Can eat the exception because the overall type is not based on the type of the condition
    }
    
    final IRNode ifTrue = ConditionalExpression.getIftrue(expr);
    final IType type2 = doAccept(ifTrue);
    final IRNode ifFalse = ConditionalExpression.getIffalse(expr);
    final IType type3 = doAccept(ifFalse);
    
    if (isVoidType(type2)) {
      error(JavaError.VOID_NOT_ALLOWED, ifTrue, type2);
    }
    if (isVoidType(type3)) {
      error(JavaError.VOID_NOT_ALLOWED, ifFalse, type3);
    }
    
    IType result = null;
    if (isSameType(type2,  type3)) {
      result = type2;
    } else if (isPrimitiveType(type2) && isSameType(box(type2), type3)) {
      result = type2;
    } else if (isPrimitiveType(type3) && isSameType(box(type3), type2)) {
      result = type3;
    } else if (isNullType(type2) && isReferenceType(type3)) {
      result = type3;
    } else if (isNullType(type3) && isReferenceType(type2)) {
      result = type2;
    } else if (isConvertibleToNumericType(type2) && isConvertibleToNumericType(type3)) {
      /* Need to be careful about order here: check "constant expressions" 
       * first, even though they are described after the byte–short option.
       * 
       *  XXX: Not worried about constant expressions for now.
       */
      if (((isByteType(type2) || isNamedType(type2, JAVA_LANG_BYTE)) &&
              (isShortType(type3) || isNamedType(type3, JAVA_LANG_SHORT))) ||
          ((isByteType(type3) || isNamedType(type3, JAVA_LANG_BYTE)) &&
              (isShortType(type2) || isNamedType(type2, JAVA_LANG_SHORT)))) {
        result = typeFactory.getShortType();
      } else {
        result = binaryNumericPromotion(type2, type3);
      }
    } else {
      result = capture(lub(box(type2), box(type3)));
    }
    return postProcessConditionalExpression(expr, result);
  }
  
  protected IType postProcessConditionalExpression(final IRNode expr, final IType type) {
    // TODO: What to do here, if anything?  Could be smart about processing
    // reference types.
    return postProcessType(type);
  }
  
  

  // ======================================================================
  // == §15.26 Assignment Operators
  // ======================================================================

  /*
   * The result of the first operand of an assignment operator must be a
   * variable, or a compile-time error occurs.
   * 
   * This operand may be a named variable, such as a local variable or a field
   * of the current object or class, or it may be a computed variable, as can
   * result from a field access (§15.11) or an array access (§15.13).
   * 
   * The type of the assignment expression is the type of the variable after
   * capture conversion (§5.1.10).
   */

  @Override
  public final IType visitAssignExpression(final IRNode expr)
  throws TypeCheckingFailed {
    /*
     * The type of the assignment expression is the type of the variable after
     * capture conversion (§5.1.10).
     * 
     * A compile-time error occurs if the type of the right-hand operand cannot
     * be converted to the type of the variable by assignment conversion (§5.2).
     */
    
    // Don't catch failures on the lhs
    final IType lhsType = doAccept(AssignExpression.getOp1(expr));
    try {
      final IType rhsType = doAccept(AssignExpression.getOp2(expr));
      if (!assignment(lhsType, rhsType)) {
        error(JavaError.NOT_ASSIGNABLE, expr, lhsType, rhsType);
      }
    } catch (final TypeCheckingFailed e) {
      // Ignore: the type of this expression is derived from the type of the LHS
    }
    
    return postProcessAssignExpression(expr, capture(lhsType));
  }
  
  protected IType postProcessAssignExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
  
  @Override
  public final IType visitOpAssignExpression(final IRNode expr)
  throws TypeCheckingFailed {
    /*
     * A compound assignment expression of the form E1 op= E2 is equivalent to
     * E1 = (T) ((E1) op (E2)), where T is the type of E1, except that E1 is
     * evaluated only once.
     * 
     * op is one of * / % + - << >> >>> & ^ |
     */
    
    // TODO: Work out the handling of the promotions, etc, necessary.  This should go away when I switch over to using Box/UnboxExpressions
    
    // don't catch failures on the lhs
    final IType lhsType = doAccept(OpAssignExpression.getOp1(expr));
    return postProcessOpAssignExpression(expr, capture(lhsType));
  }
  
  protected IType postProcessOpAssignExpression(final IRNode expr, final IType type) {
    return postProcessType(type);
  }
}



