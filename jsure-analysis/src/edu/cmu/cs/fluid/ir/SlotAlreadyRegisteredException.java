package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidException;

public class SlotAlreadyRegisteredException extends FluidException {
  public SlotAlreadyRegisteredException() { super(); }
  public SlotAlreadyRegisteredException(String s) { super(s); }
}
