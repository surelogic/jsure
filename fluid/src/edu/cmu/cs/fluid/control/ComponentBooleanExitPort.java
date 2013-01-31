package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentBooleanExitPort extends DoubleOutputPort
    implements ComponentPort, NormalExitPort
{
  protected Component component;
  
  public ComponentBooleanExitPort(Component comp) {
    component = comp;
    comp.registerNormalExitPort(this);
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Port getDual() {
    Subcomponent sub = component.getSubcomponentInParent();
    if (sub == null) return null;
    return sub.getNormalExitPort();
  }  

  @Override
  public IRNode getSyntax() {
    return component.getSyntax();
  }

  @Override
  public Subcomponent getSubcomponent() {
    return component.getSubcomponentInParent();
  }
}
