package com.surelogic.dropsea.irfree;

import com.surelogic.NonNull;
import com.surelogic.common.IViewable;

public interface IDiffNode extends IViewable {
	public enum Status { 
		N_A, CHANGED, OLD, NEW
	}
	
	@NonNull
	Status getDiffStatus();
}
