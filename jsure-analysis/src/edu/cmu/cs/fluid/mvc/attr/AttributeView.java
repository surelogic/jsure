// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/AttributeView.java,v 1.10 2003/07/15 21:47:19 aarong Exp $
package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.set.SetView;

/**
 * A view of an {@link AttributeModel}.
 *
 * <P>An implementation must support the
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface AttributeView
extends SetView
{
}

