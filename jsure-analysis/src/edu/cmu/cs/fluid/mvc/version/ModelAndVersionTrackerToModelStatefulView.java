// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/ModelAndVersionTrackerToModelStatefulView.java,v 1.4 2003/07/15 18:39:11 thallora Exp $
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.mvc.*;

/**
 * StatefulView of a single {@link Model} and 
 * a single {@link VersionTrackerModel}.  Exports a Model
 * that is a projection of the source Model fixed at
 * the version referred to by the version tracker.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link View#VIEW_NAME}
 * <li>{@link View#SRC_MODELS}
 * </ul>
 *
 * <p>The values of the MODEL_NAME and VIEW_NAME attributes do not
 * need to be the same.
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ModelAndVersionTrackerToModelStatefulView
extends ModelToModelStatefulView, VersionTrackerView
{
}

