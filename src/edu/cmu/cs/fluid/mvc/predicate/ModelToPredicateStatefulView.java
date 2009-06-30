/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/ModelToPredicateStatefulView.java,v 1.13 2003/07/15 21:47:19 aarong Exp $*/
package edu.cmu.cs.fluid.mvc.predicate;

import edu.cmu.cs.fluid.mvc.sequence.ModelToSequenceStatefulView;

/**
 * A view of exactly one <code>Model</code> that exports an
 * <code>PredicateModel</code> of the model's attributes.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.set.SetModel#SIZE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link PredicateModel#PREDICATES_OF}
 * </ul>
 *
 * <p>The value of {@link PredicateModel#PREDICATES_OF} must be a member
 * of the set that is the value of {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}.
 * The values of the <code>MODEL_NAME</code> and
 * <code>VIEW_NAME</code> attributes do not need to be the same.
 *
 * <P>An implementation  must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#LOCATION}
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#INDEX}
 * <li>{@link PredicateModel#ATTR_NODE}
 * <li>{@link PredicateModel#PREDICATE}
 * <li>{@link PredicateModel#IS_VISIBLE}
 * <li>{@link PredicateModel#IS_STYLED}
 * <li>{@link PredicateModel#ATTRIBUTE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ModelToPredicateStatefulView
extends ModelToSequenceStatefulView, PredicateModel
{
}

