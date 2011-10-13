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

	/**
	 * Here only to be overridden by Comment/AnnotationElement
	 */
	public boolean isModified() {
		return false;
	}
	
	void markAsDirty() {
		isDirty = true;
	}
	
	void markAsClean() {
		isDirty = false;
	}
	
	/**
	 * Only s the contents at this node
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
	static <T extends IMergeableElement> T merge(T me, T other, MergeType t) {		
		final int myRev = me.getRevision();
		final int otherRev = other.getRevision();
		if (t == MergeType.MERGE) { // to fluid
			checkIf(myRev != otherRev, "Merging different revisions: "+myRev+" vs "+otherRev);	
			// check other for mods
			if (!other.isModified()) {
				// Try to merge everything attached if it's modified
				me.mergeAttached(other);
				return me; 
			}
			checkIf(me.isModified(), "Merging into a modified "+me);			
		
			// Same revision, and the other's modified, so			
			// use the other
			T updated = (T) other.cloneMe();			
			updated.incrRevision();
			// TODO what about attached stuff?
			// TODO what about updating other to match?
			return updated;
		}
		else if (t == MergeType.UPDATE) { // to client
			if (me.isModified()) {
				me.mergeAttached(other);
				return me; // Conflict, so keep my changes
			}
			if (myRev == otherRev) {
				me.mergeAttached(other);
				return me; // Same revision, so there's nothing to change
			}
			checkIf(other.isModified(), "Updating with a modified "+other);
			checkIf(myRev > otherRev, "Updating with "+myRev+" > "+otherRev);
			// Use other, since the other's a newer	revision
			// TODO what about attached stuff?
			return (T) other.cloneMe(); 
		}
		throw new IllegalStateException("Unexpected merge type: "+t);		
	}
	
	private static void checkIf(boolean cond, String issue) {
		if (cond) {
			throw new IllegalStateException(issue);
		}
	}
	
	protected <T extends IMergeableElement> void mergeList(List<T> orig, List<T> other, MergeType type) {
		if (orig.isEmpty() && other.isEmpty()) {
			return;
		}
		if (type != MergeType.MERGE && type != MergeType.UPDATE) { 
			throw new IllegalStateException("Unexpected type: "+type);
		}
		// MERGE  = take explicitly marked mods/deletes from other into orig
		if (type == MergeType.MERGE) {
			if (other.isEmpty()) {
				return; // Nothing to do, since there aren't any marked changes
			}
		}
		// UPDATE = take (implicit) changes from other unless there's a conflict
		else if (type == MergeType.UPDATE) {
			if (orig.isEmpty()) {
				// Take everything in the other, since there's nothing to conflict with
				copyList(other, orig);
				return;
			} 
		}
		final List<T> baseline = handleNonconflictingChanges(orig, other, type);		
		for(int i=0; i<baseline.size(); i++) {
			final T e = baseline.get(i);
			// TODO could be a slow lookup?
			final T o0, o2;
			final int i0 = orig.indexOf(e);
			if (i0 < 0) {				
				if (type == MergeType.MERGE) {					
					// Merging something new 
					e.incrRevision();
				}
				continue;
			} else {
				o0 = orig.get(i0);
			}
			final int i2 = other.indexOf(e);
			if (i2 < 0) {
				continue;
			} else {
				o2 = other.get(i2);
			}
			T syncd = (T) merge(o0, o2, type);
			if (syncd != e) {
				baseline.set(i, syncd);
			}			
		}
		//return baseline;
		orig.clear();
		orig.addAll(baseline);
	}
	
	/**
	 * Computes the baseline of which elements will be in the final list
	 * Deals with inserts/deletes
	 */
	protected <T extends IMergeableElement> List<T> handleNonconflictingChanges(List<T> orig, List<T> other, MergeType type) {		
		// Compute which deltas don't conflict
		final Patch p = DiffUtils.diff(orig, other);			
		final List<Delta> nonConflicts = new ArrayList<Delta>();	
		deltas:
		for(final Delta d : p.getDeltas()) {
			/*
            final Chunk origC = d.getOriginal();
			final Chunk otherC = d.getRevised();
			System.out.println("Delta: "+d);
			*/
			if (d instanceof InsertDelta) {
				nonConflicts.add(d);
			} 
			else if (d instanceof DeleteDelta) { 
				checkIf(type == MergeType.MERGE, "Deletes should be explicitly marked for "+type);
				
				// Check for conflicting changes in the original (client)
				for(Object o : d.getOriginal().getLines()) {
					@SuppressWarnings("unchecked")
					T l = (T) o;
					if (l.isModified()) {
						continue deltas;
					}
				}
				nonConflicts.add(d);
			}
			// Ignore others
		}
		// Apply nonconflicting deltas
		final Patch filtered = new Patch();
		filtered.setDeltas(nonConflicts);		
		try {
			final List<T> temp = new ArrayList<T>();	
			for(Object o : DiffUtils.patch(orig, filtered)) {
				@SuppressWarnings("unchecked")
				T t = (T) o;
				temp.add(t);
			}
			return temp;
		} catch (PatchFailedException e) {
			e.printStackTrace();
			return orig;
		}
	}
	
	private <T extends IMergeableElement> void copyList(List<T> src, List<T> dest) {
		for(T e : src) {
			@SuppressWarnings("unchecked")
			T c = (T) e.cloneMe();
			dest.add(c);
			c.setParent(this);
		}
		markAsDirty();
	}
}
