package edu.cmu.cs.fluid.mvc.tree;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * <p>An (empty) specialization of {@link ForestModel} that always supports 
 * only a single root.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link ForestModel#ROOTS}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link DigraphModel#CHILDREN}
 * <li>{@link SymmetricDigraphModel#PARENTS}
 * <li>{@link ForestModel#LOCATION}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface PureTree
extends ForestModel
{
  /**
   * Factory for creating {@link PureForest}s.
   */
  public static interface Factory
  {
    public PureTree create( String name, SlotFactory sf )
    throws SlotAlreadyRegisteredException;
  }
}
