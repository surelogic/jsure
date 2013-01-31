package edu.cmu.cs.fluid.mvc;

import java.util.ArrayList;
import java.util.List;
import edu.cmu.cs.fluid.FluidError;

/**
 * Wrapper of {@link ModelListener}s that insures the wrapped listeners are
 * <ul>
 * <li>Executed in a thread distinct from the thread that sent the event it is
 * responding to
 * <li>Executed in a context in which no locks are held
 * </ul>
 * 
 * <p>The intent behind this thread is that multiple model listeners for a 
 * single model can <em>share</em> a single thread.  It is a misuse of this
 * class to register an instance as a listener with two different models, and
 * an exception will be thrown if this done.  This class is used by first
 * {@link #SharedThreadModelAdapter(Model) creating a new instance}.  The 
 * adapter can then be added as a listener to the model it was previously
 * associated with.  The <code>ModelListener</code> object can be 
 * {@link #addModelListener(ModelListener) registered with the adapter}.
 * 
 * <p>If the adapter is registered as a listener with a model that is not the
 * model it was initialized with, then the {@link SharedThreadModelAdapter#addedToModel(Model)}
 * method will throw an <code>IllegalArgumentException</code>.
 * 
 * <p>
 * The created threads are all {@link java.lang.Thread#setDaemon(boolean) daemon}
 * threads.  The thread is stopped when the adapter is removed from the model
 * it is attached to.
 * 
 * @author Aaron Greenhouse
 */
public final class SharedThreadModelAdapter
  extends ModelAdapter
  implements Runnable
{
  /**
   * The model this adapter is associated with.
   */
  private final Model model;
  
  /**
   * The list of listener delegates to be executed when a model event is
   * received.
   */
  private ModelListener[] listeners = new ModelListener[0];
  
  /** Indicates when it is time to die. */
  private boolean shouldStop = false;
  
  /** Queue of events awaiting handling. */
  private final List<ModelEvent> events;
  
  public SharedThreadModelAdapter(final Model m) {
    model = m;
    events = new ArrayList<ModelEvent>( 5 );
  }
  
  /**
   * Thread body.  Loop waiting for a signal that an event
   * has arrived.  When signaled, invokes the wrapped
   * event listener once for each event that has arrived.
   */
  @Override
  public void run() {
    boolean localShouldStop = false;
    int numEvents = 0;
    ModelEvent[] copy = new ModelEvent[1];

    // run forever
    while (!localShouldStop) {
      // Wait for an event to arrive
      synchronized (events) {
        while (!shouldStop && events.isEmpty()) {
          try {
            events.wait();
          } catch (final InterruptedException e) {
          }
        }
        
        localShouldStop = shouldStop;

        if (!shouldStop) {
          // Copy the incoming events to a local array
          // so that the events can be processed while
          // new events arrive
          numEvents = events.size();
          copy = events.toArray(copy);
          events.clear();
        }
      }

      if (!localShouldStop) {
        // Execute the nested event handlers
        // this *must be done while holding no locks*
        // Otherwise the whole point of this adapter class
        // is compromised.
        // atomic copy of reference!
        final ModelListener[] ml = listeners;
        for (int i = 0; i < numEvents; i++) {
          for (int j = 0; j < ml.length; j++) {
            ml[j].breakView(copy[i]);
          }
        }
      }
    }
  }
  
  public void addModelListener(final ModelListener l) {
    synchronized (this) {
      final int len = listeners.length;

      for (int i = 0; i < len; i++) {
        if (l == listeners[i]) {
          throw new FluidError("Duplicated listener");
        }
      }

      final ModelListener[] new_list = new ModelListener[len + 1];
      System.arraycopy(listeners, 0, new_list, 0, len);
      new_list[len] = l;
      // Atomic value assignment!!!
      listeners = new_list;
    }
    // Inform the listener that it was added to a model
    l.addedToModel(model);
  }

  public void removeModelListener(final ModelListener l) {
    synchronized (this) {
      final int len = listeners.length;
      final ModelListener[] new_list = new ModelListener[len - 1];
      int where = -1;
      for (int i = 0;(where == -1) && (i < len); i++) {
        if (l == listeners[i])
          where = i;
      }

      if (where != -1) {
        System.arraycopy(listeners, 0, new_list, 0, where);
        System.arraycopy(listeners, where + 1, 
            new_list, where, len - where - 1);
        // Atomic value assignment!!!
        listeners = new_list;
      }
    }
    
    // Inform listener that it was removed
    l.removedFromModel(model);
  }

  /**
   * Signals the worker thread to wake up when an event comes in.
   * This runs in the thread of the event's sender.
   */
  @Override
  public void breakView( final ModelEvent e ) 
  {
    synchronized( events ) {
      // Add event to the list of events
      events.add( e );
      // Signal that events is non-empty
      events.notify();
    }
  }
  
  /**
   * When we are removed from our source model then we kill the
   * processing thread.
   */
  @Override 
  public void removedFromModel(final Model m) {
    if (m == model) {
      // Stop the tread
      synchronized (events) {
        shouldStop = true;
        events.notify();
      }

      // Kill the listeners
      final ModelListener[] copy;
      synchronized (this) {
        copy = listeners;
        listeners = new ModelListener[0];
      }
      
      for (int i = 0; i < copy.length; i++) {
        copy[i].removedFromModel(model);
        copy[i] = null;
      }
    }
  }
  
  @Override
  public void addedToModel(final Model m) {
    if (m != model) {
      throw new IllegalArgumentException(
          "Can only be added to model \"" + model.getName() +
          "\" but added to \"" + m.getName() + "\"");
    }
    
    /* Don't forward to our delegates because they are notified
     * when they are added to the adapter.
     */ 
    
    // Start the thread
    final Thread t = new Thread(this);
    t.setDaemon(true);
    t.start();
  }
}
