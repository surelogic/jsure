/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/AttributeBasedPredicateVisibilityViewFactory.java,v 1.6 2003/07/15 18:39:10 thallora Exp $
 *
 * AttributeBasedPredicateVisibilityViewFactory.java
 * Created on March 20, 2002, 10:18 AM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.attr.AttributeModel;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
/**
 * Factory implementation that returns minimal implementations of 
 * {@link AttributeBasedPredicateVisibilityView}.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor.  Clients should use the {@link #prototype} 
 * field to access the only instance of this class.
 *
 * @author Aaron Greenhouse
 */
public final class AttributeBasedPredicateVisibilityViewFactory
implements AttributeBasedPredicateVisibilityView.Factory
{
  /**
   * The singleton reference.
   */
  public static final AttributeBasedPredicateVisibilityView.Factory prototype =
    new AttributeBasedPredicateVisibilityViewFactory();
  
  
  
  /**
   * Use the singleton reference {@link #prototype}.
   */
  private AttributeBasedPredicateVisibilityViewFactory()
  {
  }

  /**
   * Create a new instance of a {@link AttributeBasedPredicateVisibilityView}.
   * @param name The name of the model.
   * @param attrModel The attribute model to use to control the visibility
   *  of the nodes in the predicate model.
   * @param predModel The predicate model whose visibility is being
   *  modeled by the new stateful viev.
   */
  @Override
  public AttributeBasedPredicateVisibilityView create(
    final String name, final AttributeModel attrModel,
    final PredicateModel predModel )
  throws SlotAlreadyRegisteredException
  {
    return new AttributeBasedPredicateVisibilityViewImpl(
                 name, predModel, attrModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, VisibilityModelCore.standardFactory );
  }
}
