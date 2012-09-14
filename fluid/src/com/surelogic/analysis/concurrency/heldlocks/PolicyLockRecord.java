/* Created on Mar 30, 2005
 */
package com.surelogic.analysis.concurrency.heldlocks;

import java.text.MessageFormat;

import com.surelogic.dropsea.ir.drops.promises.LockModel;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;

/**
 * This class contains all the information that is available in a 
 * PolicyLockDeclaration node, but in a more convenient form.  In particular,
 * the lock implementation is already bound, and the region is already bound
 * and wrapped into a {@link Region} object.
 */
final class PolicyLockRecord extends AbstractLockRecord {
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
  public PolicyLockRecord(
      final IBinder binder, final IJavaDeclaredType cd, final LockModel ld) throws GlobalLockModel.UnsupportedLockException {
    super(binder, cd, ld);
  }

  @Override
  public String toString() {
    return MessageFormat.format(
      "Policy Lock {0} is {1} (declared in class {2})",
      new Object[] {
        name,
        getLockImplAsString(),
        ClassDeclaration.getId(lockDecl.getNode())});
  }
}
