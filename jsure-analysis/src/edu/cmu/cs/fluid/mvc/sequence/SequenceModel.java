/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SequenceModel.java,v 1.15 2006/03/29 19:54:51 chance Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;

/**
 * A model of an ordered sequence of nodes, and a subtype of Sets.
 * This is basically
 * a way to pull {@link edu.cmu.cs.fluid.ir.IRSequence} into the model&ndash;view
 * regime.  As a {@link SetModel}, the method {@link SetModel#addNode} appends
 * the node to the sequence (being equivalent to {@link #appendElement}.
 *
 * <P>An implementation must support the
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link SetModel#SIZE}
 * <li>{@link SequenceModel#FIRST}
 * </ul>
 *
 * <P>An implementaiton must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link #LOCATION}
 * <LI>{@link #INDEX}
 * <LI>{@link #NEXT}
 * <LI>{@link #PREVIOUS}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SequenceModel
extends SetModel, IRSequence<IRNode>
{
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * The first element of the sequence or <code>null</code>
   * if the sequence is empty.  Type is {@link edu.cmu.cs.fluid.ir.IRNodeType}
   * and the value is immutable.
   */
  public static final String FIRST = "SequenceModel.FIRST";


  
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * Node attribute indicating the (relative) location of a node 
   * in the sequence.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRLocationType}.
   * This attribute may or may not be mutable.  If it is mutable,
   * setting the value causes the given node to be
   * moved so that is located at the given location in the sequence.
   */
  public static final String LOCATION = "SequenceModel.location";

  /**
   * Node attribute indicating the absolute position of a node in the sequence.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRIntegerType}.
   * This attribute may or may not be mutable.  If it is mutable,
   * setting the value causes the given node to be
   * moved so that is located at the given index in the sequence.
   */
  public static final String INDEX = "SequenceModel.index";

  /**
   * Node attribute indicating the next node in the 
   * sequence, or <code>null</code> if their is no
   * next node.  The value's type is {@link edu.cmu.cs.fluid.ir.IRNodeType}
   * and the value is immutable.
   */
  public static final String NEXT = "SequenceModel.next";

  /**
   * Node attribute indicating the previous node in the 
   * sequence, or <code>null</code> if their is no
   * previous node.  The value's type is {@link edu.cmu.cs.fluid.ir.IRNodeType}
   * and the value is immutable.
   */
  public static final String PREVIOUS = "SequenceModel.previous";
  


  //===========================================================
  //== Node methods
  //===========================================================

  /**
   * Returns an iterator over the nodes in the order they
   * appear in the sequence.
   */
  @Override
  public Iterator<IRNode> getNodes();



  //===========================================================
  //== Attribute convienence methods
  //===========================================================

  /**
   * Get the value of the {@link #LOCATION} attribute.
   */
  public IRLocation getLocation( IRNode node );

  /**
   * Get the value of the {@link #INDEX} attribute.
   */
  public int getIndex( IRNode node );
}

