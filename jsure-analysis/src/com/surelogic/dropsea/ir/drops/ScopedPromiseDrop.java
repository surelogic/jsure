package com.surelogic.dropsea.ir.drops;

import com.surelogic.NonNull;
import com.surelogic.aast.promise.ScopedPromiseNode;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IScopedPromiseDrop;
import com.surelogic.dropsea.ir.PromiseDrop;

/**
 * Abstract promise drop class for scoped promises.
 * 
 * @subtypedBy edu.cmu.cs.fluid.sea.drops.promises.AssumePromiseDrop,
 *             edu.cmu.cs.fluid.sea.drops.promises.PromisePromiseDrop
 */
public abstract class ScopedPromiseDrop extends PromiseDrop<ScopedPromiseNode>
    implements IScopedPromiseDrop, IDerivedDropCreator<PromiseDrop<ScopedPromiseNode>> {

  public ScopedPromiseDrop(ScopedPromiseNode a) {
    super(a);
  }

  /**
   * Scoped promises are not checked by analysis, but we don't want the user
   * interface to show them as trusted, so we lie to it.
   * 
   * @see com.surelogic.dropsea.ir.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    return true;
  }

  @Override
  public void validated(PromiseDrop<ScopedPromiseNode> pd) {
    throw new UnsupportedOperationException();
  }

  @NonNull
  @Override
  public final DropType getDropType() {
    return DropType.SCOPED_PROMISE;
  }
}