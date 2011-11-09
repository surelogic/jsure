/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IUndefinedSlotHandler.java,v 1.3 2008/08/22 18:19:55 chance Exp $*/
package edu.cmu.cs.fluid.ir;

@SuppressWarnings("unchecked")
public interface IUndefinedSlotHandler {
  boolean handleSlotUndefinedException(PersistentSlotInfo si, IRNode n);
  
  IUndefinedSlotHandler nullPrototype = new IUndefinedSlotHandler() {
    public boolean handleSlotUndefinedException(PersistentSlotInfo si, IRNode n) {
      return false;
    }
  };
}
