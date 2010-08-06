package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.resources.IProject;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

public class DisableVerificationContextAction extends
		edu.cmu.cs.fluid.dc.RemoveFluidNatureAction {
	private boolean changed = false;

	@Override
	protected boolean doRun(IProject current) {
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
		ClearProjectListener.postNatureChangeUtility();
	}
}
