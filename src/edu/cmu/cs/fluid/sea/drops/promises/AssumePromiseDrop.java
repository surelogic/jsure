package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ScopedPromiseNode;

/**
 * Promise drop for "assume" scoped promises.
 */
public final class AssumePromiseDrop extends ScopedPromiseDrop {
  public AssumePromiseDrop(ScopedPromiseNode a) {
    super(a);
  }
}