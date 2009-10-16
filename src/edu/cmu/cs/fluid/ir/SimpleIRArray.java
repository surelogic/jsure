package edu.cmu.cs.fluid.ir;

/**
 * Specialized from IRArray to save the space
 * of having separate SimpleSlots.
 * Radically rewritten by John Boyland.
 * 
 * @author chance
 *
 */
@SuppressWarnings("unchecked")
public class SimpleIRArray<T> extends IRArray<T,T>  {
  private static final ImplicitSlotStorage prototype =
	  new ImplicitSlotStorage(SimpleSlotFactory.prototype);

  /**
   * @param size
   * @param ss
   */
  public SimpleIRArray(int size) {
    super(size, prototype);
  }
}