package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;

final class AASTHeldInstanceLock extends HeldInstanceLock {
  final ExpressionNode objAAST;
  
  AASTHeldInstanceLock(
      final ExpressionNode o2, final LockModel lm, final IRNode src,
      final PromiseDrop<?> sd, final boolean assumed, final Type type) {
    super(lm, src, sd, assumed, type);
    objAAST = o2;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof AASTHeldInstanceLock) {
      final AASTHeldInstanceLock hil = (AASTHeldInstanceLock) o;
      return baseEquals(hil) && objAAST.equals(hil.objAAST);
    } else {
      return false;
    }
  }
  
  @Override
  public HeldLock changeSource(final IRNode newSrc) {
    return new AASTHeldInstanceLock(objAAST, lockPromise, newSrc, supportingDrop, isAssumed, type);
  }

  @Override
  boolean mustAliasLockExpr(
      final HeldInstanceLock lock, final ThisExpressionBinder teb) {
    return lock.mustAliasAAST(this, teb);
  }
  
  @Override
  boolean mustAliasAAST(
      final AASTHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkSyntacticEquality(objAAST, lock.objAAST, teb);
  }
  
  @Override
  boolean mustAliasIR(
      final IRHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkSyntacticEquality(lock.obj, objAAST, teb);
  }
  
  @Override
  boolean mustAliasFieldRef(
      final HeldFieldRefLock lock, final ThisExpressionBinder teb) {
    return checkFieldRef(teb, lock.obj, lock.varDecl, objAAST);
  }

  @Override
  boolean mustSatisfyLockExpr(
      final AbstractNeededInstanceLock lock, final ThisExpressionBinder teb) {
    return lock.satisfiesAAST(this, teb);
  }
  
  @Override
  protected String objToString() {
    return objAAST.toString();
  }
}
