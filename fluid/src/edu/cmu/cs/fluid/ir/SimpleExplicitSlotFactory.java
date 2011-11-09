/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SimpleExplicitSlotFactory.java,v 1.2 2006/03/27 21:35:50 boyland Exp $
 */
package edu.cmu.cs.fluid.ir;


/**
 * A slot factory which (unnecessarily) uses explicit slots to store information
 * @see SimpleSlotFactory
 * @author boyland
 */
public class SimpleExplicitSlotFactory extends AbstractExplicitSlotFactory {
  private SimpleExplicitSlotFactory() {}
  public static final SimpleExplicitSlotFactory prototype = new SimpleExplicitSlotFactory();
  static {
    IRPersistent.registerSlotFactory(prototype,'s');
  }
  public <T> Slot<T> undefinedSlot() {
    return new UndefinedSimpleSlot<T>();
  }
  public <T> Slot<T> predefinedSlot(T value) {
    return new PredefinedSimpleSlot<T>(value);
  }
  
  public void noteChange(IRState state) {
    SimpleSlotFactory.noteChanged(state);
  }
}
