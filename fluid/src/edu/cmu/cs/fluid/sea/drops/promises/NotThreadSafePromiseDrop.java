package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.NotThreadSafeNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "NotThreadSafe" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class NotThreadSafePromiseDrop extends BooleanPromiseDrop<NotThreadSafeNode> {

  public NotThreadSafePromiseDrop(NotThreadSafeNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = JavaNames.getTypeName(getNode());
    setResultMessage(Messages.LockAnnotation_notThreadSafeDrop, name);
  }
}