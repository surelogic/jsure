package edu.cmu.cs.fluid.sea;

import java.util.*;

import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;

import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * A code/model consistency result drop recording an analysis result in terms of
 * what promises are (partially or wholly) established in terms of a (possibly
 * empty) set of precondition promises.
 * <p>
 * Not intended to be subclassed.
 */
@SuppressWarnings("unchecked")
public final class ResultDrop extends ProofDrop implements IResultDrop {
	public static final String TIMEOUT = "timeout";
	public static final String VOUCHED = "vouched";
	public static final String CONSISTENT = "consistent";
	public static final String CHECKED_PROMISE = "checked-promise";
	public static final String TRUSTED_PROMISE = "trusted-promise";
	public static final String OR_TRUSTED_PROMISE = "or-trusted-promise";
	public static final String OR_LABEL = "or-label";
	public static final String OR_USES_RED_DOT = "or-uses-red-dot";
	public static final String OR_PROVED = "or-proved-consistent";

	/**
	 * Holds key that describes which message this result is.
	 */
	private final String type;

	/**
	 * @param t
	 *            Key that describes which message this result drop represents.
	 */
	public ResultDrop(String t) {
		if (t == null) {
			throw new RuntimeException();
		}
		type = t;
	}

	public ResultDrop() {
		this(null);
	}

	/**
	 * The set of promise drops being checked, or established, by this result.
	 */
	private Set<PromiseDrop> checks = new HashSet<PromiseDrop>();

	/**
	 * The set of promise drops trusted by this result, its preconditions.
	 */
	private Set<PromiseDrop> trusts = new HashSet<PromiseDrop>();

	/**
	 * Map from "or" logic trust labels (String) to sets of drop promises. One
	 * complete set of promises must be proved consistent for this result to be
	 * consistent.
	 */
	private Map<String, Set<PromiseDrop>> or_TrustLabelToTrusts = new HashMap<String, Set<PromiseDrop>>(); // String
	// ->
	// Set
	// (promise
	// drops)

	/**
	 * Flags if this result indicates consistency with code.
	 */
	private boolean consistent = false;

	/**
	 * Adds a promise to the set of promises this result establishes, or
	 * <i>checks</i>.
	 * 
	 * @param promise
	 *            the promise being supported by this result
	 */
	public void addCheckedPromise(PromiseDrop promise) {
		checks.add(promise);
		promise.addDependent(this);
	}

	public void addCheckedPromises(Collection<? extends PromiseDrop> promises) {
		// no null check -- fail-fast
		for (PromiseDrop promise : promises) {
			// Iterator promiseIter = promises.iterator();
			// while (promiseIter.hasNext()) {
			// PromiseDrop promise = (PromiseDrop) promiseIter.next();
			addCheckedPromise(promise);
		}
	}

	/**
	 * Adds a promise to the set of promises this result uses as a precondition,
	 * or <i>trusts</i>.
	 * 
	 * @param promise
	 *            the promise being trusted by this result
	 */
	public void addTrustedPromise(PromiseDrop promise) {
		trusts.add(promise);
		promise.addDependent(this);
	}

	public void addTrustedPromises(Collection<? extends PromiseDrop> promises) {
		// no null check -- fail-fast
		for (PromiseDrop promise : promises) {
			// Iterator promiseIter = promises.iterator();
			// while (promiseIter.hasNext()) {
			// PromiseDrop promise = (PromiseDrop) promiseIter.next();
			addTrustedPromise(promise);
		}
	}

	/**
	 * Adds a promise to the set of promises this result uses as a precondition,
	 * or <i>trusts</i>.
	 * 
	 * @param promise
	 *            the promise being trusted by this result
	 */
	public void addTrustedPromise_or(String orKey, PromiseDrop promise) {
		String key = orKey.intern();
		Set<PromiseDrop> s = or_TrustLabelToTrusts.get(key);
		if (s == null) {
			s = new HashSet<PromiseDrop>();
			or_TrustLabelToTrusts.put(key, s);
		}
		s.add(promise);
		promise.addDependent(this);
	}

	/**
	 * @return the set of promise drops established, or checked, by this result.
	 *         All members of the returned set will are of the PromiseDrop type.
	 */
	public Set<? extends PromiseDrop> getChecks() {
		return checks;
	}

	/**
	 * Returns the preconditions of this result, including any "or"
	 * preconditions. However, using this call it is impossible to distingish
	 * "and" preconditions from "or"preconditions.
	 * 
	 * @return the set of promises trusted by this result, its preconditions.
	 *         All members of the returned set will are of the PromiseDrop type.
	 * 
	 * @see #getTrusts()
	 * @see #hasOrLogic()
	 * @see #get_or_TrustLabelSet()
	 * @see #get_or_Trusts(String)
	 */
	public Set<? extends PromiseDrop> getTrustsComplete() {
		Set<PromiseDrop> result = new HashSet<PromiseDrop>();
		result.addAll(trusts);
		if (hasOrLogic()) {
			Set<String> orLabels = get_or_TrustLabelSet();
			for (String orKey : orLabels) {
				// String orKey = (String) i.next();
				result.addAll(get_or_Trusts(orKey));
			}
		}
		return result;
	}

