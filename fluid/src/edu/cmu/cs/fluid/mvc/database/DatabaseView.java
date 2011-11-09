/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/database/DatabaseView.java,v 1.1 2004/10/22 19:12:20 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.database;

import edu.cmu.cs.fluid.mvc.View;


/**
 * A view of a {@link DatabaseModel}.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link View#VIEW_NAME}
 * <li>{@link View#SRC_MODELS}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface DatabaseView
extends View
{
}

