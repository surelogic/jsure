package com.surelogic.xml;

import java.util.*;

import com.surelogic.common.logging.IErrorListener;

import difflib.*;

abstract class AbstractJavaElement implements IJavaElement {
	private IJavaElement parent;
	private boolean isDirty;
	
	public final IJavaElement getParent() {
		return parent;
	}

	public final void setParent(IJavaElement p) {
		if (parent != null) {
			throw new IllegalStateException("Already has a parent");
		}
		parent = p;
	}
	
	public boolean canModify() {
		return false;
	}
	
	public void modify(String value, IErrorListener l) {
		throw new UnsupportedOperationException();
	}
	
	boolean isDirty() {
		return isDirty;
	}

	void markAsDirty() {
		isDirty = true;
	}
	
	void markAsClean() {
		isDirty = false;
	}
	
	/**
	 * Only merges the contents at this node
	 */
	void mergeThis(AbstractJavaElement changed, MergeType type) {
		// Nothing to do yet
	}
	
	/**
	 * Finishes the deep copy
	 */
	void copyToClone(CommentedJavaElement clone) {
		// Nothing to do yet
	}
	
	/**
	 * Do a deep copy
	 */
	abstract AbstractJavaElement cloneMe();
	
	/**
	 * If one is modified or has a higher revision, take everything on that one
	 * Otherwise, we need to merge whatever's attached to both
	 * (e.g. comments on the annotation)
	 * 
	 * @return Either me, or a new element
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends IMergeableElement> T merge(T me, T other) {		
		if (me.isModified()) {
		 	return me; // Keep what we've edited
		}
		final int thisRev = me.getRevision();
		final int otherRev = other.getRevision();
		if (thisRev == otherRev) {
			if (!other.isModified()) {
				// Merge everything attached if it's modified
				me.mergeAttached(other);
				return me; 
			}
			// Same revision, and the other's modified, so			
			// use the other
			T updated = (T) other.cloneMe();
			updated.incrRevision();
			return updated;
		} else if (otherRev > thisRev) {
			// Use other, since the other's a newer	revision
			return (T) other.cloneMe(); 
		} else {
			// Ignore the other, since it's an older rev
			return me;
		}	
	}
	
	protected <T extends AbstractJavaElement> void mergeList(List<T> orig, List<T> other, MergeType type) {
		if (type != MergeType.MERGE) {
			throw new IllegalStateException("Unexpected type: "+type);
		}
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
			*/		
		//diff(orig, other);
	}
	
	protected <T extends AbstractJavaElement> void diff(List<T> orig, List<T> other) {
		final List<T> temp = new ArrayList<T>();			
		final Patch p = DiffUtils.diff(orig, other);			
		// Describes the changes -- anything else is the same
		
		int lastPosition = 0;
		for(final Delta d : p.getDeltas()) {
			final Chunk origC = d.getOriginal();
			/*
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
			*/
		}
		/*
		for(i=lastPosition; i<orig.size(); i++) {
			CommentElement e = orig.get(i).cloneMe();
			e.setParent(this);
			temp.add(e);
		}
		*/
	}
	
	private <T extends AbstractJavaElement> void copyList(List<T> src, List<T> dest) {
		for(T e : src) {
			@SuppressWarnings("unchecked")
			T c = (T) e.cloneMe();
			dest.add(c);
			c.setParent(this);
		}
		markAsDirty();
	}
}
