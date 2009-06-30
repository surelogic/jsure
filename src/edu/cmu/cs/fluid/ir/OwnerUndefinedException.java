package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.FluidRuntimeException;

public class OwnerUndefinedException extends FluidRuntimeException {
  public final IRNode node;
  public OwnerUndefinedException() { super("owner undefined"); node = null;}
  public OwnerUndefinedException(IRNode n) { super("owner undefined"); node=n;}
  public IRNode getUnowned() { return node; }
}
