/*
 * Created on Jul 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Set;

import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleNameModel;
import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleRenameDrop;


import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRLeafExpr extends TRExpr {

  private final TRoleNameModel leaf;
  private final boolean tf;
  
  private TRLeafExpr(boolean trueFalse) { 
    leaf = null;
    operator = TRExpr.leafOp;
    tf = trueFalse;
  }
  
  @Override public boolean isTrue() {
    if (leaf == null) {
      return tf;
    } else {
      return false;
    }
  }
  
  @Override public boolean isFalse() {
    if (leaf == null) {
      return !tf;
    } else {
      return false;
    }
  }
  
  public static TRLeafExpr getTrue() {
    return new TRLeafExpr(true);
  }
  
  public static TRLeafExpr getFalse() {
    return new TRLeafExpr(false);
  }
  
  public TRLeafExpr(TRoleNameModel theTRName) {
    leaf = theTRName;
    operator = TRExpr.leafOp;
    tf = false;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#clone()
   */
  @Override
  public TRExpr doClone() {
    if (leaf == null) {
      return new TRLeafExpr(tf);
    } else {
      return new TRLeafExpr(leaf);
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#cloneWithRename(edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop)
   */
  @Override
  protected TRExpr cloneWithRename(TRoleRenameDrop rename) {
    if (leaf == null) return doClone();
    final String name = leaf.getCanonicalNameModel().getTRoleName(); 
    if (rename.simpleName.equals(name)) {
      return rename.getRawExpr().doClone();
    } else {
      return doClone();
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override public String toString() {
    if (leaf == null) {
      return tf ? "true" : "false";
    }
    return leaf.getTRoleName();
  }

  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#toSB(java.lang.StringBuilder)
   */
  @Override
  protected void toSB(StringBuilder sb) {
    sb.append(this.toString());
  }

  private TRLeafExpr idExpr(int parentOpFlags) {
    final boolean parentOr = ((parentOpFlags & andOrFlag) != 0) ? true : false;
    final boolean parentNot = ((parentOpFlags & notFlag) != 0) ? true : false;
  
    TRLeafExpr res = new TRLeafExpr(parentOr ^ parentNot);
    
    return res;
  }
  
  
  
//  /* (non-Javadoc)
//   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set, int)
//   */
//   @Override
//  protected TRExpr exclude(Set<TRoleName> exclusions, int parentOpFlags) {
//    if (exclusions.contains(leaf.getCanonicalTRole())) {
//      return idExpr(parentOpFlags);
//    } else {
//      return new TRLeafExpr(leaf);
//    }
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
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(java.util.Set, boolean, int)
   */
  @Override
  public JBDD computeExpr(boolean wantConflicts) {
    if (leaf != null) {
      final TRoleNameModel leafCanonTRNM = leaf.getCanonicalNameModel();
      if (leafCanonTRNM != null) {
        JBDD selfExpr = leafCanonTRNM.getSelfExpr();
        if (wantConflicts) {
          return selfExpr.and(leaf.getCanonicalNameModel().getConflictExpr());
        } else {
          return selfExpr;
        }
      } else {
        // leafCanonTColor was null!
        return null;
      }
    } else {
      // leaf was null.  That means that the LeafExpr represents either TRUE or FALSE.
      if (isTrue()) {
        return TRoleBDDPack.getBddFactory().ONE();
      } else {
        return TRoleBDDPack.getBddFactory().ZERO();
      }
    }
  }
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
   @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    
    if (obj instanceof TRLeafExpr) {
      if (leaf.equals(((TRLeafExpr)obj).leaf)) {
        return true;
      }
    }
    return false;
  }
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#referencedColorNames(java.util.Collection)
   */
   @Override
  public void referencedTRNames(Set<String> addHere, boolean posOnly) {
    if (isFalse() || isTrue()) return;
    
    final String leafName = leaf.getTRoleName();
    addHere.add(leafName);
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
      // o must be a CLeafExpr because it has the same operator I do
      final TRLeafExpr lExp = (TRLeafExpr) exp;
      res = (leaf.getCanonicalNameModel().compareTo(lExp.leaf.getCanonicalNameModel()));
    } 
    return res;
  }
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
   @Override
  public int hashCode() {
    // TODO Auto-generated method stub
    return leaf.hashCode();
  }
}
