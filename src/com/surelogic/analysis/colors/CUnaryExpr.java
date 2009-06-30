/*
 * Created on Jul 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.colors;

import java.util.Set;

import com.surelogic.sea.drops.colors.ColorRenameDrop;

import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CUnaryExpr extends CExpr {

  final int operator;
  final CExpr opnd;
  
  private CUnaryExpr() {
    operator = -1;
    opnd = null;
  }
  
  /**
   * @param operator
   * @param opnd
   */
  private CUnaryExpr(final int operator, final CExpr opnd) {
    this.operator = operator;
    this.opnd = opnd;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#clone()
   */
  @Override
  public CExpr doClone() {
    final CExpr newOpnd = opnd.doClone();
    return new CUnaryExpr(operator, newOpnd);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#cloneWithRename(edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop)
   */
  @Override
  protected CExpr cloneWithRename(ColorRenameDrop rename) {
    final CExpr newOpnd = opnd.cloneWithRename(rename);
    return new CUnaryExpr(operator, newOpnd);
  }

  public static CExpr cNot(CExpr expr) {
    return new CUnaryExpr(CExpr.notOp, expr);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(java.util.Set)
   */
  @Override
  protected CExpr exclude(final Set<TColor> exclusions, int parentOpFlags) {
    return cNot(opnd.exclude(exclusions, (parentOpFlags^notFlag)));
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(boolean)
   */
  @Override
  public JBDD computeExpr(boolean wantConflicts) {
    boolean lWantConfl = wantConflicts;
    if (opnd instanceof CLeafExpr) {
      lWantConfl = false;
    }
    final JBDD res = opnd.computeExpr(lWantConfl).not();
    return res;
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set)
   */
  @Override
  public CExpr exclude(Set<TColor> exclusions) {
    // TODO Auto-generated method stub
    return exclude(exclusions, 0);
  }
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
    
    if (obj instanceof CUnaryExpr) {
      final CUnaryExpr o = (CUnaryExpr) obj;
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
  public void referencedColorNames(Set<String> addHere, boolean posOnly) {
    if ((operator == CExpr.notOp) && posOnly) {
      if (opnd instanceof CLeafExpr) {
        // not interested in this name when we're a not-over-leaf and posOnly is set.
        return;
      } else if (opnd instanceof CUnaryExpr) {
        // we have a not-over-not and posOnly is set. skip over both NOTs and head
        // on down to the child.
        final CUnaryExpr child = (CUnaryExpr) opnd;
        child.opnd.referencedColorNames(addHere, posOnly);
        return;
      } else {
        LOG.severe("can't handle combination of posOnly and NOT-over-binary!");
        return;
      }
    }	
    opnd.referencedColorNames(addHere, posOnly);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(CExpr o) {
    if (this == o) return 0;
    int res = 0;
    
    final CExpr exp = o;
    res = operator - exp.operator;
    
    if (res == 0) {
      final CUnaryExpr uExp = (CUnaryExpr) exp;
      res = opnd.compareTo(uExp.opnd);
    } 
    return res;
  }
}
