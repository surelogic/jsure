// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/diff/DifferenceModel.java,v 1.12 2003/07/15 21:47:19 aarong Exp $

package edu.cmu.cs.fluid.mvc.diff;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelToModelStatefulView;

/**
 * A stateful view that computes the difference of two input
 * models.  The two models are distinguished as the <em>base</em>
 * and the <em>delta</em>.  The difference describes how to
 * convert the base model into the delta model.
 *
 * <p>Implementing classes should define new attributes that
 * impose semenatics on the difference.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link #BASE_MODEL}
 * <li>{@link #DELTA_MODEL}
 * </ul>
 *
 * <p>The values of the <code>MODEL_NAME</code> and
 * <code>VIEW_NAME</code> attributes do not need to be the same.
 * The <code>BASE_MODEL</code> and <code>DELTA_MODEL</code>
 * attributes must contain models that are part of the
 * <code>SRC_MODELS</code> attribute.
 *
 * <P>An implementation  must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface DifferenceModel
extends ModelToModelStatefulView
{
  //===========================================================
  //== Model Attributes
  //===========================================================

  /**
   * The base model; immutable, of type {@link edu.cmu.cs.fluid.mvc.ModelType}.
   * This must also be a member of the {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
   * attribute.
   */
  public static final String BASE_MODEL = "DifferenceModel.BASE";

  /**
   * The delta model; immutable, of type {@link edu.cmu.cs.fluid.mvc.ModelType}.
   * This must also be a member of the {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
   * attribute.
   */
  public static final String DELTA_MODEL = "DifferenceModel.DELTA";


  
  //===========================================================
  //== Convienence Methods
  //===========================================================

  /** Get the base model. */
  public Model getBaseModel();

  /** Get the delta model. */
  public Model getDeltaModel();
}

