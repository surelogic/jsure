package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;

/**
 * The interface for consistent/inconsistent judgment drops reported by
 * verifying analyses within the sea, intended to allow multiple
 * implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IResultDrop extends IAnalysisResultDrop {

  /**
   * Gets if this result indicates model/code consistency.
   * 
   * @return {@code true} if the result indicates model/code consistency,
   *         {@code false} otherwise.
   */
  boolean isConsistent();

  /**
   * Checks if this result drop was vouched for by a programmer even though it
   * is inconsistent.
   * 
   * @return {@code true} if this result drop was vouched for by a programmer
   *         even though it is inconsistent, {@code false} otherwise.
   */
  boolean isVouched();

  /**
   * Checks if this result is inconsistent because the analysis timed out.
   * 
   * @return {@code true} if this result drop is inconsistent because the
   *         analysis timed out, {@code false} otherwise.
   */
  boolean isTimeout();

  /**
   * Checks if this drop has any prerequisite assertions at all.
   * 
   * @return {@code true} if this drop has one or more prerequisite assertions,
   *         {@code false} if it has no prerequisite assertions.
   */
  boolean hasTrusted();

  /**
   * Returns <i>all</i> prerequisite assertions of this result. However, using
   * this call it is impossible to distinguish "and" prerequisite assertions
   * from "or" prerequisite assertions. The returned set is a copy.
   * 
   * @return a new non-null (possibly empty) set of proof drops trusted by this
   *         result, its prerequisite assertions.
   */
  @NonNull
  Collection<? extends IProofDrop> getAllTrusted();

  /**
   * Returns the prerequisite assertions of this result, this set does not
   * include any "or" prerequisite assertions. Do <b>not</b> modify the returned
   * set in any way.
   * 
   * @return a non-null (possibly empty) set of proof drops trusted by this
   *         result.
   */
  @NonNull
  Collection<? extends IProofDrop> getTrusted_and();

  /**
   * Flags if this result has groups of "or" prerequisite assertion sets which
   * must be considered in the whole-program consistency proof.
   * 
   * @return {@code true} if "or" prerequisite assertion logic exists,
   *         {@code false} otherwise.
   */
  boolean hasOrLogic();

  /**
   * Returns the set of "or" keys used for this promise. Do <b>not</b> modify
   * the returned set in any way.
   * 
   * @return the non-null (possibly empty) set of "or" keys used by this promise
   */
  @NonNull
  Collection<String> getTrusted_orKeys();

  /**
   * Returns the set of proof drops for a specific "or" key. Do <b>not</b>
   * modify the returned set in any way.
   * 
   * @param orKey
   *          the key to provide the proof drop set for
   * @return the non-null (possibly empty) proof drop set
   */
  @NonNull
  Collection<? extends IProofDrop> getTrusted_or(String key);

  /**
   * Returns if the proof of "or" trusted promises uses a red dot.
   * 
   * @return {@code true} if a red dot is used, {@code false} otherwise.
   */
  boolean get_or_proofUsesRedDot();

  /**
   * Returns if the proof of "or" trusted promises is consistent.
   * 
   * @return {@code true} if consistent, {@code false} otherwise.
   */
  boolean get_or_provedConsistent();
}
