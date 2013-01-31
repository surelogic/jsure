/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/regions/AbstractRegion.java,v 1.2 2007/07/16 19:47:39 chance Exp $*/
package com.surelogic.analysis.regions;

import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

public abstract class AbstractRegion implements IRegion {  
  @Override
  public abstract String toString();
  
  /**
   * Returns <code>true</code> if this IRegion is an ancestor of the
   * other IRegion or if they are equal
   * 
   * @param one "This IRegion"
   * @param other
   *            The IRegion to check if it is a descendant of this model
   * @return true if this IRegion is an ancestor the other, passed in,
   *         IRegion
   */
  public static boolean ancestorOf(final IRegion one, final IRegion other) {
    boolean ancestorOf = false;
    IRegion current = other;
    do {
      if (one.isSameRegionAs(current)) {
        ancestorOf = true;
      }
      else {
        try {
          current = current.getParentRegion();
        }
        catch (Exception e) {
          SLLogger.getLogger().log(Level.SEVERE, "Problem while getting ancestor region", e);
          break;
        }
      }
    } while (current != null && !ancestorOf);
    return ancestorOf;
  }

  /**
   * Returns <code>true</code> if this IRegion encapsulates the other
   * IRegion
   * 
   * @param one "This IRegion"
   * @param other
   *            The IRegion to see if this IRegion encompasses.
   * @return true if this IRegion includes the other, passed in,
   *         IRegion
   */
  public static boolean includes(final IRegion one, final IRegion other) {
    boolean includes = false;
    IRegion current = other;
    do{
      if(current.isSameRegionAs(one)){
        includes = true;
      }
      else{
        try {
          current = current.getParentRegion();
        }
        catch (Exception e) {
          SLLogger.getLogger().log(Level.SEVERE, "Problem while computing includes()", e);
          break;
        }
      }
    }while(current != null && !includes);
    
    return includes;
  }
  
  @Override
  public final boolean ancestorOf(IRegion other) {
    return ancestorOf(this, other);
  }

  @Override
  public final boolean includes(IRegion other) {
    return includes(this, other);
  }
  
  @Override
  public final boolean overlapsWith(final IRegion other){
    return ancestorOf(other) || other.ancestorOf(this);
  }
}
