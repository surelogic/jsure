// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/ModelToAttributeStatefulView.java,v 1.9 2003/07/15 21:47:19 aarong Exp $
package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.set.ModelToSetStatefulView;

/**
 * A stateful view that models that attributes of one of its source models.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.set.SetModel#SIZE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link AttributeModel#ATTRIBUTES_OF}
 * </ul>
 *
 * <P>An implementation  must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link AttributeModel#ATTR_NAME}
 * <li>{@link AttributeModel#ATTR_LABEL}
 * <li>{@link AttributeModel#ATTR_TYPE}
 * <li>{@link AttributeModel#IS_MUTABLE}
 * <li>{@link AttributeModel#IS_NODE_ATTR}
 * </ul>
 *
 * @author Edwin Chan
 */
public interface ModelToAttributeStatefulView
extends ModelToSetStatefulView, AttributeModel
{
}
