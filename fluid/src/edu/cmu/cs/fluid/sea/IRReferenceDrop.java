package edu.cmu.cs.fluid.sea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
//import edu.cmu.cs.fluid.java.bind.AbstractJavaBinder;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.xml.*;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;

/**
 * The abstract base class for all drops within the sea which reference fAST
 * nodes, intended to be subclassed and extended.
 * 
 * @see Sea
 */
public abstract class IRReferenceDrop extends Drop {
  public static final String PROPOSED_PROMISE = "proposed-promise";

  /**
   * The fAST node that this drop is attached to.
   */
  private IRNode attachedToNode;

  /**
   * The slot info of {@link #attachedToNode} this drop is on.
   */
  private SlotInfo<? extends Drop> attachedToSI;

  /**
   * Allows this drop to understand where it is referenced within the IR so it
   * can remove the reference when it is invalidated. This is mostly used by
   * {@link PromiseDrop} subtypes.
   * 
   * @param node
   *          the node containing <code>slot</code>
   * @param slot
   *          the slot info this drop is attached to
   * 
   * @see #deponentInvalidAction(Drop)
   */
  public final void setAttachedTo(IRNode node, SlotInfo<? extends Drop> slot) {
    attachedToNode = node;
    attachedToSI = slot;
  }

  /**
   * We override to remove the reference (if any) within the IR to this drop.
   * 
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction(Drop)
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    super.deponentInvalidAction(invalidDeponent);
    // FIX this does the wrong thing if it's a sequence
    // Also doesn't reclaim storage
    if (attachedToNode != null && attachedToSI != null) {
      attachedToNode.setSlotValue(attachedToSI, null);
      attachedToNode = null;
      attachedToSI = null;
    }
  }

  /*
   * &
   * 
   * @Override protected void invalidate_internal() { if (getNode() != null) {
   * if (!AbstractJavaBinder.isBinary(getNode())) {
   * System.err.println("Invalidating "+getMessage()); } } else {
   * System.err.println("Invalidating "+getMessage()); } }
   */

  /**
   * The fAST node that this PromiseDrop is associated with.
   */
  private IRNode node;
  /**
   * Used for dependency checking
   */
  private IRNode lastNonNullNode;

  /**
   * Gets the source reference of this drop.
   * 
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  @Override
  public ISrcRef getSrcRef() {
    if (node != null) {
      ISrcRef ref = JavaNode.getSrcRef(node);
      if (ref == null) {
        IRNode parent = JavaPromise.getParentOrPromisedFor(node);
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
    return node;
  }

  /**
   * Sets the fAST node associated with this drop. The fAST node provided should
   * be the {@link IRNode} that the analysis creating the result wishes to focus
   * the user's attention on.
   * 
   * @param node
   *          the fAST node this drop is associated with
   * 
   * @see #setNodeAndCompilationUnitDependency(IRNode)
   */
  public final void setNode(IRNode node) {
    if (node == null) {
      throw new IllegalArgumentException("Use clearNode()");
    }
    this.lastNonNullNode = node;
    this.node = node;
    if (node != null) {
      computeBasedOnAST();
    }
  }

  public/* final */void clearNode() {
    // lastNonNullNode is not cleared
    node = null;
  }

  /**
   * Used for dependency checking
   */
  public final IRNode getLastNonnullNode() {
    return lastNonNullNode;
  }

  protected void computeBasedOnAST() {
    // behavior extended by subclasses
  }

  /**
   * Sets the fAST node associated with this drop and makes this drop dependent
   * upon the compilation unit the given fAST node exists within. The fAST node
   * provided should be the {@link IRNode} that the analysis creating the result
   * wishes to focus the user's attention on.
   * 
   * @param node
   *          the fAST node this drop is associated with
   * 
   * @see #setNode(IRNode)
   */
  public final void setNodeAndCompilationUnitDependency(IRNode node) {
    setNode(node);
    dependUponCompilationUnitOf(node);
  }

  /**
   * Look in the deponent drops of the current drop to find a CU on which this
   * drop depends. Expect to find either 0 or 1 such drop.
   * 
   * @return null, if no such drop; the CUDrop if one is found; the first CUDrop
   *         if more than one such drop is present. Note that the returned
   *         CUDrop may be invalid.
   */
  public final CUDrop getCUDeponent() {
    final Collection<? extends CUDrop> cus = Sea.filterDropsOfType(CUDrop.class, getDeponents());
    final int numCUdeponents = cus.size();
    if (numCUdeponents == 0) {
      return null;
    } else if (numCUdeponents > 1) {
      LOG.severe("Drop " + this + "has more than one CU deponent");
    }
    return cus.iterator().next();
  }

