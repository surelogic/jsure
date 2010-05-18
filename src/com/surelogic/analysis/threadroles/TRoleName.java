/*
 * Created on Jul 20, 2004
 *
 */
package com.surelogic.analysis.threadroles;

import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleNameModel;
import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 */
public class TRoleName implements Comparable {
  TRoleNameModel canonicalName;
  private JBDD selfExpr;
//  private JBDD conflictExpr;
  TRoleName eqTRName;
  
  private static final TRoleBDDPack cBDD = TRoleBDDPack.getInstance();
  
  
  TRoleName() {
    canonicalName = null;
    selfExpr = null;
//    conflictExpr = null;
    eqTRName = null;
  }
  
  public TRoleName(TRoleNameModel canonName) {
    canonicalName = canonName;
    selfExpr = null;
//    conflictExpr = null;
    eqTRName = null;
  }
  
  public TRoleName getCanonicalTColor() {
    TRoleName res = this;
    while (res.eqTRName != null) {
      res = res.eqTRName;
    }
    
    return res;
  }
  
  public TRoleNameModel getCanonicalNameModel() {
    final TRoleName rTRName = getCanonicalTColor();
    final TRoleNameModel nameModel = rTRName.canonicalName;
    return nameModel;
  }
  public String computeCanonicalName() {
    return getCanonicalNameModel().getTRoleName();
  }
  /**
   * @return Returns the conflictExpr.
   */
  public JBDD getConflictExpr() {
    return canonicalName.getIncompatibleSummary().getConflictExpr().copy();
  }
  /**
   * @return Returns the selfExpr.
   */
  public JBDD getSelfExpr() {
    if (selfExpr == null) {
      selfExpr = TRoleBDDPack.getBddFactory().posBddOf(canonicalName.getTheBddVar());
    }
    return selfExpr.copy();
  }
  
  public JBDD getSelfExprNeg() {
    return getSelfExpr().not();
  }
//  /**
//   * @param conflictExpr The conflictExpr to set.
//   */
//  public void setConflictExpr(JBDD conflictExpr) {
//    this.conflictExpr = conflictExpr;
//  }
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    if (this == o) return 0;
    final TRoleName other = (TRoleName) o;
    
    return this.computeCanonicalName().compareTo(other.computeCanonicalName());
  }
}
