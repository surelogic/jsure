/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentBlankEntryPort.java,v 1.4 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentBlankEntryPort extends BlankInputPort
    implements ComponentPort, EntryPort
{
  protected Component component;
  
  public ComponentBlankEntryPort(Component comp) {
    component = comp;
    comp.registerEntryPort(this);
  }

  public Component getComponent() {
    return component;
  }

  public Port getDual() {
    Subcomponent sub = component.getSubcomponentInParent();
    if (sub == null) return null;
    return sub.getEntryPort();
  }  

  public IRNode getSyntax() {
    return component.getSyntax();
  }

  public Subcomponent getSubcomponent() {
    return component.getSubcomponentInParent();
  }
}
