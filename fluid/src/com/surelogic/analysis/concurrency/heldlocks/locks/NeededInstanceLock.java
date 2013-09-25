package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser; 
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;

final class NeededInstanceLock extends AbstractNeededInstanceLock {
  NeededInstanceLock(final IRNode o, final LockModel lm, final Type type) {
    super(o, lm, type);
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof NeededInstanceLock) {
      final NeededInstanceLock other = (NeededInstanceLock) o;
      return baseEquals(other) && obj.equals(other.obj);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('<');
    sb.append(DebugUnparser.toString(obj));
    sb.append(">:");
    sb.append(getName());
    sb.append(type.getPostFix());
    return sb.toString();
  }

  @Override
  boolean satisfiesAAST(
      final AASTHeldInstanceLock lock, final ThisExpressionBinder teb, final IBinder b) {
    return checkSyntacticEquality(obj, lock.objAAST, teb, b);
  }

  @Override
  boolean satisfiesIR(
      final IRHeldInstanceLock lock, final ThisExpressionBinder teb, final IBinder b) {
    return checkSyntacticEquality(obj, lock.obj, teb, b);
  }

  @Override
  boolean satisfiesFieldRef(
      final HeldFieldRefLock lock, final ThisExpressionBinder teb, final IBinder b) {
    return checkFieldRef(teb, b, lock.obj, lock.varDecl, obj);
  }
  
  @Override
  public boolean mayHaveAliasInCallingContext() {
    return QualifiedReceiverDeclaration.prototype.includes(obj);
  }
  
  @Override
  public NeededLock getAliasInCallingContext(
      final EnclosingRefs enclosingRefs, final NeededLockFactory lockFactory) {
    final IRNode newObj = enclosingRefs.replace(obj);
    if (newObj != null) {
      return lockFactory.createInstanceLock(newObj, lockPromise, type);
    } else {
      return null;
    }
  }
}
