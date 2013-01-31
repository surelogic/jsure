/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/MultipleEllipsisVerticalEllipsisPolicy.java,v 1.4 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.InsertionPoint;

/**
 * An ellipsis policy that can produce multiple ellipses as children
 * for a single node.  The policy will add a single ellipsis for
 * each sequence of contiguously skipped nodes.
 * For example, if a node <code>P</code> had children
 * <code>V</code>, <code>W</code>, <code>X</code>, <code>Y</code>,
 * and <code>Z</code>, and <code>P</CODE> was added the sub-model,
 * but <code>V</code>, <code>W</code>, and <code>Y</code> were not,
 * ellispses would be inserted before child <code>X</code>,
 * and between children <code>X</code> and <code>Z</code>.
 *
 * @author Aaron Greenhouse
 */
public final class MultipleEllipsisVerticalEllipsisPolicy
implements ForestVerticalEllipsisPolicy
{
  /**
   * Dummy object used to represent a root-level ellipsis in
   * the hashtable.
   */
  private static final Object treeLevel = new Object();  

  /**
   * Hashtable whose keys are all nodes that have had potential children
   * not added to them in the sub-model.  Each key maps to 
   * a {@link LinkedList} used a stack of <code>Integer</code>s indicating
   * the positions at which nodes were not added.
   */
  private final Map<Object,LinkedList<Record>> map = new HashMap<Object,LinkedList<Record>>();
  
  /**
   * Map from ellipsis node to set of nodes ellided by the ellipsis.
   */
  private final Map<IRNode,Set<IRNode>> ellidedMap = new HashMap<IRNode,Set<IRNode>>();
  
  /** The <CODE>TreeView</code> that this policy is for. */
  private final ConfigurableForestView forest;

  /** Should an ellipsis node be merged with a parent ellipsis node? */
  private final boolean merge;
  
  
  
  /**
   * Create a new policy instance.
   * @param fm The view to be associated with.
   */
  public MultipleEllipsisVerticalEllipsisPolicy(
      final ConfigurableForestView fm, final boolean shouldMerge)
  {
    forest = fm;
    merge = shouldMerge;
  }

  @Override
  public void resetPolicy()
  {
    map.clear();
    ellidedMap.clear();
  }

  @Override
  public IRNode nodeSkipped(final IRNode node, final IRNode parent,
      final int pos, final int oldPos) {
    if (merge && parent != null && forest.isEllipsis(parent)) {
      ellidedMap.get(parent).add(node);
      return parent;
    } else {
      final Object key = (parent == null) ? treeLevel : parent;
      LinkedList<Record> stack = map.get(key);
      Set<IRNode> ellidedNodes;
      IRNode ellipsis;
      if (stack == null) {
        stack = new LinkedList<Record>();
        map.put(key, stack);
        ellipsis = forest.createEllipsisNode();
        final Record rec = new Record(ellipsis, pos);
        ellidedNodes = new HashSet<IRNode>();
        ellidedMap.put(rec.ellipsis, ellidedNodes);
        stack.addFirst(rec);
      } else {
        Record rec = stack.getFirst();
        if (pos != rec.pos) {
          ellipsis = forest.createEllipsisNode();
          rec = new Record(ellipsis, pos);
          ellidedNodes = new HashSet<IRNode>();
          ellidedMap.put(rec.ellipsis, ellidedNodes);
          stack.addFirst(rec);
        } else {
          ellipsis = rec.ellipsis;
          ellidedNodes = ellidedMap.get(rec.ellipsis);
        }
      }
      ellidedNodes.add(node);
      return ellipsis;
    }
  }

  @Override
  public void applyPolicy()
  {
    final IRSequence roots =
      (IRSequence)forest.getCompAttribute( ForestModel.ROOTS ).getValue();

    for( Iterator keys = map.keySet().iterator(); keys.hasNext(); )
    {
      final Object key = keys.next();
      final LinkedList<Record> stack = map.get( key );

      final IRNode node = (key == treeLevel) ? null : (IRNode)key;
      if( node == null ) {
        /* Unroll the first iteration of the while loop so that
         * we can do a special case for the first element.  But the we know
         * the stack has at least 1 element, so we don't need to check if it is
         * empty the first time.
         */
        Record rec = stack.removeFirst();
        final Set<IRNode> ellidedNodes = ellidedMap.get(rec.ellipsis);
        if( rec.pos == roots.size() ){
          forest.appendEllipsis( rec.ellipsis, null, ellidedNodes );
        } else {
          final IRLocation loc = roots.location( rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( rec.ellipsis, null, ip, ellidedNodes );
        }
        while( !stack.isEmpty() ) {
          rec = stack.removeFirst();
          final IRLocation loc = roots.location( rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( rec.ellipsis, null, ip, ellidedNodes );
        }
      } else {
        /* Unroll the first iteration of the while loop so that
         * we can do a special case for the first element.  But the we know
         * the stack has at least 1 element, so we don't need to check if it is
         * empty the first time.
         */
        Record rec = stack.removeFirst();
        final Set<IRNode> ellidedNodes = ellidedMap.get(rec.ellipsis);
        if( rec.pos == forest.numChildren( node ) ) {
          forest.appendEllipsis( rec.ellipsis, node, ellidedNodes );
        } else {
          final IRLocation loc = forest.childLocation( node, rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( rec.ellipsis, node, ip, ellidedNodes );
        }
        while( !stack.isEmpty() ) {
          rec = stack.removeFirst();
          final IRLocation loc = forest.childLocation( node, rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( rec.ellipsis, node, ip, ellidedNodes );
        }
      }
    }
  }


  private static class Record
  {
    public final IRNode ellipsis;
    public final int pos;

    public Record( final IRNode e, final int p ) 
    {
      ellipsis = e;
      pos = p;
    }
  }

  @Override
  public String toString() { return "Multiple ellipses"; }
}
