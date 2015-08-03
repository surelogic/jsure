package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;

/**
 * The interface for all analysis results, or verification judgment, drops
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

  /**
   * Checks if this drop is used by the verification proof of any promise in the
   * sea. This indicates, if {@code false}, most of the time, that this drop was
   * not chosen by a folder with OR logic. It could also indicate that the drop
   * doesn't support, directly or indirectly, any promise at all.
   * <p>
   * This method is intended to help results viewers that show the analysis
   * results outside of the context of the proof structure, for example, by Java
   * declaration. Avoid chaining up inconsistent results that were unused, or
   * put another way don't matter, and confusing the tool user.
   * 
   * @return {@code true} if the drop is used by the verification proof of any
   *         promise in the sea, {@code false} otherwise.
   */
  boolean usedByProof();
}
