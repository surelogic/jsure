package com.surelogic.dropsea.irfree;

import com.surelogic.common.i18n.AnalysisResultMessage;

public abstract class AbstractDiffNode implements IDiffNode, Comparable<IDiffNode> {
	Status status = null;
	
	void setAsOld() {
		status = Status.OLD;
	}

	void setAsNewer() {
		status = Status.NEW;
	}
	
	public Status getDiffStatus() {
		return status;
	}
	
	public boolean isOld() {
		return status == Status.OLD;
	}
	
	public boolean isNewer() {
		return status == Status.NEW;
	}
	
//	@Override
	public int compareTo(IDiffNode o) {
	      int rv = getText().compareTo(o.getText());
	      if (rv == 0) {
	        return getDiffStatus().compareTo(o.getDiffStatus());
	      }
	      return rv;
	}
	
//	@Override
	public boolean hasChildren() {
		return false;
	}
	
//	@Override
	public Object[] getChildren() {
		return AnalysisResultMessage.noArgs;
	}
}
