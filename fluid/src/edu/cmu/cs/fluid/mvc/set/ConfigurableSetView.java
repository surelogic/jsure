// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/ConfigurableSetView.java,v 1.13 2003/07/15 21:47:18 aarong Exp $
package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ConfigurableView;
import edu.cmu.cs.fluid.mvc.ProxyAttributePolicy;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * A view of an <em>unversioned</em> set that allows for 
 * nodes to be ellided.  The exported model can contain nodes
 * for which {@link #isEllipsis} is <code>true</code>.
 *
 * @author Aaron Greenhouse
 */
public interface ConfigurableSetView
extends ConfigurableView, SetToSetStatefulView
{
  /**
   * Attribute containing the ellipsis policy that should be used.
   * The value is of type {@link edu.cmu.cs.fluid.ir.IRBooleanType} and
   * is mutable.  A value of <code>true</code> means that an ellipsis
   * should be inserted, while a value of <code>false</code> means that
   * an ellipsis should not be inserted.
   */
  public static final String ELLIPSIS_POLICY = "SetView.ELLIPSIS_POLICY";

  
  
  /**
   * Set the ellipsis policy.
   */
  public void setSetEllipsisPolicy( boolean p );

  /**
   * Get the ellipsis policy
   */
  public boolean getSetEllipsisPolicy();
  
  
  
  /**
   * Factory interface for generating instances of {@link ConfigurableSetView}.
   */
  public static interface Factory
  {
    /**
     * Create a new instances of {@link ConfigurableSetView}.
     * @param name The name of the new model.
     * @param src The SetModel to be viewed.
     * @param vizModel The Visibility Model of the provided SetModel.
     * @param attrPolicy The policy for inheriting attributes in the
     *   exported model.
     * @param proxyPolicy The policy for attributing proxy nodes.
     * @param ellipsisPolicy The initial value for the
     *  {@link ConfigurableSetView#ELLIPSIS_POLICY} attribute.
     */
    public ConfigurableSetView create(
      String name, SetModel src, VisibilityModel vizModel,
      AttributeInheritancePolicy attrPolicy, ProxyAttributePolicy proxyPolicy,
      boolean ellipsisPolicy )
    throws SlotAlreadyRegisteredException;
  }
}
