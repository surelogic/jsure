/* Created on Mar 30, 2005
 */
package com.surelogic.analysis.concurrency.heldlocks;

import java.text.MessageFormat;

import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.LockDeclarationNode;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.sea.drops.promises.*;

/**
 * This class contains all the information that is available in a 
 * LockDeclaration node, but in a more convenient form.  In particular,
 * the lock implementation is already bound, and the region is already bound
 * and wrapped into a {@link Region} object.
 */
public final class RegionLockRecord extends AbstractLockRecord {
  /** The region protected by the lock */
  public final RegionModel region;

  /**
   * Create a new lock record for the given lock declaration associated with
   * the given class.
   * 
   * @param binder
   *          The binder to use.
   * @param cd
   *          The class declaration that is annotated.
   * @param ld
   *          The lock declaration.
   */
  public RegionLockRecord(
      final IBinder binder, final IJavaDeclaredType cd, final LockModel ld) throws GlobalLockModel.UnsupportedLockException {
    super(binder, cd, ld);
    LockDeclarationNode lock = (LockDeclarationNode) ld.getAST(); 
    if (lock != null) {
      final IRegionBinding b = lock.getRegion().resolveBinding();
      if (b == null) {
    	  lock.getRegion().resolveBinding();
      }
      region = b.getModel();
    } else {
      region = RegionModel.getInstanceRegion(ld.getNode());
    }
  }

  /**
   * Get the name of the protected region.
   */
  public String getRegionAsString() {
    return region.toString();
  }

  @Override
  public String toString() {
    return MessageFormat.format(
      "Lock {0} is {1} protects {2} (declared in class {3})",
      new Object[] {
        name,
        getLockImplAsString(),
        getRegionAsString(),
        ClassDeclaration.getId(lockDecl.getNode())});
  }
}
