package com.surelogic.dropsea.ir.drops.locks;

import com.surelogic.aast.promise.AbstractLockListNode;
import com.surelogic.dropsea.ir.PromiseDrop;

public abstract class AbstractLockListPromiseDrop<N extends AbstractLockListNode> extends PromiseDrop<N> {

  AbstractLockListPromiseDrop(N node) {
    super(node);
  }

  /**
   * @see com.surelogic.dropsea.ir.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    return true;
  }
}
