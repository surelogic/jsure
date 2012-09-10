package edu.cmu.cs.fluid.sea;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.InRegion;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
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
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;

//import edu.cmu.cs.fluid.java.bind.AbstractJavaBinder;

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
  @InRegion("DropState")
  private IRNode f_attachedToNode;

  /**
   * The slot info of {@link #f_attachedToNode} this drop is on.
   */
  @InRegion("DropState")
  private SlotInfo<? extends Drop> f_attachedToSI;

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
    synchronized (f_seaLock) {
      f_attachedToNode = node;
      f_attachedToSI = slot;
    }
  }

  /**
   * We override to remove the reference (if any) within the IR to this drop.
   * 
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction(Drop)
   */
  @Override
  @RequiresLock("SeaLock")
  protected void deponentInvalidAction(Drop invalidDeponent) {
    super.deponentInvalidAction(invalidDeponent);
    // FIX this does the wrong thing if it's a sequence
    // Also doesn't reclaim storage
    if (f_attachedToNode != null && f_attachedToSI != null) {
      f_attachedToNode.setSlotValue(f_attachedToSI, null);
      f_attachedToNode = null;
      f_attachedToSI = null;
    }
  }

  /**
   * The fAST node that this PromiseDrop is associated with.
   */
  @InRegion("DropState")
  private IRNode f_node;

  /**
   * Used for dependency checking
   */
  @InRegion("DropState")
  private IRNode f_lastNonNullNode;

  /**
   * Gets the source reference of this drop.
   * 
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  @Override
  public ISrcRef getSrcRef() {
    final IRNode node;
    synchronized (f_seaLock) {
      node = f_node;
    }
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
    synchronized (f_seaLock) {
      return f_node;
    }
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
    synchronized (f_seaLock) {
      f_lastNonNullNode = node;
      f_node = node;
      if (node != null) {
        computeBasedOnAST();
      }
    }
  }

  public void clearNode() {
    // lastNonNullNode is not cleared
    synchronized (f_seaLock) {
      f_node = null;
    }
  }

  /**
   * Used for dependency checking
   */
  public final IRNode getLastNonnullNode() {
    synchronized (f_seaLock) {
      return f_lastNonNullNode;
    }
  }

  @RequiresLock("SeaLock")
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
    synchronized (f_seaLock) {
      setNode(node);
      dependUponCompilationUnitOf(node);
    }
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
    final ArrayList<CUDrop> cus;
    synchronized (f_seaLock) {
      cus = Sea.filterDropsOfType(CUDrop.class, getDeponentsReference());
    }
    if (cus.size() < 1) {
      return null;
    } else if (cus.size() > 1) {
      LOG.severe("Drop " + this + "has more than one CU deponent");
    }
    return cus.get(0);
  }

  /**
   * A set of supporting information about this drop, all elements are of type
   * {@link edu.cmu.cs.fluid.sea.ISupportingInformation}
   */
  @InRegion("DropState")
  @UniqueInRegion("DropState")
  private List<ISupportingInformation> f_supportingInformation = null;

  /**
   * Reports an item of supporting information about this drop. This can be used
   * to add any curio about the drop.
   * 
   * @param link
   *          an fAST node, can be <code>null</code>, to reference
   * @param num
   *          The message number for the user interface
   */
  public final void addSupportingInformation(IRNode link, int num, Object... args) {
    if (num >= 0) {
      synchronized (f_seaLock) {
        if (f_supportingInformation == null) {
          f_supportingInformation = new ArrayList<ISupportingInformation>(1);
        }
        for (ISupportingInformation si : f_supportingInformation) {
          if (si.sameAs(link, num, args)) {
            LOG.fine("Duplicate supporting information");
            return;
          }
        }
        ISupportingInformation info = new SupportingInformationViaAnalysisResultMessage(link, num, args);
        f_supportingInformation.add(info);
      }
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
  public final void addSupportingInformation(String message, IRNode link) {
    if (message != null) {
      synchronized (f_seaLock) {
        if (f_supportingInformation == null) {
          f_supportingInformation = new ArrayList<ISupportingInformation>(1);
        }
        for (ISupportingInformation si : f_supportingInformation) {
          if (si.sameAs(link, message)) {
            LOG.fine("Duplicate supporting information");
            return;
          }
        }
        SupportingInformationViaString info = new SupportingInformationViaString();
        info.location = link;
        info.message = message;
        f_supportingInformation.add(info);
      }
    }
  }

  /**
   * Gets the supporting information about this drop. The returned list may not
   * be modified.
   * 
   * @return the list of supporting information about this drop. The returned
   *         list may not be modified.
   */
  public final List<ISupportingInformation> getSupportingInformation() {
    synchronized (f_seaLock) {
      if (f_supportingInformation == null)
        return Collections.emptyList();
      else
        return Collections.unmodifiableList(f_supportingInformation);
    }
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

  /**
   * A user interface reporting category for this drop.
   * 
   * @see Category
   */
  @InRegion("DropState")
  private Category f_category = null;

  /**
   * Gets the user interface reporting category for this drop.
   * 
   * @return a category, or {@code null} if none is set.
   */
  public final Category getCategory() {
    synchronized (f_seaLock) {
      return f_category;
    }
  }

  /**
   * Sets the user interface reporting category for this drop.
   * 
   * @param category
   *          a category to set, or {@code null} to clear the category.
   */
  @Override
  public final void setCategory(Category category) {
    synchronized (f_seaLock) {
      f_category = category;
    }
  }

  @Override
  @RequiresLock("SeaLock")
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

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
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
}