/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/MultipleEllipsisSyntaxVerticalEllipsisPolicy.java,v 1.8 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree.syntax;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.InsertionPoint;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;

/**
 * An ellipsis policy that can produce multiple ellipses as children
 * for a single node.  The policy will add a single ellipsis for
 * each skipped node in a list of children.  It can optionally 
 * merge vertical ellipsis, that is, if an ellipsis would be
 * the child of an ellipsis, it would be merged.
 *
 * @author Aaron Greenhouse
 */
public final class MultipleEllipsisSyntaxVerticalEllipsisPolicy
implements SyntaxForestVerticalEllipsisPolicy
{
  /**
   * Dummy object used to represent a root-level ellipsis in
   * the hashtable.
   */
  private static final Object treeLevel = new Object();  

  /**
   * Hashtable whose keys are all nodes that have had potential children
   * not added to them in the sub-model.  Each key maps to 
   * a {@link LinkedList} used a stack of <code>Record</code>s indicating
   * the positions at which nodes were not added.
   */
  private final Map<Object,LinkedList<Record>> map = new HashMap<Object,LinkedList<Record>>();
  
  /**
   * Map from ellipsis node to set of nodes ellided by the ellipsis.
   */
  private final Map<IRNode,Set<IRNode>> ellidedMap = new HashMap<IRNode,Set<IRNode>>();
  
  /** The <CODE>TreeView</code> that this policy is for. */
  private final ConfigurableSyntaxForestView forest;

  /** Should an ellipsis node be merged with a parent ellipsis node? */
  private final boolean merge;
  
  
  
  /**
   * Create a new policy instance.
   * @param fm The view to be associated with.
   */
  public MultipleEllipsisSyntaxVerticalEllipsisPolicy(
      final ConfigurableSyntaxForestView fm, final boolean shouldMerge)
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
      final int newPos, final int oldPos) {
    // merge vertical ellipsis
    if (parent != null && forest.isEllipsis(parent) && merge) {
      ellidedMap.get(parent).add(node);
      return parent;
    }

    // Check to see if the parent node is an ellipsis, is a variable child
    // node, or a is non-existent (that is node is a root node)
    //
    // short circuit evaluation of || prevents null pointer exception
    //
    // use of forest.getOperator() isn't quite right.  SHould really use
    // the actual source model, but we don't have it.  SHould be okay
    // though because the parent node will alreayd be present in the 
    // exported model.
    if (parent == null || forest.isEllipsis(parent)
        || (forest.getOperator(parent).numChildren() < 0)) {
      /* Here we can merge horizontal ellipsis because we know we don't
       * have to be sensitive to children position.
       */
      final Object key = (parent == null) ? treeLevel : parent;
      LinkedList<Record> stack = map.get(key);
      Set<IRNode> ellidedNodes;
      IRNode ellipsis;
      if (stack == null) {
        stack = new LinkedList<Record>();
        map.put(key, stack);
        ellipsis = forest.createEllipsisNode();
        final Record rec = new Record(ellipsis, -(newPos+1));
        ellidedNodes = new HashSet<IRNode>();
        ellidedMap.put(rec.ellipsis, ellidedNodes);
        stack.addFirst(rec);
      } else {
        Record rec = stack.getFirst();
        final int pos = -(newPos+1);
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
    } else { 
      // Here we cannot merge horizontal ellipsis
      // parent must not be null
//      final Object key = (parent == null) ? treeLevel : parent;
      final Object key = parent;
      LinkedList<Record> stack = map.get(key);
      Set<IRNode> ellidedNodes;
      IRNode ellipsis;
      if (stack == null) {
        stack = new LinkedList<Record>();
        map.put(key, stack);
      }
      ellipsis = forest.createEllipsisNode();
      final Record rec = new Record(ellipsis, oldPos);
      ellidedNodes = new HashSet<IRNode>();
      ellidedMap.put(rec.ellipsis, ellidedNodes);
      stack.addFirst(rec);
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
         * we can do a special case for the first element.  But we know
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
        if (rec.pos < 0) {
          final int pos = -rec.pos - 1;
          if( pos == forest.numChildren( node ) ) {
            forest.appendEllipsis( rec.ellipsis, node, ellidedNodes );
          } else {
            final IRLocation loc = forest.childLocation( node, pos );
            final InsertionPoint ip = InsertionPoint.createBefore( loc );
            forest.insertEllipsisAt( rec.ellipsis, node, ip, ellidedNodes );
          }
        } else {
          final IRLocation loc = forest.childLocation( node, rec.pos );
          forest.setEllipsisAt( rec.ellipsis, node, loc, ellidedNodes );
        }
        while( !stack.isEmpty() ) {
          rec = stack.removeFirst();
          if (rec.pos < 0) {
            final int pos = -rec.pos - 1;
            final IRLocation loc = forest.childLocation( node, pos );
            final InsertionPoint ip = InsertionPoint.createBefore( loc );
            forest.insertEllipsisAt( rec.ellipsis, node, ip, ellidedNodes );
          } else {
            final IRLocation loc = forest.childLocation( node, rec.pos );
            forest.setEllipsisAt( rec.ellipsis, node, loc, ellidedNodes );
          }
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
