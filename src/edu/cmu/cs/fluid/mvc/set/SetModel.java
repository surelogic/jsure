/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/SetModel.java,v 1.9 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * <P>Interface for models of sets.  There is no structure
 * imposed on the set other than set membership.  That is, the model
 * simply consists of the IRNodes for which {@link Model#isPresent} is
 * <code>true</code>.
 *
 * <p>Set models are distinguished from Models because it is not always
 * convienent to keep track of the number of nodes in a model.  
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link #SIZE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SetModel
extends Model
{
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * The size of the set.  The value's type is
   * {@link edu.cmu.cs.fluid.ir.IRIntegerType} and is immutable.
   * The value of the attribute will change, however,
   * as the model's size changes.
   */
  public static final String SIZE = "SetModel.SIZE";

  
  
  //===========================================================
  //== Methods
  //===========================================================

  /**
   * Insure that a node is in the model, but do not set any attribute
   * values of the node.  (The {@link Model#IS_ELLIPSIS} attribute will be
   * <code>false</code>, and {@link Model#ELLIDED_NODES} will be 
   * undefined.)
   */
  public void addNode( IRNode node );

  /** Get the number of elements in the set. */
  public int size();
}

