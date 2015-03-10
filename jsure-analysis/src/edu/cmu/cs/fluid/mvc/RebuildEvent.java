/*
 * RebuildEvent.java
 *
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/RebuildEvent.java,v 1.9 2007/07/05 18:15:16 aarong Exp $
 *
 * Created on November 20, 2001, 9:38 AM
 */

package edu.cmu.cs.fluid.mvc;

/**
 * Event sent by a Stateful View to indicate that is beginning or completing
 * a rebuild of its internal state.  This event is not meant to trigger
 * rebuilds.
 *
 * @see RebuildEventFactory
 *
 * @author  Aaron Greenhouse
 */
public class RebuildEvent extends ModelEvent {

  /** Constant defining value for an event indicating that the sender has begun
   * the rebuild process.
   */  
  public static final boolean REBUILD_BEGUN = true;
  
  /** Constant defining the value of an event indicating that the sender has
   * completed the rebuild process.
   */  
  public static final boolean REBUILD_COMPLETED = false;
  
  /** The value of the event: {@link #REBUILD_BEGUN} if the event indicates
   * the start of arebuild; {@link #REBUILD_COMPLETED} if the event indicates
   * the completion of a rebuild.
   */  
  private final boolean isBegin;
  
  /** Creates new RebuildEvent with the given status.
   * @param src The model sending the event.
   * @param isBegin The value for the event, {@link #REBUILD_BEGUN} if the event
   * signals the start of a rebuild; {@link #REBUILD_COMPLETED} if the
   * event signals the completion of a rebuild.
   */
  public RebuildEvent( final Model src, final boolean isBegin ) {
    super( src );
    this.isBegin = isBegin;
  }

  @Override
  public boolean shouldCauseRebuild() {
    return false;
  }
  
  /** Querty the rebuild state carried by the event.
   * @return {@link #REBUILD_BEGUN} xor {@link #REBUILD_COMPLETED}
   */
  public boolean getRebuildState() {
    return isBegin;
  }
  
  /** Query if the event signals that start of a rebuild.
   * @return <CODE>true</CODE> iff the event signals the start of a rebuild.
   */  
  public boolean isBegin() {
    return isBegin;
  }
    
  /** Query if the event signals the completion of a rebuild.
   * @return <CODE>true</CODE> iff the event signals the completion of a rebuild.
   */  
  public boolean isCompleted() {
    return !isBegin;
  }
  
  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (RebuildEvent): [" );
    buf.append( isBegin ? "begin]" : "end]" );
    return buf.toString();
  }
}
