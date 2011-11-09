// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/ModelAndVisibilityToModelStatefulView.java,v 1.11 2003/07/15 21:47:18 aarong Exp $

package edu.cmu.cs.fluid.mvc.visibility;
import edu.cmu.cs.fluid.mvc.ModelToModelStatefulView;

/**
 * StatefulView of a single <code>Model</code> and 
 * a single <code>VisibilityModel</code>.  Exports a <code>Model</code>
 * based on the source model, whose nodes are influenced by the
 * visibility information in the <code>VisibilityModel</code>.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
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
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ModelAndVisibilityToModelStatefulView
extends ModelToModelStatefulView, VisibilityView
{
}

