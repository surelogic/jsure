/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/LockMethods.java,v 1.7 2007/09/11 20:32:40 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks;

import edu.cmu.cs.fluid.CommonStrings;

/**
 * Enumeration that describes the different Lock methods in 
 * java.util.concurrent.locks.Lock, plus a value indicating "none of the above",
 * and a value indicating a method with the same name as a Lock method, but that
 * is not from the Lock class.
 */
public enum LockMethods {
  LOCK(CommonStrings.intern("lock"), true, true),
  LOCK_INTERRUPTIBLY(CommonStrings.intern("lockInterruptibly"), true, true),
  TRY_LOCK(CommonStrings.intern("tryLock"), true, false),
  UNLOCK(CommonStrings.intern("unlock"), false, true),
  NOT_A_LOCK_METHOD(CommonStrings.intern("not a method"), false, true),
  IDENTICALLY_NAMED_METHOD(CommonStrings.intern("fake"), false, true); 

  
  
  /** The name of the method represented */
  public final String name;
  
  /**
   * Whether the represented method has the potential to acquire a lock.
   * @see isUnconditional
   */
  public final boolean isLock;

  /**
   * Whether the effects of the method are unconditional.  If <code>false</code>
   * it means the method may not acquire/release the lock, and returns a boolean
   * result indicating whether it did.
   */
  public final boolean isUnconditional;
  
  
  
  private LockMethods(final String name, final boolean isLock, final boolean isUnconditional) {
    this.name = name;
    this.isLock = isLock;
    this.isUnconditional = isUnconditional;
  }


  
  public static LockMethods whichLockMethod(final String mname) {
    final String internedName = CommonStrings.intern(mname);
    if (internedName == LockMethods.LOCK.name) {
      return LockMethods.LOCK;
    } else if (internedName == LockMethods.LOCK_INTERRUPTIBLY.name) {
      return LockMethods.LOCK_INTERRUPTIBLY;
    } else if (internedName == LockMethods.TRY_LOCK.name) {
      return LockMethods.TRY_LOCK;
    } else if (internedName == LockMethods.UNLOCK.name) {
      return LockMethods.UNLOCK;
    } else {
      return LockMethods.NOT_A_LOCK_METHOD;
    }
  }
}
