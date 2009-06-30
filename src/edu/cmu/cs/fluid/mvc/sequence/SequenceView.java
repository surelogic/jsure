/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SequenceView.java,v 1.11 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.View;

/**
 * A view of a {@link SequenceModel}.
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
public interface SequenceView
extends View
{
}

