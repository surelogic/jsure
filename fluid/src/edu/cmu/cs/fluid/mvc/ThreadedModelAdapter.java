package edu.cmu.cs.fluid.mvc;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper of {@link ModelListener}s that insures the wrapped listener is
 * <ul>
 * <li>Executed in a thread distinct from the thread that sent the event it is
 * responded to
 * <li>Executed in a context in which no locks are held
 * </ul>
 * <p>
 * Each adapter has its own thread, which is used over the lifetime of the
 * adapter. The thread is a {@link Thread#setDaemon(boolean) daemon thread} and
 * thus its existence will not prevent the shutdown of the system.
 * 
 * <p>Each <code>ThreadedModelAdapter</code> has a single thread associated with
 * it in which the delegated model listener's {@link #breakView(ModelEvent)}
 * method is executed.  The delegate's {@link #addedToModel(Model)} and
 * {@link #removedFromModel(Model)} methods are left to execute in whatever
 * thread they were called from.
 * 
 * <p>A single instance of <code>ThreadedModelAdapter</code> may be registered
 * as a listener with many models.  <em>The thread associated with the
 * <code>ThreadedModelAdapter</code> will be stopped as soon as the adapter is
 * no longer registered with any models.</em>  The thread does not start
 * execution until the adapter is registered with its first model.
 * 
 * @author Aaron Greenhouse
 */
public final class ThreadedModelAdapter
  extends ModelAdapter
  implements Runnable
{
  /** 
   * The ids (from {@link GlobalModelInformation#getModelID(Model)} of the
   * models with which this adapter is registered.
   */
  private long[] models;
  /** Indicates when it is time to die. */
  private boolean shouldStop = false;
  /** Queue of events awaiting handling. */
  private final List<ModelEvent> events;
  /** The wrapped listener. */
  private final ModelListener listener;

  /**
   * Create a new adapter that insures the given listener
   * is invoked in a thread distinct from the event's sender
   * and is invoked with no locks held.
   */
  public ThreadedModelAdapter( final ModelListener l )
  {
    listener = l;
    events = new ArrayList<ModelEvent>( 5 );
    models = new long[0];
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
        // Execute the nested event handler
        // this *must be done while holding no locks*
        // Otherwise the whole point of this adapter class
        // is compromised.
        for (int i = 0; i < numEvents; i++) {
          listener.breakView(copy[i]);
        }
      }
    }
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
    // Update list of models
    final int len;
    int where = -1;
    synchronized (this) {
      final long id = GlobalModelInformation.getInstance().getModelID(m);
      len = models.length;
      final long[] new_list = new long[len - 1];
      for (int i = 0;(where == -1) && (i < len); i++) {
        if (id == models[i]) where = i;
      }

      if (where != -1) {
        System.arraycopy(models, 0, new_list, 0, where);
        System.arraycopy(models, where + 1, 
            new_list, where, len - where - 1);
        models = new_list;
      }
    }
    
    // Stop the thread if just removed the last model
    if ((len == 1) && (where != -1)) {
      synchronized (events) {
        shouldStop = true;
        events.notify();
      }
    }
    
    // forward to delegate
    listener.removedFromModel(m);
  }
  
  @Override
  public void addedToModel(final Model m) {
    /* Update list of models -- rely on fact that ModelCore already
     * prevents duplicate listeners.
     */ 
    final int len;
    synchronized (this) {
      len = models.length;
      final long[] new_list = new long[len + 1];
      System.arraycopy(models, 0, new_list, 0, len);
      new_list[len] = GlobalModelInformation.getInstance().getModelID(m);
      models = new_list;
    }
    
    // Start thread if we just got the first src model
    if (len == 0) {
      final Thread thread = new Thread( this );
      thread.setDaemon(true);
      thread.start();
    }
    
    // forward to delegate
    listener.addedToModel(m);
  }
}
