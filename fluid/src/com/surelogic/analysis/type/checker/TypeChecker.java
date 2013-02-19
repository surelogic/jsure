package com.surelogic.analysis.type.checker;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.parse.JJNode;

public class TypeChecker extends Visitor<IType> {
  private final ITypeFactory typeFactory;
  
  public TypeChecker(final ITypeFactory tf) {
    typeFactory = tf;
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
  public IType visitIntLiteral(final IRNode intLiteral) {
    /*
     * The type of an integer literal (¤3.10.1) that ends with L or l is long
     * (¤4.2.1). The type of any other integer literal is int (¤4.2.1).
     */
    return getTypeBasedOnEndTag(IntLiteral.getToken(intLiteral), 'L', 
        typeFactory.getLongType(), typeFactory.getIntType());
  }

  @Override
  public IType visitFloatLiteral(final IRNode floatLiteral) {
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
  public IType visitBooleanLiteral(final IRNode booleanLiteral) {
    /*
     * The type of a boolean literal (¤3.10.3) is boolean (¤4.2.5).
     */
    return typeFactory.getBooleanType();
  }
  
  @Override
  public IType visitCharLiteral(final IRNode charLiteral) {
    /*
     * The type of a character literal (¤3.10.4) is char (¤4.2.1).
     */
    return typeFactory.getCharType();
  }
  
  @Override
  public IType visitStringLiteral(final IRNode stringLiteral) {
    /*
     * The type of a string literal (¤3.10.5) is String (¤4.3.3).
     */
    return typeFactory.getStringType();
  }
  
  @Override
  public IType visitNullLiteral(final IRNode nullLiteral) {
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
  public IType visitClassExpression(final IRNode classExpr) {
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
    if (PrimitiveType.prototype.includes(typeExpr)) { // case #2
      return typeFactory.getClassType(
          typeFactory.box(
              typeFactory.getPrimitiveType(
                  JJNode.tree.getOperator(typeExpr))));
    } else if (VoidType.prototype.includes(typeExpr)) { // case #3
      return typeFactory.getClassType(typeFactory.getVoidType());
    } else { // case #1
      // XXX: To do
      return null;
    }
  }
}

