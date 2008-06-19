package com.surelogic.jsure.client.eclipse.actions;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

public class FocusFluidNatureAction 
extends edu.cmu.cs.fluid.dc.FocusFluidNatureAction {	
	@Override
	protected void cleanup() {
		ClearProjectListener.clearJSureState();
	}
}
