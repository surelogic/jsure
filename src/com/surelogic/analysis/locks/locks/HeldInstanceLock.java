package com.surelogic.analysis.locks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

abstract class HeldInstanceLock extends AbstractHeldLock {
  HeldInstanceLock(
      final LockModel ld, final IRNode src, final PromiseDrop<?> sd,
      final boolean assumed, final boolean write, final boolean rw) {
    super(ld, src, sd, assumed, write, rw);
  }
  
  /**
	 * Here we cop out and use syntactic equality of final expressions instead
	 * of dealing with aliasing.
	 */
  public boolean mustAlias(
      final HeldLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    /* First check that the lock name are equal.
     */
    if (!(lock instanceof HeldInstanceLock)) {
      return false;
    } else {
      HeldInstanceLock hil = (HeldInstanceLock) lock;    
      if (getUniqueIdentifier().equals(hil.getUniqueIdentifier())) {
        if (checkSyntacticEquality(teb, binder, hil)) {
          return isWrite == lock.isWrite();
        }
      }
      return false;
    }
  }

  public boolean mustSatisfy(
      final NeededLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    /* First check that the lock name are equal.
     */
    if (!(lock instanceof NeededInstanceLock)) {
      return false;
    } else {
      if (getUniqueIdentifier().equals(lock.getUniqueIdentifier())) {
        if (checkSyntacticEquality(teb, binder, ((NeededInstanceLock) lock).getObject())) {
          return isWrite || (!isWrite && !lock.isWrite());
        }
      }
      return false;
    }
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof HeldInstanceLock) {
      final HeldInstanceLock hil = (HeldInstanceLock) o;
      return baseEquals(hil) && getObject().equals(hil.getObject());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('<');
    sb.append(objToString());
    sb.append(">:");
    sb.append(getName());
    if (isRW) {
      if (isWrite) {
        sb.append(".writeLock()");
      } else {
        sb.append(".readLock()");
      }
    }
    return sb.toString();
  }
  
  protected abstract String objToString();
  
  protected abstract Object getObject();
  
  protected abstract boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, IRNode other);
  
  protected abstract boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, ExpressionNode other);
  
  protected abstract boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, HeldInstanceLock other);
}
