package com.surelogic.dropsea;

import com.surelogic.NonNull;

/**
 * The interface for the base class for all information drops within the sea,
 * intended to allow multiple implementations. The analysis uses the IR drop-sea
 * and the Eclipse client loads snapshots using a IR-free drop-sea.
 */
public interface IInfoDrop extends IDrop {

  /**
   * Gets the level of this information drop.
   */
  @NonNull
  InfoDropLevel getLevel();
}
