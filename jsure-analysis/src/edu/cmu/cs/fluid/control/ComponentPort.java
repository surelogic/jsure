/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentPort.java,v 1.5 2005/05/25 03:28:36 boyland Exp $ */
package edu.cmu.cs.fluid.control;

public interface ComponentPort extends Port,ComponentNode {
  @Override
  public abstract Component getComponent();
}
