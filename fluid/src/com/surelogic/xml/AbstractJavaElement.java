package com.surelogic.xml;

import java.util.*;

import com.surelogic.common.logging.IErrorListener;

import difflib.*;
import edu.cmu.cs.fluid.util.ArrayUtil;

abstract class AbstractJavaElement implements IJavaElement {
	private IJavaElement parent;
	private boolean isDirty;
	
	public final IJavaElement getParent() {
		return parent;
	}

	public final void setParent(IJavaElement p) {
		if (p == null) {
			throw new IllegalStateException("Trying to clear the parent");
		}
		if (parent == p) {
			return; // Already the same
		}
		if (parent != null) {
			if (this instanceof AnnotationElement) {
				if (parent.getClass() != p.getClass()) {
					throw new IllegalStateException("Resetting the parent of an annotation to a different type: "+
							parent.getClass().getSimpleName()+" -> "+p.getClass().getSimpleName());
				}
			} else {
				throw new IllegalStateException("Already has a parent");
			}
		}
		parent = p;
	}
	
	public boolean isBad() {
		return false;
	}
	
	public boolean canModify() {
		return false;
	}
	
	public boolean modify(String value, IErrorListener l) {
		throw new UnsupportedOperationException();
	}
	
	public boolean isDirty() {
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
	 * Only syncs the contents at this node
	 * 
	 * @return true if changed
	 */
	boolean mergeThis(AbstractJavaElement changed, MergeType type) {
		// Nothing to do yet
		return false;
	}
	
	/**
	 * Finishes the deep copy
	 */
	void copyToClone(AbstractJavaElement clone) {
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
		if (me.isReference()) {
			return (T) other.cloneMe();
		}
		if (other.isReference()) {
			return me;
		}
		
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
			// update and use the other
			other.incrRevision();
			T updated = (T) other.cloneMe();			
			//updated.incrRevision();
			// TODO what about attached stuff?
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
	
	/**
	 * @return true if changed
	 */
	protected <T extends IMergeableElement> 
	boolean mergeList(IJavaElement parent, List<T> orig, List<T> other, MergeType type) {
		if (orig.isEmpty() && other.isEmpty()) {
			return false;
		}
		if (type != MergeType.MERGE && type != MergeType.UPDATE) { 
			throw new IllegalStateException("Unexpected type: "+type);
		}
		// MERGE  = take explicitly marked mods/deletes from other into orig
		if (type == MergeType.MERGE) {
			if (other.isEmpty()) {
				return false; // Nothing to do, since there aren't any marked changes
			}
		}
		// UPDATE = take (implicit) changes from other unless there's a conflict
		else if (type == MergeType.UPDATE) {
			if (orig.isEmpty()) {
				// Take everything in the other, since there's nothing to conflict with
				copyList(other, orig);
				return false;
			} 
		}
		final List<T> baseline = handleNonconflictingChanges(orig, other, type);		
		boolean changed = false;
		for(int i=0; i<baseline.size(); i++) {
			final T e = baseline.get(i);
			// TODO could be a slow lookup?
			final T o0, o2;
			final int i0 = orig.indexOf(e);
			if (i0 < 0) {				
				if (type == MergeType.MERGE) {					
					// Merging something new 
					e.incrRevision();
					changed = true;
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
				changed = true;
			}			
		}
		//return baseline;
		orig.clear();
		orig.addAll(baseline);
		// TODO does this include deletes?
		for(T e : orig) {
			e.setParent(parent);
		}
		return changed;
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
			else {
				System.out.println("Ignoring "+d);
			}
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
	
	public Object[] getChildren() {
		if (hasChildren()) {
			List<Object> children = new ArrayList<Object>();
			collectOtherChildren(children);
			return children.toArray();
		}
		return ArrayUtil.empty;
	}
	
	protected void collectOtherChildren(List<Object> children) {
		// Nothing to do right now
	}
	
	/**
	 * NOTE: using this means that hasChildren() and other methods may not match what gets returned here
	 */
	protected static <T extends IMergeableElement> 
	void filterDeleted(Collection<? super T> result, Collection<T> orig) {
		for(T e : orig) {
			if (e.isToBeDeleted()) {
				continue;
			}
			result.add(e);
		}
	}
}
