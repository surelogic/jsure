/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SortedViewFactory.java,v 1.10 2006/03/29 19:54:51 chance Exp $
 *
 * SortedSetViewFactory.java
 * Created on March 7, 2002, 10:45 AM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating instances of SortedView.  Models returned by the factory
 * implement only the minimum requirements of {@link SortedView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public final class SortedViewFactory
implements SortedView.Factory
{
  /**
   * The singletone reference
   */
  public static final SortedView.Factory prototype = new SortedViewFactory();
  
  
  
  /** 
   * Private constructor; use the prototype reference {@link #prototype}.
   */
  private SortedViewFactory()
  {
  }
  
  
  
  /**
   * Create a new sorted view of a Model.
   * @param name The name of the new model.
   * @param srcModel The SetModel to be sorted.
   * @param attr The initial attribute whose values should be used to
   *   order the nodes.
   * @param isAscending Whether the nodes should be initially sorted in
   *   ascending order.
   * @param policy The policy controlling which attributes of the
   *   source model are inherited by the sorted view.
   * @exception IllegalArgumentException Thrown if <code>attr</code>
   *  does not name a node-level attribute of the source model that
   *  has been inherited by the sorted view.
   */
  @Override
  public SortedView create(
    final String name, final Model srcModel, final String attr,
    final boolean isAscending, final AttributeInheritancePolicy policy )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = SimpleSlotFactory.prototype.newSequence(~0);
    return new SortedViewImpl(
                 name, srcModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, 
                 new SequenceModelCore.StandardFactory(
                       seq,
                       SimpleSlotFactory.prototype, false ),
                 policy, attr, isAscending );
  }
}
