// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ConfigurableView.java,v 1.14 2003/07/15 21:47:18 aarong Exp $

package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.mvc.visibility.ModelAndVisibilityToModelStatefulView;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A View of a model and it's visibility model that is used to 
 * construct a view of the model that is used for rendering.
 * A ConfigurableView may hide nodes from being presented.  The hidden
 * status of a node is outside of any versioning in the
 * source models; if a source model changes and breaks the
 * view, the hidden status of a node does not change.  On the other hand,
 * the use of {@link #setHiddenForAllNodes}
 * does not leave any residue indicating that <em>all</em>
 * the nodes were hidden.  All nodes in the view
 * as determined at the time the method is called
 * are hidden, but if new nodes are added to the model
 * they are not automatically hidden.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link View#VIEW_NAME}
 * <li>{@link View#SRC_MODELS}
 * </ul>
 *
 * <p>The values of the MODEL_NAME and VIEW_NAME attributes do not
 * need to be the same.
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * <li>{@link #IS_HIDDEN}
 * <li>{@link #PROXY_NODE}
 * <li>{@link #IS_PROXY}
 * </ul>
 *
 * @author Aaron Greenhouse
 */

public interface ConfigurableView
extends ModelAndVisibilityToModelStatefulView
{
  //===========================================================
  //== Node Attributes
  //===========================================================

  /**
   * The proxy node for a given node.  If a node has a proxy node,
   * then this value is non-<code>null</code>, and is an IRNode
   * whose attribute values represent the attribute values of
   * ellided structure.  If the node does not have a proxy node then
   * this value is <code>null</code>.  Type is {@link edu.cmu.cs.fluid.ir.IRNodeType},
   * and the attribute is immutable.
   */
  public static final String PROXY_NODE = "ConfigurableView.proxyNode";
  
  /** 
   * Node attribute indicating whether a node
   * has been hidden.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRBooleanType}
   * and is mutable.
   * In the case of <code>StatefulViews</code>,
   * this attribute is indexed using nodes of the 
   * source models.
   */
  public static final String IS_HIDDEN = "ConfigurableView.isHidden";

  /** 
   * Node attribute indicating whether a node
   * is a proxy node.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRBooleanType}
   * and is immutable.
   */
  public static final String IS_PROXY = "ConfigurableView.isProxy";


  
  //===========================================================
  //== Convienence Methods
  //===========================================================

  /**
   * Is the given node a proxy node?
   */
  public boolean isProxyNode( IRNode node );

  /**
   * Get the proxy node for a give node.
   */
  public IRNode getProxyNode( IRNode node );

  /**
   * Query if a node is hidden.
   * This must always return the inverse of {@link #isShown}.
   */
  public boolean isHidden( IRNode node );

  /**
   * Query if a node is shown.
   * This must always return the inverse of {@link #isHidden}.
   */
  public boolean isShown( IRNode node );
 
  /**
   * Set the hidden status of a node.
   * @param node The IRNode whose status is to be set.
   * @param isHidden <code>true</code> if the node should be hidden;
   * <code>false</code> if the node should be shown.
   */
  public void setHidden( IRNode node, boolean isHidden );

  /**
   * Set the hidden status for all the nodes present in the source
   * models at the time this method is called.
   * @param isHidden <code>true</code> if the nodes should be hidden;
   * <code>false</code> if the nodes should be shown.
   */
  public void setHiddenForAllNodes( boolean isHidden );

  
  
  //===========================================================
  //== New Descriptors (for actions)
  //================================================x===========

  public static final String HIDE_NODE = "ConfigurableView.hideNode";
  public static final String SHOW_NODE = "ConfigurableView.showNode";
  public static final String SHOW_ALL  = "ConfigurableView.showAllNodes";
}

