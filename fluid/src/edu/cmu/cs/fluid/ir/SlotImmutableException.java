package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidRuntimeException;

public class SlotImmutableException extends FluidRuntimeException {
  public SlotImmutableException() { super(); }
  public SlotImmutableException(String s) { super(s); }
}
