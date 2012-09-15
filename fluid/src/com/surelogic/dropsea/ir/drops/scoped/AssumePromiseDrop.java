package com.surelogic.dropsea.ir.drops.scoped;

import com.surelogic.aast.promise.AssumeScopedPromiseNode;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "assume" scoped promises.
 */
public final class AssumePromiseDrop extends ScopedPromiseDrop {

  public AssumePromiseDrop(AssumeScopedPromiseNode a) {
    super(a);
    setCategory(JavaGlobals.PROMISE_CAT);
    setMessage(13, getAAST());
  }
}