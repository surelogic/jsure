/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ColorPromisesSupport.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.annotation.rules;

import static com.surelogic.annotation.rules.ColorRules.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorCtxSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorDeclareDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorGrantDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorImportDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorIncompatibleDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorReqSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorRequireDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorRevokeDrop;
import edu.cmu.cs.fluid.sea.drops.colors.RegionColorDeclDrop;

public class ColorPromisesSupport extends AbstractPromiseAnnotation {
  public static ColorPromisesSupport INSTANCE = new ColorPromisesSupport();
  
  private ColorPromisesSupport() {
    // do nothing
  }
  
  private static SlotInfo<Set<ColorImportDrop>> importDropSetSI = SimpleSlotFactory.prototype.newAttribute(null);

  //private static SlotInfo renameDropSI = SimpleSlotFactory.prototype.newAttribute(null);
  

  private static SlotInfo<ColorCtxSummaryDrop> ctxInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);


  private static SlotInfo<Set<ColorDeclareDrop>> declDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<Set<ColorGrantDrop>> grantDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<Set<ColorRevokeDrop>> revokeDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<Set<ColorIncompatibleDrop>> incompDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<RegionColorDeclDrop>> regionColorDeclDropSetSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  private static SlotInfo<Set<ColorRequireDrop>> reqDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  

  private static SlotInfo<Set<ColorRequireDrop>> reqInheritDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> reqSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<ColorCtxSummaryDrop> ctxSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> reqInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);

private static SlotInfo<Boolean> colorImportsProcessedSI =
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);

  private static SlotInfo<Boolean> colorRenamesProcessedSI =
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);
  
  private static SlotInfo<Boolean> colorStructureBuiltSI = 
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);


  public static boolean areColorImportsProcessed(IRNode node) {
    return isXorFalse_filtered(colorImportsProcessedSI, node);
  }
  
  public static void setColorImportsProcessed(IRNode node, boolean processed) {
    setX_mapped(colorImportsProcessedSI, node, processed);
  }
  
  public static boolean areColorRenamesProcessed(IRNode node) {
    return isXorFalse_filtered(colorRenamesProcessedSI, node);
  }
  
  public static void setColorRenamesProcessed(IRNode node, boolean processed) {
    setX_mapped(colorRenamesProcessedSI, node, processed);
  }
  
  public static boolean isColorStructureBuilt(IRNode node) {
    return isXorFalse_filtered(colorStructureBuiltSI, node);
  }
  
  public static void setColorStructureBuilt(IRNode node, boolean processed) {
    setX_mapped(colorStructureBuiltSI, node, processed);
  }
  
  private static <T extends Drop> Set<T> getMutableSet(IRNode forNode, SlotInfo<Set<T>> si) {
    Set<T> result = forNode.getSlotValue(si);
    if (result == null) {
      result = new HashSet<T>();
      forNode.setSlotValue(si, result);
    }
    return result;
  }
  
  private static <T extends Drop> Set<T> getCopyOfMutableSet(IRNode forNode, SlotInfo<Set<T>> si) {
    Set<T> current = getMutableSet(forNode, si);
    if (current.size() == 0) {
      return new HashSet<T>(0);
    }
    Set<T> result = new HashSet<T>(current.size());
    Iterator<T> currIter = current.iterator();
    while (currIter.hasNext()) {
      T dr = currIter.next();
      if (dr.isValid()) {
        result.add(dr);
      }
    }
    if (result.size() < current.size()) {
      // we must have skipped over some invalid entries.  update the saved
      // set.
      current = new HashSet<T>(result.size());
      current.addAll(result);
      forNode.setSlotValue(si, current);
    }
    return result;
  }
  
  
  /** Remove all invalid drops from a MutableXXXSet.  Do this by building a new set
   * and transferring only valid drops from old to newSet.  Finish by installing
   * the new set as the mutableXXXSet for node.
   * 
   * @param node the node whose set should be updated
   * @param si the SlotInfo to get the set from.
   */
  private static <T extends Drop> void purgeMutableSet(IRNode node, SlotInfo<Set<T>> si) {
    Set<T> old = getMutableSet(node, si);
    final int newSize = Math.max(old.size()-1, 0);
    Set<T> newSet = new HashSet<T>(newSize);
    Iterator<T> oldIter = old.iterator();
    while (oldIter.hasNext()) {
      T dr = oldIter.next();
      if (dr.isValid()) {
        newSet.add(dr);
      }
    }
    node.setSlotValue(si, newSet);
  }

  
  public static Set<ColorDeclareDrop> getMutableColorDeclSet(IRNode forNode) {
    return getMutableSet(forNode, declDropSetSI);
  }

  public static Set<RegionColorDeclDrop> getMutableRegionColorDeclsSet(IRNode forNode) {
    return getMutableSet(forNode, regionColorDeclDropSetSI);
  }
 
  public static Set<ColorRequireDrop> getMutableInheritedRequiresSet(IRNode forNode) {
    return getMutableSet(forNode, reqInheritDropSetSI);
  }
  public static Set<ColorGrantDrop> getMutableColorGrantSet(IRNode node) {
    return getMutableSet(node, grantDropSetSI);
  }
  public static Set<ColorRevokeDrop> getMutableColorRevokeSet(IRNode node) {
    return getMutableSet(node, revokeDropSetSI);
  }
  public static Collection<ColorRequireDrop> getInheritedRequireDrops(IRNode node) {
    return getCopyOfMutableSet(node, reqInheritDropSetSI);
  }
  
  public static ColorReqSummaryDrop getReqSummDrop(IRNode node) {
    return node.getSlotValue(reqSummDropSI);
  }
  
  public static void setReqSummDrop(IRNode node, ColorReqSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(reqSummDropSI, summ);
    summ.setAttachedTo(node, reqSummDropSI);
  }
  
  public static ColorReqSummaryDrop getInheritedReqSummDrop(IRNode node) {
    return node.getSlotValue(reqInheritSummDropSI);
  }
  
  public static void setInheritedReqSummDrop(IRNode node, ColorReqSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(reqInheritSummDropSI, summ);
    summ.setAttachedTo(node, reqInheritSummDropSI);
  }

  public static ColorCtxSummaryDrop getCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxSummDropSI);
  }
  
  public static void setCtxSummDrop(IRNode node, ColorCtxSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(ctxSummDropSI, summ);
    summ.setAttachedTo(node, ctxSummDropSI);
  }
  
  public static ColorCtxSummaryDrop getInheritedCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxInheritSummDropSI);
  }
  
  public static void setInheritedCtxSummDrop(IRNode node, ColorCtxSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(ctxInheritSummDropSI, summ);
    summ.setAttachedTo(node, ctxInheritSummDropSI);
  }
  
  
  public static void addInheritedRequireDrop(IRNode toThisNode, ColorRequireDrop crDrop) {
    if (!colorDropsEnabled) return;
    getMutableInheritedRequiresSet(toThisNode).add(crDrop);
  }


  @Override
  protected IPromiseRule[] getRules() {
    
    return new IPromiseRule[] {};
  }
  
  // Support for Module promises that have no AAST.


}
