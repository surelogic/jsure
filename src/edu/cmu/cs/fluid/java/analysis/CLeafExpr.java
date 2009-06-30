/*
 * Created on Jul 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Set;

import edu.cmu.cs.fluid.sea.drops.promises.ColorNameModel;
import edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop;

import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */ 
@Deprecated
public class CLeafExpr extends CExpr {

  private final ColorNameModel leaf;
  private final boolean tf;
  
  private CLeafExpr(boolean trueFalse) { 
    leaf = null;
    operator = CExpr.leafOp;
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
  
  public static CLeafExpr getTrue() {
    return new CLeafExpr(true);
  }
  
  public static CLeafExpr getFalse() {
    return new CLeafExpr(false);
  }
  
  public CLeafExpr(ColorNameModel theColorName) {
    leaf = theColorName;
    operator = CExpr.leafOp;
    tf = false;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#clone()
   */
  @Override
  public CExpr doClone() {
    if (leaf == null) {
      return new CLeafExpr(tf);
    } else {
      return new CLeafExpr(leaf);
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.analysis.CExpr#cloneWithRename(edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop)
   */
  @Override
  protected CExpr cloneWithRename(ColorRenameDrop rename) {
    if (leaf == null) return doClone();
    final String name = leaf.getCanonicalNameModel().getColorName(); 
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
    return leaf.getColorName();
  }

  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#toSB(java.lang.StringBuilder)
   */
  @Override
  protected void toSB(StringBuilder sb) {
    sb.append(this.toString());
  }

  private CLeafExpr idExpr(int parentOpFlags) {
    final boolean parentOr = ((parentOpFlags & andOrFlag) != 0) ? true : false;
    final boolean parentNot = ((parentOpFlags & notFlag) != 0) ? true : false;
  
    CLeafExpr res = new CLeafExpr(parentOr ^ parentNot);
    
    return res;
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#exclude(java.util.Set, int)
   */
   @Override
  protected CExpr exclude(Set<TColor> exclusions, int parentOpFlags) {
    if (exclusions.contains(leaf.getCanonicalTColor())) {
      return idExpr(parentOpFlags);
    } else {
      return new CLeafExpr(leaf);
    }
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
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#computeExpr(java.util.Set, boolean, int)
   */
  @Override
  public JBDD computeExpr(boolean wantConflicts) {
    if (leaf != null) {
      final TColor leafCanonTColor = leaf.getCanonicalTColor();
      if (leafCanonTColor != null) {
        JBDD selfExpr = leafCanonTColor.getSelfExpr();
        if (wantConflicts) {
          return selfExpr.and(leaf.getCanonicalTColor().getConflictExpr());
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
        return ColorBDDPack.getBddFactory().ONE();
      } else {
        return ColorBDDPack.getBddFactory().ZERO();
      }
    }
  }
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
   @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    
    if (obj instanceof CLeafExpr) {
      if (leaf.equals(((CLeafExpr)obj).leaf)) {
        return true;
      }
    }
    return false;
  }
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.colors.CExpr#referencedColorNames(java.util.Collection)
   */
   @Override
  public void referencedColorNames(Set<String> addHere, boolean posOnly) {
    if (isFalse() || isTrue()) return;
    
    final String leafName = leaf.getColorName();
    addHere.add(leafName);
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
      // o must be a CLeafExpr because it has the same operator I do
      final CLeafExpr lExp = (CLeafExpr) exp;
      res = (leaf.getCanonicalTColor().compareTo(lExp.leaf.getCanonicalTColor()));
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
