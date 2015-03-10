/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AggregateEvent.java,v 1.9 2007/01/12 18:53:29 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A ModelEvent that contains a sequence of model events.
 * Typically caused by the execution of an atomic action
 * using {@link edu.cmu.cs.fluid.mvc.Model#atomizeAction(AtomizedModelAction)}.
 */
public class AggregateEvent 
extends ModelEvent
{
  /** The list of events. */
  private final List<ModelEvent> events;
  
  /**
   * Generate a new aggregate event.
   * @param src The model sending the event.
   * @param events The ordered list of events to aggregate.
   */
  public AggregateEvent( final Model src, final List<ModelEvent> events )
  {
    super( src );
    this.events = new ArrayList<ModelEvent>( events );
  }

  /**
   * Get an iterator over the <code>ModelEvent</code>s 
   * aggregated by this event.  The events are presented
   * in the order in which the occured.
   */
  public Iterator<ModelEvent> getEvents()
  {
    return events.iterator();
  }

  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder( getStringLeader() );
    buf.append( " (AggregateEvent): [" );
    final Iterator<ModelEvent> iter = getEvents();
    while( iter.hasNext() ) {
      buf.append( iter.next().toString() );
      if( iter.hasNext() ) buf.append( ", " );
    }
    buf.append( ']' );
    return buf.toString();
  }
}

