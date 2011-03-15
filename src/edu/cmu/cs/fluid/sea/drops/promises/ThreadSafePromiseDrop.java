package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ThreadSafeNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.*;

/**
 * Promise drop for "ThreadSafe" promises.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ThreadSafePromiseDrop extends ModifiedBooleanPromiseDrop<ThreadSafeNode> {
  public ThreadSafePromiseDrop(ThreadSafeNode a) {
    super(a); 
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    String name = JavaNames.getTypeName(getNode());
    setResultMessage(Messages.LockAnnotation_threadSafeDrop, name);
  }
}