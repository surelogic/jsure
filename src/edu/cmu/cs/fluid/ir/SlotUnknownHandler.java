/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotUnknownHandler.java,v 1.2 2003/07/02 20:19:13 thallora Exp $ */

package edu.cmu.cs.fluid.ir;

/** A handler is asked to load information to handle
 * a slot unknown error.  If no registered handler
 * can handle the problem, an exception is raised.
 * @see SlotUnknownException
 */
public interface SlotUnknownHandler {
    /* Returns 'true' to
     * mean to retry the slot access, or 'false' to mean
     * that the handler was unable to help.  A handler must not
     * return true without being preared for being called again if 
     * the information didn't work.
     */
    public boolean canHandle(SlotUnknownException e);
}
