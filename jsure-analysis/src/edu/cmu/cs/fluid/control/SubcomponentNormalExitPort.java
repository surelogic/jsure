package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class SubcomponentNormalExitPort extends SimpleInputPort
    implements SubcomponentPort, NormalExitPort
{
  protected ISubcomponent subcomponent;
  
  public SubcomponentNormalExitPort(Subcomponent subcomp) {
    subcomponent = subcomp;
    subcomp.registerNormalExitPort(this);
  }

  @Override
  public ISubcomponent getSubcomponent() {
    return subcomponent;
  }

  @Override
  public Port getDual() {
    Component childComp = subcomponent.getComponentInChild();
    if (childComp == null) return null;
    return childComp.getNormalExitPort();
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
