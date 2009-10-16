package edu.cmu.cs.fluid.sea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * The abstract base class for all drops within the sea which reference
 * fAST nodes, intended to be subclassed and extended.
 * 
 * @see Sea
 */
public abstract class IRReferenceDrop extends Drop {

  /**
   * The fAST node that this drop is attached to.
   */
  private IRNode attachedToNode;

  /**
   * The slot info of {@link #attachedToNode} this drop is on.
   */
  private SlotInfo<? extends Drop> attachedToSI;

  /**
   * Allows this drop to understand where it is referenced within the IR so
   * it can remove the reference when it is invalidated.  This is mostly used
   * by {@link PromiseDrop} subtypes.
   * 
   * @param node the node containing <code>slot</code>
   * @param slot the slot info this drop is attached to
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

  /**
   * The fAST node that this PromiseDrop is associated with.
   */
  private IRNode node;

  /**
   * @return the source reference of the fAST node this information
   *   references, can be <code>null</code>
   */
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
   * Sets the fAST node associated with this drop.
   * The fAST node provided should be the {@link IRNode} that the analysis
   * creating the result wishes to focus the user's attention on.
   * 
   * @param node the fAST node this drop is associated with
   * 
   * @see #setNodeAndCompilationUnitDependency(IRNode)
   */
  public final void setNode(IRNode node) {
    this.node = node;
    computeBasedOnAST();
  }
  
  public final void clearNode() {
	node = null;
  }
  
  protected void computeBasedOnAST() {
  }
  
  /**
   * Sets the fAST node associated with this drop and makes this drop
   * dependent upon the compilation unit the given fAST node exists within.
   * The fAST node provided should be the {@link IRNode} that the analysis
   * creating the result wishes to focus the user's attention on.
   * 
   * @param node the fAST node this drop is associated with
   * 
   * @see #setNode(IRNode)
   */
  public final void setNodeAndCompilationUnitDependency(IRNode node) {
    setNode(node);
    dependUponCompilationUnitOf(node);
  }
  
  /** Look in the deponent drops of the current drop to find a CU on which this drop
   * depends.  Expect to find either 0 or 1 such drop. 
   * @return null, if no such drop; the CUDrop if one is found; the first CUDrop 
   * if more than one such drop is present. Note that the returned CUDrop may be 
   * invalid.
   */
  public final CUDrop getCUDeponent() {
    final Collection<? extends CUDrop> cus = 
      Sea.filterDropsOfType(CUDrop.class, getDeponents());
    final int numCUdeponents = cus.size();
    if (numCUdeponents == 0) {
      return null;
    } else if (numCUdeponents > 1) {
      LOG.severe("Drop " + this + "has more than one CU deponent");
    }
    return cus.iterator().next();
  }

  private static List<SupportingInformation> noSupportingInfo = 
	  Collections.emptyList();
  
  /**
   * A set of supporting information about this drop, all elements are of
   *   type {@link edu.cmu.cs.fluid.sea.SupportingInformation}
   */
  private List<SupportingInformation> supportingInformation = null;

  /**
   * Reports an item of supporting information about this drop.  This can be
   * used to add any curio about the drop.
   * 
   * @param message a text message for the user interface
   * @param link an fAST node, can be <code>null</code>, to reference
   */
  public void addSupportingInformation(String message, IRNode link) {
    if (message != null) {
      if (supportingInformation == null) {
        supportingInformation = new ArrayList<SupportingInformation>(1);
      }
      for (SupportingInformation si : supportingInformation) {
        if (message.equals(si.message) && 
            si.location != null && (si.location == link || si.location.equals(link))) {
          LOG.warning("Duplicate supporting information");
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
   * @return the set of supporting information about this drop, all elements
   *   are of type {@link edu.cmu.cs.fluid.sea.SupportingInformation}
   */
  public List<SupportingInformation> getSupportingInformation() {
	if (supportingInformation == null) {
		return noSupportingInfo;
	}
    return supportingInformation;
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
   * @param category The category to set.
   */
  public final void setCategory(Category category) {
    this.category = category;
  }
  
  @Override
  public String getEntityName() {
	  return "ir-drop";
  }	
  
  @Override
  public void snapshotAttrs(SeaSnapshot s) {
	  super.snapshotAttrs(s);
	  if (getCategory() != null) {
		  s.addAttribute("category", getCategory().getKey());
	  }
  }
  
  @Override
  public void snapshotRefs(SeaSnapshot s) {
	  super.snapshotRefs(s);
	  s.addSrcRef(getNode(), getSrcRef());
	  for(SupportingInformation si : getSupportingInformation()) {
		  s.addSupportingInfo(si);
	  }
  }
}