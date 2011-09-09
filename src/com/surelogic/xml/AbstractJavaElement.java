package com.surelogic.xml;

import com.surelogic.common.logging.IErrorListener;

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
	
	protected static void merge(IMergeableElement me, IMergeableElement other) {		
		if (me.isModified()) {
		 	return; // Keep what we've edited
		}
		final int thisRev = me.getRevision();
		final int otherRev = other.getRevision();
		MergeType type;
		if (thisRev == otherRev) {
			if (!other.isModified()) {
				return; // These should be the same
			}
			// Same revision, and the other's modified, so			
			// Overwrite things in common
			/*
			me.merge(other, MergeType.USE_OTHER);
			incrRevision();			
			*/
		} else if (otherRev > thisRev) {
			// Overwrite this completely, since the other's newer
			/*
			attributes.clear();
			contents = other.contents;
			attributes.putAll(other.attributes);
			*/
			type = MergeType.USE_OTHER;
		} else {
			// Ignore the other, since it's an older rev
			return;
		}	
	}
}
