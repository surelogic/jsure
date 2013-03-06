/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/ConfigurableSetViewFactory.java,v 1.6 2003/07/15 18:39:12 thallora Exp $
 *
 * ConfigurableSetViewFactory.java
 * Created on March 25, 2002, 10:54 AM
 */

package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory implementation that returns minimal implementations of 
 * {@link ConfigurableSetView}.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor.  Clients should use the {@link #prototype} field
 * to access the only instances of this class.
 *
 * @author Aaron Greenhouse
 */
public final class ConfigurableSetViewFactory
implements ConfigurableSetView.Factory
{
  /**
   * The singleton reference.
   */
  public static final ConfigurableSetView.Factory prototype =
    new ConfigurableSetViewFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private ConfigurableSetViewFactory()
  {
  }
  
  /**
   * Create a new instances of {@link ConfigurableSetView}.
   * @param name The name of the new model.
   * @param src The SetModel to be viewed.
   * @param vizModel The Visibility Model of the provided SetModel.
   * @param attrPolicy The policy for inheriting attributes in the
   *  exported model.
   * @param proxyPolicy The policy for attributing proxy nodes.
   * @param ellipsisPolicy The initial value for the
   * {@link ConfigurableSetView#ELLIPSIS_POLICY} attribute.
   */
  @Override
  public ConfigurableSetView create(
    final String name, final SetModel src, final VisibilityModel vizModel,
    final AttributeInheritancePolicy attrPolicy,
    final ProxyAttributePolicy proxyPolicy, final boolean ellipsisPolicy )
  throws SlotAlreadyRegisteredException
  {
    return new ConfigurableSetViewImpl( 
                 name, src, vizModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, 
                 new SetModelCore.StandardFactory(
                       SimpleSlotFactory.prototype,
                       SimpleComponentSlotFactory.simplePrototype ),
                 ConfigurableViewCore.standardFactory, 
                 attrPolicy, proxyPolicy,
                 (ellipsisPolicy ? Boolean.TRUE : Boolean.FALSE) );
  }
}
