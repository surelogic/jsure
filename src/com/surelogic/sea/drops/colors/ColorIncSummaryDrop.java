/*
 * Created on Nov 5, 2004
 *
 */
package com.surelogic.sea.drops.colors;

import java.util.Collection;
import java.util.logging.Logger;

import com.surelogic.analysis.colors.ColorBDDPack;
import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.sea.PhantomDrop;
import edu.cmu.cs.fluid.sea.Sea;

/**
 * @author dfsuther
 *
 */
public class ColorIncSummaryDrop extends PhantomDrop {
  private final JBDD conflictExpr;
  private final ColorNameModel summaryFor;
  
  private static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");
  
  public ColorIncSummaryDrop(ColorNameModel model) {
    ColorNameModel canonModel = model.getCanonicalNameModel();
    
    JBDD resExpr = computeIncSummaryExpr(canonModel);
    conflictExpr = resExpr;
    summaryFor = model;
    model.addDependent(this);
    setMessage("Incompatibility expression for color " + canonModel.getColorName() + ": " + conflictExpr);
  }
  
  
  /**
   * @param canonModel
   * @return
   */
  private JBDD computeIncSummaryExpr(ColorNameModel canonModel) {
    JBDD resExpr = ColorBDDPack.one();
    
    Collection<? extends ColorIncompatibleDrop> incompatibles =
      Sea.filterDropsOfTypeMutate(ColorIncompatibleDrop.class, canonModel.getDependents());
    
    for (ColorIncompatibleDrop inc : incompatibles) {      
      JBDD tExpr = inc.getConflictExpr();
      resExpr.andWith(tExpr);
      inc.addDependent(this);
    }
    return resExpr;
  }


  public void updateIncSummaryDrop(ColorNameModel model) {
    ColorNameModel canonModel = model.getCanonicalNameModel();
    
    JBDD resExpr = computeIncSummaryExpr(canonModel);
    if (resExpr.equals(conflictExpr)) return;
    
    //we've computed a new incompatible summary.  this means we have to invalidate
    // the old one, and create a new one.
    // Easier to just invalidate the current InSummaryDrop.  That way, a new
    // one will be created on demand.
    this.invalidate();
  }
  
  
  
  /**
   * @return Returns the conflictExpr.
   */
  public JBDD getConflictExpr() {
    return conflictExpr.copy();
  }
  
}
