package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.resources.*;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

import edu.cmu.cs.fluid.dc.Nature;

public class RemoveFluidNatureAction 
extends edu.cmu.cs.fluid.dc.RemoveFluidNatureAction {	
	private boolean changed = false;
	
	@Override
	protected boolean doRun(Object current) {
		boolean rv = super.doRun(current);
		changed = rv || changed;		
		return rv;
	}
	
	@Override
	protected void finishRun() {
		boolean changed = this.changed;
		this.changed = false;
		
		if (!changed) {
			return;
		}
		ClearProjectListener.clearJSureState();
		
		// Handle projects that are still active
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		IProject first = null;
		
		for(IProject p : projects) {
			if (Nature.hasNature(p)) {
				if (first == null) {
					first = p;
				} 
				else {
					LOG.severe("Multiple projects with JSure nature: "+first.getName()+" and "+p.getName());
				}
			}
		}
		if (first != null) {
			Nature.runAnalysis(first);
		}
	}
}
