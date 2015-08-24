package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.java.operator.ClassDeclaration;

public final class RegionLock
extends AbstractNamedLock<LockModel>
implements StateLock<LockModel, NamedLockImplementation> {
  // Derived from the lock annotation, so does not participate in hash code or equality
  private final IRegion protectedRegion;
  
  public RegionLock(
      final LockModel lockModel, final NamedLockImplementation lockImpl) {
    super(lockModel, lockImpl);
    final LockDeclarationNode lock = (LockDeclarationNode) lockModel.getAAST();
    protectedRegion = lock.getRegion().resolveBinding().getModel();
  }
  
  @Override
  public IRegion getRegion() {
    return protectedRegion;
  }
  
  @Override
  public boolean protects(final IRegion region) {
    return protectedRegion.ancestorOf(region);
  }
  
  @Override
  public int hashCode() {
    return super.partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof RegionLock) {
      return super.partialEquals((RegionLock) other);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "@RegionLock(" + lockImpl + " protects " + 
        protectedRegion.getName() + ") on class " +
        ClassDeclaration.getId(declaredInClass.getDeclaration());
  }
}
