package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ThreadSafeNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

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
    if (!XUtil.useExperimental()) {
    final String name = XUtil.useExperimental() ? JavaNames.getRelativeTypeName(getNode()) : JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAAST().isImplementationOnly();
    final boolean isVerify = getAAST().verify();
    if (isVerify) {
        if (!XUtil.useExperimental()) {
      if (!isImplementationOnly) { // default case
        setMessage(Messages.LockAnnotation_threadSafeDrop, name);
      } else  {
        setMessage(Messages.LockAnnotation_threadSafe_implOnly, name);
      }
        }
    } else {
      if (isImplementationOnly) {
        setMessage(Messages.LockAnnotation_threadSafe_implOnly_noVerify, name);
      } else {
        setMessage(Messages.LockAnnotation_threadSafe_noVerify, name);
      }
    }
    }
  }

  public void validated(final ThreadSafePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}