/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/VisibilityModel.java,v 1.11 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.visibility;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Interface for models of visibility sets used by
 * ConfigurableViews.  A visibility model describes via the {@link #IS_VISIBLE}
 * attribute which nodes of a model should be made visible by a configurable
 * view.  The configurable view may make more nodes visible if they are required
 * for a sensible represenation of the model, e.g., the "show path to root"
 * mode of a tree view.
 *
 * <p>The nodes in a visibility model are exactly those nodes that are
 * present in the {@link #VISIBILITY_OF} source model.  Essentially this
 * model just adds the {@link #IS_VISIBLE} attribute to the nodes in the
 * source model.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link #VISIBILITY_OF}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link #IS_VISIBLE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface VisibilityModel
extends Model
{
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * The model whose visibility information is stored in this model.
   * Value is of type {@link edu.cmu.cs.fluid.mvc.ModelType}, and is immutable.
   */
  public static final String VISIBILITY_OF = "VisibilityModel.VISIBILITY_OF";


  
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * Whether a node should be visible in a renderer.
   * Value is of type {@link edu.cmu.cs.fluid.ir.IRBooleanType}, and is immutable.
   */
  public static final String IS_VISIBLE = "VisibilityModel.isVisible";


  
  //===========================================================
  //== Methods
  //===========================================================

  /**
   * Short cut to get the {@link #IS_VISIBLE} value.
   */
  public boolean isVisible( IRNode node );
}
