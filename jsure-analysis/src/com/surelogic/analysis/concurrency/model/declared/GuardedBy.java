package com.surelogic.analysis.concurrency.model.declared;

import com.surelogic.analysis.concurrency.model.implementation.UnnamedLockImplementation;
import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * A lock from a GuardedBy field annotation.
 *
 */
public final class GuardedBy
extends AbstractModelLock<GuardedByPromiseDrop, UnnamedLockImplementation>
implements StateLock<GuardedByPromiseDrop, UnnamedLockImplementation>{
  // Derived from the annotation so does not participate in hash code or equality
  private final IRegion protectedRegion;
  
  public GuardedBy(
      final GuardedByPromiseDrop sourceDrop,
      final UnnamedLockImplementation lockImpl) {
    super(sourceDrop, lockImpl, VisitUtil.getEnclosingType(sourceDrop.getNode()));
    protectedRegion = new FieldRegion(sourceDrop.getNode());
  }
  
  @Override
  public IRegion getRegion() {
    return protectedRegion;
  }
  
  @Override
  public final boolean protects(final IRegion region) {
    return (!lockImpl.isFinalProtected() && region.isFinal()) ? false : protectedRegion.ancestorOf(region);
  }

  @Override
  public int hashCode() {
    return super.partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof GuardedBy) {
      return super.partialEquals((GuardedBy) other);
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "@GuardedBy(" + lockImpl + ") on field " + 
        VariableDeclarator.getId(sourceDrop.getNode()) + " of class " +
        JavaNames.getQualifiedTypeName(declaredInClass.getDeclaration());

  }
}
