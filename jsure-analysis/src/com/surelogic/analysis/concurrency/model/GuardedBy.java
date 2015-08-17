package com.surelogic.analysis.concurrency.model;

import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;

import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * A lock from a GuardedBy field annotation.
 *
 */
public class GuardedBy
extends AbstractModelLock<GuardedByPromiseDrop, UnnamedLockImplementation>
implements StateLock<GuardedByPromiseDrop, UnnamedLockImplementation>{
  // Derived from the annotation so does not participate in hash code or equality
  private final IRegion protectedRegion;
  
  protected GuardedBy(
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
        ClassDeclaration.getId(declaredInClass.getDeclaration());
  }
}
