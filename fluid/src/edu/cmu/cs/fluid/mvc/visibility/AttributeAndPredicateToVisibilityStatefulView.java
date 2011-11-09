/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/AttributeAndPredicateToVisibilityStatefulView.java,v 1.8 2003/07/15 21:47:18 aarong Exp $
 *
 * AttributeAndPredicateToVisibilityStatefulView.java
 * Created on March 19, 2002, 3:23 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.attr.AttributeView;
import edu.cmu.cs.fluid.mvc.predicate.PredicateView;


/**
 * A stateful view that views and AttributeModel and PredicateModel, both
 * of which are views of the same model, and exports a VisibilityModel.
 *
 * <p><em>This interface is not a sub-interface of
 * {@link ModelAndPredicateToVisibilityStatefulView} because that interface
 * is for VisibilityModels whose visibility is influenced by the state of
 * the PredicateModel, whereas this model is for the visibility of the 
 * PreciateModel itself, and is influenced by the state of the 
 * AttributeModel.</em>
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
public interface AttributeAndPredicateToVisibilityStatefulView
extends AttributeView, PredicateView, ModelToVisibilityStatefulView
{
  
}
