package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidException;

public class SlotNotRegisteredException extends FluidException {
  public SlotNotRegisteredException() { super(); }
  public SlotNotRegisteredException(String s) { super(s); }
}
