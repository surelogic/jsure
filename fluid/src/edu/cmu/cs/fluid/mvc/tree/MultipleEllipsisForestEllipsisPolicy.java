/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/MultipleEllipsisForestEllipsisPolicy.java,v 1.17 2007/07/05 18:15:17 aarong Exp $ */
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
public final class MultipleEllipsisForestEllipsisPolicy
implements ForestEllipsisPolicy
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

  /** The <CODE>TreeView</code> that this policy is for. */
  private final ConfigurableForestView forest;

  /**
   * Create a new policy instance.
   * @param fm The view to be associated with.
   */
  public MultipleEllipsisForestEllipsisPolicy( final ConfigurableForestView fm )
  {
    forest = fm;
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
    LinkedList<Record> stack = map.get( key );
    Record rec;
    if( stack == null ) {
      stack = new LinkedList<Record>();
      map.put( key, stack );
      rec = new Record( pos );
      stack.addFirst( rec );
    } else {
      rec = stack.getFirst();
      if( pos != rec.pos ) {
        rec = new Record( pos );
        stack.addFirst( rec );
      }
    } 
    rec.set.add( node );
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
        if( rec.pos == roots.size() ){
          forest.appendEllipsis( forest.createEllipsisNode(), null, rec.set );
        } else {
          final IRLocation loc = roots.location( rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( forest.createEllipsisNode(), null, ip, rec.set );
        }
        while( !stack.isEmpty() ) {
          rec = stack.removeFirst();
          final IRLocation loc = roots.location( rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( forest.createEllipsisNode(), null, ip, rec.set );
        }
      } else {
        /* Unroll the first iteration of the while loop so that
         * we can do a special case for the first element.  But the we know
         * the stack has at least 1 element, so we don't need to check if it is
         * empty the first time.
         */
        Record rec = stack.removeFirst();
        if( rec.pos == forest.numChildren( node ) ) {
          forest.appendEllipsis( forest.createEllipsisNode(), node, rec.set );
        } else {
          final IRLocation loc = forest.childLocation( node, rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( forest.createEllipsisNode(), node, ip, rec.set );
        }
        while( !stack.isEmpty() ) {
          rec = stack.removeFirst();
          final IRLocation loc = forest.childLocation( node, rec.pos );
          final InsertionPoint ip = InsertionPoint.createBefore( loc );
          forest.insertEllipsisAt( forest.createEllipsisNode(), node, ip, rec.set );
        }
      }
    }
  }


  private static class Record
  {
    public final int pos;
    public final Set<IRNode> set;

    public Record( final int p ) 
    {
      pos = p;
      set = new HashSet<IRNode>();
    }
  }

  @Override
  public String toString() { return "Multiple ellipses"; }
}
