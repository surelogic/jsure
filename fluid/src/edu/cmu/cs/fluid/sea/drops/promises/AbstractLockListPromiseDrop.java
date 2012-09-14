package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.AbstractLockListNode;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public abstract class AbstractLockListPromiseDrop<N extends AbstractLockListNode> extends PromiseDrop<N> {

  AbstractLockListPromiseDrop(N node) {
    super(node);
    setMessage(Messages.LockAnnotation_requiresLockDrop, getAAST().toString(), JavaNames.genMethodConstructorName(getNode()));
  }

  /**
   * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    return true;
  }
}
