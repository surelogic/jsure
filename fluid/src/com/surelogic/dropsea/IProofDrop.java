/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea;

import java.util.Collection;

import com.surelogic.NonNull;

/**
 * The interface for all drops involved with the JSure mode-code consistency
 * proof within the sea, intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IProofDrop extends IDrop {

  /**
   * Returns if this element is able to be proved consistent (model/code
   * consistency) with regards to the whole-program.
   * 
   * @return <code>true</code> if consistent, <code>false</code> if
   *         inconsistent.
   */
  boolean provedConsistent();

  /**
   * Returns if the proof of this element depends upon a "red dot," or a user
   * vouching for or assuming something which may not be true, with regards to
   * the whole-program.
   * 
   * @return<code>true</code> if red dot, <code>false</code> if no red dot.
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
   * Returns if this promise is from source code or from another location, such
   * as XML. The default value for a promise drop is <code>true</code>.
   * 
   * @return <code>true</code> if the promise was created from an annotation in
   *         source code, <code>false</code> otherwise
   */
  boolean isFromSrc();

  /**
   * Returns the set of analysis hints about this proof drop.
   * 
   * @return the set of analysis hints about this proof drop.
   */
  @NonNull
  Collection<? extends IAnalysisHintDrop> getAnalysisHintsAbout();
}
