package com.surelogic.jsure.client.eclipse.actions;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

public class AddFluidNatureAction extends
		edu.cmu.cs.fluid.dc.AddFluidNatureAction {

	@Override
	protected void finishRun() {
		ClearProjectListener.postNatureChangeUtility();
	}
}
