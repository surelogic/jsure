package com.surelogic.dropsea.ir.drops.locks;

import com.surelogic.aast.promise.ThreadConfinedNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "ThreadConfined" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ThreadConfinedPromiseDrop extends BooleanPromiseDrop<ThreadConfinedNode> {

  public ThreadConfinedPromiseDrop(ThreadConfinedNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
}