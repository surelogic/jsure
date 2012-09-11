package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.RequiresLockNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.UiPlaceInASubFolder;

/**
 * Promise drop for "requiresLock" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class RequiresLockPromiseDrop extends AbstractLockListPromiseDrop<RequiresLockNode> implements UiPlaceInASubFolder {
  public RequiresLockPromiseDrop(RequiresLockNode n) {
    super(n);
    setCategory(JavaGlobals.LOCK_REQUIRESLOCK_CAT);
  }
}