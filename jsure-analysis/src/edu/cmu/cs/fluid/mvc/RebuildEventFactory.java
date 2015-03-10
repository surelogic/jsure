/*
 * RebuildEventFactory.java
 *
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/RebuildEventFactory.java,v 1.3 2003/07/15 18:39:10 thallora Exp $
 *
 * Created on November 20, 2001, 9:55 AM
 */

package edu.cmu.cs.fluid.mvc;

/** Instances of this class generate {@link RebuildEvent}s for a specific
 * StatefulView.  <em>A StatefulView should not use an instance of
 * this class if the identity of events is significant</em>.
 *
 * @author Aaron Greenhouse
 */
public class RebuildEventFactory {

  /** Cached rebuild begun event.
   */  
  private final RebuildEvent rebuildBegunEvent;
  
  /** Cached rebuild completed event.
   */  
  private final RebuildEvent rebuildCompletedEvent;
  
  /** Create a new rebuild event factory for the given StatefulView.
   * @param src The stateful view for which RebuildEvents are generated.
   */  
  public RebuildEventFactory( final ModelToModelStatefulView src ) {
    rebuildBegunEvent = new RebuildEvent( src, RebuildEvent.REBUILD_BEGUN );
    rebuildCompletedEvent = new RebuildEvent( src, RebuildEvent.REBUILD_COMPLETED );
  }

  /** Get a rebuild event indicating that a rebuild has begun.
   * @return A rebuild event for the StatefulView associated with the factory
   * indicating that a rebuild has begun.  <em>This event is not
   * guaranteed to differ between calls</em>.
   *
   */  
  public RebuildEvent getBegunEvent() {
    return rebuildBegunEvent;
  }
   
  /** Get a rebuild event indicating that a rebuild has completed.
   * @return A rebuild event for the StatefulView associated with the factory
   * indicating that a rebuild has completed.  <em>This event is not
   * guaranteed to differ between calls</em>.
   */  
  public RebuildEvent getCompletedEvent() {
    return rebuildCompletedEvent;
  }
}
