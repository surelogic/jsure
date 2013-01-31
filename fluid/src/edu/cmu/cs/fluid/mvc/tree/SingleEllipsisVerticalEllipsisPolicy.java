/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/SingleEllipsisVerticalEllipsisPolicy.java,v 1.4 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An ellipsis policy that will only add a single ellipsis child
 * to any given node.  That ellipsis may be either at the head 
 * or the tail of the node's children.  Also merges any child ellipses
 * into the ellipsis.
 *
 * @author Aaron Greenhouse
 */
public final class SingleEllipsisVerticalEllipsisPolicy
implements ForestVerticalEllipsisPolicy
{
  /*
  private static class EllipsisRecord
  {
    public final IRNode ellipsis;
    public final Set skippedNodes;
    
    public EllipsisRecord(final IRNode eNode) {
      ellipsis = eNode;
      skippedNodes = new HashSet();
    }
  }
  */
  /**
   * Dummy object used to represent a root-level ellipsis in
   * the hashtable.
   */
  private static final Object treeLevel = new Object();  

  /**
   * Constant indicating that the ellipsis should be added 
   * to the head of the node's children.
   */
  public static final boolean AT_TOP = false;

  /**
   * Constant indicating that the ellipsis should be added
   * to the tail of the node's children.
   */
  public static final boolean AT_BOTTOM = true;

  /**
   * Hashtable whose keys are all nodes that have had potential children not
   * added to them in the sub-model. Keys map to {@link IRNode} objects.
   */
  private final Map<Object,IRNode> ellipsisMap = new HashMap<Object,IRNode>();
  
  /**
   * Map from ellipsis nodes to sets containing the nodes that the ellipsis
   * hides. 
   */
  private final Map<IRNode,Set<IRNode>> ellidedMap = new HashMap<IRNode,Set<IRNode>>();
  
  /** Either <CODE>AT_BOTTOM</CODE> or <CODE>AT_TOP</CODE>. */
  private final boolean where;

  /** Should parent and child ellipsis be merged? */
  private final boolean merge;
  
  /** The <CODE>TreeView</code> that this policy is for. */
  private final ConfigurableForestView forest;

  /**
   * Create a new policy instance.
   * @param c The view be associated with.
   * @param atBottom Where the ellipsis should be added.
   */
  public SingleEllipsisVerticalEllipsisPolicy( 
      final ConfigurableForestView c, final boolean atBottom,
      final boolean shouldMerge )
  {
    where = atBottom;
    merge = shouldMerge;
    forest = c;
  }

  public boolean isAtBottom() {
    return where;
  }

  @Override
  public void resetPolicy()
  {
    ellipsisMap.clear();
    ellidedMap.clear();
  }

  @Override
  public IRNode nodeSkipped(final IRNode node, final IRNode parent,
      final int pos, final int oldPos) {
    IRNode ellipsis;
    final Set<IRNode> ellidedNodes;
    if (merge && parent != null && forest.isEllipsis(parent)) {
      ellipsis = parent;
      ellidedNodes = ellidedMap.get(parent);
    } else {
      final Object key = (parent == null) ? treeLevel : parent;
      ellipsis = ellipsisMap.get(key);
      if (ellipsis == null) {
        ellipsis = forest.createEllipsisNode();
        ellidedNodes = new HashSet<IRNode>();
        ellipsisMap.put(key, ellipsis);
        ellidedMap.put(ellipsis, ellidedNodes);
      } else {
        ellidedNodes = ellidedMap.get(ellipsis);
      }
    }
    ellidedNodes.add(node);
    return ellipsis;
  }

  @Override
  public void applyPolicy()
  {
    for( Iterator keys = ellipsisMap.keySet().iterator(); keys.hasNext(); )
    {
      final Object key = keys.next();
      final IRNode ellipsis = ellipsisMap.get(key);
      final Set<IRNode> ellidedNodes = ellidedMap.get(ellipsis);
      final IRNode parent = (key == treeLevel) ? null : (IRNode)key;
      
      if( where == AT_BOTTOM ) {
        forest.appendEllipsis( ellipsis, parent, ellidedNodes );
      } else {
        forest.insertEllipsis( ellipsis, parent, ellidedNodes);
      }
    }
  }

  @Override
  public String toString() { 
    return where ? "Ellipsis at bottom" : "Ellipsis at top"; 
  } 
}

