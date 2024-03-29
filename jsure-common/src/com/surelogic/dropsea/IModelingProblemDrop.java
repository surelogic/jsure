package com.surelogic.dropsea;

/**
 * The interface for all modeling problem drops reported by promise scrubbing
 * within the sea, intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IModelingProblemDrop extends IDrop, ISnapshotDrop {
  // a marker interface.
}
