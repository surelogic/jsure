package com.surelogic.xml;

import java.util.*;

//import difflib.*;

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
	
	private void copyList(List<CommentElement> src, List<CommentElement> dest) {
		for(CommentElement e : src) {
			CommentElement c = e.cloneMe();
			dest.add(c);
			c.setParent(this);
		}
		markAsDirty();
	}
	
	private void mergeList(List<CommentElement> orig, List<CommentElement> other, MergeType type) {
		if (type == MergeType.USE_OTHER) {
			orig.clear();
			copyList(other, orig);
			return;
		}
		if (other.isEmpty()) {
			return; // Nothing to do
		}
		if (orig.isEmpty()) {
			copyList(other, orig);
			return;
		} 
		// Keep the original
		
		/*
		// Something to merge, so first find what's shared
		final Set<String> shared = new HashSet<String>();
		for(CommentElement e : orig) {
			shared.add(e.getLabel());
		}
		int i=0;
		boolean same = true;
		for(CommentElement e : other) {
			if (!shared.contains(e.getLabel())) {
				shared.remove(e.getLabel());
			}
			if (!e.equals(orig.get(i))) {
				same = false;
			}
			i++;
		}
		if (same) {
			return; // Both are the same, so there's nothing to do
		}
		if (shared.isEmpty()) {
			if (type == MergeType.PREFER_OTHER) {
				// Replace
				orig.clear();
				copyList(other, orig);
				return;
			} else {
				// Keep the original comments
				return;
			}
		} else {
			// TODO is this just a complicated way of saying "keep or overwrite"?
			final List<CommentElement> temp = new ArrayList<CommentElement>();			
			final Patch p = DiffUtils.diff(orig, other);			
			int lastPosition = 0;
			for(final Delta d : p.getDeltas()) {
				final Chunk origC = d.getOriginal();
				// Copy everything between where we left off and where this chunk starts
				for(i=lastPosition; i<origC.getPosition(); i++) {
					CommentElement e = orig.get(i).cloneMe();
					e.setParent(this);
					temp.add(e);
				}
				final Chunk src = type == MergeType.PREFER_OTHER ? d.getRevised() : d.getOriginal();
				for(Object o : src.getLines()) {
					CommentElement e = ((CommentElement) o).cloneMe();
					e.setParent(this);
					temp.add(e);
				}
				lastPosition = origC.getPosition() + origC.getSize();
			}
			for(i=lastPosition; i<orig.size(); i++) {
				CommentElement e = orig.get(i).cloneMe();
				e.setParent(this);
				temp.add(e);
			}
		}
		*/
	}
	
	void mergeThis(CommentedJavaElement changed, MergeType type) {
		super.mergeThis(changed, type);
		mergeList(comments, changed.comments, type);
		mergeList(lastEnclosedComments, changed.lastEnclosedComments, type);
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
