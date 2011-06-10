package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class SubcomponentBooleanEntryPort extends DoubleOutputPort 
	implements SubcomponentPort, EntryPort
{
	protected Subcomponent subcomponent;

	public SubcomponentBooleanEntryPort(Subcomponent subcomp) {
		subcomponent = subcomp;
		subcomp.registerEntryPort(this);
	}

	public Subcomponent getSubcomponent() {
		return subcomponent;
	}

	public Port getDual() {
		Component childComp = subcomponent.getComponentInChild();
		if (childComp == null) return null;
		return childComp.getEntryPort();
	}  

	public IRNode getSyntax() {
		return subcomponent.getSyntax();
	}

	public Component getComponent() {
		return subcomponent.getComponentInChild();
	}

}
