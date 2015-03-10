/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/SimplePredicateViewFactory.java,v 1.9 2006/03/29 19:54:51 chance Exp $
 *
 * SimplePredicateViewFactory.java
 * Created on March 14, 2002, 4:30 PM
 */

package edu.cmu.cs.fluid.mvc.predicate;


import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating instances of SimplePredicateView.
 * Models returned by the factory
 * implement only the minimum requirements of {@link SimplePredicateView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public final class SimplePredicateViewFactory
implements SimplePredicateView.Factory
{
  /**
   * The singletone reference
   */
  public static final SimplePredicateView.Factory prototype =
    new SimplePredicateViewFactory();
  
  
  
  /** 
   * Private constructor; use the prototype reference {@link #prototype}.
   */
  private SimplePredicateViewFactory()
  {
  }

  @Override
  public SimplePredicateView create( final String name, final Model src )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = SimpleSlotFactory.prototype.newSequence(~0);
    return new SimplePredicateViewImpl(
                 name, src, ModelCore.simpleFactory,
                 ViewCore.standardFactory,
                 new SequenceModelCore.StandardFactory(
                       seq,                       
                       SimpleSlotFactory.prototype, true ),
                 new PredicateModelCore.StandardFactory(
                       SimpleSlotFactory.prototype ) );
  }  
}
