package com.surelogic.analysis.type.checker;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AddExpression;
import edu.cmu.cs.fluid.java.operator.AndExpression;
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.ComplementExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalAndExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalOrExpression;
import edu.cmu.cs.fluid.java.operator.DivExpression;
import edu.cmu.cs.fluid.java.operator.EqExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.GreaterThanEqualExpression;
import edu.cmu.cs.fluid.java.operator.GreaterThanExpression;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.LeftShiftExpression;
import edu.cmu.cs.fluid.java.operator.LessThanEqualExpression;
import edu.cmu.cs.fluid.java.operator.LessThanExpression;
import edu.cmu.cs.fluid.java.operator.MinusExpression;
import edu.cmu.cs.fluid.java.operator.MulExpression;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.NonPolymorphicMethodCall;
import edu.cmu.cs.fluid.java.operator.NotEqExpression;
import edu.cmu.cs.fluid.java.operator.NotExpression;
import edu.cmu.cs.fluid.java.operator.OrExpression;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.PlusExpression;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.RemExpression;
import edu.cmu.cs.fluid.java.operator.RightShiftExpression;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.SubExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.TypeExpression;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;
import edu.cmu.cs.fluid.java.operator.UnsignedRightShiftExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.operator.XorExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;

/**
 * Visitor that is used to determine if an expression is a "constant
 * expression" as defined in JLS 15.28.
 */
public class ConstantExpressionVisitor extends Visitor<Boolean> {
  private static final String NULL_AS_STRING = "\"null\"";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String TO_STRING = "toString";
  private final IBinder binder;
  
  
  
  public ConstantExpressionVisitor(final IBinder b) {
    binder = b;
  }

  
  
  @Override
  public Boolean visit(final IRNode n) {
    // Unless otherwise noted, expressions are not "constant"
    return false;
  }
  
  
  
  /*
   * Literals of primitive type and literals of type String
   * (JLS 3.10.1,JLS 3.10.2,JLS 3.10.3, JLS 3.10.4, JLS 3.10.5)
   */
  
  @Override
  public Boolean visitIntLiteral(final IRNode e) {
    return true;
  }
  
  @Override
  public Boolean visitFloatLiteral(final IRNode e) {
    return true;
  }
  
  @Override
  public Boolean visitBooleanLiteral(final IRNode e) {
    return true;
  }
  
  @Override
  public Boolean visitCharLiteral(final IRNode e) {
    return true;
  }
  
  @Override
  public Boolean visitStringLiteral(final IRNode e) {
    /*
     * The Java Canonicalizer converts the NullLiteral to the StringLiteral
     * "null" if the NullLiteral is part of a StringConcat operation. We need to
     * check for this because the NullLiteral is never a constant expression.
     */
    if (JavaNode.wasImplicit(e) &&
        StringLiteral.getToken(e).equals(NULL_AS_STRING)) {
      return false;
    } else {
      return true;
    }
  }
  
  
  
  /*
   * Casts to primitive types and casts to type String (JLS 15.16)
   */
  
  @Override
  public Boolean visitCastExpression(final IRNode e) {
    final IRNode type = CastExpression.getType(e);
    if (isPrimitiveTypeOrString(type)) {
      return doAccept(CastExpression.getExpr(e));
    } else {
      return false;
    }
  }



  private boolean isPrimitiveTypeOrString(final IRNode type) {
    return PrimitiveType.prototype.includes(type) ||
        (NamedType.prototype.includes(type) &&
            NamedType.getType(type).equals(JAVA_LANG_STRING));
  }
  
  
  
  /*
   * The unary operators +, -, ~, and ! (but not ++ or --) (JLS 15.15.3, JLS 15.15.4,
   * JLS 15.15.5, JLS 15.15.6)
   */
  
  @Override
  public Boolean visitPlusExpression(final IRNode e) {
    return doAccept(PlusExpression.getOp(e));
  }
  
  @Override
  public Boolean visitMinusExpression(final IRNode e) {
    return doAccept(MinusExpression.getOp(e));
  }
  
