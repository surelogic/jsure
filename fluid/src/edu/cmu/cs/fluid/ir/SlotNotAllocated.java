/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotNotAllocated.java,v 1.3 2003/07/02 20:19:14 thallora Exp $ */
package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidException;

public class SlotNotAllocated extends FluidException {
  public SlotNotAllocated() { super(); }
  public SlotNotAllocated(String s) { super(s); }
  
}
