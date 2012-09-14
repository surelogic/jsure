/*
 * Created on Nov 5, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.Collection;
import java.util.logging.Logger;

import SableJBDD.bdd.JBDD;

import com.surelogic.analysis.threadroles.TRoleBDDPack;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.Sea;


/**
 * @author dfsuther
 *
 */
public class TRoleIncSummaryDrop extends IRReferenceDrop implements IThreadRoleDrop {
  private final JBDD conflictExpr;
  private final TRoleNameModel summaryFor;
  
  private static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");
  
  public TRoleIncSummaryDrop(TRoleNameModel model) {
    super(model.getNode()); // will (maybe) blow up!
    TRoleNameModel canonModel = model.getCanonicalNameModel();
    
    JBDD resExpr = computeIncSummaryExpr(canonModel);
    conflictExpr = resExpr;
    summaryFor = model;
    model.addDependent(this);
    setMessage(12,"Incompatibility expression for thread role " + canonModel.getTRoleName() + ": " + conflictExpr);
  }
  
  
  /**
   * @param canonModel
   * @return
   */
  private JBDD computeIncSummaryExpr(TRoleNameModel canonModel) {
    JBDD resExpr = TRoleBDDPack.one();
    
    Collection<? extends TRoleIncompatibleDrop> incompatibles =
      Sea.filterDropsOfType(TRoleIncompatibleDrop.class, canonModel.getDependents());
    
    for (TRoleIncompatibleDrop inc : incompatibles) {      
      JBDD tExpr = inc.getConflictExpr();
      resExpr.andWith(tExpr);
      inc.addDependent(this);
    }
    return resExpr;
  }


  public void updateIncSummaryDrop(TRoleNameModel model) {
    TRoleNameModel canonModel = model.getCanonicalNameModel();
    
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
