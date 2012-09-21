package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROPOSED_PROMISE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.ISupportingInformation;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.dropsea.irfree.SeaSummary;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;

/**
 * The abstract base class for all drops within the sea which reference fAST
 * nodes, intended to be subclassed and extended.
 * 
 * @see Sea
 */
public abstract class IRReferenceDrop extends Drop {

  /**
   * Constructs a drop referencing the passed node. The {@link IRNode} passed
   * must be non-null.
   * 
   * @param node
   *          a non-null node related to this drop.
   */
  protected IRReferenceDrop(final IRNode node) {
    if (node == null)
      throw new IllegalArgumentException(I18N.err(44, "node"));
    f_node = node;
  }

  /**
   * The non-null fAST node that this PromiseDrop is associated with.
   */
  @NonNull
  private final IRNode f_node;

  /**
   * Gets the source reference of this drop.
   * 
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  @Override
  public ISrcRef getSrcRef() {
    if (f_node != null) {
      ISrcRef ref = JavaNode.getSrcRef(f_node);
      if (ref == null) {
        final IRNode parent = JavaPromise.getParentOrPromisedFor(f_node);
        return JavaNode.getSrcRef(parent);
      }
      return ref;
    }
    return null;
  }

  /**
   * Gets the fAST node associated with this drop.
   * 
   * @return a fAST node
   */
  public final IRNode getNode() {
    return f_node;
  }
  

  public final void addSupportingInformation(IRNode link, int num, Object... args) {
    if (link == null)
      link = getNode();
    final AnalysisHintDrop info = AnalysisHintDrop.newInformation(link);
    info.setMessage(num, args);
    addDependent(info);
  }
  
  public final void addSupportingInformation(IRNode link, String msg) {
    if (link == null)
      link = getNode();
    final AnalysisHintDrop info = AnalysisHintDrop.newInformation(link);
    info.setMessage(msg);
    addDependent(info);
  }

  /**
   * Holds the set of promises proposed by this drop.
   */
  @InRegion("DropState")
  @UniqueInRegion("DropState")
  private List<ProposedPromiseDrop> f_proposals = null;

  /**
   * Adds a proposed promise to this drop. Typically this is done to
   * {@link ResultDrop}s.
   * 
   * @param proposal
   *          the proposed promise.
   */
  public final void addProposal(ProposedPromiseDrop proposal) {
    if (proposal != null) {
      synchronized (f_seaLock) {
        if (f_proposals == null) {
          f_proposals = new ArrayList<ProposedPromiseDrop>(1);
        }
        f_proposals.add(proposal);
      }
    }
  }

  /**
   * Gets the set of proposed promises for this drop. The returned list may not
   * be modified.
   * 
   * @return the, possibly empty but non-null, set of proposed promises for this
   *         drop. The returned list may not be modified.
   */
  public final List<ProposedPromiseDrop> getProposals() {
    synchronized (f_seaLock) {
      if (f_proposals == null)
        return Collections.emptyList();
      else
        return Collections.unmodifiableList(f_proposals);
    }
  }

  @Override
  @RequiresLock("SeaLock")
  protected JavaSourceReference createSourceRef() {
    return DropSeaUtility.createJavaSourceReferenceFromOneOrTheOther(getNode(), getSrcRef());
  }

  @Override
  public final Long getTreeHash() {
    return SeaSummary.computeHash(getNode(), false);
  }

  @Override
  public final Long getContextHash() {
    return SeaSummary.computeContext(getNode(), false);
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  @MustInvokeOnOverride
  public void preprocessRefs(SeaSnapshot s) {
    super.preprocessRefs(s);
    for (ProposedPromiseDrop pd : getProposals()) {
      s.snapshotDrop(pd);
    }
  }

  @MustInvokeOnOverride
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    try {
      s.addSrcRef(db, getNode(), getSrcRef());
    } catch (SlotUndefinedException e) {
      SLLogger.getLogger().log(Level.WARNING, "Undefined info for " + getMessage() + " on " + getNode(), e);
      throw e;
    }
    for (ISupportingInformation si : getSupportingInformation()) {
      s.addSupportingInfo(db, si);
    }
    for (ProposedPromiseDrop pd : getProposals()) {
      s.refDrop(db, PROPOSED_PROMISE, pd);
    }
  }
}