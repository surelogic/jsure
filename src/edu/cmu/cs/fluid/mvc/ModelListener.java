package edu.cmu.cs.fluid.mvc;

import java.util.EventListener;

/**
 * Interface for receiving change notification from models.
 * Implementations should not assume anything about the
 * current thread or about what locks are held.
 * In particular, implementations should assume that no locks
 * are held.  In general, to avoid deadlock caused by listener
 * implementations acquiring locks and to avoid starvation of
 * the thread that sends events caused by excessive computation
 * within the listener, listeners should always be wrapped using
 * each {@link ThreadedModelAdapter} or {@link SharedThreadModelAdapter}.
 * 
 * @author Aaron Greenhouse
 */
public interface ModelListener
extends EventListener
{
  /** 
   * Called whenever the model changes.
   */
  public void breakView(ModelEvent e);
  
  /**
   * Called when the listener is added to a model.
   * 
   * @param m
   *          The model that the listener has just been added to
   */
  public void addedToModel(Model m);
  
  /**
   * Called when the listener is removed from the model it was listening to.
   * 
   * @param m
   *          The model that is no longer being listened to
   */
  public void removedFromModel(Model m);
}
