package edu.cmu.cs.fluid.sea;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.InRegion;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;

import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * A code/model consistency result drop recording an analysis result in terms of
 * what promises are (partially or wholly) established in terms of a (possibly
 * empty) set of precondition promises.
 * <p>
 * Not intended to be subclassed.
 */
public final class ResultDrop extends AnalysisResultDrop implements IResultDrop {

  /*
   * XML attribute constants
   */
  public static final String TIMEOUT = "timeout";
  public static final String VOUCHED = "vouched";
  public static final String CONSISTENT = "consistent";
  public static final String TRUSTED_PROMISE = "trusted-promise";
  public static final String OR_TRUSTED_PROMISE = "or-trusted-promise";
  public static final String OR_LABEL = "or-label";
  public static final String OR_USES_RED_DOT = "or-uses-red-dot";
  public static final String OR_PROVED = "or-proved-consistent";
  public static final String ENCLOSED_IN_FOLDER = "enclosed-in-folder";

  /**
   * Constructs a new analysis result.
   */
  public ResultDrop() {
  }

  /**
   * The set of promise drops trusted by this result, its preconditions.
   */
  @UniqueInRegion("DropState")
  private final HashSet<PromiseDrop<? extends IAASTRootNode>> trusts = new HashSet<PromiseDrop<? extends IAASTRootNode>>();

  /**
   * Map from "or" logic trust labels (String) to sets of drop promises. One
   * complete set of promises must be proved consistent for this result to be
   * consistent.
   */
  @UniqueInRegion("DropState")
  private final Map<String, Set<PromiseDrop<? extends IAASTRootNode>>> or_TrustLabelToTrusts = new HashMap<String, Set<PromiseDrop<? extends IAASTRootNode>>>();

  /**
   * Flags if this result indicates consistency with code.
   */
  @InRegion("DropState")
  private boolean consistent = false;

  /**
   * Returns if this result is within a {@link ResultFolderDrop} instance.
   * 
   * @return {@code true} if this result is within a {@link ResultFolderDrop}
   *         instance, {@code false} otherwise.
   */
  public boolean isInResultFolder() {
    synchronized (f_seaLock) {
      return !Sea.filterDropsOfType(ResultFolderDrop.class, getDeponentsReference()).isEmpty();
    }
  }

  /**
   * Adds a promise to the set of promises this result uses as a precondition,
   * or <i>trusts</i>.
   * 
   * @param promise
   *          the promise being trusted by this result
   */
  public void addTrustedPromise(PromiseDrop<? extends IAASTRootNode> promise) {
    synchronized (f_seaLock) {
      trusts.add(promise);
      promise.addDependent(this);
    }
  }

  /**
   * Adds a set of promises to the set of promises this result uses as a
   * precondition, or <i>trusts</i>.
   * 
   * @param promises
   *          the promises being trusted by this result
   */
  public void addTrustedPromises(Collection<? extends PromiseDrop<? extends IAASTRootNode>> promises) {
    if (promises == null)
      return;

    synchronized (f_seaLock) {
      for (PromiseDrop<? extends IAASTRootNode> promise : promises) {
        // Iterator promiseIter = promises.iterator();
        // while (promiseIter.hasNext()) {
        // PromiseDrop promise = (PromiseDrop) promiseIter.next();
        addTrustedPromise(promise);
      }
    }
  }

  /**
   * Adds a promise to the set of promises this result uses as a precondition,
   * or <i>trusts</i>.
   * 
   * @param promise
   *          the promise being trusted by this result
   * @throws IllegalArgumentException
   *           if either parameter is null.
   */
  public void addTrustedPromise_or(String orKey, PromiseDrop<? extends IAASTRootNode> promise) {
    if (orKey == null)
      throw new IllegalArgumentException(I18N.err(44, "orKey"));
    if (promise == null)
      throw new IllegalArgumentException(I18N.err(44, "promise"));

    synchronized (f_seaLock) {
      Set<PromiseDrop<? extends IAASTRootNode>> s = or_TrustLabelToTrusts.get(orKey);
      if (s == null) {
        s = new HashSet<PromiseDrop<? extends IAASTRootNode>>();
        or_TrustLabelToTrusts.put(orKey, s);
      }
      s.add(promise);
      promise.addDependent(this);
    }
  }

  /**
   * Returns the preconditions of this result, including any "or" preconditions.
   * However, using this call it is impossible to distinguish "and"
   * preconditions from "or"preconditions. The returned set is a copy.
   * 
   * @return a new non-null (possibly empty) set of promises trusted by this
   *         result, its preconditions.
   * 
   * @see #getTrusts()
   * @see #hasOrLogic()
   * @see #get_or_TrustLabelSet()
   * @see #get_or_Trusts(String)
   */
  public HashSet<? extends PromiseDrop<? extends IAASTRootNode>> getTrustsComplete() {
    synchronized (f_seaLock) {
      final HashSet<PromiseDrop<? extends IAASTRootNode>> result = new HashSet<PromiseDrop<? extends IAASTRootNode>>(trusts);
      if (hasOrLogic()) {
        Set<String> orLabels = get_or_TrustLabelSet();
        for (String orKey : orLabels) {
          // String orKey = (String) i.next();
          result.addAll(get_or_Trusts(orKey));
        }
      }
      return result;
    }
  }

