// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/AttributeToSequenceStatefulView.java,v 1.9 2003/07/15 21:47:19 aarong Exp $
package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.sequence.SetToSequenceStatefulView;

/**
 * A <code>StatefulView</code> that is a view of exactly 1
 * <code>AttributeModel</code>s and exports a <code>SequenceModel</code>.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.set.SetModel#SIZE}
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#FIRST}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
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
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#LOCATION}
 * <LI>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#INDEX}
 * <LI>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#NEXT}
 * <LI>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#PREVIOUS}
 * </ul>
 * 
 * @author Aaron Greenhouse
 * @author Edwin Chan
 */
public interface AttributeToSequenceStatefulView 
  extends AttributeView, SetToSequenceStatefulView
{
}
