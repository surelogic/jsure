package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ScopedPromiseNode;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "promise" scoped promises.
 */
public final class PromisePromiseDrop extends ScopedPromiseDrop {

  public PromisePromiseDrop(ScopedPromiseNode a) {
    super(a);
    setCategory(JavaGlobals.PROMISE_CAT);
    setMessage("Promise " + getAAST());
  }
}