  /**
   * Returns the preconditions of this result, this set does not include any
   * "or" preconditions. Use the "get_or_" methods to obtain those
   * preconditions. Do <b>not</b> modify the returned set in any way.
   * 
   * @return the non-null (possibly empty) set of promises trusted by this
   *         result, its preconditions.
   * 
   * @see #hasOrLogic()
   * @see #get_or_TrustLabelSet()
   * @see #get_or_Trusts(String)
   */
  public HashSet<PromiseDrop<? extends IAASTRootNode>> getTrusts() {
    synchronized (f_seaLock) {
      return trusts;
    }
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
    synchronized (f_seaLock) {
      return !or_TrustLabelToTrusts.keySet().isEmpty();
    }
  }

  /**
   * Returns the set of "or" keys used for this promise. Do <b>not</b> modify
   * the returned set in any way.
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
  public Set<String> get_or_TrustLabelSet() {
    synchronized (f_seaLock) {
      return or_TrustLabelToTrusts.keySet();
    }
  }

  /**
   * Returns the set of promise drops for a specific "or" key. Do <b>not</b>
   * modify the returned set in any way.
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
  public Set<PromiseDrop<? extends IAASTRootNode>> get_or_Trusts(String key) {
    synchronized (f_seaLock) {
      return or_TrustLabelToTrusts.get(key);
    }
  }

  /**
   * Gets if this result indicates model/code consistency.
   * 
   * @return <code>true</code> if the result indicates model/code consistency,
   *         <code>false</code> otherwise.
   */
  public boolean isConsistent() {
    synchronized (f_seaLock) {
      return consistent;
    }
  }

  /**
   * Sets this result to indicate model/code consistency.
   */
  public void setConsistent() {
    synchronized (f_seaLock) {
      consistent = true;
    }
  }

  /**
   * Sets this result to indicate model/code inconsistency.
   */
  public void setInconsistent() {
    synchronized (f_seaLock) {
      consistent = false;
    }
  }

  /**
   * Sets this result to indicate model/code inconsistency.
   * 
   * @param value
   *          the consistency setting.
   */
  public void setConsistent(final boolean value) {
    synchronized (f_seaLock) {
      consistent = value;
    }
  }

  /**
   * Flags if this result drop was "vouched" for by a programmer even though it
   * is inconsistent.
   */
  @InRegion("DropState")
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
    synchronized (f_seaLock) {
      return vouched;
    }
  }

  /**
   * Sets this result as being "vouched" for by a programmer even though it is
   * inconsistent.
   */
  public void setVouched() {
    synchronized (f_seaLock) {
      vouched = true;
    }
  }

  /**
   * Flags if this result drop is inconsistent because the analysis timed out.
   */
  @InRegion("DropState")
  private boolean timeout = false;

  /**
   * Sets this analysis result to inconsistent and marks that this is because
   * its verifying analysis timed out.
   */
  public void setTimeout() {
    synchronized (f_seaLock) {
      setInconsistent();
      timeout = true;
    }
  }

  /**
   * Returns if this result is inconsistent because the analysis timed out.
   * 
   * @return <code>true</code> if this result drop is inconsistent because the
   *         analysis timed out, <code>false</code> otherwise.
   */
  public boolean isTimeout() {
    synchronized (f_seaLock) {
      return timeout;
    }
  }

  /**
   * Flags of the proof of "or" trusted promises uses a red dot.
   */
  @InRegion("DropState")
  private boolean or_proofUsesRedDot = false;

  /**
   * Returns if the proof of "or" trusted promises uses a red dot.
   * 
   * @return <code>true</code> if a red dot is used, <code>false</code>
   *         otherwise.
   */
  public boolean get_or_proofUsesRedDot() {
    synchronized (f_seaLock) {
      return or_proofUsesRedDot;
    }
  }

  void set_or_proofUsesRedDot(boolean value) {
    synchronized (f_seaLock) {
      or_proofUsesRedDot = value;
    }
  }

  /**
   * Flags if the proof of "or" trusted promises is consistent.
   */
  @InRegion("DropState")
  private boolean or_provedConsistent = false;

  /**
   * Returns if the proof of "or" trusted promises is consistent.
   * 
   * @return<code>true</code> if consistent, <code>false</code> otherwise.
   */
  public boolean get_or_provedConsistent() {
    synchronized (f_seaLock) {
      return or_provedConsistent;
    }
  }

  void set_or_provedConsistent(boolean value) {
    synchronized (f_seaLock) {
      or_provedConsistent = value;
    }
  }

  @Override
  public String getXMLElementName() {
    return AbstractXMLReader.RESULT_DROP;
  }

  @Override
  public void preprocessRefs(SeaSnapshot s) {
    super.preprocessRefs(s);
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
    s.addAttribute(ENCLOSED_IN_FOLDER, isInResultFolder());
    s.addAttribute(PromiseDrop.FROM_SRC, isFromSrc());
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
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