/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/IdentityVisibilityViewFactory.java,v 1.6 2003/07/15 18:39:10 thallora Exp $
 *
 * IdentityVisibilityViewFactory.java
 * Created on March 15, 2002, 5:12 PM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Factory for creating instances of {@link IdentityVisibilityView}.
 *
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public class IdentityVisibilityViewFactory
implements IdentityVisibilityView.Factory
{
  /**
   * The singleton reference.
   */
  public static final IdentityVisibilityView.Factory prototype =
    new IdentityVisibilityViewFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private IdentityVisibilityViewFactory()
  {
  }
  
  
  
  @Override
  public IdentityVisibilityView create( final String name, final Model srcModel )
  throws SlotAlreadyRegisteredException
  {
    return new IdentityVisibilityViewImpl(
                 name, srcModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, VisibilityModelCore.standardFactory );
  }
}
