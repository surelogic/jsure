package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentBlankAbruptExitPort extends BlankOutputPort
    implements ComponentPort, AbruptExitPort
{
  protected Component component;
  
  public ComponentBlankAbruptExitPort(Component comp) {
    component = comp;
    comp.registerAbruptExitPort(this);
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Port getDual() {
    ISubcomponent sub = component.getSubcomponentInParent();
    if (sub == null) return null;
    return sub.getAbruptExitPort();
  }  

  @Override
  public IRNode getSyntax() {
    return component.getSyntax();
  }

  @Override
  public ISubcomponent getSubcomponent() {
    return component.getSubcomponentInParent();
  }
}
