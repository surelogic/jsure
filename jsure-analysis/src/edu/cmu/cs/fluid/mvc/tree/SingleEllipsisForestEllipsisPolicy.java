/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/SingleEllipsisForestEllipsisPolicy.java,v 1.16 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An ellipsis policy that will only add a single ellipsis child
 * to any given node.  That ellipsis may be either at the head 
 * or the tail of the node's children.
 *
 * @author Aaron Greenhouse
 */
public final class SingleEllipsisForestEllipsisPolicy
implements ForestEllipsisPolicy
{
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
   * Hashtable whose keys are all nodes that have had potential children
   * not added to them in the sub-model.
   */
  private final Map<Object,Set<IRNode>> map = new HashMap<Object,Set<IRNode>>();

  /** Either <CODE>AT_BOTTOM</CODE> or <CODE>AT_TOP</CODE>. */
  private final boolean where;

  /** The <CODE>TreeView</code> that this policy is for. */
  private final ConfigurableForestView forest;

  /**
   * Create a new policy instance.
   * @param c The view be associated with.
   * @param atBottom Where the ellipsis should be added.
   */
  public SingleEllipsisForestEllipsisPolicy( final ConfigurableForestView c, final boolean atBottom )
  {
    this.where = atBottom;
    forest = c;
  }

  public boolean isAtBottom() {
    return where;
  }

  @Override
  public void resetPolicy()
  {
    map.clear();
  }

  @Override
  public void nodeSkipped( final IRNode node, final IRNode parent, final int pos )
  {
    final Object key = (parent == null) ? treeLevel : parent;
    Set<IRNode> nodes = map.get( key );
    if( nodes == null ) {
      nodes = new HashSet<IRNode>();
      map.put( key, nodes );
    }
    nodes.add( node );
  }

  @Override
  public void applyPolicy()
  {
    for( Iterator keys = map.keySet().iterator(); keys.hasNext(); )
    {
      final Object key = keys.next();
      final Set<IRNode> nodes = map.get( key );
      final IRNode parent = (key == treeLevel) ? null : (IRNode)key;

      if( where == AT_BOTTOM ) {
        forest.appendEllipsis( forest.createEllipsisNode(), parent, nodes );
      } else {
        forest.insertEllipsis( forest.createEllipsisNode(), parent, nodes );
      }
    }
  }

  @Override
  public String toString() { 
    return where ? "Ellipsis at bottom" : "Ellipsis at top"; 
  } 
}

