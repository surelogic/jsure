package com.surelogic.dropsea.irfree;

import com.surelogic.NonNull;
import com.surelogic.common.IViewable;
import com.surelogic.dropsea.IDrop;

public interface IDiffNode extends IViewable {
	public enum Status { 
		N_A, CHANGED, OLD, NEW
	}
	
	@NonNull
	Status getDiffStatus();
	
	IDrop getDrop();
}
