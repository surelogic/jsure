package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CHECKED_PROMISE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TRUSTED_PROOF_DROP;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A partial implementation of an analysis result that consolidates code shared
 * by {@link ResultDrop} and {@link ResultFolderDrop}. In particular, management
 * of the set of promise drops being checked, or established, by this result is
 * provided by this class.
 * <p>
 * Not intended to be subclassed by any types except {@link ResultDrop} and
 * {@link ResultFolderDrop}.
 * 
 * @see ResultDrop
 * @see ResultFolderDrop
 */
public abstract class AnalysisResultDrop extends ProofDrop implements IAnalysisResultDrop {

  protected AnalysisResultDrop(IRNode node) {
    super(node);
  }

  /**
   * The set of promise drops being checked, or established, by this result.
   */
  @UniqueInRegion("DropState")
  private final Set<PromiseDrop<? extends IAASTRootNode>> f_checks = new HashSet<PromiseDrop<? extends IAASTRootNode>>();

  public boolean hasChecked() {
    return !f_checks.isEmpty();
  }

  /**
   * Gets the set of promise drops established, or checked, by this result. The
   * returned set is a copy.
   * 
   * @return the non-null (possibly empty) set of promise drops established, or
   *         checked, by this result.
   */
  public final HashSet<? extends PromiseDrop<? extends IAASTRootNode>> getChecked() {
    synchronized (f_seaLock) {
      return new HashSet<PromiseDrop<? extends IAASTRootNode>>(f_checks);
    }
  }

  final Set<? extends PromiseDrop<? extends IAASTRootNode>> getCheckedReference() {
    synchronized (f_seaLock) {
      return f_checks;
    }
  }

  /**
   * Adds a promise to the set of promises this result establishes, or
   * <i>checks</i>.
   * 
   * @param promise
   *          the promise being supported by this result
   */
  public final void addChecked(PromiseDrop<? extends IAASTRootNode> promise) {
    synchronized (f_seaLock) {
      f_checks.add(promise);
      promise.addDependent(this);
    }
  }

  /**
   * Adds a set of promises to the set of promises this result establishes, or
   * <i>checks</i>.
   * 
   * @param promises
   *          the promises being supported by this result
   */
  public final void addChecked(Collection<? extends PromiseDrop<? extends IAASTRootNode>> promises) {
    if (promises == null)
      return;

    synchronized (f_seaLock) {
      for (PromiseDrop<? extends IAASTRootNode> promise : promises) {
        addChecked(promise);
      }
    }
  }

  /**
   * The set of proof drops trusted by this result, its prerequisite assertions.
   */
  @UniqueInRegion("DropState")
  private final HashSet<ProofDrop> f_trusts = new HashSet<ProofDrop>(0);

  /**
   * Adds a proof drop to the set of drops this result uses as a prerequisite
   * assertion, or <i>trusts</i>. For a result folder, this method adds a proof
   * drop into the folder.
   * 
   * @param proofDrop
   *          the proof drop being trusted by this result.
   */
  public void addTrusted(ProofDrop proofDrop) {
    synchronized (f_seaLock) {
      f_trusts.add(proofDrop);
      proofDrop.addDependent(this);
    }
  }

  /**
   * Adds a set of proof drop to the set of proof drops this result uses as a
   * prerequisite assertion, or <i>trusts</i>. For a result folder, this method
   * adds the set of proof drops into the folder.
   * 
   * @param proofDrops
   *          the proof drops being trusted by this result.
   */
  public void addTrusted(Collection<? extends ProofDrop> proofDrops) {
    if (proofDrops == null)
      return;

    synchronized (f_seaLock) {
      for (ProofDrop pd : proofDrops) {
        addTrusted(pd);
      }
    }
  }

  @NonNull
  public HashSet<ProofDrop> getTrusted() {
    synchronized (f_seaLock) {
      return new HashSet<ProofDrop>(f_trusts);
    }
  }

  final Set<ProofDrop> getTrustedReference() {
    synchronized (f_seaLock) {
      return f_trusts;
    }
  }

  public boolean hasTrusted() {
    synchronized (f_seaLock) {
      return !f_trusts.isEmpty();
    }
  }

  /*
   * Consistency proof methods
   */

  @Override
  @MustInvokeOnOverride
  @RequiresLock("SeaLock")
  protected void proofInitialize() {
    // analysis result drops, by definition, can not start off with a red dot
    setProofUsesRedDot(false);
    setDerivedFromSrc(isFromSrc());
    setDerivedFromWarningHint(hasWarningHints());
  }

  @Override
  @RequiresLock("SeaLock")
  protected final void proofAddToWorklistOnChange(Collection<ProofDrop> mutableWorklist) {
    // add all result drops trusted by this result
    mutableWorklist.addAll(getTrustedBy());
    // add all promise drops that this result checks
    mutableWorklist.addAll(getCheckedReference());
    // add all result folder drops that this result is within
    mutableWorklist.addAll(Sea.filterDropsOfType(ResultFolderDrop.class, getDeponentsReference()));
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public final void preprocessRefs(SeaSnapshot s) {
    for (Drop c : getCheckedReference()) {
      s.snapshotDrop(c);
    }
    for (Drop t : getTrustedReference()) {
      s.snapshotDrop(t);
    }
  }

  @Override
  public final void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    for (Drop c : getCheckedReference()) {
      s.refDrop(db, CHECKED_PROMISE, c);
    }
    for (Drop t : getTrustedReference()) {
      s.refDrop(db, TRUSTED_PROOF_DROP, t);
    }
  }
}
