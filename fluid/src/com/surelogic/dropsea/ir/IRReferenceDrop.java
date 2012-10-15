package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROPOSED_PROMISE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.InRegion;
import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IHintDrop.HintType;
import com.surelogic.dropsea.irfree.SeaSnapshot;

import edu.cmu.cs.fluid.ir.IRNode;
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

  @Override
  @Nullable
  public IJavaRef getJavaRef() {
    final IJavaRef javaRef = JavaNode.getJavaRef(f_node);
    if (javaRef != null)
      return javaRef;
    final IRNode parent = JavaPromise.getParentOrPromisedFor(f_node);
    return JavaNode.getJavaRef(parent);
  }

  /**
   * Gets the fAST node associated with this drop.
   * 
   * @return a fAST node
   */
  public final IRNode getNode() {
    return f_node;
  }

  public final HintDrop addInformationHint(IRNode link, int num, Object... args) {
    return addHint(HintType.INFORMATION, -1, link, num, args);
  }

  public final HintDrop addInformationHintWithCategory(IRNode link, int catNum, int num, Object... args) {
    return addHint(HintType.INFORMATION, catNum, link, num, args);
  }

  public final HintDrop addInformationHint(IRNode link, String msg) {
    return addHint(HintType.INFORMATION, -1, link, msg);
  }

  public final HintDrop addInformationHintWithCategory(IRNode link, int catNum, String msg) {
    return addHint(HintType.INFORMATION, catNum, link, msg);
  }

  public final HintDrop addWarningHint(IRNode link, int num, Object... args) {
    return addHint(HintType.WARNING, -1, link, num, args);
  }

  public final HintDrop addWarningHintWithCategory(IRNode link, int catNum, int num, Object... args) {
    return addHint(HintType.WARNING, catNum, link, num, args);
  }

  public final HintDrop addWarningHint(IRNode link, String msg) {
    return addHint(HintType.WARNING, -1, link, msg);
  }

  public final HintDrop addWarningHintWithCategory(IRNode link, int catNum, String msg) {
    return addHint(HintType.WARNING, catNum, link, msg);
  }

  private HintDrop addHint(IHintDrop.HintType hintType, int catNum, IRNode link, int num, Object... args) {
    if (link == null)
      link = getNode();
    final HintDrop hint = new HintDrop(link, hintType);
    if (catNum > 0)
      hint.setCategorizingMessage(catNum);
    hint.setMessage(num, args);
    addDependent(hint);
    return hint;
  }

  private HintDrop addHint(IHintDrop.HintType hintType, int catNum, IRNode link, String msg) {
    if (link == null)
      link = getNode();
    final HintDrop hint = new HintDrop(link, hintType);
    if (catNum > 0)
      hint.setCategorizingMessage(catNum);
    hint.setMessage(msg);
    addDependent(hint);
    return hint;
  }

  /**
   * Holds the set of promises proposed by this drop.
   */
  @InRegion("DropState")
  @UniqueInRegion("DropState")
  private List<ProposedPromiseDrop> f_proposals = null;

  /**
   * Asks subtypes if they have any other proposals to add to the set of
   * promises proposed by this drop.
   * <p>
   * It is okay to return a reference to an internal collection because
   * {@link #getProposals()} will copy the elements out of the returned
   * collection and not keep an alias.
   * 
   * @return a possibly empty list of proposed promises.
   */
  @RequiresLock("SeaLock")
  @NonNull
  protected List<ProposedPromiseDrop> getConditionalProposals() {
    return Collections.emptyList();
  }

  /**
   * Adds a proposed promise to this drop for the tool user to consider.
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

  @NonNull
  public final List<ProposedPromiseDrop> getProposals() {
    synchronized (f_seaLock) {
      final List<ProposedPromiseDrop> conditionalProposals = getConditionalProposals();
      if (f_proposals == null && conditionalProposals.isEmpty())
        return Collections.emptyList();
      else {
        final List<ProposedPromiseDrop> result = new ArrayList<ProposedPromiseDrop>();
        if (f_proposals != null)
          result.addAll(f_proposals);
        result.addAll(conditionalProposals);
        return result;
      }
    }
  }

  @Override
  public final Long getTreeHash() {
    return SeaSnapshot.computeHash(getNode(), false);
  }

  @Override
  public final Long getContextHash() {
    return SeaSnapshot.computeContext(getNode(), false);
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
    for (ProposedPromiseDrop pd : getProposals()) {
      s.refDrop(db, PROPOSED_PROMISE, pd);
    }
  }
}