package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.tree.MutableEdgeDigraphInterface;

/**
 * The edge diagraph model interface.
 * 
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * <li>{@link #CHILDREN}
 * <li>{@link #IS_EDGE}
 * <li>{@link #CHILD_EDGES}
 * <li>{@link #SINK}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface EdgeDigraphModel
extends Model, MutableEdgeDigraphInterface
{
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * The children of a node, as an {@link edu.cmu.cs.fluid.ir.IRSequence}.
   * The attribute is mutable?
   */
  public static final String CHILDREN = "EdgeDigraphModel.children";

  /**
   * Whether a node represents an edge or not.
   * Values are of type {@link edu.cmu.cs.fluid.ir.IRBooleanType}.
   */
  public static final String IS_EDGE = "EdgeDigraphModel.isEdge";

  /**
   * The child edges of a node, as an {@link edu.cmu.cs.fluid.ir.IRSequence}?.
   */
  public static final String CHILD_EDGES = "EdgeDigraphModel.childEdges";

  /**
   * The node an edge connects to.  Values are of type
   * {@link edu.cmu.cs.fluid.ir.IRNodeType}.
   */
  public static final String SINK = "EdgeDigraphModel.sink";
}

