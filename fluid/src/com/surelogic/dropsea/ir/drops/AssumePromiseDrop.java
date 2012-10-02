package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.AssumeScopedPromiseNode;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "assume" scoped promises.
 */
public final class AssumePromiseDrop extends ScopedPromiseDrop {

  public AssumePromiseDrop(AssumeScopedPromiseNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.SCOPED_PROMISE_CAT);
  }
}
