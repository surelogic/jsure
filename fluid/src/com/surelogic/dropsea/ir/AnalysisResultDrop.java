package com.surelogic.dropsea.ir;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IAnalysisResultDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

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

  /*
   * XML attribute constants
   */
  public static final String CHECKED_PROMISE = "checked-promise";

  protected AnalysisResultDrop(IRNode node) {
    super(node);
  }

  /**
   * The set of promise drops being checked, or established, by this result.
   */
  @UniqueInRegion("DropState")
  private final Set<PromiseDrop<? extends IAASTRootNode>> f_checks = new HashSet<PromiseDrop<? extends IAASTRootNode>>();

  /**
   * Gets the set of promise drops established, or checked, by this result. The
   * returned set is a copy.
   * 
   * @return the non-null (possibly empty) set of promise drops established, or
   *         checked, by this result.
   */
  public final HashSet<? extends PromiseDrop<? extends IAASTRootNode>> getChecks() {
    synchronized (f_seaLock) {
      return new HashSet<PromiseDrop<? extends IAASTRootNode>>(f_checks);
    }
  }

  final Set<? extends PromiseDrop<? extends IAASTRootNode>> getChecksReference() {
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
  public final void addCheckedPromise(PromiseDrop<? extends IAASTRootNode> promise) {
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
  public final void addCheckedPromises(Collection<? extends PromiseDrop<? extends IAASTRootNode>> promises) {
    if (promises == null)
      return;

    synchronized (f_seaLock) {
      for (PromiseDrop<? extends IAASTRootNode> promise : promises) {
        addCheckedPromise(promise);
      }
    }
  }

  @Override
  @MustInvokeOnOverride
  @RequiresLock("SeaLock")
  void proofInitialize() {
    // analysis result drops, by definition, can not start off with a red dot
    setProofUsesRedDot(false);

    setDerivedFromSrc(isFromSrc());
  }

  @Override
  @RequiresLock("SeaLock")
  final void proofAddToWorklistOnChange(Collection<ProofDrop> mutableWorklist) {
    // add all result drops trusted by this result
    mutableWorklist.addAll(getTrustedBy());
    // add all promise drops that this result checks
    mutableWorklist.addAll(getChecksReference());
    // add all result folder drops that this result is within
    mutableWorklist.addAll(Sea.filterDropsOfType(ResultFolderDrop.class, getDeponentsReference()));
  }

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  @MustInvokeOnOverride
  public void preprocessRefs(SeaSnapshot s) {
    for (Drop c : getChecksReference()) {
      s.snapshotDrop(c);
    }
  }

  @Override
  @MustInvokeOnOverride
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    for (Drop c : getChecksReference()) {
      s.refDrop(db, CHECKED_PROMISE, c);
    }
  }
}
