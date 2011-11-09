/*
 * Created on Oct 6, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.database;

import edu.cmu.cs.fluid.mvc.*;


/**
 * A <code>StatefulView</code> that is a view of one or more
 * generic <code>Model</code>s and exports a <code>DatabaseModel</code>.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link DatabaseModel#TABLE_NAME}
 * <li>{@link DatabaseModel#TABLE_SIZE}
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
 * <li>{@link DatabaseModel#COLUMN_NAME}
 * <LI>{@link DatabaseModel#COLUMN_TYPE}
 * </ul>
 * 
 * @author chance
 */
public interface ModelToDatabaseStatefulView extends ModelToModelStatefulView, DatabaseModel {

}
