/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/IdentityVisibilityView.java,v 1.8 2003/07/15 21:47:18 aarong Exp $
 *
 * IdentityVisibilityView.java
 * Created on March 15, 2002, 3:59 PM
 */
package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Specialization of {@link ModelToVisibilityStatefulView} that always says
 * that all nodes of its source model are visible.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link VisibilityModel#VISIBILITY_OF}
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
 * <li>{@link VisibilityModel#IS_VISIBLE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface IdentityVisibilityView
extends ModelToVisibilityStatefulView
{
  /**
   * Factory for creating instances of {@link IdentityVisibilityView}.
   */
  public static interface Factory
  {
    /**
     * Create a new instance of {@link IdentityVisibilityView} that
     * views that given model.
     */
    public IdentityVisibilityView create( String name, Model srcModel )
    throws SlotAlreadyRegisteredException;
  }
}
