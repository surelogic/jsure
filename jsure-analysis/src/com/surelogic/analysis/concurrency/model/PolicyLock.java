package com.surelogic.analysis.concurrency.model;

import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.java.operator.ClassDeclaration;

public final class PolicyLock extends AbstractNamedLock<LockModel>{
  public PolicyLock(
      final LockModel lockModel, final NamedLockImplementation lockImpl) {
    super(lockModel, lockImpl);
  }
  
  @Override
  public int hashCode() {
    return super.partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof PolicyLock) {
      return super.partialEquals((PolicyLock) other);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "@PolicyLock(" + lockImpl + ") on class " +
        ClassDeclaration.getId(declaredInClass.getDeclaration());
  }
}
