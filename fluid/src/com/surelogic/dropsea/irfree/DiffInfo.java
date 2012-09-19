package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.ISupportingInformation;

/**
 * Encapsulating a SupportingInfo
 * 
 * @author Edwin
 */
public class DiffInfo extends AbstractDiffNode {
	final ISupportingInformation info;
	
	DiffInfo(ISupportingInformation d) {
		if (d == null) {
			throw new IllegalArgumentException();
		}
		info = d;
	}

	@Override
	public String getText() {
		return info.getMessage();
	}
}
