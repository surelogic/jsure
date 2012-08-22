package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "NotContainable" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class NotContainablePromiseDrop extends BooleanPromiseDrop<NotContainableNode> {
  public NotContainablePromiseDrop(NotContainableNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    String name = JavaNames.getTypeName(getNode());
    setResultMessage(Messages.LockAnnotation_notContainableDrop, name);
  }
}