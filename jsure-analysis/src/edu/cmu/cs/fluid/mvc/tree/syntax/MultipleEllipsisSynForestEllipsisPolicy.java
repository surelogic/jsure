/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/MultipleEllipsisSynForestEllipsisPolicy.java,v 1.11 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.tree.syntax;

import java.util.*;

import edu.cmu.cs.fluid.mvc.tree.ForestEllipsisPolicy;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;

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
 * @author Edwin Chan
 */
public class MultipleEllipsisSynForestEllipsisPolicy
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
   * a <code>Stack</code> of <code>Record</code>s indicating
   * the positions at which nodes were not added.
   */
  private final Map<Object,Stack<Record>> map = new HashMap<Object,Stack<Record>>();

  /** The <CODE>TreeView</code> that this policy is for. */
  private final ConfigurableSyntaxForestView forest;

  /**
   * Create a new policy instance.
   * @param fm The view to be associated with.
   */
  public MultipleEllipsisSynForestEllipsisPolicy(
    final ConfigurableSyntaxForestView fm )
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
    Stack<Record> stack = map.get( key );
    if( stack == null ) {
      stack = new Stack<Record>();
      map.put( key, stack );
    }

    Record rec;
    if( stack.empty() ) {
      rec = new Record( pos );
      stack.push( rec );
    } else {
      rec = stack.peek();
      if( pos != rec.pos ) {
        rec = new Record( pos );
        stack.push( rec );
      }
    } 

    // add nodes to rec's set, special case if the node
    // being skipped is itself an ellipsis
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
      final Stack<Record> stack = map.get( key );

      final IRNode node = (key == treeLevel) ? null : (IRNode)key;
      if( node == null ) {
        // unroll the first iteration of the while loop so that
        // we can do a special case for the first element
        if( !stack.empty() ) {
          Record rec = stack.pop();
//          System.out.println( "pos = " + rec.pos );
          if( rec.pos == roots.size() ){
            forest.appendEllipsis( forest.createEllipsisNode(), null, rec.set );
          } else {
            final IRLocation loc = roots.location( rec.pos );
            forest.setEllipsisAt( forest.createEllipsisNode(), null, loc, rec.set );
          }
          while( !stack.empty() ) {
            rec = stack.pop();
//            System.out.println( "pos = " + rec.pos );
            final IRLocation loc = roots.location( rec.pos );
            forest.setEllipsisAt( forest.createEllipsisNode(), null, loc, rec.set );
          }
        }
      } else {
        // unroll the first iteration of the while loop so that
        // we can do a special case for the first element
        if( !stack.empty() ) {
          Record rec = stack.pop();
          if( rec.pos == forest.numChildren( node ) ) {
            forest.appendEllipsis( forest.createEllipsisNode(), node, rec.set );
          } else {
            final IRLocation loc = forest.childLocation( node, rec.pos );
            forest.setEllipsisAt( forest.createEllipsisNode(), node, loc, rec.set );
          }
          while( !stack.empty() ) {
            rec = stack.pop();
            final IRLocation loc = forest.childLocation( node, rec.pos );
            forest.setEllipsisAt( forest.createEllipsisNode(), node, loc, rec.set );
          }
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

