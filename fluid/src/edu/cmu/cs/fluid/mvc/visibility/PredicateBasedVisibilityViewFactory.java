/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/PredicateBasedVisibilityViewFactory.java,v 1.6 2003/07/15 18:39:10 thallora Exp $
 *
 * PredicateaBasedVisibilityViewFactory.java
 * Created on March 18, 2002, 6:05 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating minimal instances of PredicateBasedVisibilityView.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public class PredicateBasedVisibilityViewFactory
implements PredicateBasedVisibilityView.Factory
{
  /**
   * The singleton reference.
   */
  public static final PredicateBasedVisibilityView.Factory prototype =
    new PredicateBasedVisibilityViewFactory();
  
  
  
  /** Use the singleton reference {@link #prototype} */
  private PredicateBasedVisibilityViewFactory()
  {
  }
  
  /**
   * Create a new PredicateVisibilityView model.
   * @param name The name of the model.
   * @param srcModel The model whose visibility is to be modeled.
   * @param predModel The predicate model used to control the visibility.
   *  A run-time exception will be thrown if this model is not a
   *  predicate model of the <cide>srcModel</code>.
   */
  @Override
  public PredicateBasedVisibilityView create(
    final String name, final Model srcModel, final PredicateModel predModel )
  throws SlotAlreadyRegisteredException
  {
    return new PredicateBasedVisibilityViewImpl(
                 name, srcModel, predModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, VisibilityModelCore.standardFactory );
  }
}
