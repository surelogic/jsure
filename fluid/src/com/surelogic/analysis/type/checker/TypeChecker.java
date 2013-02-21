package com.surelogic.analysis.type.checker;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ArrayCreationExpression;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/*
 * NOTE: Holding off on the "typeCheck" methods because I need to figure out
 * still how I'm going to allow subclasses to hook in an modify the generated 
 * types when they find an annotation.  For now I'm going to leave 
 * crumbs in the code by inserting "// TYPECHECK" in the code with appropriate
 * additional comments.
 */
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
public class TypeChecker extends Visitor<IType> {
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
  // == ¤6.5.6 Expression Names
  // ======================================================================

  /* Qualified expression names refer to fields, and are handled by
   * visitFieldRef() below.  Here we only handle simple expression names
   * that refer to local variables or parameters.  Simple expressions that
   * refer to fields are converted by the parse tree to have explicit "this"
   * references and thus are also handled by visitFieldRef() below.
   */
  
  @Override
  public IType visitVariableUseExpression(final IRNode varUseExpr) {
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
    boolean shouldCaptureConvert = true;
    if (parentOp instanceof AssignmentInterface) {
      if (((AssignmentInterface) parentOp).getTarget(parent) == varUseExpr) {
        shouldCaptureConvert = false;
      }
    }
    
    final IRNode nameDecl = binder.getBinding(varUseExpr);
    final IRNode typeExpr;
    if (ParameterDeclaration.prototype.includes(nameDecl)) {
      typeExpr = ParameterDeclaration.getType(nameDecl);
    } else { // VariableDeclarator
      typeExpr = DeclStatement.getType(
          JJNode.tree.getParent(JJNode.tree.getParent(nameDecl)));
    }
    IType type = typeFactory.getTypeFromExpression(typeExpr);
    if (shouldCaptureConvert) type = conversionEngine.capture(type);
    return type;
  }
  
  
  
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
    return getTypeBasedOnEndTag(IntLiteral.getToken(intLiteral), 'L', 
        typeFactory.getLongType(), typeFactory.getIntType());
  }

  @Override
  public final IType visitFloatLiteral(final IRNode floatLiteral) {
    /*
     * The type of a floating-point literal (¤3.10.2) that ends with F or f is
     * float and its value must be an element of the float value set (¤4.2.3).
     * The type of any other floating-point literal is double and its value must
     * be an element of the double value set (¤4.2.3).
     */
    return getTypeBasedOnEndTag(FloatLiteral.getToken(floatLiteral), 'F', 
        typeFactory.getFloatType(), typeFactory.getDoubleType());
  }
  
  @Override
  public final IType visitBooleanLiteral(final IRNode booleanLiteral) {
    /*
     * The type of a boolean literal (¤3.10.3) is boolean (¤4.2.5).
     */
    return typeFactory.getBooleanType();
  }
  
  @Override
  public final IType visitCharLiteral(final IRNode charLiteral) {
    /*
     * The type of a character literal (¤3.10.4) is char (¤4.2.1).
     */
    return typeFactory.getCharType();
  }
  
  @Override
  public final IType visitStringLiteral(final IRNode stringLiteral) {
    /*
     * The type of a string literal (¤3.10.5) is String (¤4.3.3).
     */
    return typeFactory.getStringType();
  }
  
  @Override
  public final IType visitNullLiteral(final IRNode nullLiteral) {
    /*
     * The type of the null literal null (¤3.10.7) is the null type (¤4.1); its
     * value is the null reference.
     */
    return typeFactory.getNullType();
  }

  
  
  // ======================================================================
  // == ¤15.8.2 Class Literals
  // ======================================================================

  @Override
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
    if (PrimitiveType.prototype.includes(typeExprOp)) { // case #2
      return typeFactory.getClassType(
          conversionEngine.box(typeFactory.getPrimitiveType(typeExprOp)));
    } else if (VoidType.prototype.includes(typeExprOp)) { // case #3
      return typeFactory.getClassType(typeFactory.getVoidType());
    } else { // case #1
      /* 
       * Expression is a ReferenceType, more specifically it must be a
       * NamedType or an ArrayType.
       */
      return typeFactory.getClassType(
          typeFactory.getReferenceTypeFromExpression(typeExpr));
    }
  }

  
  
  // ======================================================================
  // == ¤15.8.3 this
  // ======================================================================
  
  @Override
  public final IType visitThisExpression(final IRNode thisExpr) {
    /*
     * The type of this is the class C within which the keyword this occurs.
     */
    return typeFactory.getReferenceTypeFromDeclaration(
        VisitUtil.getEnclosingType(thisExpr));
  }

  
  
  // ======================================================================
  // == ¤15.8.4 Qualified this
  // ======================================================================
  
  @Override
  public final IType visitQualifiedThisExpression(final IRNode qualifiedThisExpr) {
    /*
     * Let C be the class denoted by ClassName. Let n be an integer such that C
     * is the n'th lexically enclosing class of the class in which the qualified
     * this expression appears.
     * 
     * The type of the expression is C.
     */
    final IRNode decl = binder.getBinding(
        QualifiedThisExpression.getType(qualifiedThisExpr));
    return typeFactory.getReferenceTypeFromDeclaration(decl);
  }

  
  
  // ======================================================================
  // == ¤15.8.5 Parenthesized Expressions
  // ======================================================================
  
  @Override
  public IType visitParenExpression(final IRNode parenExpr) {
    /*
     * A parenthesized expression is a primary expression whose type is the type
     * of the contained expression and whose value at run-time is the value of
     * the contained expression.
     */
    return doAccept(ParenExpression.getOp(parenExpr)); 
  }

  
  
  // ======================================================================
  // == ¤15.9 Class Instance Creation Expressions
  // ======================================================================

  // TODO

  
  
  // ======================================================================
  // == ¤15.10 Array Creation Expressions
  // ======================================================================
  
  @Override
  public IType visitArrayCreationExpression(final IRNode arrayExpr) {
    /* Process the children first.  We assume the code compiles, so we don't
     * care about checking that the types of the dimension expressions are
     * integer. 
     */
    // TYPECHECK: We do care about handling the initialization expressions.
    // TYPECHECK: See ¤10.6.
    doAcceptForChildren(arrayExpr);
    
    /*
     * The type of the array creation expression is an array type that can
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
    int dims = JJNode.tree.numChildren(ArrayCreationExpression.getAllocated(arrayExpr));
    if (ArrayType.prototype.includes(baseType)) {
      dims += ArrayType.getDims(baseType); // must go first
      baseType = ArrayType.getBase(baseType); // resets baseType
    }
    return typeFactory.getArrayType(
        typeFactory.getTypeFromExpression(baseType), dims);
  }

  
  
  // ======================================================================
  // == ¤15.11 Field Access Expressions
  // ======================================================================
  
  @Override
  public IType visitFieldRef(final IRNode fieldRefExpr) {
    doAcceptForChildren(fieldRefExpr);

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
    
    /* Binding the field reference expression gets the VariableDeclarator
     * of the accessed field, or the EnumConstantDeclaration of the accessed
     * enumeration constant.
     * 
     * N.B. Per ¤8.9.2 the type of an enumeration constant is the enumeration
     * type E that contains the constant declaration.
     */
    final IRNode varDecl = binder.getBinding(fieldRefExpr);
    final IType fieldType;
    if (VariableDeclarator.prototype.includes(varDecl)) {
      final IRNode fieldDecl = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
      final IRNode typeExpr = FieldDeclaration.getType(fieldDecl);
      fieldType = typeFactory.getTypeFromExpression(typeExpr);
      
      // TYPECHECK: If the field is not static, need to check the type
      // TYPECHECK: of the object expression (for null)
    } else {
      final IRNode enumDecl = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
      fieldType = typeFactory.getReferenceTypeFromDeclaration(enumDecl);
    }
    return conversionEngine.capture(fieldType);
  }

  
  
  // ======================================================================
  // == ¤15.12 Method Invocation Expressions
  // ======================================================================
  
  // TODO

  
  
  // ======================================================================
  // == ¤15.13 Array Access Expressions
  // ======================================================================
 
  @Override
  public IType visitArrayRefExpression(final IRNode arrayRefExpr) {
    /*
     * The type of the array reference expression must be an array type (call it
     * T[], an array whose components are of type T).
     * 
     * (Ignoring the type checking of the index expression: it only involves
     * primitive types.)
     * 
     * The type of the array access expression is the result of applying capture
     * conversion (¤5.1.10) to T.
     */
    // TYPECHECK: Need to check that the array reference expression isn't null.
    final List<IType> componentTypes =
        doAcceptForChildrenWithResults(arrayRefExpr);
    final IType elementType =
        typeFactory.getArrayElementType(componentTypes.get(0));
    return conversionEngine.capture(elementType);
  }
}

