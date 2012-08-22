package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

// Only needed in the analysis package, no need to be public
abstract class AbstractILock implements ILock {
  /**
   * The lock drop for {@link #lockDecl}.
   */
  protected final LockModel lockPromise;
  
  /**
   * Whether the lock is monolithic, read only, or write.
   */
  protected final Type type; 
  
  
  
  /**
   * Create a new lock object.
   */
  AbstractILock(final LockModel lm, final Type t) {
    lockPromise = lm;
    type = t;
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
	if (getLockDecl() == null) {
		return null;
	}
    return getLockDecl().getId();
  }

  public final boolean isWrite() {
    return type != Type.READ;
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
        && (type == other.type);
  }
  
  @Override
  public final int hashCode() {
    final int baseHashCode = getUniqueIdentifier().hashCode();
    // Bug 1010: Need to differentiate read lock from write lock
    return type == Type.WRITE ? ~baseHashCode : baseHashCode; 
  }
}

