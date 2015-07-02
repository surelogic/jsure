package com.surelogic.analysis.concurrency.heldlocks.locks;

import java.util.Iterator;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.java.ClassExpressionNode;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;
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


// Only needed in the analysis package, no need to be public
abstract class AbstractILock implements ILock {
  /**
   * The lock drop for {@link #lockDecl}.
   */
  protected final LockModel lockPromise;
  
  /**
   * Whether the lock is monolithic, read only, or write.
   */
  protected final Type type; 
  
  
  
  /**
   * Create a new lock object.
   */
  AbstractILock(final LockModel lm, final Type t) {
    lockPromise = lm;
    type = t;
  }
  
  /**
   * Get the lock declaration node.
   */
  @Override
  public final AbstractLockDeclarationNode getLockDecl() {
    return lockPromise.getAAST();
  }
  
  /**
   * @return Returns the promise drop associated with the lock.
   */
  @Override
  public final LockModel getLockPromise() {
    return lockPromise;
  }

  /**
   * Get the name of the lock.
   */
  @Override
  public final String getName() {
	if (getLockDecl() == null) {
		return null;
	}
    return getLockDecl().getId();
  }

  @Override
  public final boolean isWrite() {
    return type != Type.READ;
  }
  
  /**
   * Used to implement hashCode() and equals()
   */
  public Object getUniqueIdentifier() {
    return lockPromise;
  }
  
  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object o);

  protected final boolean baseEquals(final AbstractILock other) {
    return getUniqueIdentifier().equals(other.getUniqueIdentifier())
        && (type == other.type);
  }
  
  @Override
  public final int hashCode() {
    final int baseHashCode = getUniqueIdentifier().hashCode();
    // Bug 1010: Need to differentiate read lock from write lock
    return type == Type.WRITE ? ~baseHashCode : baseHashCode; 
  }



  
  /**
   * Check if two lock expressions are syntactically equal. Basically this means
   * that they represent equivalent parse trees.
   * 
   * @param expr1
   *          The first expression
   * @param expr2
   *          The second expression
   * @param binder
   *          The binder to use.
   * @return <code>true</code> if the two expressions are syntactically equal.
   */
  protected static boolean checkSyntacticEquality(
      final IRNode expr1, final IRNode expr2,
      final ThisExpressionBinder thisExprBinder) {
    /* We need to unwrap type casts and parenthesized expressions in the first operand */
    final Operator op1 = JJNode.tree.getOperator(expr1);
    if (CastExpression.prototype.includes(op1)) {
      return checkSyntacticEquality(
          CastExpression.getExpr(expr1), expr2, thisExprBinder);
    } else if (ParenExpression.prototype.includes(op1)) {
      return checkSyntacticEquality(
          ParenExpression.getOp(expr1), expr2, thisExprBinder);
    }

    /* Then unwrap type casts and parenthesized expressions in the second operand */
    final Operator op2 = JJNode.tree.getOperator(expr2);
    if (CastExpression.prototype.includes(op2)) {
      return checkSyntacticEquality(
          expr1, CastExpression.getExpr(expr2), thisExprBinder);
    } else if (ParenExpression.prototype.includes(op2)) {
      return checkSyntacticEquality(
          expr1, ParenExpression.getOp(expr2), thisExprBinder);
    }
    
    if (op1.equals(op2)) {
      if (FieldRef.prototype.includes(op1)) {
        // check that the fields are the same
        if (thisExprBinder.getBinding(expr1).equals(thisExprBinder.getBinding(expr2))) {
          /* If the field is an instance field, check that the dereferenced
           * objects are the same.
           */
          if (TypeUtil.isStatic(thisExprBinder.getBinding(expr1))) {
            return true;
          } else {
            return checkSyntacticEquality(
                thisExprBinder.bindThisExpression(FieldRef.getObject(expr1)),
                thisExprBinder.bindThisExpression(FieldRef.getObject(expr2)),
                thisExprBinder);
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
        return checkSyntacticEquality(array1, array2, thisExprBinder)
            && checkSyntacticEquality(idx1, idx2, thisExprBinder);
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
        if (checkSyntacticEquality(object1, object2, thisExprBinder)) {
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
  
  protected static boolean checkSyntacticEquality(
      final AASTNode expr1, final AASTNode expr2,
      final ThisExpressionBinder thisExprBinder) {
    if (expr1.getClass().equals(expr2.getClass())) {
      if (expr1 instanceof FieldRefNode) {
        // check that the fields are the same
        FieldRefNode f1 = (FieldRefNode) expr1;
        FieldRefNode f2 = (FieldRefNode) expr2;
        IRNode vd1      = f1.resolveBinding().getNode();
        if (vd1.equals(f2.resolveBinding().getNode())) {            
          /* If the field is an instance field, check that the dereferenced
           * objects are the same.
           */
          if (TypeUtil.isStatic(vd1)) {
            return true;
          } else {
            final IRNode fixed1 = thisExprBinder.bindThisExpression(f1.getObject());
            final IRNode fixed2 = thisExprBinder.bindThisExpression(f2.getObject());
            if (fixed1 == null && fixed2 == null) {
              return checkSyntacticEquality(f1.getObject(), f2.getObject(), thisExprBinder);
            } else if (fixed1 != null && fixed2 != null) {
              return checkSyntacticEquality(fixed1, fixed2, thisExprBinder);
            } else {
              return false;
            }
          }
        } else { // fields are not equal
          return false;
        }
      } else if (expr1 instanceof ClassExpressionNode) {
        // Check that the two expressions are for the same class
        final IRNode class1 = ((ClassExpressionNode) expr1).resolveType().getNode();
        final IRNode class2 = ((ClassExpressionNode) expr2).resolveType().getNode();
        return class1.equals(class2);
      } else if (expr1 instanceof VariableUseExpressionNode) {
        VariableUseExpressionNode v1 = (VariableUseExpressionNode) expr1;
        VariableUseExpressionNode v2 = (VariableUseExpressionNode) expr2;
        return v1.resolveBinding().getNode().equals(v2.resolveBinding().getNode());
      } else if (expr1 instanceof ReceiverDeclarationNode) {
        return expr1.equals(expr2);
      } else if (expr1 instanceof QualifiedReceiverDeclarationNode) {
          return expr1.equals(expr2);
      } else if (expr1 instanceof NamedTypeNode) {
        NamedTypeNode t1 = (NamedTypeNode) expr1;
        NamedTypeNode t2 = (NamedTypeNode) expr2;
        return t1.resolveType().getNode().equals(t2.resolveType().getNode());
      } else {
        throw new IllegalArgumentException(
            "Cannot check syntactic equality of " + expr1);
      }
    } else {
      return false;
    }
  }
  
  protected static boolean checkSyntacticEquality(
      final IRNode expr1, final AASTNode expr2, 
      final ThisExpressionBinder thisExprBinder) {
    /* We need to unwrap type casts and parenthesized expressions in the first operand */
    final Operator op1 = JJNode.tree.getOperator(expr1);
    if (CastExpression.prototype.includes(op1)) {
      return checkSyntacticEquality(
          CastExpression.getExpr(expr1), expr2, thisExprBinder);
    } else if (ParenExpression.prototype.includes(op1)) {
      return checkSyntacticEquality(
          ParenExpression.getOp(expr1), expr2, thisExprBinder);
    }
    
    final Operator op2 = expr2.getOp();
    if (op1.equals(op2)) {
      if (FieldRef.prototype.includes(op1)) {
        // check that the fields are the same
        FieldRefNode f2 = (FieldRefNode) expr2;
        if (thisExprBinder.getBinding(expr1).equals(f2.resolveBinding().getNode())) {
          /* If the field is an instance field, check that the dereferenced
           * objects are the same.
           */
          if (TypeUtil.isStatic(thisExprBinder.getBinding(expr1))) {
            return true;
          } else {
            final IRNode fixed1 = thisExprBinder.bindThisExpression(FieldRef.getObject(expr1));
            final IRNode fixed2 = thisExprBinder.bindThisExpression(f2.getObject());
            if (fixed2 == null) {
              return checkSyntacticEquality(fixed1, f2.getObject(), thisExprBinder);
            }
            return checkSyntacticEquality(fixed1, fixed2, thisExprBinder);
          }
        } else { // fields are not equal
          return false;
        }
      } else if (ClassExpression.prototype.includes(op1)) {
        // Check that the two expressions are for the same class
        final IRNode class1 = thisExprBinder.getBinding(expr1);
        final IRNode class2 = ((ClassExpressionNode) expr2).resolveType().getNode();
        return class1.equals(class2);
      } else if (VariableUseExpression.prototype.includes(op1)) {
        VariableUseExpressionNode v2 = (VariableUseExpressionNode) expr2;
        return thisExprBinder.getBinding(expr1).equals(v2.resolveBinding().getNode());
      } else if (NamedType.prototype.includes(op1)) {
        IJavaType t1 = thisExprBinder.getJavaType(expr1);
        NamedTypeNode nt2 = (NamedTypeNode) expr2;
        return t1.equals(nt2.resolveType().getJavaType());
      } else if (MethodCall.prototype.includes(op1)) {
        /* Objects must equivalent and must be the same method and must be a
         * no-arg method. This simplifies checking because we currently only
         * care about this case for handling the readLock() and writeLock()
         * methods of ReadWriteLock.
         */
        
        /*
        final MethodCall call1 = (MethodCall) op1;
        final IRNode object1 = call1.get_Object(expr1);
        final IRNode object2 = call2.get_Object(expr2);
        // Are the receivers equivalent?
        if (checkSyntacticEquality(object1, object2, binderParam)) {
          final IRNode mdecl1 = binderParam.getBinding(expr1);
          final IRNode mdecl2 = binderParam.getBinding(expr2);
          // Are the methods the same?
          if (mdecl1 == mdecl2) {
            // Is the method a no-arg method?
            final IRNode params1 = MethodDeclaration.getParams(mdecl1);
            final Iterator<IRNode> iter1 = Parameters.getFormalIterator(params1);
            if (!iter1.hasNext()) return true;
          }
        }
                return false;
        */
        throw new UnsupportedOperationException("Need to handle Write/ReadLockNode");
      } else {
        throw new IllegalArgumentException(
            "Cannot check syntactic equality of " + op1.name());
      }
    } else {
      return false;
    }
  }  
  
  
  
  static boolean checkFieldRef(final ThisExpressionBinder thisExprBinder,
      final IRNode obj1, final IRNode varDecl1, final IRNode lockExpr2) {
    // Check field equality first because syntactic equality is more expensive
    if (FieldRef.prototype.includes(lockExpr2)) {
      return varDecl1.equals(thisExprBinder.getBinding(lockExpr2))
          && checkSyntacticEquality(obj1, thisExprBinder.bindThisExpression(FieldRef.getObject(lockExpr2)), thisExprBinder);
    } else {
      return false;
    }
  }

  static boolean checkFieldRef(final ThisExpressionBinder thisExprBinder,
      final IRNode obj1, final IRNode varDecl1, final ExpressionNode lockExpr2) {
    if (lockExpr2 instanceof FieldRefNode) {
      final FieldRefNode fieldRef = (FieldRefNode) lockExpr2;
      // Check field equality first because syntactic equality is more expensive
      return varDecl1.equals(fieldRef.resolveBinding().getNode())
          && checkSyntacticEquality(obj1, thisExprBinder.bindThisExpression(fieldRef.getObject()), thisExprBinder);
    } else {
      return false;
    }
  }  
}

