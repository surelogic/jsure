/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import java.util.Collection;

public interface IProofDropInfo extends IDrop {

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
   * Returns if this PromiseDrop is <i>intended</i> to be checked by analysis or
   * not. Most promises are supported by analysis results (i.e., they have
   * ResultDrops attached to them), however some are simply well-formed (e.g.,
   * region models). If the promise is simply well-formed then it should
   * override this method and return <code>false</code>.
   * 
   * @return <code>true</code> if the PromiseDrop is intended to be checked by
   *         analysis, <code>false</code> otherwise.
   */
  boolean isIntendedToBeCheckedByAnalysis();

  /**
   * Gets if this result indicates model/code consistency.
   * 
   * @return <code>true</code> if the result indicates model/code consistency,
   *         <code>false</code> otherwise.
   */
  boolean isConsistent();

  /**
   * Returns if this PromiseDrop has been checked by analysis or not. If this
   * PromiseDrop (or any deponent PromiseDrop of this PromiseDrop) has results
   * it is considered checked, otherwise it is considered trusted. This approach
   * is designed to allow the system to detect when a PromiseDrop that usually
   * is checked by an analysis has not been checked (i.e., the analysis is
   * turned off). This method is intended to be overridden by subclasses where
   * the default behavior is wrong. That said, however, usually the subclass
   * should override {@link #isIntendedToBeCheckedByAnalysis()} and note that
   * the promise is not intended to be checked by analysis (which will cause
   * this method to return <code>true</code>).
   * <p>
   * We currently trust XML promises as having been checked by analysis, or
   * defined by the JLS.
   * 
   * @return <code>true</code> if the PromiseDrop is considered checked by
   *         analysis, <code>false</code> otherwise.
   */
  boolean isCheckedByAnalysis();

  /**
   * Gets if this promise is assumed.
   * 
   * @return <code>true</code> if the promise is assumed, <code>false</code>
   *         otherwise.
   */
  boolean isAssumed();

  /**
   * Gets if this promise is virtual.
   * 
   * @return <code>true</code> if the promise is virtual, <code>false</code>
   *         otherwise.
   */
  boolean isVirtual();

  /**
   * Returns if this result drop was "vouched" for by a programmer even though
   * it is inconsistent.
   * 
   * @return <code>true</code> if this result drop was "vouched" for by a
   *         programmer even though it is inconsistent, <code>false</code>
   *         otherwise.
   */
  boolean isVouched();

  /**
   * Returns if this result is inconsistent because the analysis timed out.
   * 
   * @return <code>true</code> if this result drop is inconsistent because the
   *         analysis timed out, <code>false</code> otherwise.
   */
  boolean isTimeout();

  /**
   * Returns the preconditions of this result, this set does not include any
   * "or" preconditions. Use the "get_or_" methods to obtain those
   * preconditions.
   * 
   * @return the non-null (possibly empty) set of promises trusted by this
   *         result, its preconditions.
   * 
   * @see #hasOrLogic()
   * @see #get_or_TrustLabelSet()
   * @see #get_or_Trusts(String)
   */
  Collection<? extends IProofDropInfo> getTrusts();

  /**
   * Gets the set of promise drops established, or checked, by this result.
   * 
   * @return the non-null (possibly empty) set of promise drops established, or
   *         checked, by this result.
   */
  Collection<? extends IProofDropInfo> getChecks();

  /**
   * Flags if this result has groups of "or" precondition sets which must be
   * considered in the whole-program consistency proof. Most results do not
   * include "or" precondition logic.
   * <p>
   * Typical use of this method is:
   * 
   * <pre>
   * if (rd.hasOrLogic()) {
   *   Set orLabels = rd.get_or_TrustLabelSet()
   *   for (Iterator i = orLabels.iterator(); i.hasNext();) {
   *      String orKey = (String) i.next();
   *      Set promiseSet = rd.get_or_Trusts(orKey);
   *      for (Iterator j = promiseSet.iterator(); j.hasNext();) {
   *         PromiseDrop promise = (PromiseDrop) j.next();
   *         // do something
   *      }   
   *   }
   * }
   * </pre>
   * 
   * @return <code>true</code> if "or" precondition logic exists,
   *         <code>false</code> otherwise.
   */
  boolean hasOrLogic();

  /**
   * Returns the set of "or" keys used for this promise.
   * <p>
   * Typical use of this method is:
   * 
   * <pre>
   * if (rd.hasOrLogic()) {
   *   Set orLabels = rd.get_or_TrustLables()
   *   Set orLabels = rd.get_or_TrustLabelSet()
   *   for (Iterator i = orLabels.iterator(); i.hasNext();) {
   *      String orKey = (String) i.next();
   *      Set promiseSet = rd.get_or_Trusts(orKey);
   *      for (Iterator j = promiseSet.iterator(); j.hasNext();) {
   *         PromiseDrop promise = (PromiseDrop) j.next();
   *         // do something
   *      }   
   *   }
   * }
   * </pre>
   * 
   * @return the non-null (possibly empty) set of "or" keys used by this promise
   */
  Collection<String> get_or_TrustLabelSet();

  /**
   * Returns if the proof of "or" trusted promises uses a red dot.
   * 
   * @return <code>true</code> if a red dot is used, <code>false</code>
   *         otherwise.
   */
  boolean get_or_proofUsesRedDot();

  /**
   * Returns if the proof of "or" trusted promises is consistent.
   * 
   * @return<code>true</code> if consistent, <code>false</code> otherwise.
   */
  boolean get_or_provedConsistent();

  /**
   * Returns the set of promise drops for a specific "or" key.
   * <p>
   * Typical use of this method is:
   * 
   * <pre>
   * if (rd.hasOrLogic()) {
   *   Set orLabels = rd.get_or_TrustLabelSet()
   *   for (Iterator i = orLabels.iterator(); i.hasNext();) {
   *      String orKey = (String) i.next();
   *      Set promiseSet = rd.get_or_Trusts(orKey);
   *      for (Iterator j = promiseSet.iterator(); j.hasNext();) {
   *         PromiseDrop promise = (PromiseDrop) j.next();
   *         // do something
   *      }   
   *   }
   * }
   * </pre>
   * 
   * @param key
   *          the key to provide the promise drop set for
   * @return the non-null (possibly empty) promise drop set
   */
  Collection<? extends IProofDropInfo> get_or_Trusts(String key);

  /**
   * Returns a copy of the set of result drops which directly check this promise
   * drop.
   * 
   * @return a non-null (possibly empty) set which check this promise drop
   */
  Collection<? extends IProofDropInfo> getCheckedBy();

  /**
   * Returns the preconditions of this result, including any "or" preconditions.
   * However, using this call it is impossible to distingish "and" preconditions
   * from "or"preconditions.
   * 
   * @return the non-null (possibly empty) set of promises trusted by this
   *         result, its preconditions. All members of the returned set will are
   *         of the PromiseDrop type.
   * 
   * @see #getTrusts()
   * @see #hasOrLogic()
   * @see #get_or_TrustLabelSet()
   * @see #get_or_Trusts(String)
   */
  Collection<? extends IProofDropInfo> getTrustsComplete();
}
