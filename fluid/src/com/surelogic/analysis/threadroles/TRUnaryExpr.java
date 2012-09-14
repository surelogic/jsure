/*
 * Created on Jul 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Set;

import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleRenameDrop;


import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRUnaryExpr extends TRExpr {

  final int operator;
  final TRExpr opnd;
  
  private TRUnaryExpr() {
    operator = -1;
    opnd = null;
  }
  
  /**
   * @param operator
   * @param opnd
   */
  private TRUnaryExpr(final int operator, final TRExpr opnd) {
    this.operator = operator;
    this.opnd = opnd;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#clone()
   */
  @Override
  public TRExpr doClone() {
    final TRExpr newOpnd = opnd.doClone();
    return new TRUnaryExpr(operator, newOpnd);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#cloneWithRename(edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop)
   */
  @Override
  protected TRExpr cloneWithRename(TRoleRenameDrop rename) {
    final TRExpr newOpnd = opnd.cloneWithRename(rename);
    return new TRUnaryExpr(operator, newOpnd);
  }

  public static TRExpr trNot(TRExpr expr) {
    return new TRUnaryExpr(TRExpr.notOp, expr);
  }
  
//  /* (non-Javadoc)
//   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(java.util.Set)
//   */
//  @Override
//  protected TRExpr exclude(final Set<TRoleName> exclusions, int parentOpFlags) {
//    return trNot(opnd.exclude(exclusions, (parentOpFlags^notFlag)));
//  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(boolean)
   */
  @Override
  public JBDD computeExpr(boolean wantConflicts) {
    boolean lWantConfl = wantConflicts;
    if (opnd instanceof TRLeafExpr) {
      lWantConfl = false;
    }
    final JBDD res = opnd.computeExpr(lWantConfl).not();
    return res;
  }
//  /* (non-Javadoc)
//   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set)
//   */
//  @Override
//  public TRExpr exclude(Set<TRoleName> exclusions) {
//    // TODO Auto-generated method stub
//    return exclude(exclusions, 0);
//  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#toSB(java.lang.StringBuilder)
   */
  @Override
  protected void toSB(StringBuilder sb) {
    sb.append(operatorImage(operator));
    opnd.toSB(sb);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toSB(sb);
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int res = operator;
    res = res * 17 + opnd.hashCode();
    return res;
  }
  
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    
    if (obj instanceof TRUnaryExpr) {
      final TRUnaryExpr o = (TRUnaryExpr) obj;
      if (operator == o.operator) {
        if (opnd.equals(o.opnd)) {
          return true;
        }
      }
    }
    return false;
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#referencedColorNames(java.util.Collection)
   */
  @Override
  public void referencedTRNames(Set<String> addHere, boolean posOnly) {
    if ((operator == TRExpr.notOp) && posOnly) {
      if (opnd instanceof TRLeafExpr) {
        // not interested in this name when we're a not-over-leaf and posOnly is set.
        return;
      } else if (opnd instanceof TRUnaryExpr) {
        // we have a not-over-not and posOnly is set. skip over both NOTs and head
        // on down to the child.
        final TRUnaryExpr child = (TRUnaryExpr) opnd;
        child.opnd.referencedTRNames(addHere, posOnly);
        return;
      } else {
        LOG.severe("can't handle combination of posOnly and NOT-over-binary!");
        return;
      }
    }	
    opnd.referencedTRNames(addHere, posOnly);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(TRExpr o) {
    if (this == o) return 0;
    int res = 0;
    
    final TRExpr exp = o;
    res = operator - exp.operator;
    
    if (res == 0) {
      final TRUnaryExpr uExp = (TRUnaryExpr) exp;
      res = opnd.compareTo(uExp.opnd);
    } 
    return res;
  }
}
