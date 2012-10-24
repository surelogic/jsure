package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ContainableNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

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
  }

  public boolean allowReferenceObject() {
    return getAAST().allowReferenceObject();
  }
  
  public void validated(final ContainablePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}