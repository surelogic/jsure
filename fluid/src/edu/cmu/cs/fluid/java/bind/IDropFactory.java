/*
 * Created on Jan 25, 2005
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.sea.*;


/**
 * Primarily used for boolean promises
 * 
 * @author Edwin
 *
 */
public interface IDropFactory<D extends PromiseDrop, V> {
  /**
   * @return The SlotInfo used to hold the drops
   */
  SlotInfo<D> getSI();

  /**
   * Create a new drop for the node
   * 
   * @param n The node that the drop is associated with
   * @return
   */
  D createDrop(IRNode n, V val);
  
  /**
   * Get a drop for the node, creating one only 
   * if there isn't one already
   * 
   * @param n The node that the drop is associated with
   * @return
   */
  D getDrop(IRNode n, V val);
}
