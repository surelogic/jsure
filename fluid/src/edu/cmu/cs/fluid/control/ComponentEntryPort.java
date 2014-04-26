package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentEntryPort extends SimpleInputPort 
    implements ComponentPort, EntryPort
{
  protected Component component;
  
  public ComponentEntryPort(Component comp) {
    component = comp;
    comp.registerEntryPort(this);
  }
 
  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Port getDual() {
    ISubcomponent sub = component.getSubcomponentInParent();
    if (sub == null) return null;
    return sub.getEntryPort();
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
