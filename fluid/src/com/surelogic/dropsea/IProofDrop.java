/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea;

/**
 * The interface for all drops involved with the JSure mode-code consistency
 * proof within the sea, intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IProofDrop extends IDrop, ISnapshotDrop {

  /**
   * Returns if this element is able to be proved consistent (model/code
   * consistency) with regards to the whole-program.
   * 
   * @return {@code true} if consistent, {@code false} if not proved consistent.
   */
  boolean provedConsistent();

  /**
   * Returns if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   * 
   * @return {@code true} if red dot, {@code false} if no red dot.
   */
  boolean proofUsesRedDot();

  /**
   * Checks is this result depends upon something from source code.
   * 
   * @return {@code true} if this result depends on something from source code,
   *         {@code false} otherwise.
   */
  boolean derivedFromSrc();

  /**
   * Checks if this result depends upon something with a warning hint about it.
   * 
   * @return {@code true} if this result depends on something with a warning
   *         hint about it, {@code false} otherwise.
   */
  boolean derivedFromWarningHint();
}
