package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ImmutableNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

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
  }

  @Override
  public void validated(final ImmutablePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}