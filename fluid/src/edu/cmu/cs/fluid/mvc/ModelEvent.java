/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ModelEvent.java,v 1.9 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.EventObject;

/**
 * Event sent by a model when its contents have changed. All events sent by a
 * model will be instances of this class or one of its subclasses. Receipt of an
 * instance of <code>ModelEvent</code> contains the least amount of
 * information, and simply indicates that sender broke in some way and that the
 * receiver should rebuild itself. Subclasses of this event generally indicate
 * more specific information, for example, that a particular attribute's value
 * changed for a given node, or that a node changed position within an ordered
 * list.
 * 
 * <p>
 * A Model can also send a model event to indicate information about what it is
 * doing. Such events should not cause their Stateful Views to rebuild. For
 * example, the {@link edu.cmu.cs.fluid.mvc.RebuildEvent} is used to communicate
 * when the model has started and finished rebuilding. It is used by various
 * internal processes to manage the overall rebuild of a model&ndash;view chain,
 * and the stateful views filter them out for the purposes of refreshing
 * themselves. The method {@link #shouldCauseRebuild} should be checked to
 * determine if the event is intended to convey that the model changed in some
 * way interesting to its views.
 * 
 * @see ModelListener
 * @author Aaron Greenhouse
 */
public class ModelEvent extends EventObject
{
  /**
   * Create a new event.
   * @param source The model sending the event.
   */
  public ModelEvent( final Model source )
  {
    super( source );
  }

  /**
   * Indicates whether this event should cause a stateful view to rebuild.
   * 
   * @return This implementation always returns <code>true</code>.
   */
  public boolean shouldCauseRebuild() {
    return true;
  }
  
  /**
   * Get the model that changed.  This returns the same object
   * as {@link java.util.EventObject#getSource} but typecast
   * to be a <code>Model</code>.
   */
  public Model getSourceAsModel()
  {
    return (Model)getSource();
  }

  protected final String getStringLeader()
  {
    return "Event from \"" + getSourceAsModel().getName() + "\"";
  }
  
  @Override
  public String toString()
  {
    return getStringLeader() + " (ModelEvent)";
  }
}

