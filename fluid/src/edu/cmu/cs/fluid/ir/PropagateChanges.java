/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/PropagateChanges.java,v 1.1 2007/05/25 02:12:41 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.Observable;
import java.util.Observer;


/**
 * An adapter that causes change propagation.
 * @author boyland
 */
public abstract class PropagateChanges implements Observer {
  private final ChangeRecord changeInfo;
  
  public PropagateChanges(ChangeRecord ci) {
    changeInfo = ci;
    changeInfo.addObserver(this);
  }

  @Override
  public void update(Observable o, Object arg) {
    if (o == changeInfo && arg instanceof IRNode) noteChange((IRNode)arg);
  }
  
  /**
   * Node which information should be propagated from.
   * @param n node change appeared on (never null)
   */
  protected abstract void noteChange(IRNode n);
  
  /**
   * Call this method to propagate change to this new node.
   * @param n node to proagate change to.  Ignored if null.
   */
  protected void propagateChange(IRNode n) {
    if (n != null) {
      changeInfo.setChanged(n);
    }
  }
}
