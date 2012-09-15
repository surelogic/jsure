package com.surelogic.dropsea;

/**
 * The interface for the base class for all information drops within the sea,
 * intended to allow multiple implementations. The analysis uses the IR drop-sea
 * and the Eclipse client loads snapshots using a IR-free drop-sea.
 */
public interface IInfoDrop extends IDrop {
  // defined by super-interfaces
}
