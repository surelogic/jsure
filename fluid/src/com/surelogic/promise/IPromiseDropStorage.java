/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/IPromiseDropStorage.java,v 1.9 2007/07/13 18:02:57 chance Exp $*/
package com.surelogic.promise;

import java.util.*;

import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.sea.*;

/**
 * An interface for defining the storage/mapping from an IRNode
 * to a promise drop
 * 
 * @author Edwin.Chan
 */
public interface IPromiseDropStorage<D extends PromiseDrop<?>> {  
  /**
   * @return The name of the SlotInfo to be created
   */
  String name();
  
  /**
   * @return The type of storage (boolean, drop, seq of drops) for each IRNode
   */
  StorageType type();
  
  /**
   * @return The kind of drops being stored
   */
  Class<D> baseDropType();
  
  /**
   * @throws UnsupportedOperationException if n/a
   */
  SlotInfo<D> getSlotInfo();
  
  /**
   * @throws UnsupportedOperationException if n/a
   */
  SlotInfo<List<D>> getSeqSlotInfo();

  /**
   * Associate the drop with the IRNode.
   * May check for existing drops if of boolean/drop type
   * 
   * @return The drop that was added to the SlotInfo
   */
  D add(IRNode n, D d);
  
  /**
   * Disassociate the drop from the IRNode.
   */
  void remove(IRNode n, D d);

  /**   
   * @param node 
   * @return true if there is at least one associated drop 
   */
  boolean isDefined(IRNode node);
  
  Iterable<D> getDrops(IRNode n);
}
