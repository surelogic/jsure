package com.surelogic.dropsea.ir.drops.promises;

import com.surelogic.aast.promise.AbstractLockListNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

public abstract class AbstractLockListPromiseDrop<N extends AbstractLockListNode> extends PromiseDrop<N> {

  AbstractLockListPromiseDrop(N node) {
    super(node);
    setMessage(Messages.LockAnnotation_requiresLockDrop, getAAST().toString(), JavaNames.genMethodConstructorName(getNode()));
  }

  /**
   * @see com.surelogic.dropsea.ir.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    return true;
  }
}
