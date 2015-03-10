/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/WrappingLabeledSequenceFactory.java,v 1.7 2006/03/30 19:47:20 chance Exp $
 *
 * WrappingLabeledSequenceFactory.java
 * Created on March 1, 2002, 3:44 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.*;

/**
 * Factory for creating instances of LabeledSequence that wrap a provided
 * IRSequence.  That is, the model structure is defined by the given sequence.
 * Models returned by the factory implement only the minimum requirements of
 * {@link LabeledSequence} and the {@link SequenceModel#INDEX} and
 * {@link SequenceModel#LOCATION} attributes may or may not be mutable..
 *
 * @author Aaron Greenhouse
 */
public class WrappingLabeledSequenceFactory
implements LabeledSequence.Factory
{
  /**
   * <code>true</code> if {@link SequenceModel#INDEX} and
   * {@link SequenceModel#LOCATION} attributes of the models created by this
   * factory should be mutable.
   */
  private final boolean isMutable;
  
  /**
   * The sequence to use as the underlying structure of the modles
   * created by this factory.
   */
  private final IRSequence<IRNode> wrapped;
  
  
  /** 
   * Creates a new instance of WrappingLabeledSequenceFactory.
   * @param seq The sequence to wrap.
   * @param isMut Whether the {@link SequenceModel#INDEX} and
   *   {@link SequenceModel#LOCATION} attributes of the created models 
   *   should be mutable.
   */
  public WrappingLabeledSequenceFactory( final IRSequence<IRNode> seq, final boolean isMut )
  {
    isMutable = isMut;
    wrapped = seq;
  }
  
  
  
  /**
   * Create a new LabeledSequence model instance.
   * @param name The name of the model.
   * @param sf The slot factory to use to create the model's structural and
   *   informational attributes.  Note, however, the structure of the 
   *   sequence is is already defined by the specific IRSequence wrapped
   *   by the model.)
   */
  @Override
  public LabeledSequence create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    return new LabeledSequenceImpl(
                 name, sf, new ModelCore.StandardFactory( sf ), 
                 new SequenceModelCore.StandardFactory( wrapped, sf, isMutable ) );
  }
}
