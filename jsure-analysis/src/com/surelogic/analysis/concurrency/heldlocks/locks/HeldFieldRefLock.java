package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

final class HeldFieldRefLock extends HeldInstanceLock {
  /**
   * The object-valued expression that is the receiver for the lock
   * expression.
   */
  final IRNode obj;
  
  /**
   * VariableDeclarator of the field that is being referenced.
   */
  final IRNode varDecl;

  
  
  HeldFieldRefLock(
      final IRNode o, final IRNode vd, final LockModel lm, final IRNode src,
      final PromiseDrop<?> sd, final boolean assumed, final Type type) {
    super(lm, src, sd, assumed, type);
    if (o == null) {
      throw new NullPointerException("obj is null");
    }
    obj = o;
    varDecl = vd;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof HeldFieldRefLock) {
      final HeldFieldRefLock hil = (HeldFieldRefLock) o;
      return baseEquals(hil) && obj.equals(hil.obj) && varDecl.equals(hil.varDecl);
    } else {
      return false;
    }
  }

  @Override
  public HeldLock changeSource(final IRNode newSrc) {
    return new HeldFieldRefLock(obj, varDecl, lockPromise, newSrc, supportingDrop, isAssumed, type);
  }

  @Override
  boolean mustAliasLockExpr(
      final HeldInstanceLock lock, final ThisExpressionBinder teb) {
    return lock.mustAliasFieldRef(this, teb);
  }
  
  @Override
  boolean mustAliasAAST(
      final AASTHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkFieldRef(teb, obj, varDecl, lock.objAAST);
  }
  
  @Override
  boolean mustAliasIR(
      final IRHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkFieldRef(teb, obj, varDecl, lock.obj);
  }
  
  @Override
  boolean mustAliasFieldRef(
      final HeldFieldRefLock lock, final ThisExpressionBinder teb) {
    return varDecl.equals(lock.varDecl)
        && checkSyntacticEquality(obj, lock.obj, teb);
  }

  @Override
  boolean mustSatisfyLockExpr(
      final AbstractNeededInstanceLock lock, final ThisExpressionBinder teb) {
    return lock.satisfiesFieldRef(this, teb);
  }
  
  @Override
  protected String objToString() {
    return DebugUnparser.toString(obj) + "." + VariableDeclarator.getId(varDecl);
  }
}
