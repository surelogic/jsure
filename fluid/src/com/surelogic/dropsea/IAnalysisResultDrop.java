package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;

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
  @NonNull
  Collection<? extends IPromiseDrop> getChecked();

  /**
   * Checks if this drop has any prerequisite assertions at all. For a results
   * folder, this method checks if any proof drops are in the folder.
   * 
   * @return {@code true} if this drop has one or more prerequisite assertions,
   *         {@code false} if it has no prerequisite assertions.
   */
  boolean hasTrusted();

  /**
   * Returns the prerequisite assertions of this result. For a results folder,
   * this method returns its contents. The returned set is a copy.
   * 
   * @return a new non-null (possibly empty) set of proof drops trusted by this
   *         result, its prerequisite assertions.
   */
  @NonNull
  Collection<? extends IProofDrop> getTrusted();
}
