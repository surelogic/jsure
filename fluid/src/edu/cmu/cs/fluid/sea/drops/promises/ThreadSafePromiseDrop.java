package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ThreadSafeNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.*;

/**
 * Promise drop for "ThreadSafe" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ThreadSafePromiseDrop extends
    ModifiedBooleanPromiseDrop<ThreadSafeNode> implements
    ValidatedDropCallback<ThreadSafePromiseDrop> {
  public ThreadSafePromiseDrop(ThreadSafeNode a) {
    super(a); 
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    final String name = JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAAST().isImplementationOnly();
    final boolean isVerify = getAAST().verify();
    if (isVerify) {
      if (!isImplementationOnly) { // default case
        setResultMessage(Messages.LockAnnotation_threadSafeDrop, name);
      } else {
        setResultMessage(Messages.LockAnnotation_threadSafe_implOnly, name);
      }
    } else {
      if (isImplementationOnly) {
        setResultMessage(Messages.LockAnnotation_threadSafe_implOnly_noVerify, name);
      } else {
        setResultMessage(Messages.LockAnnotation_threadSafe_noVerify, name);
      }
    }
  }
  
  public void validated(final ThreadSafePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}