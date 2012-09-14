package edu.cmu.cs.fluid.sea;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.InRegion;
import com.surelogic.NonNull;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * A code/model consistency result drop recording an analysis result in terms of
 * what promises are (partially or wholly) established in terms of a (possibly
 * empty) set of prerequisite assertion promises.
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
  public static final String TRUSTED_FOLDER = "trusted-folder";
  public static final String OR_TRUSTED_PROMISE = "or-trusted-promise";
  public static final String OR_LABEL = "or-label";
  public static final String OR_USES_RED_DOT = "or-uses-red-dot";
  public static final String OR_PROVED = "or-proved-consistent";
  public static final String ENCLOSED_IN_FOLDER = "enclosed-in-folder";

  /**
   * Constructs a new analysis result.
   */
  public ResultDrop(IRNode node) {
    super(node);
  }

  /**
   * The set of promise drops trusted by this result, its prerequisite
   * assertions.
   */
  @UniqueInRegion("DropState")
  private final HashSet<PromiseDrop<? extends IAASTRootNode>> trusts = new HashSet<PromiseDrop<? extends IAASTRootNode>>(1);

  /**
   * The set of result folder drops trusted by this result, its prerequisite
   * assertions.
   */
  @UniqueInRegion("DropState")
  private final HashSet<ResultFolderDrop> folderTrusts = new HashSet<ResultFolderDrop>(0);
  /**
   * Map from "or" logic trust labels (String) to sets of drop promises. One
   * complete set of promises must be proved consistent for this result to be
   * consistent.
   */
  @UniqueInRegion("DropState")
  private final Map<String, Set<PromiseDrop<? extends IAASTRootNode>>> or_TrustLabelToTrusts = new HashMap<String, Set<PromiseDrop<? extends IAASTRootNode>>>(
      0);

  /**
   * Flags if this result indicates consistency with code.
   */
  @InRegion("DropState")
  private boolean consistent = false;

  public boolean isInResultFolder() {
    synchronized (f_seaLock) {
      return !Sea.filterDropsOfType(ResultFolderDrop.class, getDeponentsReference()).isEmpty();
    }
  }

  /**
   * Adds a promise to the set of drops this result uses as a prerequisite
   * assertion, or <i>trusts</i>.
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
   * Adds a result folder to the set of drops this result uses as a prerequisite
   * assertion, or <i>trusts</i>.
   * 
   * @param folder
   *          the result folder being trusted by this result
   */
  public void addTrustedResultFolder(ResultFolderDrop folder) {
    synchronized (f_seaLock) {
      folderTrusts.add(folder);
      folder.addDependent(this);
    }
  }

  /**
   * Adds a set of promises to the set of promises this result uses as a
   * prerequisite assertion, or <i>trusts</i>.
   * 
   * @param promises
   *          the promises being trusted by this result
   */
  public void addTrustedPromises(Collection<? extends PromiseDrop<? extends IAASTRootNode>> promises) {
    if (promises == null)
      return;

    synchronized (f_seaLock) {
      for (PromiseDrop<? extends IAASTRootNode> promise : promises) {
        addTrustedPromise(promise);
      }
    }
  }

  /**
   * Adds a promise to the set of drops this result uses as a prerequisite
   * assertion, or <i>trusts</i>. All promises added under the same "or" key are
   * conjoined then disjoined with other "or" prerequisite assertions. Finally,
   * the overall "or" result is conjoined with non-"or" prerequisite assertions.
   * 
   * @param orKey
   *          the key or label for the "or" condition
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

  @NonNull
  public HashSet<ProofDrop> getAllTrusted() {
    synchronized (f_seaLock) {
      final HashSet<ProofDrop> result = new HashSet<ProofDrop>(trusts);
      result.addAll(folderTrusts);
      for (Set<PromiseDrop<? extends IAASTRootNode>> orTrusted : or_TrustLabelToTrusts.values()) {
        result.addAll(orTrusted);
      }
      return result;
    }
  }

  /**
   * Returns the prerequisite assertions of this result, this set does not
   * include any "or" prerequisite assertions. Use the "get_or_" methods to
   * obtain those prerequisite assertions. Do <b>not</b> modify the returned set
   * in any way.
   * 
   * @return the non-null (possibly empty) set of promises trusted by this
   *         result, its prerequisite assertions.
   * 
   * @see #hasOrLogic()
   * @see #getTrustedPromises_orKeys()
   * @see #getTrustedPromises_or(String)
   */
  @NonNull
  public HashSet<PromiseDrop<? extends IAASTRootNode>> getTrustedPromises() {
    synchronized (f_seaLock) {
      return trusts;
    }
  }

  @NonNull
  public HashSet<ResultFolderDrop> getTrustedFolders() {
    synchronized (f_seaLock) {
      return folderTrusts;
    }
  }

  public boolean hasTrusted() {
    synchronized (f_seaLock) {
      return hasOrLogic() || !trusts.isEmpty() || !folderTrusts.isEmpty();
    }
  }

  @Override
  @NonNull
  public final Set<ResultDrop> getTrustedBy() {
    /*
     * Result drops cannot be trusted by a proof drop.
     */
    return Collections.emptySet();
  }

  public boolean hasOrLogic() {
    synchronized (f_seaLock) {
      return !or_TrustLabelToTrusts.isEmpty();
    }
  }

  public Set<String> getTrustedPromises_orKeys() {
    synchronized (f_seaLock) {
      return or_TrustLabelToTrusts.keySet();
    }
  }

  @NonNull
  public Set<PromiseDrop<? extends IAASTRootNode>> getTrustedPromises_or(String orKey) {
    synchronized (f_seaLock) {
      final Set<PromiseDrop<? extends IAASTRootNode>> result = or_TrustLabelToTrusts.get(orKey);
      if (result != null)
        return result;
      else
        return Collections.emptySet();
    }
  }

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

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return AbstractXMLReader.RESULT_DROP;
  }

  @Override
  public void preprocessRefs(SeaSnapshot s) {
    super.preprocessRefs(s);
    for (Drop t : getTrustedPromises()) {
      s.snapshotDrop(t);
    }
    for (Drop t : getTrustedFolders()) {
      s.snapshotDrop(t);
    }
    if (hasOrLogic()) {
      for (String label : getTrustedPromises_orKeys()) {
        for (Drop t : getTrustedPromises_or(label)) {
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
    for (Drop t : getTrustedPromises()) {
      s.refDrop(db, TRUSTED_PROMISE, t);
    }
    for (Drop t : getTrustedFolders()) {
      s.refDrop(db, TRUSTED_FOLDER, t);
    }
    if (hasOrLogic()) {
      for (String label : getTrustedPromises_orKeys()) {
        for (Drop t : getTrustedPromises_or(label)) {
          s.refDrop(db, OR_TRUSTED_PROMISE, t, OR_LABEL, label);
        }
      }
    }
  }
}