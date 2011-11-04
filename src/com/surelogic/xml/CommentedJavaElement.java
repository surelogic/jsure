package com.surelogic.xml;

import java.util.*;

import edu.cmu.cs.fluid.util.ArrayUtil;

@Deprecated
public abstract class CommentedJavaElement extends AbstractJavaElement {
	/**
	 * These come before the element at the same level
	 */
	private final List<CommentElement> comments = new ArrayList<CommentElement>(0);
	
	/**
	 * These come after the last enclosing element at the next indent
	 */
	private final List<CommentElement> lastEnclosedComments = new ArrayList<CommentElement>(0);
	
	public boolean addCommentBefore(CommentElement newC, CommentElement targetC) {
		int index = comments.indexOf(targetC);
		if (index < 0) {
			return false;
		}
		comments.add(index, newC);
		newC.setParent(this);
		markAsDirty();
		return true;
	}
	
	public boolean addCommentAfter(CommentElement newC, CommentElement targetC) {
		final int index = comments.indexOf(targetC);
		if (index < 0) {
			return false;
		}
		final int size = comments.size();
		final int newIndex = index+1;
		if (newIndex == size) {
			// It's the last element, so just append
			comments.add(newC);
		} else {
			comments.add(newIndex, newC);
		}
		newC.setParent(this);
		markAsDirty();
		return true;
	}
	
    public void addComment(CommentElement c) {
        comments.add(c);
        c.setParent(this);
        markAsDirty();
    }
	
	public final void addComments(Collection<String> c) {		
		for(String s : c) {
			CommentElement e = CommentElement.make(s);
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
			CommentElement e = CommentElement.make(s);
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
	
	@Override
	public final Object[] getChildren() {
		if (hasChildren()) {
			List<Object> children = new ArrayList<Object>();
			filterDeleted(children, comments);
			collectOtherChildren(children);
			filterDeleted(children, lastEnclosedComments);
			return children.toArray();
		}
		return ArrayUtil.empty;
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
	
	/**
	 * Only merges the contents at this node
	 * (i.e. the comments)
	 * 
	 * @return true if changed
	 */
	boolean mergeThis(CommentedJavaElement changed, MergeType type) {
		boolean modified = super.mergeThis(changed, type);
		modified |= mergeList(comments, changed.comments, type);
		modified |= mergeList(lastEnclosedComments, changed.lastEnclosedComments, type);
		return modified;
	}
	
	/**
	 * Finishes the deep copy of comments
	 */
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
