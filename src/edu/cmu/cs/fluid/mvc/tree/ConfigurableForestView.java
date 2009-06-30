// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/ConfigurableForestView.java,v 1.61 2006/03/29 19:54:51 chance Exp $
package edu.cmu.cs.fluid.mvc.tree;

import java.util.Set;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ConfigurableView;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.IREnumeratedType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.InsertionPoint;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;


/**
 * A view of an <em>unversioned</em> forest that allows for 
 * nodes to be ellided, and the display mode to be
 * altered.  The exported model can contain nodes
 * for which {@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS} is <code>true</code>.
 *
 *
 *
 * <p>The view allows tree nodes to be <em>expanded</em> or <em>collapsed</em>.
 * This is controlled by the {@link #IS_EXPANDED} node-level attribute.  The
 * attribute maintains independent values for each view mode.
 * The configurable view allows a forest to be viewed in two 
 * modes, controlled by {@link #IS_FLATTENED} model-level attribute.  
 * When <code>false</code>, the view is in <em>path-to-root</em> mode, which
 * insures that if a node is supposed to be in the tree, then all its
 * ancestors are also in the tree.  The base case&mdash;the nodes that are
 * "supposed to be in the tree"&mdash;is determined by
 * the {@link #IS_HIDDEN} attribute and the views's {@link VisibilityModel}.
 * When <code>true</code>, the view is in <em>flattened</em> mode, which 
 * builds the forest as a list.  Any visible node that has a collapsed 
 * parent is made an element of the list.  Expanded nodes are expanded in
 * place.  A fully collapsed tree is built as a forest of one node trees;
 * a fully expanded tree is built as traditional tree.  
 *
 *
 *
 * <p>As a service to renderers, the view maintains the immutable 
 * boolean-valued node-level attribute {@link #RENDER_AS_PARENT}.
 * It is true only when the node should be rendered as a parent node (even
 * though it may not have children in the exported model).  The renderer
 * should use the {@link #IS_EXPANDED} attribute to determine the open/closed
 * status of the node.
 *
 * <p>Proxy nodes are used to capture attributes of collapsed model structure.
 * A {@link ForestProxyAttributePolicy} is used to construct the attributes
 * of proxy nodes.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link ForestModel#ROOTS}
 * <li>{@link #ELLIPSIS_POLICY}
 * <li>{@link #VIEW_MODE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link DigraphModel#CHILDREN}
 * <li>{@link SymmetricDigraphModel#PARENTS}
 * <li>{@link ForestModel#LOCATION}
 * <li>{@link ConfigurableView#IS_HIDDEN}
 * <li>{@link ConfigurableView#PROXY_NODE}
 * <li>{@link ConfigurableView#IS_PROXY}
 * <li>{@link #IS_EXPANDED}
 * <li>{@link #RENDER_AS_PARENT}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface ConfigurableForestView
extends ConfigurableView, ForestToForestStatefulView
{
  //===========================================================
  //== Strings for the enumeration used by VIEW_MODE
  //===========================================================

  /** 
   * The name of the enumeration used by the VIEW_MODE attribute.
   */
  public static final String VIEW_MODE_ENUM = "ConfigurableForestView$ViewModeEnumeration";

  /**
   * The name of the enumeration element indicating that the forest
   * should be viewed in the FLATTENED mode.  This
   * must be the first element of the enumeration.
   */
  public static final String VIEW_FLATTENED = "Flattened";

  /**
   * The name of the enumeration element indicating that the forest
   * shoudl be viewed in the PATH_TO_ROOT mode.  This
   * must be the second element of the enumeration.
   */
  public static final String VIEW_PATH_TO_ROOT = "Path To Root";

  /**
   * The name of the enumeration element indicating that the forest should
   * be viewed in the VERTICAL_ELLIPSIS mode.  This
   * must be the third (and final) element of the enumeration.
   */
  public static final String VIEW_VERTICAL_ELLIPSIS = "Vertical Ellipsis";

  
  
  //===========================================================
  //== New Component-Level Attributes
  //===========================================================

  /**
   * Attribute containing the ellipsis policy that should be used.
   * The value is of type {@link ForestEllipsisPolicyType} and
   * is mutable.
   */
  public static final String ELLIPSIS_POLICY =
      "ConfigurableForestView.ELLIPSIS_POLICY";

  /**
   * Attribute controlling the view mode.
   * The value's type is the {@link edu.cmu.cs.fluid.ir.IREnumeratedType} returned
   * by <code>IREnumeratedType.getIterator(ConfigurableForestView.VIEW_MODE_ENUM)</code>.
   * and is mutable.
   */
  public static final String VIEW_MODE =
      "ConfigurableForestView.VIEW_MODE";
  
  
  
  //===========================================================
  //== New Node-Level Attributes
  //===========================================================

  /**
   * The expanded/collapsed state of a node:
   * True if the node is expanded;  False if the node is
   * collapsed.  Value is of type {@link edu.cmu.cs.fluid.ir.IRBooleanType}
   * and is mutable.
   */
  public static final String IS_EXPANDED = 
      "ConfigurableForestView.isExpanded";

  /**
   * Indicates whether a node should be rendered as a parent node,
   * despite the fact that it might be a leaf node in the projected
   * forest.  Values are of type {@link edu.cmu.cs.fluid.ir.IRBooleanType} and
   * are immutable.
   */
  public static final String RENDER_AS_PARENT = 
      "ConfigurableForestView.renderAsParent";



  //===========================================================
  //== New Descriptors (for actions)
  //================================================x===========

  public static final String HIDE_NODE = "ConfigurableForestView.hideNode";
  public static final String SHOW_NODE = "ConfigurableForestView.showNode";
  public static final String HIDE_CHILDREN = 
      "ConfigurableForestView.hideChildren";
  public static final String SHOW_CHILDREN = 
      "ConfigurableForestView.showChildren";
  public static final String HIDE_SUBTREE = 
      "ConfigurableForestView.hideSubtree";
  public static final String SHOW_SUBTREE = 
      "ConfigurableForestView.showSubtree";
  public static final String COLLAPSE_TREE =
      "ConfigurableForestView.collapseTree";
  public static final String EXPAND_TREE   = 
      "ConfigurableForestView.expandTree";
  public static final String TOGGLE_TREE   = 
      "ConfigurableForestView.toggleTree";

  public static final String HIDE_SIBLINGS = 
      "ConfigurableForestView.hideSiblings";
  public static final String SHOW_SIBLINGS = 
      "ConfigurableForestView.showSiblings";

  public static final String HIDE_SIBLINGSUBTREES = 
      "ConfigurableForestView.hideSiblingSubtrees";
  public static final String SHOW_SIBLINGSUBTREES = 
      "ConfigurableForestView.showSiblingSubtrees";

  
  
  //===========================================================
  //== Tree Presentation Parameters
  //===========================================================

  /**
   * Set the {@link #VIEW_MODE} to the flattened state.  A flattened tree 
   * is presented in list mode.
   */
  public void setViewFlattened();

  /**
   * Set the {@link #VIEW_MODE} to be path to root.  A tree in path
   * to root mode makes sure that all the ancestors of a visible
   * node are visible.
   */
  public void setViewPathToRoot();

  /**
   * Set the {@link #VIEW_MODE} to be vertical ellipsis.
   */
  public void setViewVerticalEllipsis();
  
  /**
   * </code>true</code> iff the {@link #VIEW_MODE} is flattened.
   */
  public boolean isViewFlattened();

  /**
   * </code>true</code> iff the {@link #VIEW_MODE} is path to root.
   */
  public boolean isViewPathToRoot();

  /**
   * </code>true</code> iff the {@link #VIEW_MODE} is vertical ellipsis.
   */
  public boolean isViewVerticalEllipsis();
  
  /** Get the visibility Element corresponding to the String */
  public IREnumeratedType.Element getEnumElt( String name );

  /**
   * Set the view mode.
   * @param mode The view mode.
   */
  public void setViewMode( IREnumeratedType.Element mode );
  
  /** Get the view mode. */
  public IREnumeratedType.Element getViewMode();
  
  

  //===========================================================
  //== Ellipsis Control
  //===========================================================

  /**
   * Set the ellipsis policy.
   */
  public void setForestEllipsisPolicy( ForestEllipsisPolicy p );

  /**
   * Get the ellipsis policy
   */
  public ForestEllipsisPolicy getForestEllipsisPolicy();


  
  //===========================================================
  //== Node Hiding methods
  //===========================================================

  /**
   * Set the hidden status for an entire sub-tree.
   * @param root The root of the sub-tree
   * @param hidden <code>true</code> if the sub-tree should be
   * hidden; <code>false</code> if it should be shown.
   */
  public void setHiddenForSubtree( IRNode root, boolean hidden );

  /**
   * Set the hidden status for an a node's children
   * @param root The root of the sub-tree
   * @param hidden <code>true</code> if the sub-tree should be
   * hidden; <code>false</code> if it should be shown.
   */
  public void setHiddenForChildren( IRNode root, boolean hidden );

  /**
   * Set the hidden status for an a node's siblings
   * @param root The root of the sub-tree
   * @param hidden <code>true</code> if the sub-tree should be
   * hidden; <code>false</code> if it should be shown.
   */
  public void setHiddenForSiblings( IRNode root, boolean hidden );

  /**
   * Set the hidden status for an a node's siblings' subtrees
   * @param root The root of the sub-tree
   * @param hidden <code>true</code> if the sub-tree should be
   * hidden; <code>false</code> if it should be shown.
   */
  public void setHiddenForSiblingSubtrees( IRNode root, boolean hidden );
  
  /**
   * Check the hidden status for a node's siblings
   * @param root The root of the sub-tree
   * @param hidden <code>true</code> if looking for hidden nodes
   * ; <code>false</code> if looking for visible nodes
   */
  public boolean areSomeHiddenForSiblings( IRNode root, boolean hidden );
  
  /**
   * Check the hidden status for the subtrees of a node's siblings.
   * @param root The root of the sub-tree
   * @param hidden <code>true</code> if looking for hidden nodes
   * ; <code>false</code> if looking for visible nodes
   */
  public boolean areSomeHiddenForSiblingSubtrees(
    IRNode root, boolean hidden );



  //===========================================================
  //== Node Expansion methods
  //===========================================================

  /** 
   * Set the expanded state of a node.
   * @param flag <code>true</code> if the node should be
   * presented expanded; <code>false</code> if it should
   * be presented collapsed.
   */
  public void setExpanded( IRNode node, boolean flag );

  /**
   * Returns <code>true</code> iff the node is being
   * presented expanded.  This must also return the
   * opposide of <code>isCollapsed</code>.
   */
  public boolean isExpanded( IRNode node );

  /**
   * Returns <code>true</code> iff the node is being
   * presented collapsed.  This must also return the
   * opposide of <code>isExpanded</code>.
   */
  public boolean isCollapsed( IRNode node );

  /**
   * Toggle the expanded state of a node.  Makes an expanded
   * node collapsed; a collapsed node becomes expanded.
   */
  public boolean toggleExpanded( IRNode node );

  /**
   * Set the expanded state of an entire sub-tree.
   * @param root The root of the sub-tree to operate on.
   * @param flag The state to set all the nodes in the given sub-tree.
   */
  public void setExpandedForSubtree( IRNode root, boolean flag );


  
  //===========================================================
  //== Convienence Methods for the rendering attributes
  //===========================================================

  public boolean shouldRenderAsParent( IRNode node );



  //===========================================================
  //== Ellipsis Insertion
  //===========================================================

  // Model will/must already be locked when this is called
  public IRNode createEllipsisNode();
  
  // Model will/must already be locked when this is called
  public void insertEllipsisAt( IRNode ellipsis, IRNode parent, InsertionPoint ip, Set<IRNode> nodes );

  // Model will/must already be locked when this is called
  public void insertEllipsis( IRNode ellipsis, IRNode parent, Set<IRNode> nodes );

  // Model will/must already be locked when this is called
  public void appendEllipsis( IRNode ellipsis, IRNode parent, Set<IRNode> nodes );

  /**
   * Factory interface for creating instances of {@link ConfigurableForestView}.
   */
  public static interface Factory
  {
    public ConfigurableForestView create(
      String name, ForestModel src, VisibilityModel vizModel,
      AttributeInheritancePolicy aip, ForestProxyAttributePolicy pp,
      ForestEllipsisPolicy ellipsisPolicy,
      boolean expFlat, boolean expPath )
    throws SlotAlreadyRegisteredException;
  }
}



