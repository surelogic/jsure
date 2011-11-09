/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ChangeRecord.java,v 1.2 2007/07/10 22:16:31 aarong Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.Observer;


/**
 * Recording and combiner of change information.
 * @author boyland
 */
public interface ChangeRecord {
  /**
   * Record that something has changed for this node.
   * Return true if no change was recorded before.
   */
  public boolean setChanged(IRNode node);
  
  /**
   * Return whether any changes have been recorded for this node.
   * @param node
   * @return true if we have recorded a change for this node (since the last clear).
   */
  public boolean isChanged(IRNode node);
  
  /**
   * Record the current state as "unchanged".
   * After this call, all calls to {@link #isChanged(IRNode)} will return false,
   * until explicit set as changed.
   */
  public void clearChanges();
  
  /**
   * The observer will be informed of all changes that happen here.
   * <p>
   * TODO: The following behavior is probably too expensive to support
   * <blockquote>
   * If there are current changes, the observer will be called on them
   * immediately.
   * </blockquote>
   * @param o
   */
  public void addObserver(Observer o);

}
