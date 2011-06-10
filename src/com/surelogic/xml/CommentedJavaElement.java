package com.surelogic.xml;

import java.util.*;

import edu.cmu.cs.fluid.util.ArrayUtil;

public abstract class CommentedJavaElement extends AbstractJavaElement {
	/**
	 * These come before the element at the same level
	 */
	private final List<CommentElement> comments = new ArrayList<CommentElement>(0);
	
	/**
	 * These come after the last enclosing element at the next indent
	 */
	private final List<CommentElement> lastEnclosedComments = new ArrayList<CommentElement>(0);
	
	public final void addComments(Collection<String> c) {		
		for(String s : c) {
			comments.add(new CommentElement(s));
		}
		markAsDirty();
	}
	
	final Iterable<CommentElement> getComments() {
		return comments;
	}
	
	void setLastComments(Collection<String> c) {
		for(String s : c) {
			lastEnclosedComments.add(new CommentElement(s));
		}
		markAsDirty();
	}
	
	Iterable<CommentElement> getLastComments() {
		return lastEnclosedComments;
	}
	
	public boolean hasChildren() {
		return !comments.isEmpty() || !lastEnclosedComments.isEmpty();
	}
	
	public final Object[] getChildren() {
		if (hasChildren()) {
			List<Object> children = new ArrayList<Object>();
			children.addAll(comments);
			collectOtherChildren(children);
			children.addAll(lastEnclosedComments);
			return children.toArray();
		}
		return ArrayUtil.empty;
	}

	protected void collectOtherChildren(List<Object> children) {
		// Nothing to do right now
	}
	
	@Override
	public boolean isDirty() {
		if (super.isDirty()) {
			return true;
		}
		for(CommentElement a : comments) {
			if (a.isDirty()) {
				return true;
			}
		}
		for(CommentElement a : lastEnclosedComments) {
			if (a.isDirty()) {
				return true;
			}
		}
		return false;
	}
	
	public void markAsClean() {
		super.markAsClean();
		for(CommentElement a : comments) {
			a.markAsClean();
		}
		for(CommentElement a : lastEnclosedComments) {
			a.markAsClean();
		}
	}
}
