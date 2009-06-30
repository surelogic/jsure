package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelEvent;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Event sent by a SequenceModel when it has changed.  The payload of the
 * event is a flag, a node, and a location.  The meaning of the node and
 * location depend on the flag:
 *
 * <table>
 * <thead>
 * <tr><th>Flag <th>Node <th>Location
 * <tbody>
 * <tr><td><code>{@link #NODE_INSERTED}
 *     <td>The new node inserted into the model.
 *     <td>The location at which the node was inserted.
 *
 * <tr><td><code>{@link #NODE_REMOVED}
 *     <td><em>Unused</em>.  (Node is not provided because the node is no 
 *         longer in the model and thus no information about the node can be
 *         discovered.)
 *     <td>The location at which the removal occured.
 *
 * <tr><td><code>{@link #NODE_MOVED}
 *     <td>The previously and still existing node in the model that was
 *         moved with respect to the other nodes in the model.
 *     <td>The new location of the node.  (Note that the locations of the other
 *         nodes in the model will have also changed).
 *
 * <tr><td><code>{@link #NODE_REPLACED}
 *     <td>The new node added to the model.
 *     <td>The location at which it <em>replaced</em> another node.  The node
 *         previously at this location is no longer in the model.  (The size
 *         of the sequence remains unchanged.)
 * </table>
 *
 * @author Aaron Greenhouse
 */
public final class SequenceModelEvent
extends ModelEvent
{
  /**
   * The node in this event was inserted to the model.
   * The location is the location of the node.
   */
  public static final int NODE_INSERTED = 0;

  /**
   * A node was removed at the location contained in
   * this event.  The node portion of the event is 
   * not used.
   */
  public static final int NODE_REMOVED = 1;

  /**
   * The node in this event changed location in the model.
   * The location is the location the node was moved to.
   * This affects the locations of other nodes in the model!
   */
  public static final int NODE_MOVED = 2;

  /**
   * The node in this event replaced another node
   * at the location given by the event.  The node
   * previously at the given location is no longer
   * in the model
   */
  public static final int NODE_REPLACED = 3;

  private final int flag;
  private final IRNode node;
  private final IRLocation location;

  /**
   * Create a new event.
   * @param source The model sending the event.
   * @param f Flag indicating the kind of the event.
   * @param n IRNode that was added
   * @param loc The location in the sequence where the node was added
   */
  public SequenceModelEvent(
    final Model source, final int f, final IRNode n, final IRLocation loc )
  {
    super( source );
    node = n;
    location = loc;
    flag = f;
  }

  /**
   * Create a new event.
   * @param source The model sending the event.
   * @param f Flag indicating the kind of the event.
   * @param loc The location in the sequence where change occured
   */ 
  public SequenceModelEvent(
    final Model source, final int f, final IRLocation loc )
  {
    this( source, f, null, loc );
  }

  /** Get the node that was affected. */
  public IRNode getNode()
  {
    return node;
  }

  /** Get the location of the changed */
  public IRLocation getLocation()
  { 
    return location;
  }

  /** Query if the event is a node inserted event. */
  public boolean isNodeInserted()
  {
    return flag == NODE_INSERTED;
  }

  /** Query if the event is a node removed event. */
  public boolean isNodeRemoved()
  {
    return flag == NODE_REMOVED;
  }

  /** Query if the event is a node moved event. */
  public boolean isNodeMoved()
  {
    return flag == NODE_MOVED;
  }

  /** Query if the event is a node replaced event. */
  public boolean isNodeReplaced()
  {
    return flag == NODE_REPLACED;
  }
  
  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (SequenceModelEvent): [" );
    if( flag == NODE_INSERTED ) {
      buf.append( "inserted " );
      buf.append( node.toString() );
      buf.append( " at " );
      buf.append( location.toString() );      
    } else if( flag == NODE_REMOVED ) {
      buf.append( "removed node at " );
      buf.append( location.toString() );      
    } else if( flag == NODE_MOVED ) {
      buf.append( "moved " );
      buf.append( node.toString() );
      buf.append( " to " );
      buf.append( location.toString() );      
    } else if( flag == NODE_REPLACED ) {
      buf.append( "replaced node at " );
      buf.append( location.toString() );      
      buf.append( " with " );
      buf.append( node.toString() );
    }
    buf.append( ']' );
    return buf.toString();
  }
}

