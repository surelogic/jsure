package com.surelogic.xml;

import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.IErrorListener;

import edu.cmu.cs.fluid.util.ArrayUtil;

public class CommentElement extends AbstractJavaElement {
	private String comment;

	public CommentElement(String c) {
		comment = c;
	}
	
	@Override
	public int hashCode() {
		return comment.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof CommentElement) {
			CommentElement other = (CommentElement) o;
			return comment.equals(other.comment);
		}
		return false;
	}
	
	public String getImageKey() {
		return CommonImages.IMG_COMMENT;
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

	@Override
	public boolean canModify() {
		return true;
	}

	@Override
	public void modify(String value, IErrorListener l) {
		comment = value;
		markAsDirty();
	}
	
	CommentElement cloneMe() {
		return new CommentElement(comment);
	}
}
