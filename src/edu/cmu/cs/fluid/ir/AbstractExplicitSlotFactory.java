/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/AbstractExplicitSlotFactory.java,v 1.4 2007/07/10 22:16:31 aarong Exp $
 */
package edu.cmu.cs.fluid.ir;


/**
 * A slot factory that uses explicit slots to store values
 * @author boyland
 */
public abstract class AbstractExplicitSlotFactory extends AbstractSlotFactory implements ExplicitSlotFactory {

  private final SlotStorage defaultStorage = new ExplicitSlotStorage(this);
  
  // the following type-unsafe method is actually OK.
  @SuppressWarnings("unchecked")
  public <T> SlotStorage<Slot<T>,T> getStorage() {
    return defaultStorage; // new ExplicitSlotStorage<T>(this); // wastes memory
  }
}
