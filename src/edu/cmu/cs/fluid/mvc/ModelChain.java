// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ModelChain.java,v 1.8 2003/07/15 21:47:18 aarong Exp $
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.mvc.attr.AttributeModel;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.visibility.AttributeBasedPredicateVisibilityView;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.render.StyleSetModel;

/**
 * Interface for classes that package up commonly used Models and Views,
 * focusing on those used for rendering.
 *
 * Assumes that there is a base model that everything else views, and that
 * there is a main chain off branching from the base model (e.g., base model
 * and configurable view).  Other chains are auxiliary (like attribute and 
 * predicate models), or derivative (not handled here)
 */
public interface ModelChain {
  /** Gets the model that the other auxiliary models are based off of.
   *  Note this is not always a PureModel.
   */
  Model getBaseModel();

  /** Gets the last model in the main chain off the base, which could be the 
   *   base model
   */
  Model getLastModel();

  /** Gets the model that the other auxiliary models are based off of.
   *  Note that the model indexed by 0 is the base model.
   *  Note this is not always a PureModel.
   * @param index An index from 0 to {@link #size}-1
   */
  Model getModelAt(int index);

  /** Gets the number of models in the main chain */
  int size();

  /** Gets a VisibilityModel for the base model */
  VisibilityModel getVisModel();

  /** Gets the {@link edu.cmu.cs.fluid.mvc.predicate.PredicateModel} for the base model */
  PredicateModel getPredModel();

  /** Gets the {@link edu.cmu.cs.fluid.mvc.attr.AttributeModel} for the base model */
  AttributeModel getAttrModel();

  /** Gets the PredicateVisibilityView based on {@link #getAttrModel} */
  AttributeBasedPredicateVisibilityView getPredVisModel();

  /** Gets the palette for this chain (possibly shared across chains) */
  StyleSetModel getPalette();
}
