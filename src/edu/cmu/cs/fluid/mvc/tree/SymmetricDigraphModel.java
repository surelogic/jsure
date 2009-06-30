package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface;

/**
 * The symmetric diagraph model interface.
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
 * <li>{@link DigraphModel#CHILDREN}
 * <li>{@link #PARENTS}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SymmetricDigraphModel
extends DigraphModel, MutableSymmetricDigraphInterface
{
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * Get the parents of a node, as an {@link edu.cmu.cs.fluid.ir.IRSequence}.
   */
  public static final String PARENTS = "SymmetricDigraphModel.parents";
}

