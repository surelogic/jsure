package com.surelogic.xml;

import edu.cmu.cs.fluid.util.ArrayUtil;

public class CommentElement extends AbstractJavaElement {
	private String comment;

	CommentElement(String c) {
		comment = c;
	}
	
	@Override
	public String getImageKey() {
		// TODO 
		return null;
	}

	@Override
	public String getLabel() {
		return comment;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public Object[] getChildren() {
		return ArrayUtil.empty;
	}
}
