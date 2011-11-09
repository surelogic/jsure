package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.tree.MutableSymmetricEdgeDigraphInterface;

/**
 * The symmetric edge diagraph model interface.
 * 
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link EdgeDigraphModel#CHILDREN}
 * <li>{@link EdgeDigraphModel#IS_EDGE}
 * <li>{@link EdgeDigraphModel#CHILD_EDGES}
 * <li>{@link EdgeDigraphModel#SINK}
 * <li>{@link #PARENTS}
 * <li>{@link #PARENT_EDGES}
 * <li>{@link #SOURCE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SymmetricEdgeDigraphModel
extends EdgeDigraphModel, MutableSymmetricEdgeDigraphInterface
{
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * Get the parents of a node, as an {@link edu.cmu.cs.fluid.ir.IRSequence}.
   */
  public static final String PARENTS = "SymmetricEdgeDigraphModel.parents";

  /**
   * The incoming edges to a node, as {@link edu.cmu.cs.fluid.ir.IRSequence}.
   */
  public static final String PARENT_EDGES = "SymmetricEdgeDigraphModel.parentEdges";

  /**
   * The node an edge extends from, as an {@link edu.cmu.cs.fluid.ir.IRNode}.
   */
  public static final String SOURCE = "SymmetricEdgeDigraphModel.source";
}
