package com.surelogic.dropsea.irfree;

import com.surelogic.common.IViewable;

public interface IDiffNode extends IViewable {
	public enum Status { 
		OLD, NEW
	}
	
	Status getDiffStatus();
}