	/**
	 * Returns the preconditions of this result, this set does not include any
	 * "or" preconditions. Use the "get_or_" methods to obtain those
	 * preconditions.
	 * 
	 * @return the set of promises trusted by this result, its preconditions.
	 *         All members of the returned set will are of the PromiseDrop type.
	 * 
	 * @see #hasOrLogic()
	 * @see #get_or_TrustLabelSet()
	 * @see #get_or_Trusts(String)
	 */
	public Set<PromiseDrop> getTrusts() {
		return trusts;
	}

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
	public boolean hasOrLogic() {
		return !or_TrustLabelToTrusts.keySet().isEmpty();
	}

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
	 * @return the set of "or" keys used by this promise
	 */
	public Set<String> get_or_TrustLabelSet() {
		return or_TrustLabelToTrusts.keySet();
	}

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
	 *            the key to provide the promise drop set for
	 * @return the promise drop set
	 */
	public Set<PromiseDrop> get_or_Trusts(String key) {
		return or_TrustLabelToTrusts.get(key);
	}

	/**
	 * @return <code>true</code> if the result indicates model/code consistency,
	 *         <code>false</code> otherwise.
	 */
	public boolean isConsistent() {
		return consistent;
	}

	/**
	 * Sets this result to indicate model/code consistency.
	 */
	public void setConsistent() {
		consistent = true;
	}

	/**
	 * Sets this result to indicate model/code inconsistency.
	 */
	public void setInconsistent() {
		consistent = false;
	}

	/**
	 * Sets this result to indicate model/code inconsistency.
	 * 
	 * @param value
	 *            the consistency setting.
	 */
	public void setConsistent(final boolean value) {
		consistent = value;
	}

	/**
	 * Flags if this result drop was "vouched" for by a programmer even though
	 * it is inconsistent.
	 */
	private boolean vouched = false;

	/**
	 * Returns if this result drop was "vouched" for by a programmer even though
	 * it is inconsistent.
	 * 
	 * @return <code>true</code> if this result drop was "vouched" for by a
	 *         programmer even though it is inconsistent, <code>false</code>
	 *         otherwise.
	 */
	public boolean isVouched() {
		return vouched;
	}

	/**
	 * Sets this result as being "vouched" for by a programmer even though it is
	 * inconsistent.
	 */
	public void setVouched() {
		vouched = true;
	}

	/**
	 * Flags if this result drop is inconsistent because the analysis timed out.
	 */
	private boolean timeout = false;

	/**
	 * Sets this analysis result to inconsistent and marks that this is because
	 * its verifying analysis timed out.
	 */
	public void setTimeout() {
		setInconsistent();
		timeout = true;
	}

	/**
	 * Returns if this result is inconsistent because the analysis timed out.
	 * 
	 * @return <code>true</code> if this result drop is inconsistent because the
	 *         analysis timed out, <code>false</code> otherwise.
	 */
	public boolean isTimeout() {
		return timeout;
	}

	/**
	 * Flags of the proof of "or" trusted promises uses a red dot.
	 */
	boolean or_proofUsesRedDot = false;

	/**
	 * Returns if the proof of "or" trusted promises uses a red dot.
	 * 
	 * @return <code>true</code> if a red dot is used, <code>false</code>
	 *         otherwise.
	 */
	public boolean get_or_proofUsesRedDot() {
		return or_proofUsesRedDot;
	}

	/**
	 * Flags if the proof of "or" trusted promises is consistent.
	 */
	boolean or_provedConsistent = false;

	/**
	 * Returns if the proof of "or" trusted promises is consistent.
	 * 
	 * @return<code>true</code> if consistent, <code>false</code> otherwise.
	 */
	public boolean get_or_provedConsistent() {
		return or_provedConsistent;
	}

	@Override
	public String getEntityName() {
		return "result-drop";
	}

	@Override
	public void preprocessRefs(SeaSnapshot s) {
		for (Drop c : getChecks()) {
			s.snapshotDrop(c);
		}
		for (Drop t : getTrusts()) {
			s.snapshotDrop(t);
		}
		if (hasOrLogic()) {
			for (String label : get_or_TrustLabelSet()) {
				for (Drop t : get_or_Trusts(label)) {
					s.snapshotDrop(t);
				}
			}
		}
	}

	@Override
	public void snapshotAttrs(XMLCreator.Builder s) {
		super.snapshotAttrs(s);
		s.addAttribute(VOUCHED, isVouched());
		s.addAttribute(CONSISTENT, isConsistent());
		s.addAttribute(OR_USES_RED_DOT, get_or_proofUsesRedDot());
		s.addAttribute(OR_PROVED, get_or_provedConsistent());
		s.addAttribute(TIMEOUT, isTimeout());
		if (type != null) {
			s.addAttribute("result-type", type);
		} else {
			System.out.println("ResultDrop result-type is null");
		}
	}

	@Override
	public void snapshotRefs(SeaSnapshot s, Builder db) {
		super.snapshotRefs(s, db);
		for (Drop c : getChecks()) {
			s.refDrop(db, CHECKED_PROMISE, c);
		}
		for (Drop t : getTrusts()) {
			s.refDrop(db, TRUSTED_PROMISE, t);
		}
		if (hasOrLogic()) {
			for (String label : get_or_TrustLabelSet()) {
				for (Drop t : get_or_Trusts(label)) {
					s.refDrop(db, OR_TRUSTED_PROMISE, t, OR_LABEL, label);
				}
			}
		}
	}
}