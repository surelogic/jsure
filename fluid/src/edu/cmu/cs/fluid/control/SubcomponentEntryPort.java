package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class SubcomponentEntryPort extends SimpleOutputPort
    implements SubcomponentPort, EntryPort
{
  protected Subcomponent subcomponent;
  
  public SubcomponentEntryPort(Subcomponent subcomp) {
    subcomponent = subcomp;
    subcomp.registerEntryPort(this);
  }

  @Override
  public Subcomponent getSubcomponent() {
    return subcomponent;
  }

  @Override
  public Port getDual() {
    Component childComp = subcomponent.getComponentInChild();
    if (childComp == null) return null;
    return childComp.getEntryPort();
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

