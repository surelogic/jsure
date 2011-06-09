/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/Worklist.java,v 1.3 2007/05/17 21:31:50 chance Exp $*/
package edu.uwm.cs.fluid.control;

import edu.cmu.cs.fluid.control.ControlNode;

/**
 * A CFG analysis worklist.
 * @author boyland
 */
public interface Worklist extends Cloneable {

  /**
   * Get the worklist ready to start adding new nodes and do a new analysis.
   * At least one call to initialize <em>must</em> occur before each
   * call to {@link #start()}.  After this call and before the call
   * to {@link #start()}, there may be one or more calls to {@link #add(ControlNode)}.
   */
  public void initialize();
  
  /**
   * The worklist is told that it will be asked to start from the beginning.
   * This call must be immediately preceded by one or more calls to
   * {@link #initialize(ControlNode)} (with following calls to {@link #add(ControlNode)}. 
   * This method may do a lot
   * of prepatory work, but should be no more than O(n lg n) where n
   * is the size of the CFG.
   */
  public void start();
  
  /**
   * Determine whether the worklist is non-empty.
   * @return true if an node remains on the worklist.
   */
  public boolean hasNext();
  
  /**
   * The current size of the worklist
   */
  public int size();
  
  /**
   * Return the next node on the worklist.
   * @return node next to consider.
   */
  public ControlNode next();
  
  /**
   * Return a copy of this worklist strategy without any initialization state.
   * @return copied worklist ready to use in a new context
   */
  public Worklist clone();
  
  /**
   * Indicate that the given node must be in the worklist.
   * After this happens, the worklist must guarantee that this node
   * is returned at some point in the future from {@link #next()}.
   * @param node node to add to worklist
   */
  public void add(ControlNode node);
  
  public static class Factory {
    public static Worklist makeWorklist(boolean isForward) {
      // TODO: use a quick property to change
      //return new QueueWorklist();
      //return new ScheduleWorklist(isForward);
      return new PriorityQueueWorklist(isForward);
      //return new BufferWorklist();      
    }
  }
}
