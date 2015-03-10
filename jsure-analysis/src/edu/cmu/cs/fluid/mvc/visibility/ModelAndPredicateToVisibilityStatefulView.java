/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/ModelAndPredicateToVisibilityStatefulView.java,v 1.12 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.predicate.PredicateView;

/**
 * A <code>StatefulView</code> that is a view of one 
 * generic <code>Model</code>, and an <code>PredicateModel</code> of
 * that model, and exports a <code>VisibilityModel</code>.
 * The nodes in the exported model are those nodes that are in
 * the source model.  The source <code>PredicateModel</code> is 
 * only used to influence the values of the <code>IS_VISIBLE</code>
 * attribute.
 *
 * <p>It is a RuntimeException if the
 * {@link edu.cmu.cs.fluid.mvc.predicate.PredicateModel#PREDICATES_OF}
 * attribute of the source predicat model is not the provided value for
 * the {@link VisibilityModel#VISIBILITY_OF} attribute of this model.
 *
 * <P>An implementation must support the component-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link VisibilityModel#VISIBILITY_OF}
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
public interface ModelAndPredicateToVisibilityStatefulView
extends ModelToVisibilityStatefulView, PredicateView
{
}
