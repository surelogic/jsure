package com.surelogic.analysis.locks.locks;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

// Only needed in the analysis package, no need to be public
abstract class AbstractILock implements ILock {
  /**
   * The lock drop for {@link #lockDecl}.
   */
  protected final LockModel lockPromise;
  
  /**
   * Whether write privileges are needed or not.
   */
  protected final boolean isWrite;

  /**
   * Whether the lock is a JUC ReadWriteLock.
   */
  protected final boolean isRW;
  
  
  
  
  /**
   * Create a new lock object.
   * 
   * @param ld
   *          The lock declaration node of the lock in question
   * @param src
   *          The node that is referring to the lock. See the class description.
   */
  AbstractILock(
      final LockModel lm, final boolean write, final boolean rw) {
    isWrite = write;
    isRW = rw;
    lockPromise = lm;
  }
  
  /**
   * Get the lock declaration node.
   */
  public final AbstractLockDeclarationNode getLockDecl() {
    return lockPromise.getAST();
  }
  
  /**
   * @return Returns the promise drop associated with the lock.
   */
  public final LockModel getLockPromise() {
    return lockPromise;
  }

  /**
   * Get the name of the lock.
   */
  public final String getName() {
    return getLockDecl().getId();
  }

  public final boolean isWrite() {
    return isWrite;
  }
  
  /**
   * Used to implement hashCode() and equals()
   */
  public Object getUniqueIdentifier() {
    return lockPromise;
  }
  
  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object o);

  protected final boolean baseEquals(final AbstractILock other) {
    return getUniqueIdentifier().equals(other.getUniqueIdentifier())
        && (isRW == other.isRW) && (isWrite == other.isWrite);
  }
  
  @Override
  public final int hashCode() {
    final int baseHashCode = getUniqueIdentifier().hashCode();
    // Bug 1010: Need to differentiate read lock from write lock
    return (isRW && isWrite) ? ~baseHashCode : baseHashCode; 
  }
}

