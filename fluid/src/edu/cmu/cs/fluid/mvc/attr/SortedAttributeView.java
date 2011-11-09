/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/SortedAttributeView.java,v 1.8 2003/07/15 21:47:19 aarong Exp $
 *
 * SortedAttributeView.java
 * Created on March 7, 2002, 3:30 PM
 */

package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.sequence.SortedView;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * A specialization of {@link edu.cmu.cs.fluid.mvc.sequence.SortedView} that specifically views
 * an {@link AttributeModel}.  All the standard attributes of 
 * {@link AttributeModel} are inherited as either immutable or mutable-source
 * at the descretion of the caller of the constructor.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#SIZE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link AttributeModel#ATTRIBUTES_OF}
 * <li>{@link SortedView#IS_ASCENDING}
 * <li>{@link SortedView#SORT_ATTR}
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
 * <li>{@link AttributeModel#ATTR_NAME}
 * <li>{@link AttributeModel#ATTR_LABEL}
 * <li>{@link AttributeModel#ATTR_KIND}
 * <li>{@link AttributeModel#ATTR_TYPE}
 * <li>{@link AttributeModel#DOMAIN}
 * <li>{@link AttributeModel#IS_MUTABLE}
 * <li>{@link AttributeModel#IS_NODE_ATTR}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SortedAttributeView
extends AttributeView, AttributeToSequenceStatefulView, SortedView, AttributeModel
{
  public static interface Factory
  {
    /**
     * Create a new sorted view of an AttributeModel.  The provided attribute
     * inheritance policy is wrapped to insure that all the requried 
     * {@link AttributeModel} attributes are inherited; the node attributes
     * may (all) be inherited either as
     * {@link edu.cmu.cs.fluid.mvc.AttributeInheritanceManager#IMMUTABLE} or
     * {@link edu.cmu.cs.fluid.mvc.AttributeInheritanceManager#MUTABLE_SOURCE}.
     *
     * @param name The name of the new model.
     * @param srcModel The SetModel to be sorted.
     * @param attr The initial attribute whose values should be used to
     *    order the nodes.
     * @param isAscending Whether the nodes should be initially sorted in
     *    ascending order.
     * @param policy The policy controlling which attributes of the
     *    source model are inherited by the sorted view.
     * @exception IllegalArgumentException Thrown if <code>attr</code>
     *   does not name a node-level attribute of the source model that
     *   has been inherited by the sorted view.
     */
    public SortedAttributeView create(
      String name, AttributeModel srcModel, String attr, boolean isAscending,
      AttributeInheritancePolicy policy )
    throws SlotAlreadyRegisteredException;
  }
}
