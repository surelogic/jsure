/* Created on Mar 30, 2005
 */
package com.surelogic.analysis.concurrency.heldlocks;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

/**
 * This abstract class contains all the information that is available in a 
 * both a LockDeclaration and a PolicyLockDeclaration node, but in a more
 * convenient form. In particular, the lock implementation is already bound.
 */
abstract class AbstractLockRecord {
  /** The declared type for which the lock is declared. */
  public final IJavaDeclaredType classDecl;
  
  /** The lock declaration node.  Either a LockDeclaration or a PolicyLockDeclaration node. */
  public final LockModel lockDecl;
  
  /** The name of the lock */
  public final String name;
  
  /**
   * The lock's implementation:
   * <ul>
   * <li>{@link GlobalLockModel#THIS} if the lock is represented by the
   * object itself;
   * <li>The class declaration node of the annotated class if the lock is
   * represented by the class object.
   * <li>A VariableDeclarator node if the lock is a field.
   */
  public final IRNode lockImpl;

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
  public AbstractLockRecord(
      final IBinder binder, final IJavaDeclaredType cd, final LockModel ld) throws GlobalLockModel.UnsupportedLockException {
    lockDecl = ld;
    classDecl = cd;
    name = ld.getAST().getId();
    lockImpl = GlobalLockModel.canonicalizeLockImpl(binder, ld.getAST().getField());
  }
  
  /**
   * Get the lock implementation is a string.
   */
  public String getLockImplAsString() {
    if (lockImpl.equals(GlobalLockModel.THIS)) {
      return "this";
    } else if (ClassDeclaration.prototype.includes(JJNode.tree.getOperator(lockImpl))) {
      return ".class";
    } else {
      return "." + VariableDeclarator.getId(lockImpl);
    }
  }

  /**
   * Force subclasses to implement this method.
   */
  @Override
  public abstract String toString();
}
