package edu.cmu.cs.fluid.mvc;


/**
 * A view of one or more models that exports a model, where the exported
 * model is derived from the viewed model. The name "Stateful" comes from
 * the notion that the view is allowed to add new state to the model it
 * exports.  This interface is the root of all stateful views.
 *
 * <p>An attribute of a stateful may be indexed either by the nodes of
 * source model(s), or by nodes of the exported model.  (This can be a bit
 * confusing because often the nodes of the exported model are shared with 
 * source models.)  <em>Unless otherwise noted, the attributes described in 
 * sub-interfaces are indexed by nodes of the exported model</em>.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link View#VIEW_NAME}
 * <li>{@link View#SRC_MODELS}
 * </ul>
 *
 * <p>The values of the <code>MODEL_NAME</code> and
 * <code>VIEW_NAME</code> attributes do not need to be the same.
 *
 * <P>An implementation  must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ModelToModelStatefulView
extends View, Model
{
}
