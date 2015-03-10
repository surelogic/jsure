/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SimpleLabeledSequenceFactory.java,v 1.9 2006/03/29 19:54:51 chance Exp $
 *
 * SimpleLabeledSequenceFactory.java
 * Created on March 1, 2002, 3:17 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating instances of LabeledSequence.  Models returned by
 * the factory implement only the minimum requirements of
 * {@link LabeledSequence} and are of variable length.  The
 * {@link SequenceModel#INDEX} and {@link SequenceModel#LOCATION} attributes of
 * the created models may or may not be mutable. 
 * 
 * <p>The class uses "singleton" patterns, and thus has a private
 * constructor.  The only instances of the class are referred to 
 * by the fields {@link #mutablePrototype} and {@link #immutablePrototype}.
 *
 * @author Aaron Greenhouse
 */
public final class SimpleLabeledSequenceFactory
implements LabeledSequence.Factory
{
  /**
   * The singleton reference to the factory that produces models with
   * mutable {@link SequenceModel#INDEX} and {@link SequenceModel#LOCATION}
   * attributes.
   */
  public static final LabeledSequence.Factory mutablePrototype =
    new SimpleLabeledSequenceFactory( true );
  
  /**
   * The singleton reference to the factory that produces models with
   * immutable {@link SequenceModel#INDEX} and {@link SequenceModel#LOCATION}
   * attributes.
   */
  public static final LabeledSequence.Factory immutablePrototype =
    new SimpleLabeledSequenceFactory( false );
  
  
  
  /**
   * Whether the attributes {@link SequenceModel#INDEX} and
   * {@link SequenceModel#LOCATION} should be mutable.
   */
  private final boolean isMutable;
  
  
  
  /**
   * Use the singleton references {@link #mutablePrototype} and
   * {@link #immutablePrototype}.
   * @param isMutable Whether the attributes
   *    {@link SequenceModel#INDEX} and {@link SequenceModel#LOCATION}
   *     should be mutable.
   */
  private SimpleLabeledSequenceFactory( final boolean isMutable )
  {
    this.isMutable = isMutable;
  }

  

  // inherit javadoc
  @Override
  public LabeledSequence create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = sf.newSequence( ~0 );
    return new LabeledSequenceImpl(
                 name, sf,
                 new ModelCore.StandardFactory( sf ), 
                 new SequenceModelCore.StandardFactory(
                       seq, sf, isMutable ) );
  }

}
