package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.tree.MutableDigraphInterface;

/**
 * The diagraph model interface.
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
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface DigraphModel
extends Model, MutableDigraphInterface
{
  //===========================================================
  //== Names of standard node attributes
  //===========================================================


  /**
   * The children of a node, as an {@link edu.cmu.cs.fluid.ir.IRSequence}.
   * The attribute is mutable?
   */
  public static final String CHILDREN = "DigraphModel.children";
}

