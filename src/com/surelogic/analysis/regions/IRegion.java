/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/regions/IRegion.java,v 1.6 2007/11/15 20:12:21 chance Exp $*/
package com.surelogic.analysis.regions;

import com.surelogic.aast.bind.IRegionBinding;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.Visibility;

/**
 * Interface for comparing regions
 * Only to be implemented by FieldRegion and RegionModel
 *
 * @author Edwin.Chan
 */
public interface IRegion extends IRegionBinding {
  boolean isStatic();

  boolean isAbstract();
  
  boolean isFinal();
  
  boolean isVolatile();
  
  /**
   * @return A relative location in FAST
   */
  IRNode getNode();
  
  IRegion getParentRegion();
  
  /** Get the simple name of the region. */
  String getName();
  
  /** Get the visibility of the region. */
  Visibility getVisibility();

  /**
   * Returns true if the region is accessible from type 't'
   */
  boolean isAccessibleFromType(ITypeEnvironment tEnv, IRNode t);
  
  boolean isSameRegionAs(IRegion other);
  
  /**
   * Returns <code>true</code> if this IRegion is an ancestor of the
   * other IRegion or if they are equal
   * 
   * @param other
   *            The IRegion to check if it is a descendant of this model
   * @return true if this IRegion is an ancestor of the other, passed in,
   *         IRegion
   */
  boolean ancestorOf(IRegion other);
  
  /**
   * Returns <code>true</code> if this IRegion encapsulates the other
   * IRegion
   * 
   * @param other
   *            The IRegion to see if this IRegion encompasses.
   * @return true if this IRegion includes the other, passed in,
   *         IRegion
   */
  boolean includes(IRegion other);
  
  /**
   * Returns true if this is an ancestor of the other IRegion or vice versa
   * @param other The IRegion to check
   * @return
   */
  boolean overlapsWith(IRegion other);
}
