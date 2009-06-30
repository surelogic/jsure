// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/diff/DifferenceSyntaxForestModel.java,v 1.7 2003/07/15 21:47:21 aarong Exp $

package edu.cmu.cs.fluid.mvc.tree.syntax.diff;

import edu.cmu.cs.fluid.mvc.tree.diff.DifferenceForestModel;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModel;

/**
 * A stateful view that computes the difference of two input <em>syntax forest</em>
 * models.  The two models are distinguished as the <em>base</em>
 * and the <em>delta</em>.  The difference describes how to
 * convert the base model into the delta model.
 *
 * <p>This interface provides a basis difference semantics based on
 * nodes changing location, being removed, or being added.  Sub-interfaces
 * are free to add additional difference semantics.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link edu.cmu.cs.fluid.mvc.diff.DifferenceModel#BASE_MODEL}: In this case the
 *   model is guaranteed to be an implementation of
 *   {@link edu.cmu.cs.fluid.mvc.tree.ForestModel}.
 * <li>{@link edu.cmu.cs.fluid.mvc.diff.DifferenceModel#DELTA_MODEL}: In this case the
 *   model is guaranteed to be an implementation of
 *   {@link edu.cmu.cs.fluid.mvc.tree.ForestModel}.
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#ROOTS}
 * <li>{@link #DEFAULT_ATTR_SRC}
 * </ul>
 *
 * <p>The values of the <code>MODEL_NAME</code> and
 * <code>VIEW_NAME</code> attributes do not need to be the same.
 * The <code>BASE_MODEL</code> and <code>DELTA_MODEL</code>
 * attributes must contain models that are part of the
 * <code>SRC_MODELS</code> attribute.
 *
 * <P>An implementation  must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#LOCATION}
 * <li>{@link SyntaxForestModel#OPERATOR}
 * <li>{@link DifferenceForestModel#DIFF_LOCAL}
 * <li>{@link DifferenceForestModel#DIFF_POSITION}
 * <li>{@link DifferenceForestModel#DIFF_SUBTREE}
 * <li>{@link DifferenceForestModel#DIFF_LABEL}
 * <li>{@link DifferenceForestModel#NODE_ATTR_SRC}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface DifferenceSyntaxForestModel
extends SyntaxForestModel, DifferenceForestModel
{
}

