package edu.cmu.cs.fluid.ir;

/**
 * Specialized from IRArray to save the space
 * of having separate SimpleSlots.
 * Radically rewritten by John Boyland.
 * 
 * @author chance
 *
 */
public class SimpleIRArray<T> extends IRArray<T,T>  {
  /**
   * @param size
   * @param ss
   */
  public SimpleIRArray(int size) {
    super(size,new ImplicitSlotStorage<T>(SimpleSlotFactory.prototype));
    // TODO Auto-generated constructor stub
  }
}