/*
 * Created on Jul 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Set;

import com.surelogic.dropsea.ir.drops.threadroles.TRoleRenameDrop;


import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRBinExpr extends TRExpr {
  
  final TRExpr left;
  final TRExpr right;
  
  /**
   * @param l
   * @param r
   */
  private TRBinExpr(final int op, final TRExpr l, final TRExpr r) {
    left = l;
    right = r;
    operator = op;
  }
  
  public static TRBinExpr cAnd(final TRExpr l, final TRExpr r) {
    return new TRBinExpr(TRExpr.andOp, l, r);
  }
  
  public static TRBinExpr cAndParen(final TRExpr l, final TRExpr r) {
    return new TRBinExpr(TRExpr.andParenOp, l, r);
  }
  
  
  public static TRBinExpr cOr(final TRExpr l, final TRExpr r) {
    return new TRBinExpr(TRExpr.orOp, l, r);
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#clone()
   */
  @Override
  public TRExpr doClone() {
    final TRExpr newL = left.doClone();
    final TRExpr newR = right.doClone();    
    return new TRBinExpr(operator, newL, newR);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#cloneWithRename(edu.cmu.cs.fluid.java.analysis.CExpr)
   */
  @Override
  protected TRExpr cloneWithRename(TRoleRenameDrop rename) {
    final TRExpr newL = left.cloneWithRename(rename);
    final TRExpr newR = right.cloneWithRename(rename);    
    return new TRBinExpr(operator, newL, newR);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(boolean)
   */
  @Override
  public JBDD computeExpr(boolean wantConflicts) {
    final JBDD l = left.computeExpr(wantConflicts);
    final JBDD r = right.computeExpr(wantConflicts);
    JBDD res = null;
    if ((operator == TRExpr.andOp) || (operator == TRExpr.andParenOp)) {
      res = l.and(r);
    } else if (operator == TRExpr.orOp) {
      res = l.or(r);
    } 
    return res;
  }
//  /* (non-Javadoc)
//   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set, int)
//   */
//   @Override
//  protected TRExpr exclude(Set<TRoleName> exclusions, int parentOpFlags) {
//    final int flag = (operator == TRExpr.orOp) ? andOrFlag : 0;
//    final TRExpr l = left.exclude(exclusions, flag);
//    final TRExpr r = right.exclude(exclusions, flag);
//    TRExpr res = null;
//    if ((operator == TRExpr.andOp) || (operator == TRExpr.andParenOp)) {
//      res = cAnd(l, r);
//    } else if (operator == TRExpr.orOp) {
//      res = cOr(l, r);
//    } 
//    return res;
//  }
//  /* (non-Javadoc)
//   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set)
//   */
//   @Override
//  public TRExpr exclude(Set<TRoleName> exclusions) {
//    // TODO Auto-generated method stub
//    return exclude(exclusions, 0);
//  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#toSB(java.lang.StringBuilder)
   */
  @Override
  protected void toSB(StringBuilder sb) {
    sb.append('(');
    left.toSB(sb);
    sb.append(operatorImage(operator));
    right.toSB(sb);
    sb.append(')');
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
    int res = left.hashCode();
    res = res * 17 + operator;
    res = res * 17 + right.hashCode();
    return res;
  }
  
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    
    if (obj instanceof TRBinExpr) {
      final TRBinExpr o = (TRBinExpr) obj;
      
      if (o.operator == operator) {
        if (left.equals(o.left)) {
          if (right.equals(o.right)) {
            return true;
          }
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
    left.referencedTRNames(addHere, posOnly);
    right.referencedTRNames(addHere, posOnly);
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
      final TRBinExpr bExp = (TRBinExpr) exp;
      res = left.compareTo(bExp.left);
      if (res == 0) {
        res = right.compareTo(bExp.right);
      }
    } 
    return res;
  }
}
