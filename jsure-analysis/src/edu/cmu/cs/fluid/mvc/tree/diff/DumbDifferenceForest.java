/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/diff/DumbDifferenceForest.java,v 1.6 2003/07/15 18:39:13 thallora Exp $
 *
 * DumbDifferenceForestModel.java
 * Created on April 26, 2002, 3:59 PM
 */

package edu.cmu.cs.fluid.mvc.tree.diff;

import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Specialization of DifferenceForestModel that does not give any additional
 * semantics to the difference.  Nodes that stay in the same place in the tree
 * but that have had attributes change are marked as {@link #DIFFERENT} in
 * the local difference attribute.
 */
public interface DumbDifferenceForest
extends DifferenceForestModel
{
  //===========================================================
  //== Constants for defining an enumeration that describes
  //== node-level changes.  These values are extended by
  //== sub-interfaces.
  //===========================================================

  /*
   * Name constant for the node-level change enumeration. 
   */
  public static final String LOCAL_ENUM = "DumbDifferenceForest$LocalEnumeration";
  
  /**
   * The name of the node enumeration element that indicates that 
   * the node is the different some how.  This is always the sixth and last
   * element of the local difference enumeration used by this model type.
   */
  public static final String NODE_DIFFERENT = "Locally Different";

  /** The index of the {@link #NODE_DIFFERENT} element. */
  public final static int DIFFERENT = 5;


  
  /**
   * Factory interface for creating DumbDifferenceForest models.
   */
  public static interface Factory
  {
    public DumbDifferenceForest create(
      String name, ForestModel base, ForestModel delta )
    throws SlotAlreadyRegisteredException;
  }
}
