package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class SubcomponentAbruptExitPort extends SimpleInputPort
    implements SubcomponentPort, AbruptExitPort
{
  protected Subcomponent subcomponent;
  
  public SubcomponentAbruptExitPort(Subcomponent subcomp) {
    subcomponent = subcomp;
    subcomp.registerAbruptExitPort(this);
  }

  @Override
  public Subcomponent getSubcomponent() {
    return subcomponent;
  }

  @Override
  public Port getDual() {
    Component childComp = subcomponent.getComponentInChild();
    if (childComp == null) return null;
    return childComp.getAbruptExitPort();
  }  

  @Override
  public IRNode getSyntax() {
    return subcomponent.getSyntax();
  }

  @Override
  public Component getComponent() {
    return subcomponent.getComponentInChild();
  }
}

