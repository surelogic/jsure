// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractModelChain.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.mvc.attr.AttributeModel;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.visibility.AttributeBasedPredicateVisibilityView;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.render.StyleSetModel;

/**
 * An partial implementation of ModelChain that provides storage for
 * the basic models needed.  Assumes that the main chain consists of
 * just the base model. 
 */
public abstract class AbstractModelChain implements ModelChain {
  protected final Model baseModel;
  protected VisibilityModel visModel;
  protected PredicateModel predModel;
  protected AttributeModel attrModel;
  protected AttributeBasedPredicateVisibilityView predVisModel;
  protected StyleSetModel palette;

  protected AbstractModelChain(Model model) {
    baseModel = model;
  }

  @Override
  public Model getBaseModel() {
    return baseModel;
  }

  @Override
  public Model getLastModel() {
    return baseModel;
  }

  @Override
  public Model getModelAt(int index) {
    if (index == 0) {
      return baseModel;
    }
    return null; 
  }

  @Override
  public int size() { return 1; }

  @Override
  public VisibilityModel getVisModel() { return visModel; }

  @Override
  public PredicateModel getPredModel() { return predModel; }

  @Override
  public AttributeModel getAttrModel() { return attrModel; }

  @Override
  public AttributeBasedPredicateVisibilityView getPredVisModel() { 
    return predVisModel; 
  }

  @Override
  public StyleSetModel getPalette() { return palette; }
}
