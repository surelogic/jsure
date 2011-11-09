package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Empty specialization of {@link SyntaxForestModel}.
 * An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#ROOTS}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#LOCATION}
 * <li>{@link SyntaxForestModel#OPERATOR}
 * </ul>
 */
public interface PureSyntaxForest
extends SyntaxForestModel
{
  /**
   * Interface for factories that create instances of {@link PureSyntaxForest}.
   */
  public static interface Factory
  {
    public PureSyntaxForest create( String name, SlotFactory sf )
    throws SlotAlreadyRegisteredException;
  }
}
