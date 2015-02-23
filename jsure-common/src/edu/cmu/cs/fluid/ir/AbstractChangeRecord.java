/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/AbstractChangeRecord.java,v 1.1 2007/05/25 02:12:41 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.Observable;
import java.util.Observer;


/**
 * Implementation of change record as a derived slot info
 * storing booleans. The derived slot info takes care of observers,
 * and can be an observer of other things.
 * @author boyland
 */
public abstract class AbstractChangeRecord extends DerivedSlotInfo<Boolean> implements ChangeRecord, Observer {

  public AbstractChangeRecord() {
    super();
  }
  
  public AbstractChangeRecord(String name) throws SlotAlreadyRegisteredException {
    super(name, IRBooleanType.prototype);
  }

  @Override
  protected Boolean getSlotValue(IRNode node) {
    return isChanged(node);
  }

  @Override
  protected boolean valueExists(IRNode node) {
    return true;
  }

  @Override
  public void update(Observable o, Object arg) {
    if (arg instanceof IRNode || arg == null) setChanged((IRNode)arg);
  }

}