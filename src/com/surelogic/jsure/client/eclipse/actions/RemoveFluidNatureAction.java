package com.surelogic.jsure.client.eclipse.actions;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

public class RemoveFluidNatureAction extends
		edu.cmu.cs.fluid.dc.RemoveFluidNatureAction {
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
		ClearProjectListener.postNatureChangeUtility();
	}
}
