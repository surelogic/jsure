/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/PredicateBasedVisibilityView.java,v 1.8 2003/07/15 21:47:18 aarong Exp $
 *
 * PredicateBasedVisibilityView.java
 * Created on March 18, 2002, 4:34 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * An empty specialization of {@link ModelAndPredicateToVisibilityStatefulView}.
 *
 * <P>An implementation must support the component-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link VisibilityModel#VISIBILITY_OF}
 * <li>{@link #DEFAULT_VISIBILITY}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link VisibilityModel#IS_VISIBLE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface PredicateBasedVisibilityView
extends ModelAndPredicateToVisibilityStatefulView
{
  /**
   * A mutable, boolean-valued, model-level attribute that indicates whether
   * nodes are visible by default.
   */
  public static final String DEFAULT_VISIBILITY =
    "PredicateBasedVisibilityView.defaultVisibility";

  
  
  /**
   * A convienence method for setting the default visibility.
   */
  public void setDefaultVisibility( boolean isVisible );
  
  /**
   * A convienence method for getting the default visibility.
   */
  public boolean getDefaultVisibility();
  

  
  /**
   * Interface for factories that return implementations of 
   * {@link PredicateBasedVisibilityView}.
   */
  public static interface Factory
  {
    /**
     * Create a new PredicateVisibilityView model.
     * @param name The name of the model.
     * @param srcModel The model whose visibility is to be modeled.
     * @param predModel The predicate model used to control the visibility.
     *   A run-time exception will be thrown if this model is not a
     *   predicate model of the <cide>srcModel</code>.
     */
    public PredicateBasedVisibilityView create(
      String name, Model srcModel, PredicateModel predModel )
    throws SlotAlreadyRegisteredException;
  }
}
