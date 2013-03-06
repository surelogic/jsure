package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentAbruptExitPort extends SimpleOutputPort
    implements ComponentPort, AbruptExitPort
{
  protected Component component;
  
  public ComponentAbruptExitPort(Component comp) {
    component = comp;
    comp.registerAbruptExitPort(this);
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Port getDual() {
    Subcomponent sub = component.getSubcomponentInParent();
    if (sub == null) return null;
    return sub.getAbruptExitPort();
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
