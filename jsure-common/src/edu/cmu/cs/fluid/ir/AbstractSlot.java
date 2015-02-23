/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/AbstractSlot.java,v 1.1 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;

/** Default implementations for storage methods of a slot.
 */
public abstract class AbstractSlot<T> implements Slot<T> {
  /** Does this slot contain changed information.
   * By default, assume not.
   */
  @Override
  public boolean isChanged() {
    return false;
  }
  @Override
  public void writeValue(IRType<T> ty, IROutput out) 
     throws IOException
  {
    ty.writeValue(getValue(),out);
  }
  @Override
  public Slot<T> readValue(IRType<T> ty, IRInput in) 
     throws IOException
  {
    // System.out.println("Reading slot of type " + ty);
    return setValue(ty.readValue(in));
  }

  @Override
  public void describe(PrintStream out) {
    out.println(getClass().getName());
  }
  
  // keep track of some things
  { edu.cmu.cs.fluid.util.CountInstances.add(this); }
}
