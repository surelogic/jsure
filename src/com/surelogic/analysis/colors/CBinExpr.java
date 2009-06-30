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
public class CBinExpr extends CExpr {
  
  final CExpr left;
  final CExpr right;
  
  /**
   * @param l
   * @param r
   */
  private CBinExpr(final int op, final CExpr l, final CExpr r) {
    left = l;
    right = r;
    operator = op;
  }
  
  public static CBinExpr cAnd(final CExpr l, final CExpr r) {
    return new CBinExpr(CExpr.andOp, l, r);
  }
  
  public static CBinExpr cAndParen(final CExpr l, final CExpr r) {
    return new CBinExpr(CExpr.andParenOp, l, r);
  }
  
  
  public static CBinExpr cOr(final CExpr l, final CExpr r) {
    return new CBinExpr(CExpr.orOp, l, r);
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#clone()
   */
  @Override
  public CExpr doClone() {
    final CExpr newL = left.doClone();
    final CExpr newR = right.doClone();    
    return new CBinExpr(operator, newL, newR);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#cloneWithRename(edu.cmu.cs.fluid.java.analysis.CExpr)
   */
  @Override
  protected CExpr cloneWithRename(ColorRenameDrop rename) {
    final CExpr newL = left.cloneWithRename(rename);
    final CExpr newR = right.cloneWithRename(rename);    
    return new CBinExpr(operator, newL, newR);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(boolean)
   */
  @Override
  public JBDD computeExpr(boolean wantConflicts) {
    final JBDD l = left.computeExpr(wantConflicts);
    final JBDD r = right.computeExpr(wantConflicts);
    JBDD res = null;
    if ((operator == CExpr.andOp) || (operator == CExpr.andParenOp)) {
      res = l.and(r);
    } else if (operator == CExpr.orOp) {
      res = l.or(r);
    } 
    return res;
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set, int)
   */
   @Override
  protected CExpr exclude(Set<TColor> exclusions, int parentOpFlags) {
    final int flag = (operator == CExpr.orOp) ? andOrFlag : 0;
    final CExpr l = left.exclude(exclusions, flag);
    final CExpr r = right.exclude(exclusions, flag);
    CExpr res = null;
    if ((operator == CExpr.andOp) || (operator == CExpr.andParenOp)) {
      res = cAnd(l, r);
    } else if (operator == CExpr.orOp) {
      res = cOr(l, r);
    } 
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
    
    if (obj instanceof CBinExpr) {
      final CBinExpr o = (CBinExpr) obj;
      
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
  public void referencedColorNames(Set<String> addHere, boolean posOnly) {
    left.referencedColorNames(addHere, posOnly);
    right.referencedColorNames(addHere, posOnly);
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
      final CBinExpr bExp = (CBinExpr) exp;
      res = left.compareTo(bExp.left);
      if (res == 0) {
        res = right.compareTo(bExp.right);
      }
    } 
    return res;
  }
}
