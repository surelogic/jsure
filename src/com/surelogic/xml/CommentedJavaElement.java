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
	

    public void addComment(CommentElement c) {
        comments.add(c);
        c.setParent(this);
        markAsDirty();
    }
	
	public final void addComments(Collection<String> c) {		
		for(String s : c) {
			CommentElement e = new CommentElement(s);
			comments.add(e);
			e.setParent(this);
		}
		markAsDirty();
	}
	
	final Iterable<CommentElement> getComments() {
		return comments;
	}
	
	private void addLastEnclosedComment(CommentElement c) {
		lastEnclosedComments.add(c);
        c.setParent(this);
        markAsDirty();
	}
	
	void setLastComments(Collection<String> c) {
		for(String s : c) {
			CommentElement e = new CommentElement(s);
			lastEnclosedComments.add(e);
			e.setParent(this);
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
	
	void mergeThis(CommentedJavaElement changed) {
		super.mergeThis(changed);
		// TODO
		comments.addAll(changed.comments);
		lastEnclosedComments.addAll(changed.lastEnclosedComments);
	}
	
	void copyToClone(CommentedJavaElement clone) {
		super.copyToClone(clone);
		for(CommentElement e : comments) {
			clone.addComment(e.cloneMe());
		}
		for(CommentElement e : lastEnclosedComments) {
			clone.addLastEnclosedComment(e.cloneMe());
		}
	}
}
