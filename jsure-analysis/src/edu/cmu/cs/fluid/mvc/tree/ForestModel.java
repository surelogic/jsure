package edu.cmu.cs.fluid.mvc.tree;

import com.surelogic.common.util.*;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.InsertionPoint;
import edu.cmu.cs.fluid.tree.MutableTreeInterface;

/**
 * A model that represents a forest, that is, a sequence of trees.  The 
 * sequence of tree roots is managed by the {@link #ROOTS} attribute.
 * Neither adding a node as a root or removing a node as a root affects the
 * parentage of the node.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link ForestModel#ROOTS}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link DigraphModel#CHILDREN}
 * <li>{@link SymmetricDigraphModel#PARENTS}
 * <li>{@link #LOCATION}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ForestModel
extends SymmetricDigraphModel, MutableTreeInterface
{
  //===========================================================
  //== Names of standard model-level attributes
  //===========================================================

  /**
   * The root nodes of the forest, in order, as an
   * {@link edu.cmu.cs.fluid.ir.IRSequence}.  Modifying the sequence
   * will alter the structure of the forest by adding
   * or removing roots, or by reordering the roots of the
   * forest.  Adding/removing a node as a root does not 
   * affect the parentage of that node.
   */
  public static final String ROOTS = "ForestModel.ROOTS";



  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * The location a node has in its parent's children list,
   * as an {@link edu.cmu.cs.fluid.ir.IRLocation}.  If the node is a root
   * the the location refers to the roots location in the
   * root seqeuence.
   */
  public static final String LOCATION = "ForestModel.location";

  /**
   * Whether a node is a root in the forest or not.  The value
   * is of type {@link edu.cmu.cs.fluid.ir.IRBooleanType} and is immutable.
   */
  public static final String IS_ROOT = "ForestModel.isRoot";



  //===========================================================
  //== Convienence methods for manipulating the roots
  //===========================================================

  /** Test if a node is a root in the forest. */
  public boolean isRoot( IRNode node );

  /** Remove a root from the forest */
  public void removeRoot( IRNode root );

  /**
   * Append a root to the forest.
   * @exception IllegalArgumentException Thrown if the node is already
   * a member of the forest.
   */
  public void addRoot( IRNode root );

  /**
   * Insert a root at the start of the roots.
   * @exception IllegalArgumentException Thrown if the node is already
   * a member of the forest.
   */
  public void insertRoot( IRNode root );

  /**
   * Insert a new root before another root.
   * @exception IllegalArgumentException Thrown if the new root is already
   * a member of the forest.
   */
  public void insertRootBefore( IRNode newRoot, IRNode root );

  /**
   * Insert a new root after another root.
   * @exception IllegalArgumentException Thrown if the new root is already
   * a member of the forest.
   */
  public void insertRootAfter( IRNode newRoot, IRNode root );

  /**
   * Insert a new root at a given location.
   */
  public void insertRootAt( IRNode newRoot, IRLocation loc );

  /**
   * Insert a new root with a given insertion point.
   */
  public void insertRootAt( IRNode newRoot, InsertionPoint ip );

  /**
   * Get the roots of the forest in order.
   */
  public Iteratable<IRNode> getRoots();
}
