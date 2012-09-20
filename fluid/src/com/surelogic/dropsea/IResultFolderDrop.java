package com.surelogic.dropsea;

import com.surelogic.NonNull;

/**
 * The interface for folders that contain consistent/inconsistent judgment drops
 * reported by verifying analyses within the sea, intended to allow multiple
 * implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IResultFolderDrop extends IAnalysisResultDrop {

  enum FolderLogic {

    AND, OR

  }

  /**
   * Gets if this folder applies conjunction ({@link FolderLogic#AND}) or
   * disjunction ({@link FolderLogic#OR}) in the model-code consistency proof.
   * 
   * @return the type of logic used by this folder in the model-code consistency
   *         proof.
   */
  @NonNull
  FolderLogic getFolderLogic();
}
