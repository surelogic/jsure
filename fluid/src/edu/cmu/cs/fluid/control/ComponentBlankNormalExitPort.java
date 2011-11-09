/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentBlankNormalExitPort.java,v 1.4 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentBlankNormalExitPort extends BlankOutputPort
    implements ComponentPort, NormalExitPort
{
  protected Component component;
  
  public ComponentBlankNormalExitPort(Component comp) {
    component = comp;
    comp.registerNormalExitPort(this);
  }

  public Component getComponent() {
    return component;
  }

  public Port getDual() {
    Subcomponent sub = component.getSubcomponentInParent();
    if (sub == null) return null;
    return sub.getNormalExitPort();
  }  

  public IRNode getSyntax() {
    return component.getSyntax();
  }

  public Subcomponent getSubcomponent() {
    return component.getSubcomponentInParent();
  }
}
