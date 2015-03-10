/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/SetModelEvent.java,v 1.9 2007/07/05 18:15:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelEvent;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Event sent by a SequenceModel when it has changed.
 * @author Aaron Greenhouse
 */
public final class SetModelEvent
extends ModelEvent
{
  /**
   * A node was added to the set.
   */
  public static final int NODE_ADDED = 0;

  /**
   * A node was removed from the set.
   */
  public static final int NODE_REMOVED = 1;


  private final int flag;
  private final IRNode node;

  /**
   * Create a new event.
   * @param source The model sending the event.
   * @param f Flag indicating the kind of the event.
   * @param n IRNode that was added
   */
  public SetModelEvent( final Model source, final int f, final IRNode n )
  {
    super( source );
    node = n;
    flag = f;
  }

  /** Get the node that was affected. */
  public IRNode getNode()
  {
    return node;
  }

  /** Query if the event is a node added event. */
  public boolean isNodeAdded()
  {
    return flag == NODE_ADDED;
  }

  /** Query if the event is a node removed event. */
  public boolean isNodeRemoved()
  {
    return flag == NODE_REMOVED;
  }
  
  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (SetModelEvent): [" );
    buf.append( (flag == NODE_ADDED) ? "added " : "removed " );
    buf.append( node.toString() );
    buf.append( ']' );
    return buf.toString();
  }
}
