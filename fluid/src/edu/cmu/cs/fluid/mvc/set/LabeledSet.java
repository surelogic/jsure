/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/LabeledSet.java,v 1.9 2004/09/10 17:33:53 boyland Exp $
 *
 * LabeledSet.java
 * Created on February 28, 2002, 10:38 AM
 */

package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

/**
 * Specialization of {@link SetModel} (via {@link PureSet} that adds a
 * String-valued {@link #LABEL} attribute to each node.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link SetModel#SIZE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li><@link LabeledSet#LABEL}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface LabeledSet
extends PureSet
{
  /**
   * The label attribute, of type {@link edu.cmu.cs.fluid.ir.IRStringType}.
   * This attribute is mutable.
   */
  public static final String LABEL = "LabeledSet.label";

  /**
   * Convienence method for getting the value of a node's label.
   */
  public String getLabel( IRNode node );

  /**
   * Convienence method for setting the value of a node's label.
   */
  public void setLabel( IRNode node, String label );


  
  /**
   * Interface for factories that create LabeledSet objects.
   * All LabeledSet model instances should be create through a factory
   * rather than through direct constructor calls.
   */
  public interface Factory
  {
    /**
     * Create a new LabeledSet model instance.
     * @param name The name of the model.
     * @param sf The slot factory to use to create the model's
     *           structural and informational (e.g., {@link LabeledSet#LABEL})
     *           attributes.
     */
    public LabeledSet create( String name, ExplicitSlotFactory sf )
    throws SlotAlreadyRegisteredException;
  }
}