  /**
   * A set of supporting information about this drop, all elements are of type
   * {@link edu.cmu.cs.fluid.sea.ISupportingInformation}
   */
  private List<ISupportingInformation> supportingInformation = null;

  /**
   * Reports an item of supporting information about this drop. This can be used
   * to add any curio about the drop.
   * 
   * @param link
   *          an fAST node, can be <code>null</code>, to reference
   * @param num
   *          The message number for the user interface
   */
  public void addSupportingInformation(IRNode link, int num, Object... args) {
    if (num >= 0) {
      if (supportingInformation == null) {
        supportingInformation = new ArrayList<ISupportingInformation>(1);
      }
      for (ISupportingInformation si : supportingInformation) {
        if (si.sameAs(link, num, args)) {
          LOG.fine("Duplicate supporting information");
          return;
        }
      }
      ISupportingInformation info = new SupportingInformation2(link, num, args);
      supportingInformation.add(info);
    }
  }

  /**
   * Reports an item of supporting information about this drop. This can be used
   * to add any curio about the drop.
   * 
   * @param message
   *          a text message for the user interface
   * @param link
   *          an fAST node, can be <code>null</code>, to reference
   */
  public void addSupportingInformation(String message, IRNode link) {
    if (message != null) {
      if (supportingInformation == null) {
        supportingInformation = new ArrayList<ISupportingInformation>(1);
      }
      for (ISupportingInformation si : supportingInformation) {
        if (si.sameAs(link, message)) {
          LOG.fine("Duplicate supporting information");
          return;
        }
      }
      SupportingInformation info = new SupportingInformation();
      info.location = link;
      info.message = message;
      supportingInformation.add(info);
    }
  }

  /**
   * @return the set of supporting information about this drop, all elements are
   *         of type {@link edu.cmu.cs.fluid.sea.SupportingInformation}
   */
  public List<ISupportingInformation> getSupportingInformation() {
    if (supportingInformation == null) {
      return Collections.emptyList();
    }
    return supportingInformation;
  }

  /**
   * Holds the set of promises proposed by this drop.
   */
  private List<ProposedPromiseDrop> proposals = null;

  /**
   * Adds a proposed promise to this drop. Typically this is done to
   * {@link ResultDrop}s.
   * 
   * @param proposal
   *          the proposed promise.
   */
  public void addProposal(ProposedPromiseDrop proposal) {
    if (proposal != null) {
      if (proposals == null) {
        proposals = new ArrayList<ProposedPromiseDrop>(1);
      }
      proposals.add(proposal);
    }
  }

  /**
   * Gets the set of proposed promises for this drop.
   * 
   * @return the, possibly empty but non-null, set of proposed promises for this
   *         drop.
   */
  public List<ProposedPromiseDrop> getProposals() {
    if (proposals == null) {
      return Collections.emptyList();
    }
    return proposals;
  }

  /**
   * A user interface reporting category for this drop.
   * 
   * @see Category
   */
  private Category category = null;

  /**
   * @return Returns the category.
   */
  public final Category getCategory() {
    return category;
  }

  /**
   * @param category
   *          The category to set.
   */
  @Override
  public final void setCategory(Category category) {
    this.category = category;
  }

  @Override
  public String getEntityName() {
    return "ir-drop";
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    if (getCategory() != null) {
      s.addAttribute(CATEGORY_ATTR, getCategory().getKey());
    }
  }

  @Override
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

  @Override
  protected JavaSourceReference createSourceRef() {
    return createSourceRef(getNode(), getSrcRef());
  }

  public static JavaSourceReference createSourceRef(IRNode n, ISrcRef ref) {
    if (ref == null) {
      if (n == null) {
        return null;
      }
      IRNode cu = VisitUtil.getEnclosingCUorHere(n);
      String pkg = VisitUtil.getPackageName(cu);
      IRNode type = VisitUtil.getPrimaryType(cu);
      return new JavaSourceReference(pkg, JavaNames.getTypeName(type));
    }
    return new JavaSourceReference(ref.getPackage(), ref.getCUName(), ref.getLineNumber(), ref.getOffset());
  }

  @Override
  public final Long getTreeHash() {
    return SeaSummary.computeHash(getNode(), false);
  }

  @Override
  public final Long getContextHash() {
    return SeaSummary.computeContext(getNode(), false);
  }
}