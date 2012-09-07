/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import java.util.Collection;

/**
 */
public interface IResultDrop extends IProofDrop, IAnalysisResultDrop {
	/**
	 * Gets if this result indicates model/code consistency.
	 * 
	 * @return <code>true</code> if the result indicates model/code consistency,
	 *         <code>false</code> otherwise.
	 */
	boolean isConsistent();

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
	Collection<? extends IPromiseDrop> getTrusts();

	/**
	 * Gets the set of promise drops established, or checked, by this result.
	 * 
	 * @return the non-null (possibly empty) set of promise drops established, or
	 *         checked, by this result.
	 */
	Collection<? extends IPromiseDrop> getChecks();

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
	Collection<? extends IPromiseDrop> getTrustsComplete();

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
	Collection<? extends IPromiseDrop> get_or_Trusts(String key);	
}
