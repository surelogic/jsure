package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ContainableNode;
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
public final class ContainablePromiseDrop extends ModifiedBooleanPromiseDrop<ContainableNode> implements
    ValidatedDropCallback<ContainablePromiseDrop> {

  public ContainablePromiseDrop(ContainableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = XUtil.useExperimental() ? JavaNames.getRelativeTypeName(getNode()) : JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAAST().isImplementationOnly();
    final boolean isVerify = getAAST().verify();
    if (isVerify) {
      if (!isImplementationOnly) { // default case
        setMessage(Messages.LockAnnotation_containableDrop, name);
      } else if (!XUtil.useExperimental()) {
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