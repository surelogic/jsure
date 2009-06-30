package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidRuntimeException;

public class SlotUndefinedException extends FluidRuntimeException {
  public SlotUndefinedException() { super(); }
  public SlotUndefinedException(String s) { super(s); }
}
