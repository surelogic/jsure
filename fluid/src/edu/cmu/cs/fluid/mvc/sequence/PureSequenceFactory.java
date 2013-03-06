/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/PureSequenceFactory.java,v 1.9 2006/03/29 19:54:51 chance Exp $
 *
 * PureSequenceFactory.java
 * Created on March 1, 2002, 2:12 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Factory for creating instances of PureSequence.  Models returned by the factory
 * implement only the minimum requirements of {@link SequenceModel}, and
 * are always mutable variable-length sequences.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public final class PureSequenceFactory
implements PureSequence.Factory
{
  /**
   * The singleton reference.
   */
  public static final PureSequence.Factory prototype = new PureSequenceFactory();
  
  
  
  /**
   * Constructor for subclassing only;
   * use the singleton reference {@link #prototype}.
   */
  private PureSequenceFactory()
  {
  }
  

  // inherit javadoc
  @Override
  public PureSequence create( final String name, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = sf.newSequence(~0);
    return new PureSequenceImpl(
                 name,
                 new ModelCore.StandardFactory( sf ), 
                 new SequenceModelCore.StandardFactory(
                       seq, sf, true ), sf );
  }
}
