package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * A specialization of {@link ForestModel} that adds a label attribute
 * to the nodes and supports multiple roots.
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
 * <li>{@link #LABEL}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface LabeledForest
extends ForestModel
{
  /**
   * The label attribute, of type {@link edu.cmu.cs.fluid.ir.IRStringType}.
   * This attribute is mutable.
   */
  public static final String LABEL = "LabeledForest.label";

  
  
  //===========================================================
  //== Attribute Convience Methods
  //===========================================================

  public void setLabel( IRNode node, String label );

  public String getLabel( IRNode node );
  
  
  
  /**
   * Factory for creating {@link LabeledForest}s.
   */
  public static interface Factory
  {
    public LabeledForest create( String name, SlotFactory sf )
    throws SlotAlreadyRegisteredException;
  }
}
