/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/SubcomponentPort.java,v 1.5 2005/05/25 03:28:36 boyland Exp $ */
package edu.cmu.cs.fluid.control;

public interface SubcomponentPort extends Port, ComponentNode {
  @Override
  public abstract ISubcomponent getSubcomponent();
}
