package com.surelogic.dropsea.irfree;

import com.surelogic.common.SLUtility;

public abstract class AbstractDiffNode implements IDiffNode, Comparable<IDiffNode> {
	Status status = Status.N_A;
	
	void setAsOld() {
		status = Status.OLD;
	}

	void setAsNewer() {
		status = Status.NEW;
	}
	
	public Status getDiffStatus() {
		return status;
	}
	
//	@Override
	public int compareTo(IDiffNode o) {
	      int rv = getDiffStatus().compareTo(o.getDiffStatus());
	      if (rv == 0) {
	        return getText().compareTo(o.getText());
	      }
	      return rv;
	}
	
//	@Override
	public boolean hasChildren() {
		return false;
	}
	
//	@Override
	public Object[] getChildren() {
		return SLUtility.EMPTY_OBJECT_ARRAY;
	}
}
