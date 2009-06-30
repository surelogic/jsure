package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.View;

/**
 * A view of a {@link ForestModel}.
 *
 * <P>An implementation must support the component-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * </ul>
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
public interface ForestView
extends View
{
}

