package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ImmutableNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "NotThreadSafe" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ImmutablePromiseDrop extends ModifiedBooleanPromiseDrop<ImmutableNode> implements
    ValidatedDropCallback<ImmutablePromiseDrop> {

  public ImmutablePromiseDrop(ImmutableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = XUtil.useExperimental() ? JavaNames.getRelativeTypeName(getNode()) : JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAAST().isImplementationOnly();
    final boolean isVerify = getAAST().verify();
    if (isVerify) {
      if (!isImplementationOnly) { // default case
        setMessage(Messages.LockAnnotation_immutableDrop, name);
      } else {
        setMessage(Messages.LockAnnotation_immutable_implOnly, name);
      }
    } else {
      if (isImplementationOnly) {
        setMessage(Messages.LockAnnotation_immutable_implOnly_noVerify, name);
      } else {
        setMessage(Messages.LockAnnotation_immutable_noVerify, name);
      }
    }
  }

  public void validated(final ImmutablePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}