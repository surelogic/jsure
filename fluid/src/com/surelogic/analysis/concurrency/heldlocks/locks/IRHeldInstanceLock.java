package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;

final class IRHeldInstanceLock extends HeldInstanceLock {
  /**
   * The object-valued expression that this lock is associated with.
   * This is a VariableDeclarator in the case that the lock is "field:lock",
   * in which case it symbolizes a reference to that field via the receiver.
   */
  final IRNode obj;
  
  IRHeldInstanceLock(
      final IRNode o, final LockModel lm, final IRNode src,
      final PromiseDrop<?> sd, final boolean assumed, final Type type) {
    super(lm, src, sd, assumed, type);
    if (o == null) {
      throw new NullPointerException("obj is null");
    }
    obj = o;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof IRHeldInstanceLock) {
      final IRHeldInstanceLock hil = (IRHeldInstanceLock) o;
      return baseEquals(hil) && obj.equals(hil.obj);
    } else {
      return false;
    }
  }

  @Override
  public HeldLock changeSource(final IRNode newSrc) {
    return new IRHeldInstanceLock(obj, lockPromise, newSrc, supportingDrop, isAssumed, type);
  }

  @Override
  boolean mustAliasLockExpr(
      final HeldInstanceLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    return lock.mustAliasIR(this, teb, binder);
  }
  
  @Override
  boolean mustAliasAAST(
      final AASTHeldInstanceLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    return checkSyntacticEquality(obj, lock.objAAST, teb, binder);
  }
  
  @Override
  boolean mustAliasIR(
      final IRHeldInstanceLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    return checkSyntacticEquality(obj, lock.obj, teb, binder);
  }
  
  @Override
  boolean mustAliasFieldRef(
      final HeldFieldRefLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    return checkFieldRef(teb, binder, lock.obj, lock.varDecl, obj);
  }

  @Override
  boolean mustSatisfyLockExpr(
      final AbstractNeededInstanceLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    return lock.satisfiesIR(this, teb, binder);
  }

  @Override
  protected String objToString() {
    return DebugUnparser.toString(obj);
  }

//  @Override
//  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, IRNode other) {
//    return checkSyntacticEquality(this.obj, other, teb, b);
//  }
//
//  @Override
//  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, ExpressionNode other) {
//    return checkSyntacticEquality(this.obj, other, teb, b);
//  }
//
//  @Override
//  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, HeldInstanceLock other) {
//    return other.checkSyntacticEquality(teb, b, this.obj);
//  }

//  @Override
//  protected boolean testFieldSpecialCase(NeededLock lock, ThisExpressionBinder teb, IBinder b) {
//    if (VariableDeclarator.prototype.includes(obj)) {
//      // We have the special case, check to see if the needed lock is "this.f"
//      return lock.isFieldExprOfThis(b, obj);
//    }
//    return false;
//  }
//  
//  @Override
//  protected IRNode getFieldOfThis(final IBinder b) {
//    final Operator op = JJNode.tree.getOperator(obj);
//    if (VariableDeclarator.prototype.includes(op)) {
//      return obj;
//    }
//    if (FieldRef.prototype.includes(op)) {
//      if (ThisExpression.prototype.includes(FieldRef.getObject(obj))) {
//        return b.getBinding(obj);
//      }
//    }
//    return null;
//  }
//  
//  @Override
//  protected boolean isFieldExprOfThis(IBinder b, IRNode varDecl) {
//    if (obj.equals(varDecl)) {
//      return true;
//    }
//    if (FieldRef.prototype.includes(obj)) {
//      if (ThisExpression.prototype.includes(FieldRef.getObject(obj))) {
//        return b.getBinding(obj).equals(varDecl);
//      }
//    }
//    return false;
//  }
}
