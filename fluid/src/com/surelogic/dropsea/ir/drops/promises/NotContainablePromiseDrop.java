package com.surelogic.dropsea.ir.drops.promises;

import com.surelogic.aast.promise.NotContainableNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

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
    final String name = JavaNames.getTypeName(getNode());
    setMessage(Messages.LockAnnotation_notContainableDrop, name);
  }
}