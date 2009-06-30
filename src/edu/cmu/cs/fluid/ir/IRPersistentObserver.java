/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRPersistentObserver.java,v 1.2 2003/07/02 20:19:14 thallora Exp $ */
package edu.cmu.cs.fluid.ir;

/** Class which wants to be informed when elements get
 * assigned to IRPersistent entities, for instance, when
 * a node is assigned to a region, or a SlotInfo to a Bundle.
 */
public interface IRPersistentObserver {
  public void updatePersistent(IRPersistent p, Object o);
}
