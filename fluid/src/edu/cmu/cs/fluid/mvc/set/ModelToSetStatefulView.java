/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/ModelToSetStatefulView.java,v 1.11 2004/10/22 19:11:17 aarong Exp $ */

package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.ModelToModelStatefulView;

/**
 * A <code>StatefulView</code> that is a view of one or more
 * generic <code>Model</code>s and exports a <code>SetModel</code>.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link SetModel#SIZE}
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
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ModelToSetStatefulView
extends SetModel, ModelToModelStatefulView
{
}

