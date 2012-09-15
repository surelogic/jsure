package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser; 
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;

final class NeededInstanceLock extends AbstractNeededLock {
  /**
	 * The object-valued expression that this lock is associated with.
	 */
  private final IRNode obj;

  NeededInstanceLock(final IRNode o, final LockModel lm, final Type type) {
    super(lm, type);
    obj = o;
  }
  
  /* Default visibility: only needs to be used by the implementation of
   * HeldInstanceLock.
   */ 
  IRNode getObject() {
    return obj;
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

  public boolean mayHaveAliasInCallingContext() {
    return QualifiedReceiverDeclaration.prototype.includes(obj);
  }
  
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
