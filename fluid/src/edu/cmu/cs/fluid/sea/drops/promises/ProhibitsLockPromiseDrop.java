package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ProhibitsLockNode;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "ProhibitsLock" promises.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ProhibitsLockPromiseDrop extends AbstractLockListPromiseDrop<ProhibitsLockNode> {
  public ProhibitsLockPromiseDrop(ProhibitsLockNode n) {
    super(n);
    setCategory(JavaGlobals.LOCK_REQUIRESLOCK_CAT);
  }
}