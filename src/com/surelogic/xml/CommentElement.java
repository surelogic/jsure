package com.surelogic.xml;

import edu.cmu.cs.fluid.util.ArrayUtil;

public class CommentElement extends AbstractJavaElement {
	private String comment;

	CommentElement(String c) {
		comment = c;
	}
	
	public String getImageKey() {
		// TODO 
		return null;
	}

	public String getLabel() {
		return comment;
	}

	public boolean hasChildren() {
		return false;
	}

	public Object[] getChildren() {
		return ArrayUtil.empty;
	}
}
