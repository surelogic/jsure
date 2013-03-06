/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/FixedSizeLabeledSequenceFactory.java,v 1.10 2007/07/10 22:16:30 aarong Exp $
 *
 * FizedSizeLabeledSequenceFactory.java
 * Created on March 1, 2002, 3:17 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating fixed size instances of LabeledSequence.
 * Models returned by the factory implement only the minimum requirements of
 * {@link LabeledSequence}.  The {@link SequenceModel#INDEX} and
 * {@link SequenceModel#LOCATION} attributes of the created models may or
 * may not be mutable. 
 *
 * @author Aaron Greenhouse
 */
public final class FixedSizeLabeledSequenceFactory
implements LabeledSequence.Factory
{
  /**
   * Whether the attributes {@link SequenceModel#INDEX} and
   * {@link SequenceModel#LOCATION} should be mutable.
   */
  private final boolean isMutable;
  
  /** The size of the sequences to create. */
  private final int size;
  
  
  /**
   * Create a new factory that returns labeled sequences of a particular size.
   * @param size The size of the labeled sequences the factory should create.
   * @param isMutable Whether the attributes
   *    {@link SequenceModel#INDEX} and {@link SequenceModel#LOCATION}
   *     should be mutable.
   */
  public FixedSizeLabeledSequenceFactory(
    final boolean isMutable, final int size )
  {
    this.isMutable = isMutable;
    this.size = size;
  }

  

  // inherit javadoc
  @Override
  public LabeledSequence create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = sf.newSequence( size );
    return new LabeledSequenceImpl(
                 name, sf,
                 new ModelCore.StandardFactory( sf ), 
                 new SequenceModelCore.StandardFactory(
                      seq, sf, isMutable ) );
  }

}
