/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/FluidError.java,v 1.5 2007/10/16 14:09:07 chance Exp $ */
package edu.cmu.cs.fluid;

public class FluidError extends RuntimeException {
  // public 
  protected FluidError() { super(); }
  public FluidError(String s) { super(s); }
  
  public FluidError(String s, Throwable t) { super(s, t); }
}
