package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ContainableNode;
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
public final class ContainablePromiseDrop extends ModifiedBooleanPromiseDrop<ContainableNode> implements
    ValidatedDropCallback<ContainablePromiseDrop> {

  public ContainablePromiseDrop(ContainableNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAAST().isImplementationOnly();
    final boolean isVerify = getAAST().verify();
    if (isVerify) {
      if (!isImplementationOnly) { // default case
        setMessage(Messages.LockAnnotation_containableDrop, name);
      } else {
        setMessage(Messages.LockAnnotation_containable_implOnly, name);
      }
    } else {
      if (isImplementationOnly) {
        setMessage(Messages.LockAnnotation_containable_implOnly_noVerify, name);
      } else {
        setMessage(Messages.LockAnnotation_containable_noVerify, name);
      }
    }
  }

  public void validated(final ContainablePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}