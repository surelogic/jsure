package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CHECKED_PROMISE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.TRUSTED_PROOF_DROP;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.USED_BY_PROOF;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.dropsea.irfree.XmlCreator;
import com.surelogic.dropsea.irfree.XmlCreator.Builder;

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
	synchronized (f_seaLock) {
		return !f_checks.isEmpty();
	}
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

  /**
   * Gets if this result, directly or indirectly, checks a promise.
   * 
   * @return {@code true} if this result checks a promise, {@code false}
   *         otherwise.
   */
  public boolean checksAPromise() {
    return checksAPromiseHelper(this);
  }

  /**
   * Recursive helper call to determine if the passed result, directly or
   * indirectly, checks a promise.
   * 
   * @param result
   *          a result.
   * @return {@code true} if the passed result checks a promise, {@code false}
   *         otherwise.
   */
  private boolean checksAPromiseHelper(final AnalysisResultDrop result) {
    // directly checks a promise
    if (result.hasChecked())
      return true;
    // indirectly checks a promise
    for (final AnalysisResultDrop trustedByResult : result.getTrustedBy())
      if (checksAPromiseHelper(trustedByResult))
        return true;
    // doesn't check a promise
    return false;
  }

  @InRegion("DropState")
  boolean f_usedByProof = true;

  public boolean usedByProof() {
    synchronized (f_seaLock) {
      return f_usedByProof;
    }
  }

  @InRegion("DropState")
  boolean f_useImmediateConsistencyResultForMessage = true;

  /**
   * Gets if this drop uses the immediate consistency result to decide if the
   * <i>consistent</i> message or the <i>not proved consistent</i> should be
   * used.
   * 
   * @return {@code true} if this drop uses the immediate consistency result to
   *         decide if the <i>consistent</i> message or the <i>not proved
   *         consistent</i> should be used, {@code false} if the verification
   *         proof result is used.
   */
  public boolean useImmediateConsistencyResultForMessage() {
    synchronized (f_seaLock) {
      return f_useImmediateConsistencyResultForMessage;
    }
  }

  /**
   * Sets if this drop uses the immediate consistency result to decide if the
   * <i>consistent</i> message or the <i>not proved consistent</i> should be
   * used. immediately consistent
   * 
   * @param value
   *          {@code true} if this drop uses the immediate consistency result to
   *          decide if the <i>consistent</i> message or the <i>not proved
   *          consistent</i> should be used, {@code false} if the verification
   *          proof result is used.
   */
  public void setUseImmediateConsistencyResultForMessage(boolean value) {
    synchronized (f_seaLock) {
      f_useImmediateConsistencyResultForMessage = value;
    }
  }

  /*
   * Consistency proof methods
   */

  @Override
  @MustInvokeOnOverride
  @RequiresLock("SeaLock")
  void proofInitialize() {
    // analysis result drops, by definition, can not start off with a red dot
    f_proofUsesRedDot = false;
    f_derivedFromSrc = isFromSrc();
    f_derivedFromWarningHint = hasWarningHints();
    f_usedByProof = hasChecked();
  }

  /**
   * Considers each trusted analysis result to determine if it is used by the
   * verification proof.
   * 
   * @return {@code true} if something changes, {@code false} otherwise.
   */
  @RequiresLock("SeaLock")
  final boolean proofTransferUsedBy() {
    boolean changed = false; // assume the best

    for (ProofDrop proofDrop : getTrusted()) {
      if (proofDrop instanceof AnalysisResultDrop) {
        changed |= proofTransferUsedByProofToTrustedResult((AnalysisResultDrop) proofDrop);
      }
    }

    return changed;
  }

  /**
   * Handles the used by proof flag for result drops and result folder drops.
   * 
   * @param trusted
   *          a trusted result drop or result folder drop.
   * @return {@code true} if something changes, {@code false} otherwise.
   */
  @RequiresLock("SeaLock")
  abstract boolean proofTransferUsedByProofToTrustedResult(@NonNull AnalysisResultDrop trusted);

  /**
   * Depending upon the value of
   * {@link #f_useImmediateConsistencyResultForMessage} either
   * {@link #immediatelyConsistent()} or {@link #provedConsistent()} is used to
   * decide if the <i>consistent</i> message or the <i>not proved consistent</i>
   * should be set for this drop.
   */
  @RequiresLock("SeaLock")
  @Override
  final void proofFinalize() {
    boolean useConsistentMessage = f_useImmediateConsistencyResultForMessage ? immediatelyConsistent() : provedConsistent();
    if (useConsistentMessage) {
      if (f_messageConsistent != null)
        setMessageHelper(f_messageConsistent, f_messageConsistentCanonical);
    } else {
      if (f_messageInconsistent != null)
        setMessageHelper(f_messageInconsistent, f_messageInconsistentCanonical);
    }
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

  @MustInvokeOnOverride
  @Override
  @RequiresLock("SeaLock")
  public void snapshotAttrs(XmlCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(USED_BY_PROOF, usedByProof());
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
