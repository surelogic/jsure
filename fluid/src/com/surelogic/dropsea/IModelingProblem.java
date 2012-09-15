package com.surelogic.dropsea;

/**
 * The interface for the base class for all modeling problem drops reported by
 * promise scrubbing within the sea, intended to allow multiple implementations.
 * The analysis uses the IR drop-sea and the Eclipse client loads snapshots
 * using a IR-free drop-sea.
 */
public interface IModelingProblem extends IDrop {
  // marker interface
}
