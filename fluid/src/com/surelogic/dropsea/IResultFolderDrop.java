package com.surelogic.dropsea;

import java.util.Collection;

/**
 * The interface for the base class for folders that contain
 * consistent/inconsistent judgment drops reported by verifying analyses within
 * the sea, intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IResultFolderDrop extends IAnalysisResultDrop {
  /**
   * Gets all the analysis results directly within this folder. If sub-folders
   * exist, analysis results within the sub-folders are <b>not</b> returned.
   * 
   * @return a non-null (possibly empty) set of analysis results.
   */
  Collection<? extends IResultDrop> getAnalysisResults();

  /**
   * Gets all the sub-folders within this folder.
   * 
   * @return a non-null (possibly empty) set of analysis result folders.
   */
  Collection<? extends IResultFolderDrop> getSubFolders();

  /**
   * Gets all the analysis results and sub-folders within this folder.
   * 
   * @return a non-null (possibly empty) set of analysis results and
   *         sub-folders.
   */
  Collection<? extends IAnalysisResultDrop> getContents();
}
