/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/PureDigraph.java,v 1.3 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * <p>An (empty) specialization of {@link DigraphModel}.
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
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface PureDigraph
extends DigraphModel
{
  /**
   * Factory for creating {@link PureDigraph}s.
   */
  public static interface Factory
  {
    public PureDigraph create( String name, SlotFactory sf )
    throws SlotAlreadyRegisteredException;
  }
}