  @Override
  public Boolean visitComplementExpression(final IRNode e) {
    return doAccept(ComplementExpression.getOp(e));
  }
  
  @Override
  public Boolean visitNotExpression(final IRNode e) {
    return doAccept(NotExpression.getOp(e));
  }
  
  
  
  /*
   * The multiplicative operators *, /, and % (JLS 15.17)
   */
  
  @Override
  public Boolean visitMulExpression(final IRNode e) {
    return doAccept(MulExpression.getOp1(e)) &&
        doAccept(MulExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitDivExpression(final IRNode e) {
    return doAccept(DivExpression.getOp1(e)) &&
        doAccept(DivExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitRemExpression(final IRNode e) {
    return doAccept(RemExpression.getOp1(e)) &&
        doAccept(RemExpression.getOp2(e));
  }
  
  
  
  /*
   * The additive operators + and - (JLS 15.18)
   */
  
  @Override
  public Boolean visitAddExpression(final IRNode e) {
    return doAccept(AddExpression.getOp1(e)) &&
        doAccept(AddExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitStringConcat(final IRNode e) {
    return doAccept(StringConcat.getOp1(e)) &&
        doAccept(StringConcat.getOp2(e));
  }
  
  @Override
  public Boolean visitSubExpression(final IRNode e) {
    return doAccept(SubExpression.getOp1(e)) &&
        doAccept(SubExpression.getOp2(e));
  }
  
  
  
  /*
   * The shift operators <<, >>, and >>> (JLS 15.19)
   */
  
  @Override
  public Boolean visitLeftShiftExpression(final IRNode e) {
    return doAccept(LeftShiftExpression.getOp1(e)) &&
        doAccept(LeftShiftExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitRightShiftExpression(final IRNode e) {
    return doAccept(RightShiftExpression.getOp1(e)) &&
        doAccept(RightShiftExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitUnsignedRightShiftExpression(final IRNode e) {
    return doAccept(UnsignedRightShiftExpression.getOp1(e)) &&
        doAccept(UnsignedRightShiftExpression.getOp2(e));
  }
  
  
  
  /*
   * The relational operators <, <=, >, and >= (but not instanceof) (JLS 15.20)
   */
  
  @Override
  public Boolean visitLessThanExpression(final IRNode e) {
    return doAccept(LessThanExpression.getOp1(e)) &&
        doAccept(LessThanExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitLessThanEqualExpression(final IRNode e) {
    return doAccept(LessThanEqualExpression.getOp1(e)) &&
        doAccept(LessThanEqualExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitGreaterThanExpression(final IRNode e) {
    return doAccept(GreaterThanExpression.getOp1(e)) &&
        doAccept(GreaterThanExpression.getOp2(e));
  }
  
  @Override
  public Boolean visitGreaterThanEqualExpression(final IRNode e) {
    return doAccept(GreaterThanEqualExpression.getOp1(e)) &&
        doAccept(GreaterThanEqualExpression.getOp2(e));
  }
  
  
  
  /*
   * The equality operators == and != (JLS 15.21)
   */
  
  @Override
  public Boolean visitEqExpression(final IRNode e) {
    return doAccept(EqExpression.getOp1(e)) &&
        doAccept(EqExpression.getOp2(e));    
  }
  
  @Override
  public Boolean visitNotEqExpression(final IRNode e) {
    return doAccept(NotEqExpression.getOp1(e)) &&
        doAccept(NotEqExpression.getOp2(e));    
  }



  /*
   * The bitwise and logical operators &, ^, and | (JLS 15.22)
   */
  
  @Override
  public Boolean visitAndExpression(final IRNode e) {
    return doAccept(AndExpression.getOp1(e)) &&
        doAccept(AndExpression.getOp2(e));    
  }
  
  @Override
  public Boolean visitXorExpression(final IRNode e) {
    return doAccept(XorExpression.getOp1(e)) &&
        doAccept(XorExpression.getOp2(e));    
  }
  
  @Override
  public Boolean visitOrExpression(final IRNode e) {
    return doAccept(OrExpression.getOp1(e)) &&
        doAccept(OrExpression.getOp2(e));    
  }
  
  
  
  /*
   * The conditional-and operator && and the conditional-or operator || (JLS 15.23,
   * JLS 15.24)
   */
  
  @Override
  public Boolean visitConditionalAndExpression(final IRNode e) {
    return doAccept(ConditionalAndExpression.getOp1(e)) &&
        doAccept(ConditionalAndExpression.getOp2(e));    
  }
  
  @Override
  public Boolean visitConditionalOrExpression(final IRNode e) {
    return doAccept(ConditionalOrExpression.getOp1(e)) &&
        doAccept(ConditionalOrExpression.getOp2(e));    
  }
  
  
  
  /*
   * The ternary conditional operator ? : (JLS 15.25)
   */
  
  @Override
  public Boolean visitConditionalExpression(final IRNode e) {
    return doAccept(ConditionalExpression.getCond(e)) &&
        doAccept(ConditionalExpression.getIftrue(e)) &&
        doAccept(ConditionalExpression.getIffalse(e));
  }
  
  
  
  /*
   * Parenthesized expressions (JLS 15.8.5) whose contained expression is a
   * constant expression.
   */
  
  @Override
  public Boolean visitParenExpression(final IRNode e) {
    return doAccept(ParenExpression.getOp(e));
  }
  
  
  
  /*
   * Simple names (JLS 6.5.6.1) that refer to constant variables (JLS 4.12.4).
   * 
   * Qualified names (JLS 6.5.6.2) of the form TypeName . Identifier that refer to
   * constant variables (JLS 4.12.4).
   * 
   * 
   * A variable of primitive type or type String, that is final and initialized
   * with a compile-time constant expression (JLS 15.28), is called a constant
   * variable.
   */
  @Override
  public Boolean visitFieldRef(final IRNode e) {
    final IRNode objectExpr = FieldRef.getObject(e);
    
    // Is it a simple name or a qualified name TypeName . Identifier 
    if ((ThisExpression.prototype.includes(objectExpr) && 
        JavaNode.wasImplicit(objectExpr)) ||
        TypeExpression.prototype.includes(objectExpr)) {
      // Check the field declaration
      final IRNode fdecl = binder.getBinding(e);
      if (TypeUtil.isJavaFinal(fdecl)) {
        // final, now check the initializer
        final IRNode init = VariableDeclarator.getInit(fdecl);
        if (Initialization.prototype.includes(init)) {
          // (1) Check the type of the field: must be primitive or String
          // (2) check if the initializer is constant
          return isPrimitiveTypeOrString(VariableDeclarator.getType(fdecl)) &&
              doAccept(Initialization.getValue(init));
        }
      }
    }
    return false;
  }
  
  
  
  /*
   * Box and Unbox expressions do not exist in the Java syntax, they are
   * introduced by our Java Canonicalizer.  We pass through them here
   * because they aren't part of the syntax.  I think we only encounter them
   * in a Constant Expression when it is a String concatenation such as
   * "foo" + 3, where the canonicalizer turns the 3 into X.toString(), where
   * X is a box expression of the literal 3.
   */
  
  @Override
  public Boolean visitBoxExpression(final IRNode e) {
    return doAccept(BoxExpression.getOp(e));
  }
  
  @Override
  public Boolean visitUnboxExpression(final IRNode e) {
    return doAccept(UnboxExpression.getOp(e));
  }
  
  
  
  /*
   * As stated above, the Java canonicalizer introduces calls to toString() 
   * in StringConcat expressions.  Such calls are modeled as
   * NonPolymorphicMethodCall nodes that have an arg list that is marked
   * as implicit.  We want to pass through them and check the status of the
   * object expression.
   */
  
  @Override
  public Boolean visitNonPolymorphicMethodCall(final IRNode e) {
    if (NonPolymorphicMethodCall.getMethod(e).equals(TO_STRING) &&
        JavaNode.wasImplicit(NonPolymorphicMethodCall.getArgs(e))) {
      return doAccept(NonPolymorphicMethodCall.getObject(e));
    } else {
      return false;
    }
  }
}
