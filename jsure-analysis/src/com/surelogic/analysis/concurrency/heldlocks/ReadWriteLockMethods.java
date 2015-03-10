/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/ReadWriteLockMethods.java,v 1.4 2007/09/11 20:32:40 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks;

import edu.cmu.cs.fluid.CommonStrings;

/**
 * Enumeration that describes the different methods in 
 * java.util.concurrent.locks.ReadWRiteLock, plus a value indicating "none of the above".
 */
public enum ReadWriteLockMethods {
  READLOCK(CommonStrings.intern("readLock")),
  WRITELOCK(CommonStrings.intern("writeLock")),
  NOT_A_READWRITELOCK_METHOD(CommonStrings.intern("not a method")),
  IDENTICALLY_NAMED_METHOD(CommonStrings.intern("fake"));
  
  public final String name;
  
  private ReadWriteLockMethods(final String name) {
    this.name = name;
  }



  
  public static ReadWriteLockMethods whichReadWriteLockMethod(final String mname) {
    final String internedName = CommonStrings.intern(mname);
    if (internedName == ReadWriteLockMethods.READLOCK.name) {
      return ReadWriteLockMethods.READLOCK;
    } else if (internedName == ReadWriteLockMethods.WRITELOCK.name) {
      return ReadWriteLockMethods.WRITELOCK;
    } else {
      return ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD;
    }
  }
}
