/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/SimpleAttributeViewFactory.java,v 1.7 2004/09/10 17:33:54 boyland Exp $
 *
 * SimpleAttributeViewFactory.java
 * Created on March 6, 2002, 2:26 PM
 */

package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.SimpleComponentSlotFactory;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating models that implement the minimum requirements of
 * {@link SimpleAttributeView}.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}. 
 *
 * @author Aaron Greenhouse
 */
public final class SimpleAttributeViewFactory
implements SimpleAttributeView.Factory
{
  /**
   * The singleton reference.
   */
  public static final SimpleAttributeView.Factory prototype =
    new SimpleAttributeViewFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private SimpleAttributeViewFactory()
  {
  }
  

  // Inherit javadoc 
  @Override
  public SimpleAttributeView create( final String name, final Model src )
  throws SlotAlreadyRegisteredException
  {
    return new SimpleAttributeViewImpl(
                 name, src, ModelCore.simpleFactory,
                 ViewCore.standardFactory,
                 new SetModelCore.StandardFactory(
                       SimpleExplicitSlotFactory.prototype,
                       new SimpleComponentSlotFactory(
                             SimpleExplicitSlotFactory.prototype ) ),
                 new AttributeModelCore.StandardFactory(
                       SimpleExplicitSlotFactory.prototype ) );
  }
}
