// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/SimpleModelChain.java,v 1.7 2005/07/01 16:15:36 chance Exp $
package edu.cmu.cs.fluid.mvc;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.attr.SimpleAttributeViewFactory;
import edu.cmu.cs.fluid.mvc.predicate.SimplePredicateViewFactory;
import edu.cmu.cs.fluid.mvc.visibility.AttributeBasedPredicateVisibilityViewFactory;
import edu.cmu.cs.fluid.mvc.visibility.PredicateBasedVisibilityViewFactory;
import edu.cmu.cs.fluid.render.StyleSetFactory;
import edu.cmu.cs.fluid.render.StyledPredicateViewFactory;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

/**
 * A concrete implementation of ModelChain that creates all the
 * auxiliary chains directly on the base model, and also uses
 * SimpleSlots for their storage.
 */
public final class SimpleModelChain extends AbstractModelChain {
  private final Set<String> namesUsed = new HashSet<String>();
  private int uniquifier = 0;

  /** 
   * Creates the chain and auxiliary models eagerly (vs on-demand)
   * Also checks to see the auxiliary models need to be renamed, due
   * to name conflicts.
   */
  public SimpleModelChain(Model model) throws SlotAlreadyRegisteredException {
    super(model);

    final ExplicitSlotFactory sf = SimpleExplicitSlotFactory.prototype;
    String name          = model.getName();
    if (namesUsed.contains(name)) {
      name += uniquifier;
      uniquifier++;
    }
    namesUsed.add(name);

    // Set up the style palette
    palette = StyleSetFactory.prototype.create(name+" palette", sf);

    // Create the predicate model
    predModel = SimplePredicateViewFactory.prototype.create(name+" preds", 
							    model);
    StyledPredicateViewFactory.prototype.configure(predModel, palette, sf);
						   
    attrModel = SimpleAttributeViewFactory.prototype.create(name+" attrs", 
							    model);

    // Init Visibility Model for nodes in the base model
    visModel = 
      PredicateBasedVisibilityViewFactory.prototype.create(name+" viz", 
							   model, predModel );

    // Init Visibility Model for predicates
    predVisModel =
      AttributeBasedPredicateVisibilityViewFactory.prototype
      .create(predModel.getName()+" viz", attrModel, predModel );
  }
}
