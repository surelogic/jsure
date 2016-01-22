package com.surelogic.analysis.concurrency.model;

import java.util.Iterator;

import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.CharLiteral;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.FalseExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.FloatLiteral;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.TrueExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.ParseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class SyntacticEquality {
  private final ThisExpressionBinder thisExprBinder;
  
  
  
  private SyntacticEquality(final ThisExpressionBinder thisExprBinder) {
    this.thisExprBinder = thisExprBinder;
  }

  
  
  /**
   * Check if two lock expressions are syntactically equal. Basically this means
   * that they represent equivalent parse trees.
   * 
   * @param expr1
   *          The first expression
   * @param expr2
   *          The second expression
   * @param thisExprBinder
   *          The binder to use.
   * @return <code>true</code> if the two expressions are syntactically equal.
   */
  public static boolean checkSyntacticEquality(
      final IRNode expr1, final IRNode expr2,
      final ThisExpressionBinder thisExprBinder) {
    final SyntacticEquality checker = new SyntacticEquality(thisExprBinder);
    return checker.check(expr1, expr2);
  }
  
  

  private final boolean check(final IRNode expr1, final IRNode expr2) {
    /* We need to unwrap type casts and parenthesized expressions in the first operand */
    final Operator op1 = JJNode.tree.getOperator(expr1);
    if (CastExpression.prototype.includes(op1)) {
      return check(CastExpression.getExpr(expr1), expr2);
    } else if (ParenExpression.prototype.includes(op1)) {
      return check(ParenExpression.getOp(expr1), expr2);
    }

    /* Then unwrap type casts and parenthesized expressions in the second operand */
    final Operator op2 = JJNode.tree.getOperator(expr2);
    if (CastExpression.prototype.includes(op2)) {
      return check(expr1, CastExpression.getExpr(expr2));
    } else if (ParenExpression.prototype.includes(op2)) {
      return check(expr1, ParenExpression.getOp(expr2));
    }
    
    if (op1.equals(op2)) {
      if (ParameterDeclaration.prototype.includes(op1)) {
        return expr1.equals(expr2);
      } else if (FieldRef.prototype.includes(op1)) {
        // check that the fields are the same
        if (thisExprBinder.getBinding(expr1).equals(thisExprBinder.getBinding(expr2))) {
          /* If the field is an instance field, check that the dereferenced
           * objects are the same.
           */
          if (TypeUtil.isStatic(thisExprBinder.getBinding(expr1))) {
            return true;
          } else {
            return check(
                thisExprBinder.bindThisExpression(FieldRef.getObject(expr1)),
                thisExprBinder.bindThisExpression(FieldRef.getObject(expr2)));
          }
        } else { // fields are not equal
          return false;
        }
      } else if (ClassExpression.prototype.includes(op1)) {
        // Check that the two expressions are for the same class
        final IRNode class1 = thisExprBinder.getBinding(expr1);
        final IRNode class2 = thisExprBinder.getBinding(expr2);
        return class1.equals(class2);
      } else if (ArrayRefExpression.prototype.includes(op1)) {
        final IRNode array1 = ArrayRefExpression.getArray(expr1);
        final IRNode array2 = ArrayRefExpression.getArray(expr2);
        final IRNode idx1 = ArrayRefExpression.getIndex(expr1);
        final IRNode idx2 = ArrayRefExpression.getIndex(expr2);
        return check(array1, array2) && check(idx1, idx2);
      } else if (VariableUseExpression.prototype.includes(op1)) {
        return (thisExprBinder.getBinding(expr1).equals(thisExprBinder.getBinding(expr2)));
      } else if (ReceiverDeclaration.prototype.includes(op1)) {
        return expr1.equals(expr2);
      } else if (QualifiedReceiverDeclaration.prototype.includes(op1)) {
          return expr1.equals(expr2);
      } else if (NamedType.prototype.includes(op1)) {
        return NamedType.getType(expr1).equals(NamedType.getType(expr2));
      } else if (FalseExpression.prototype.includes(op1)) {
        return true;
      } else if (TrueExpression.prototype.includes(op1)) {
        return true;
      } else if (CharLiteral.prototype.includes(op1)) {
        return ParseUtil.decodeCharLiteral(expr1) == ParseUtil.decodeCharLiteral(expr2);
      } else if (FloatLiteral.prototype.includes(op1)) {
        /* Don't deal with this yet.  May be sometime in the future when we 
         * have a reason to care about it.
         */
        return false;
      } else if (IntLiteral.prototype.includes(op1)) {
        /* Parse the tokens and compare the values.  This allows us to 
         * say that 0x20 is equivalent to 32, and so on.  Here we leverage the
         * fact that we only run on parseable Java programs.  We always decode
         * as longs because they can represent every valid int as well.
         */
        return ParseUtil.decodeIntLiteralAsLong(expr1) == ParseUtil.decodeIntLiteralAsLong(expr2);
      } else if (NullLiteral.prototype.includes(op1)) {
        return true;
      } else if (MethodCall.prototype.includes(op1)) {
        /* Objects must equivalent and must be the same method and must be a
         * no-arg method. This simplifies checking because we currently only
         * care about this case for handling the readLock() and writeLock()
         * methods of ReadWriteLock.
         */
        final MethodCall call = (MethodCall) op1;
        final IRNode object1 = call.get_Object(expr1);
        final IRNode object2 = call.get_Object(expr2);
        // Are the receivers equivalent?
        if (check(object1, object2)) {
          final IRNode mdecl1 = thisExprBinder.getBinding(expr1);
          final IRNode mdecl2 = thisExprBinder.getBinding(expr2);
          // Are the methods the same?
          if (mdecl1 == mdecl2) {
            // Is the method a no-arg method?
            final IRNode params1 = MethodDeclaration.getParams(mdecl1);
            final Iterator<IRNode> iter1 = Parameters.getFormalIterator(params1);
            if (!iter1.hasNext()) return true;
          }
        }
        return false;
      } else {
        throw new IllegalArgumentException(
            "Cannot check syntactic equality of " + op1.name());
      }
    } else {
      /* Different operators.  Here we have one special case: if one 
       * expression is an IntLiteral and the other is a CharLiteral.
       */
      IRNode charLiteral = null;
      IRNode intLiteral = null;
      if (IntLiteral.prototype.includes(op1)) { intLiteral = expr1; }
      else if (CharLiteral.prototype.includes(op1)) { charLiteral = expr1; }
      if (IntLiteral.prototype.includes(op2)) { intLiteral = expr2; }
      else if (CharLiteral.prototype.includes(op2)) { charLiteral = expr2; }
      if (charLiteral != null && intLiteral != null) {
        final int charValue = ParseUtil.decodeCharLiteral(charLiteral);
        final long intValue = ParseUtil.decodeIntLiteralAsLong(intLiteral);
        return charValue == intValue;
      } else {
        return false;
      }
    }
  }
}
