package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ComponentBooleanEntryPort extends DoubleInputPort 
	implements ComponentPort, EntryPort
{
	protected Component component;

	public ComponentBooleanEntryPort(Component comp) {
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
