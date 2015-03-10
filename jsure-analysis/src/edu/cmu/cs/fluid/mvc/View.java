package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;

/**
 * A View is a presentation of one or more models.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link #VIEW_NAME}
 * <li>{@link #SRC_MODELS}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface View
{
  //===========================================================
  //== View Attributes
  //===========================================================

  /**
   * The name of the view.
   * The value is of type {@link edu.cmu.cs.fluid.ir.IRStringType} and
   * is immutable.
   */
  public static final String VIEW_NAME = "View.NAME";

  /**
   * Get the Models being viewed.  
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRSequenceType} of 
   * {@link ModelType}, and is immutable.
   */
  public static final String SRC_MODELS = "View.SOURCES";



  //===========================================================
  //== Source Models convienence methods  
  //===========================================================

  /** 
   * Get the models being viewed by the view.  Reads
   * the value of the {@link #SRC_MODELS} attribute.
   * @return an <code>Iterator</code> over {@link edu.cmu.cs.fluid.mvc.Model}s.
   */
  public Iterator<Model> getSourceModels();



  //===========================================================
  //== Query about the relationship between models
  //===========================================================

  /**
   * Query if the view is below a model in a
   * model&ndash;view chain.
   */
  public boolean downChainFrom( Model m );
}
