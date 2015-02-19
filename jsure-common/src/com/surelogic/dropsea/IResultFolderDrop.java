package com.surelogic.dropsea;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;

/**
 * The interface for folders that contain consistent/inconsistent judgment drops
 * reported by verifying analyses within the sea, intended to allow multiple
 * implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 * <p>
 * It is intended that messages for folder drops, what is returned from
 * {@link #getMessage()}, may assume that
 * {@link I18N#toStringForUIFolderLabel(String, int)} is called prior to their
 * display. This allows the same special processing of messages that occurs on
 * categories to occur on these folders.
 */
public interface IResultFolderDrop extends IAnalysisResultDrop {

  enum LogicOperator {

    AND, OR

  }

  /**
   * Gets if this folder applies conjunction ({@link LogicOperator#AND}) or
   * disjunction ({@link LogicOperator#OR}) in the model-code consistency proof.
   * 
   * @return the type of logic used by this folder in the model-code consistency
   *         proof.
   */
  @NonNull
  LogicOperator getLogicOperator();
}
