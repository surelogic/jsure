package com.surelogic.dropsea;

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
}
