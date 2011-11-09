/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/SetToSetStatefulView.java,v 1.10 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.set;


/**
 * A <code>StatefulView</code> that is a view of exactly 1
 * <code>SequenceModel</code>s and exports a <code>SequenceModel</code>.
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
 * <P>An implementaiton of StatefulView must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}

 * </ul>
 * 
 * @author Aaron Greenhouse
 */
public interface SetToSetStatefulView
extends SetView, ModelToSetStatefulView
{
}

