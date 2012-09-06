/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/TRolePromisesSupport.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.annotation.rules;

import static com.surelogic.annotation.rules.ThreadRoleRules.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.threadroles.RegionTRoleDeclDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleCtxSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleDeclareDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleGrantDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleImportDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleIncompatibleDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleReqSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRequireDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRevokeDrop;

@Deprecated
public class TRolePromisesSupport extends AnnotationRules {
  public static TRolePromisesSupport INSTANCE = new TRolePromisesSupport();
  
  private TRolePromisesSupport() {
    // do nothing
  }
  
  static SlotInfo<Set<TRoleImportDrop>> importDropSetSI = SimpleSlotFactory.prototype.newAttribute(null);

  //private static SlotInfo renameDropSI = SimpleSlotFactory.prototype.newAttribute(null);
  

  private static SlotInfo<TRoleCtxSummaryDrop> ctxInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);


  private static SlotInfo<Set<TRoleDeclareDrop>> declDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<Set<TRoleGrantDrop>> grantDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<Set<TRoleRevokeDrop>> revokeDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  static SlotInfo<Set<TRoleIncompatibleDrop>> incompDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<RegionTRoleDeclDrop>> regionTRoleDeclDropSetSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  static SlotInfo<Set<TRoleRequireDrop>> reqDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  

  private static SlotInfo<Set<TRoleRequireDrop>> reqInheritDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<TRoleReqSummaryDrop> reqSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<TRoleCtxSummaryDrop> ctxSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<TRoleReqSummaryDrop> reqInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);

private static SlotInfo<Boolean> tRoleImportsProcessedSI =
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);

  private static SlotInfo<Boolean> tRoleRenamesProcessedSI =
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);
  
  private static SlotInfo<Boolean> tRoleStructureBuiltSI = 
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);


  public static boolean areTRoleImportsProcessed(IRNode node) {
    return isXorFalse_filtered(tRoleImportsProcessedSI, node);
  }

  public static void setTRoleImportsProcessed(IRNode node, boolean processed) {
    setX_mapped(tRoleImportsProcessedSI, node, processed);
  }

  public static boolean areTRoleRenamesProcessed(IRNode node) {
    return isXorFalse_filtered(tRoleRenamesProcessedSI, node);
  }
  
  public static void setTRoleRenamesProcessed(IRNode node, boolean processed) {
    setX_mapped(tRoleRenamesProcessedSI, node, processed);
  }
  
  public static boolean isTRoleStructureBuilt(IRNode node) {
    return isXorFalse_filtered(tRoleStructureBuiltSI, node);
  }
  
  public static void setTRoleStructureBuilt(IRNode node, boolean processed) {
    setX_mapped(tRoleStructureBuiltSI, node, processed);
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
  static <T extends Drop> void purgeMutableSet(IRNode node, SlotInfo<Set<T>> si) {
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

  
  public static Set<TRoleDeclareDrop> getMutableTRoleDeclSet(IRNode forNode) {
    return getMutableSet(forNode, declDropSetSI);
  }

  public static Set<RegionTRoleDeclDrop> getMutableRegionTRoleDeclsSet(IRNode forNode) {
    return getMutableSet(forNode, regionTRoleDeclDropSetSI);
  }
 
  public static Set<TRoleRequireDrop> getMutableInheritedRequiresSet(IRNode forNode) {
    return getMutableSet(forNode, reqInheritDropSetSI);
  }
  public static Set<TRoleGrantDrop> getMutableTRoleGrantSet(IRNode node) {
    return getMutableSet(node, grantDropSetSI);
  }
  public static Set<TRoleRevokeDrop> getMutableTRoleRevokeSet(IRNode node) {
    return getMutableSet(node, revokeDropSetSI);
  }
  public static Collection<TRoleRequireDrop> getInheritedRequireDrops(IRNode node) {
    return getCopyOfMutableSet(node, reqInheritDropSetSI);
  }
  
  public static TRoleReqSummaryDrop getReqSummDrop(IRNode node) {
    return node.getSlotValue(reqSummDropSI);
  }
  
  public static void setReqSummDrop(IRNode node, TRoleReqSummaryDrop summ) {
    if (!tRoleDropsEnabled) return;
    node.setSlotValue(reqSummDropSI, summ);
    summ.setAttachedTo(node, reqSummDropSI);
  }
  
  public static TRoleReqSummaryDrop getInheritedReqSummDrop(IRNode node) {
    return node.getSlotValue(reqInheritSummDropSI);
  }
  
  public static void setInheritedReqSummDrop(IRNode node, TRoleReqSummaryDrop summ) {
    if (!tRoleDropsEnabled) return;
    node.setSlotValue(reqInheritSummDropSI, summ);
    summ.setAttachedTo(node, reqInheritSummDropSI);
  }

  public static TRoleCtxSummaryDrop getCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxSummDropSI);
  }
  
  public static void setCtxSummDrop(IRNode node, TRoleCtxSummaryDrop summ) {
    if (!tRoleDropsEnabled) return;
    node.setSlotValue(ctxSummDropSI, summ);
    summ.setAttachedTo(node, ctxSummDropSI);
  }
  
  public static TRoleCtxSummaryDrop getInheritedCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxInheritSummDropSI);
  }
  
  public static void setInheritedCtxSummDrop(IRNode node, TRoleCtxSummaryDrop summ) {
    if (!tRoleDropsEnabled) return;
    node.setSlotValue(ctxInheritSummDropSI, summ);
    summ.setAttachedTo(node, ctxInheritSummDropSI);
  }
  
  
  public static void addInheritedRequireDrop(IRNode toThisNode, TRoleRequireDrop trrDrop) {
    if (!tRoleDropsEnabled) return;
    getMutableInheritedRequiresSet(toThisNode).add(trrDrop);
  }

  @Override
  public void register(PromiseFramework fw) {
	  // TODO Auto-generated method stub

  }
  
  // Support for Module promises that have no AAST.

  private static void setX_mapped(SlotInfo<Boolean> si,
		  IRNode node, boolean processed) {
	  // TODO Auto-generated method stub

  }

  private static boolean isXorFalse_filtered(
		  SlotInfo<Boolean> si, IRNode node) {
	  // TODO Auto-generated method stub
	  return false;
  }
}
