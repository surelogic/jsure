package edu.cmu.cs.fluid.mvc.tree;


/**
 * A <code>StatefulView</code> that is a view of at least one
 * <code>ForestModel</code> and exports a <code>ForestModel</code>.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link ForestModel#ROOTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
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
 * <li>{@link DigraphModel#CHILDREN}
 * <li>{@link SymmetricDigraphModel#PARENTS}
 * <li>{@link ForestModel#LOCATION}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ForestToForestStatefulView
extends ModelToForestStatefulView, ForestToModelStatefulView
{
}

