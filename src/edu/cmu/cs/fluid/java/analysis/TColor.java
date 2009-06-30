/*
 * Created on Jul 20, 2004
 *
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.sea.drops.promises.*;
import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 */
@Deprecated
public class TColor implements Comparable {
  ColorNameModel canonicalName;
  private JBDD selfExpr;
//  private JBDD conflictExpr;
  TColor eqTColor;
  
  private static final ColorBDDPack cBDD = ColorBDDPack.getInstance();
  
  
  TColor() {
    canonicalName = null;
    selfExpr = null;
//    conflictExpr = null;
    eqTColor = null;
  }
  
  public TColor(ColorNameModel canonName) {
    canonicalName = canonName;
    selfExpr = null;
//    conflictExpr = null;
    eqTColor = null;
  }
  
  public TColor getCanonicalTColor() {
    TColor res = this;
    while (res.eqTColor != null) {
      res = res.eqTColor;
    }
    
    return res;
  }
  
  public ColorNameModel getCanonicalNameModel() {
    final TColor rTColor = getCanonicalTColor();
    final ColorNameModel nameModel = rTColor.canonicalName;
    return nameModel;
  }
  public String computeCanonicalName() {
    return getCanonicalNameModel().getColorName();
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
      selfExpr = ColorBDDPack.getBddFactory().posBddOf(canonicalName.getTheBddVar());
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
    final TColor other = (TColor) o;
    
    return this.computeCanonicalName().compareTo(other.computeCanonicalName());
  }
}
