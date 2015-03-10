/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/diff/DumbDifferenceForestFactory.java,v 1.6 2003/07/15 18:39:13 thallora Exp $
 *
 * LabeledSetFactory.java
 * Created on February 28, 2002, 10:39 AM
 */

package edu.cmu.cs.fluid.mvc.tree.diff;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.diff.DiffAttributeMergingManagerFactory;
import edu.cmu.cs.fluid.mvc.diff.DifferenceModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestForestModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating instances of DumbDifferenceForest.
 * Models returned by the factory
 * implement only the minimum requirements of {@link DumbDifferenceForest}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class DumbDifferenceForestFactory
implements DumbDifferenceForest.Factory
{
  /**
   * The singleton reference.
   */
  public static final DumbDifferenceForest.Factory prototype =
    new DumbDifferenceForestFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private DumbDifferenceForestFactory()
  {
  }
  
  @Override
  public DumbDifferenceForest create(
    final String name, final ForestModel base, final ForestModel delta )
  throws SlotAlreadyRegisteredException
  {
    return new DumbDifferenceForestImpl(
                 name, base, delta, ModelCore.simpleFactory,
                 ViewCore.standardFactory,
                 new ForestForestModelCore.StandardFactory(
                       SimpleSlotFactory.prototype, false ),
                 DifferenceModelCore.standardFactory,
                 DifferenceForestModelCore.standardFactory,
                 new DiffAttributeMergingManagerFactory(
                       DifferenceForestModel.DIFF_LOCAL,
                       DifferenceForestModel.DIFF_LABEL,
                       DumbDifferenceForestImpl.localElts[DifferenceForestModel.PHANTOM],
                       DifferenceForestModel.DEFAULT_ATTR_SRC,
                       DifferenceForestModel.NODE_ATTR_SRC ) );
  } 
}
