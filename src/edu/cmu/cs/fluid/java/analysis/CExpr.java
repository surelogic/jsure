/*
 * Created on Jul 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop;

import SableJBDD.bdd.JBDD;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public abstract class CExpr implements Comparable<CExpr>{
  protected static final Logger LOG = SLLogger.getLogger("analysis.cExpr");
  
  public static final int andParenOp = 0;
  public static final int andOp = 1;
  public static final int orOp = 2;
  public static final int notOp = 3;
  public static final int leafOp = 4;
  
  
  protected static final int notFlag = 1;
  protected static final int andOrFlag = 2;
  
  protected int operator;
  
  public abstract JBDD computeExpr(final boolean wantConflicts);
  
  public abstract CExpr exclude(final Set<TColor> exclusions);
  
  protected abstract CExpr exclude(final Set<TColor> exclusions, int parentOpFlags);
  
  public boolean isTrue() {
    if (this instanceof CLeafExpr) return ((CLeafExpr) this).isTrue();
    return false;
  }
  
  public boolean isFalse() {
    if (this instanceof CLeafExpr) return ((CLeafExpr) this).isFalse();
    return false;
  }
  
  @Override
  public abstract String toString();
  
  protected abstract void toSB(StringBuilder sb);
  
  @Override
  public abstract int hashCode();
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public abstract boolean equals(Object obj);
  
  /** fill in a Set with the String versions of all the simple names of thread colors
   * referenced in this expression.
   * @param addHere The Set to fill in.
   */
  public void referencedColorNames(Set<String> addHere) {
    referencedColorNames(addHere, false);
  }
  
  protected abstract void referencedColorNames(Set<String> addHere, boolean posOnly);
  
  public final Set<String> posReferencedColorNames() {
    Set<String> res = new HashSet<String>();
    referencedColorNames(res, true);
    return res;
  }
  
  public final Set<String> referencedColorNames() {
    Set<String> res = new HashSet<String>();
    referencedColorNames(res, false);
    return res;
  }
  
  protected abstract CExpr cloneWithRename(ColorRenameDrop rename);
  
  public abstract CExpr doClone();
  
  protected String operatorImage(int op) {
    switch (op) {
    		case 0: return " & ";
    		case 1: return " & ";
    		case 2: return " | ";
    		case 3: return "!";
    		default: return " <bogusOp> ";
    }
  }
}
