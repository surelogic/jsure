/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotAlreadyAllocated.java,v 1.3 2003/07/02 20:19:12 thallora Exp $ */
package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidException;

public class SlotAlreadyAllocated extends FluidException {
  public SlotAlreadyAllocated() { super(); }
  public SlotAlreadyAllocated(String s) { super(s); }
}
