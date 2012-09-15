package com.surelogic.dropsea.ir.drops.locks;

import com.surelogic.aast.promise.ProhibitsLockNode;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "ProhibitsLock" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ProhibitsLockPromiseDrop extends AbstractLockListPromiseDrop<ProhibitsLockNode> {

  public ProhibitsLockPromiseDrop(ProhibitsLockNode n) {
    super(n);
    setCategory(JavaGlobals.LOCK_REQUIRESLOCK_CAT);
  }
}