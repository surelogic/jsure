package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ThreadSafeNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "ThreadSafe" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ThreadSafePromiseDrop extends ModifiedBooleanPromiseDrop<ThreadSafeNode> implements
    ValidatedDropCallback<ThreadSafePromiseDrop> {
  public ThreadSafePromiseDrop(ThreadSafeNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }

  @Override
  public void validated(final ThreadSafePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}