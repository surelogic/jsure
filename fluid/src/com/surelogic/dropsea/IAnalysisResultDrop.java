package com.surelogic.dropsea;

import java.util.Collection;

/**
 * The interface for all analysis results, or verification judgement, drops
 * within the sea, intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IAnalysisResultDrop extends IProofDrop, IAnalysisOutputDrop {

  /**
   * Checks if the set of promise drops established, or checked, by this result
   * contains any elements.
   * 
   * @return {@code true} if this drop has one or more consequential assertions,
   *         {@code false} if it has no consequential assertions.
   */
  boolean hasChecked();

  /**
   * Gets the set of promise drops established, or checked, by this result.
   * 
   * @return the non-null (possibly empty) set of promise drops established, or
   *         checked, by this result.
   */
  Collection<? extends IPromiseDrop> getCheckedPromises();

  /**
   * Checks if this result is within an analysis results folder.
   * 
   * @return {@code true} if this result is within a results folder,
   *         {@code false} otherwise.
   */
  boolean isInResultFolder();
}
