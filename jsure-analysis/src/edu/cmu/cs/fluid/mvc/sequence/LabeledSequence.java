/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/LabeledSequence.java,v 1.14 2003/07/15 21:47:18 aarong Exp $
 *
 * LabeledSequence.java
 * Created on March 1, 2002, 3:02 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Specialization of {@link SequenceModel} that adds a String-valued 
 * {@link #LABEL} attribute to eadch node.
 *
 * <P>An implementation must support the
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link SequenceModel#SIZE}
 * <li>{@link SequenceModel#FIRST}
 * </ul>
 *
 * <P>An implementaiton must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link SequenceModel#LOCATION}
 * <LI>{@link SequenceModel#INDEX}
 * <LI>{@link SequenceModel#NEXT}
 * <LI>{@link SequenceModel#PREVIOUS}
 * <li><@link LabeledSet#LABEL}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface LabeledSequence
extends PureSequence
{
  /**
   * The label attribute, of type {@link edu.cmu.cs.fluid.ir.IRStringType}.
   * This attribute is mutable.
   */
  public static final String LABEL = "LabeledSequence.label";

  
  
  /**
   * Convienence method for getting the value of a node's label.
   */
  public String getLabel( IRNode node );

  /**
   * Convienence method for setting the value of a node's label.
   */
  public void setLabel( IRNode node, String label );
  


  /**
   * Interface for factories that create LabeledSequence objects.
   * All LabeledSequence model instances should be create through a factory
   * rather than through direct constructor calls.
   */
  public interface Factory
  {
    /**
     * Create a new PureSequence model instance.
     * @param name The name of the model.
     * @param sf The slot factory to use to create the model's
     *           structural and informational attributes and the structure
     *           of the sequence itself.
     */
    public LabeledSequence create( String name, SlotFactory sf )
    throws SlotAlreadyRegisteredException;
  }
}
