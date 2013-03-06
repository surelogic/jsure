package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class SubcomponentBooleanExitPort extends DoubleInputPort
    implements SubcomponentPort, NormalExitPort
{
  protected Subcomponent subcomponent;
  
  public SubcomponentBooleanExitPort(Subcomponent subcomp) {
    subcomponent = subcomp;
    subcomp.registerNormalExitPort(this);
  }

  @Override
  public Subcomponent getSubcomponent() {
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

