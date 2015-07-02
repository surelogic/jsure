package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

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
      final HeldInstanceLock lock, final ThisExpressionBinder teb) {
    return lock.mustAliasIR(this, teb);
  }
  
  @Override
  boolean mustAliasAAST(
      final AASTHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkSyntacticEquality(obj, lock.objAAST, teb);
  }
  
  @Override
  boolean mustAliasIR(
      final IRHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkSyntacticEquality(obj, lock.obj, teb);
  }
  
  @Override
  boolean mustAliasFieldRef(
      final HeldFieldRefLock lock, final ThisExpressionBinder teb) {
    return checkFieldRef(teb, lock.obj, lock.varDecl, obj);
  }

  @Override
  boolean mustSatisfyLockExpr(
      final AbstractNeededInstanceLock lock, final ThisExpressionBinder teb) {
    return lock.satisfiesIR(this, teb);
  }

  @Override
  protected String objToString() {
    return DebugUnparser.toString(obj);
  }
}
