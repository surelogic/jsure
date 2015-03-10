/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/ConfigurableForestViewFactory.java,v 1.8 2004/05/25 18:34:16 chance Exp $
 *
 * ConfigurableForestViewFactory.java
 * Created on April 3, 2002, 1:38 PM
 */

package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ConfigurableViewCore;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory that returns minimal implementations of {@link ConfigurableForestView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}. 
 *
 * @author Aaron Greenhouse
 */
public class ConfigurableForestViewFactory
implements ConfigurableForestView.Factory
{
  /**
   * The singleton reference.
   */
  public static final ConfigurableForestView.Factory prototype =
    new ConfigurableForestViewFactory();
  
  
  
 
  /**
   * Use the singleton reference {@link #prototype}.
   */
  public ConfigurableForestViewFactory()
  {
  }

  
  
  @Override
  public ConfigurableForestView create(final String name,
      final ForestModel src, final VisibilityModel vizModel,
      final AttributeInheritancePolicy aip,
      final ForestProxyAttributePolicy pp,
      final ForestEllipsisPolicy ellipsisPolicy, final boolean expFlat,
      final boolean expPath) throws SlotAlreadyRegisteredException {
    return new ConfigurableForestViewImpl(name, src, vizModel,
        ModelCore.simpleFactory, ViewCore.standardFactory,
        new ForestForestModelCore.StandardFactory(SimpleSlotFactory.prototype,
            false), ConfigurableViewCore.standardFactory, aip, pp,
        ellipsisPolicy, expFlat, expPath);
  }
}
