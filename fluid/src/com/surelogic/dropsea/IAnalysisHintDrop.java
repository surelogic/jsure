package com.surelogic.dropsea;

import com.surelogic.NonNull;

/**
 * The interface for the base class for all suggestion and warning drops
 * reported by analyses within the sea, intended to allow multiple
 * implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IAnalysisHintDrop extends IAnalysisOutputDrop {

  enum HintType {

    SUGGESTION, WARNING

  }

  /**
   * Gets the level of this information drop.
   */
  @NonNull
  HintType getLevel();
}
