/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotUnknownException.java,v 1.9 2007/01/18 16:37:38 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.*;

/** A slot's value is unknown because of incomplete information:
 * the value is not loaded, for example.  The problem cannot
 * be fixed by assignment.
 * 
 * @region static Handlers
 * @lock HandlerLock is class protects Handlers
 */
public class SlotUnknownException extends SlotUndefinedException
{
  public final Slot slot;

  public SlotUnknownException(String msg,Slot s) { super(msg); slot = s; }  

  public Slot getSlot() { return slot; }

  /**
   * @mapInto Handlers
   * @unique
   * @aggregate Instance into Handlers
   */
  private static ArrayList<SlotUnknownHandler> handlers = new ArrayList<SlotUnknownHandler>();
  private static SlotUnknownHandler[] emptyHandlerArray = new SlotUnknownHandler[0];
  
  public static synchronized void addHandler(SlotUnknownHandler h) {
  	handlers.add(h);
  }
  public static synchronized void removeHandler(SlotUnknownHandler h) {
    handlers.remove(h);
  }

  /** Try to handle this exception.
   * If the exception is handled and the access should be retried,
   * this function returns normally.  If the exception could not
   * be handled, it is thrown.
   */
  public void handle() throws SlotUnknownException {
    SlotUnknownHandler[] copiedHandlers;
    synchronized (SlotUnknownException.class) {
      // this can be very efficient if the handler list is empty
      copiedHandlers = handlers.toArray(emptyHandlerArray);
    }
    for (SlotUnknownHandler h : copiedHandlers) {
      if (h.canHandle(this)) return;
    }
    throw this;
  }

  /** Try to handle this slot being unknown.
   *  Calls @{link #handle()} with a newly
   *  created exception.
   */
  public static void raise(String mesg, Slot s)
	  throws SlotUnknownException
  {
  	new SlotUnknownException(mesg,s).handle();
  }
}
