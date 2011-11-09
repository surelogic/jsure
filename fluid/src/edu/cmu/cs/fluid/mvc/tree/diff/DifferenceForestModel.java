// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/diff/DifferenceForestModel.java,v 1.11 2003/07/15 21:47:20 aarong Exp $

package edu.cmu.cs.fluid.mvc.tree.diff;

import edu.cmu.cs.fluid.mvc.diff.DifferenceModel;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.ir.IREnumeratedType;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A stateful view that computes the difference of two input <em>forest</em>
 * models.  The two models are distinguished as the <em>base</em>
 * and the <em>delta</em>.  The difference describes how to
 * convert the base model into the delta model.
 *
 * <p>This interface provides a basis difference semantics based on
 * nodes changing location, being removed, or being added.  Sub-interfaces
 * are free to add additional difference semantics.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link DifferenceModel#BASE_MODEL}: In this case the model is 
 *   guaranteed to be an implementation of {@link ForestModel}.
 * <li>{@link DifferenceModel#DELTA_MODEL}: In this case the model is 
 *   guaranteed to be an implementation of {@link ForestModel}.
 * <li>{@link ForestModel#ROOTS}
 * <li>{@link #DEFAULT_ATTR_SRC}
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
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link ForestModel#LOCATION}
 * <li>{@link #DIFF_LOCAL}
 * <li>{@link #DIFF_POSITION}
 * <li>{@link #DIFF_SUBTREE}
 * <li>{@link #DIFF_LABEL}
 * <li>{@link #NODE_ATTR_SRC}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface DifferenceForestModel
extends DifferenceModel, ForestModel
{
  //===========================================================
  //== Constants for defining an enumeration that describes
  //== positional changes within the tree.
  //===========================================================

  /** 
   * The name of the enumeration containing positional difference tags.
   */
  public static final String POSITION_ENUM =
    "DifferenceForestModel$PositionalEnumeration";

  /**
   * The name of the positional enumeration element that indicates that 
   * a node has noved.  This is always the first element of the enumeration.
   */
  public static final String POSITION_MOVED = "Moved";

  /**
   * The name of the positional enumeration element that indicates that 
   * the tag is not applicable to the node.  This is always the second
   * element of the enumeration.
   */
  public static final String POSITION_NA = "N/A";

  /**
   * The name of the positional enumeration element that indicates that 
   * a node's ancestor has moved.  This is always the third element of
   * the enumeration.
   */
  public static final String POSITION_ANCESTOR = "Ancestor Moved";

  /**
   * The name of the positional enumeration element that indicates that 
   * a node has stayed in the same location.  This is always the fourth
   * and last element of the enumeration.
   */
  public static final String POSITION_SAME = "Same";

  /** The index of the {@link #POSITION_MOVED} element. */
  public final static int POS_MOVED = 0;

  /** The index of the {@link #POSITION_NA} element. */
  public final static int POS_NA = 1;

  /** The index of the {@link #POSITION_ANCESTOR} element. */
  public final static int POS_ANC = 2;

  /** The index of the {@link #POSITION_SAME} element. */
  public final static int POS_SAME = 3;

  
  
  //===========================================================
  //== Constants for defining an enumeration that describes
  //== subtree changes.
  //===========================================================

  /**
   * The name of the enumeration containing subtree difference tags.
   */
  public static final String SUBTREE_ENUM =
    "DifferenceForestModel$SubtreeEnumeration";

  /**
   * The name of the subtree enumeration element that indicates that 
   * a subtree has not changed.  This is always the first
   * element of the enumeration.
   */
  public static final String SUBTREE_SAME = "Same";

  /**
   * The name of the subtree enumeration element that indicates that 
   * a subtree has changed.  This is always the second
   * element of the enumeration.
   */
  public static final String SUBTREE_CHANGED = "Changed";

  /**
   * The name of the subtree enumeration element that indicates that 
   * no subtree change information is available or it is not applicable.
   * This is always the third and last element of the enumeration.
   */
  public static final String SUBTREE_NA = "N/A";

  /** The index of the {@link #SUBTREE_SAME} element. */
  public final static int SUB_SAME = 0;

  /** The index of the {@link #SUBTREE_CHANGED} element. */
  public final static int SUB_DIFF = 1;

  /** The index of the {@link #SUBTREE_NA} element. */
  public final static int SUB_NA = 2;

  
  
  //===========================================================
  //== Constants for defining an enumeration that describes
  //== node-level changes.  These values are extended by
  //== sub-interfaces.
  //===========================================================

  /*
   * No name constant for the node-level change enumeration because
   * we are still too high level.
   */
  
  /**
   * The name of the node enumeration element that indicates that 
   * the node was added to the tree.  This is always the first element
   * of the enumeration.
   */
  public static final String NODE_ADDED = "Added";
  
  /**
   * The name of the node enumeration element that indicates that 
   * the node was removed from the tree.  This is always the second element
   * of the enumeration.
   */
  public static final String NODE_DELETED = "Deleted";
  
  /**
   * The name of the node enumeration element that indicates that 
   * the node is a phantom node (it is where a node used to be).  This is
   * always the third element of the enumeration.
   */
  public static final String NODE_PHANTOM = "Phantom";
  
  /**
   * The name of the node enumeration element that indicates that 
   * the node is the same.  This is always the fourth element
   * of the enumeration.
   */
  public static final String NODE_SAME = "Same";
  
  /**
   * The name of the node enumeration element that indicates that 
   * the node has changed in two or more ways.  This is always the
   * fifth element of the enumeration.
   */
  public static final String NODE_CHANGED = "2+ Changes";

  /** The index of the {@link #NODE_ADDED} element. */
  public final static int ADDED = 0;

  /** The index of the {@link #NODE_DELETED} element. */
  public final static int DELETED = 1;

  /** The index of the {@link #NODE_PHANTOM} element. */
  public final static int PHANTOM = 2;

  /** The index of the {@link #NODE_SAME} element. */
  public final static int SAME = 3;

  /** The index of the {@link #NODE_CHANGED} element. */
  public final static int CHANGED = 4; // somehow

  
  
  //===========================================================
  //== New model-level attributes
  //===========================================================

  /**
   * True if merged attribute values should default to being read
   * from the base model; false if merged attribute values should
   * default to being read from the delta model.  Applies to model-
   * and node-level attributes, although see {@link #NODE_ATTR_SRC}.
   * True if merged model-level attributes are being read from
   * Boolean-valued mutable attribute.
   */
  public static final String DEFAULT_ATTR_SRC =
    "DifferenceForestModel.defaultAttrSrc";

  /**
   * A mutable attribute whose values are elements of an enumeration
   * describing how sensitive the node is to local "changes"
   */  
  public static final String DIFF_SENSITIVITY = 
    "DifferenceForestModel.diffSensitivity";
    
  //===========================================================
  //== New node-level attributes
  //===========================================================

  /**
   * Immutable attribute whose values are elements of the 
   * {@link #POSITION_ENUM} enumeration describing whether a node's
   * position in the tree has changed.
   */
  public static final String DIFF_POSITION =
    "DifferenceForestModel.diffPosition";

  /**
   * Immutable attribute whose values are elements of the 
   * {@link #SUBTREE_ENUM} enumeration describing whether a node's
   * subtree has changed.
   */
  public static final String DIFF_SUBTREE =
    "DifferenceForestModel.diffSubtree";

  /**
   * Immutable attribute whose values are elements of the 
   * specialized (by sub-interfaces) local difference enumeration
   * describing how a node has changed.
   */
  public static final String DIFF_LOCAL =
    "DifferenceForestModel.diffLocal";

  /**
   * Immutable node-valued attribute whose value indicates the node that
   * a phantom node really represents.
   */
  public static final String DIFF_LABEL =
    "DifferenceForestModel.diffLabel";

  /**
   * True if, for the given node, merged node-level attributes are
   * being read from the base model; False if read off of the delta
   * model.  Boolean-valued immutable attribute.  For nodes that are 
   * present in both models, the value is influenced by 
   * {@link #DEFAULT_ATTR_SRC}.  The value is also influenced by the
   * structure of the difference model.
   */
  public static final String NODE_ATTR_SRC =
    "DifferenceForestModel.diffAttrSrc";

  
  
  //===========================================================
  //== Command Constants
  //===========================================================

  /**
   * Constant identifying the command that shows attribute values based 
   * on the base model.
   */
  public static final String SELECT_BASE  = "DifferenceForestModel.showBase";

  /**
   * Constant identifying the command that shows attribute values based 
   * on the delta model.
   */
  public static final String SELECT_DELTA = "DifferenceForestModel.showDelta";

  
  
  //===========================================================
  //== Convienence Methods
  //===========================================================

  /** Get the base model as a ForestModel instance. */
  public ForestModel getBaseModelAsForest();

  /** Get the delta model as a ForestModel instance. */
  public ForestModel getDeltaModelAsForest();
  



  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_POSITION} attribute.
   */
  public IREnumeratedType.Element getDiffPosition( IRNode node );

  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_SUBTREE} attribute.
   */
  public IREnumeratedType.Element getDiffSubtree( IRNode node );

  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_LOCAL} attribute.
   */
  public IREnumeratedType.Element getDiffLocal( IRNode node );

  /**
   * Get the value of the {@link DifferenceForestModel#DIFF_LABEL} attribute.
   */
  public IRNode getDiffLabel( IRNode node );
  
  /**
   * Get the value of the {@link DifferenceForestModel#NODE_ATTR_SRC} attribute.
   */
  public boolean getNodeSelector( IRNode node );
  
  /**
   * Get the value of the {@link DifferenceForestModel#DEFAULT_ATTR_SRC} attribute.
   */
  public boolean getCompSelector();
}

