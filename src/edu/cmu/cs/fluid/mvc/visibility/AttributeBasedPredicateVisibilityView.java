/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/AttributeBasedPredicateVisibilityView.java,v 1.8 2003/07/15 21:47:18 aarong Exp $
 *
 * AttributeBasedPredicateVisibilityView.java
 * Created on March 19, 2002, 3:31 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.attr.AttributeModel;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * A specialization of {@link AttributeAndPredicateToVisibilityStatefulView}
 * that uses a new node-level attribute {@link #IS_DISPLAYED}, indexed by the
 * nodes of the source AttributeModel to determine which nodes in the
 * PredicateModel should be visible: Only the predicate nodes associated with 
 * attributes whose node in the AttributeModel {@link #IS_DISPLAYED} have
 * <code>true</code> values for their {@link VisibilityModel#IS_VISIBLE}
 * attribute.  (Reminder: The mapping from attributes to predicates is
 * one-to-many, which is why some kind of more generic toggle-based 
 * visibility view is not applicable to this case.)
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link VisibilityModel#VISIBILITY_OF}
 * </ul>
 *
 * <p>The values of the MODEL_NAME and VIEW_NAME attributes do not
 * need to be the same.
 *
 * <P>An implementation must support the node-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link VisibilityModel#IS_VISIBLE}
 * <li>{@link #IS_DISPLAYED}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface AttributeBasedPredicateVisibilityView
extends AttributeAndPredicateToVisibilityStatefulView
{
  /**
   * Node-level, boolean-valued, mutable attribute indexed by the nodes
   * in the source AttributeModel.  When <code>true</code> for a node N, the 
   * nodes in the source PredicateModel that are associated with the
   * attribute identified by N are visible in the exported model.
   */
  public static final String IS_DISPLAYED =
    "AttributeBasedPredicateVisibilityView.isDisplayed";
  
  /**
   * Convienence method to set the value of {@link #IS_DISPLAYED}.
   * @param node A node in the source AttributeModel.
   * @param val <code>true</code> if the predicates associated with the
   *    attribute identified by the node should be visible.
   */
  public void setDisplayed( IRNode node, boolean val );
  
  /**
   * Convienence method to get the value of {@link #IS_DISPLAYED}.
   * @param node A node in the source AttributeModel.
   */
  public boolean isDisplayed( IRNode node );
  
  
  
  /**
   * Interface for factories that return models that implement 
   * {@link AttributeBasedPredicateVisibilityView}.
   */
  public interface Factory
  {
    /**
     * Create a new instance of a {@link AttributeBasedPredicateVisibilityView}.
     * @param name The name of the model.
     * @param attrModel The attribute model to use to control the visibility
     *   of the nodes in the predicate model.
     * @param predModel The predicate model whose visibility is being 
     *   modeled by the new stateful viev.
     */
    public AttributeBasedPredicateVisibilityView create(
      String name, AttributeModel attrModel, PredicateModel predModel )
    throws SlotAlreadyRegisteredException;
  }
}
