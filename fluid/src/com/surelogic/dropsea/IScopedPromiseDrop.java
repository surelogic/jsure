package com.surelogic.dropsea;

import com.surelogic.Assume;
import com.surelogic.Promise;

/**
 * The interface for all scoped promise drops with the sea, intended to allow
 * multiple implementations. Today there are two scoped promises: {@link Assume}
 * and {@link Promise}. The UI needs to know, in may instances, which promise
 * drops are proposed promises.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IScopedPromiseDrop extends IPromiseDrop {
  // marker interface
}
