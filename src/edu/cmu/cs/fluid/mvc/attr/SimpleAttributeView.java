/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/SimpleAttributeView.java,v 1.13 2003/07/15 21:47:19 aarong Exp $
 *
 * SimpleAttributeView.java
 * Created on March 6, 2002, 2:17 PM
 */

package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * A specialization of {@link ModelToAttributeStatefulView} that has exactly
 * one source model (which is the source for the attributes presented by the
 * stateful view.)
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
 * <li>{@link AttributeModel#DOMAIN}
 * <li>{@link AttributeModel#IS_NODE_ATTR}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SimpleAttributeView
extends ModelToAttributeStatefulView
{
  /**
   * Factory for creating instances of {@link SimpleAttributeView}.
   */
  public static interface Factory
  {
    /**
     * Create a SimpleAttributeView that models the attributes of
     * the given Model.
     */
    public SimpleAttributeView create( String name, Model src )
    throws SlotAlreadyRegisteredException;
  }
}

