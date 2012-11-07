package com.surelogic.xml;

import java.util.*;

import com.surelogic.common.SLUtility;

import difflib.DeleteDelta;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import difflib.Patch;
import difflib.PatchFailedException;

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
					throw new IllegalStateException(
							"Resetting the parent of an annotation to a different type: "
									+ parent.getClass().getSimpleName()
									+ " -> " + p.getClass().getSimpleName());
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

	public boolean isEditable() {
		return false;
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

	public PackageElement getRootParent() {
		final IJavaElement e = getParent();
		if (e instanceof PackageElement) {
			// we are at the root
			return (PackageElement) e;
		} else {
			if (e != null)
				return e.getRootParent();
			else
				throw new IllegalStateException(
						"No package at the root of XML tree: " + this);
		}
	}

	public int getReleaseVersion() {
		final PackageElement e = getRootParent();
		return e.getReleaseVersion();
	}

	public void setReleaseVersion(int value) {
		final PackageElement e = getRootParent();
		e.setReleaseVersion(value);
	}

	public final void incrementReleaseVersion() {
		setReleaseVersion(getReleaseVersion() + 1);
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
	abstract AbstractJavaElement cloneMe(IJavaElement parent);

	/**
	 * If one is modified or has a higher revision, take everything on that one
	 * Otherwise, we need to merge whatever's attached to both (e.g. comments on
	 * the annotation)
	 * 
	 * @return Either me, or a new element
	 */
	@SuppressWarnings("unchecked")
	static <T extends IMergeableElement> T merge(IJavaElement parent, T me,
			T other, MergeType t) {
		if (me.isReference()) {
			return (T) other.cloneMe(parent);
		}
		if (other.isReference()) {
			return me;
		}

		if (t == MergeType.LOCAL_TO_JSURE) { // to fluid

			// check other for mods
			if (!other.isModified()) {
				// Try to merge everything attached if it's modified
				me.mergeAttached(other);
				return me;
			}
			checkAndFailIf(me.isModified(), "Merging into a modified " + me);

			T updated = (T) other.cloneMe(parent);
			// TODO what about attached stuff?
			return updated;
		} else if (t == MergeType.JSURE_TO_LOCAL) { // to client
			if (me.isModified() && !me.isEquivalent(other)) {
				// TODO check if everything's the same?
				me.mergeAttached(other);
				return me; // Conflict, so keep my changes
			}
			// Use other, since the other's a newer revision
			// TODO what about attached stuff?
			return (T) other.cloneMe(parent);
		}
		throw new IllegalStateException("Unexpected merge type: " + t);
	}

	private static void checkAndFailIf(boolean cond, String issue) {
		if (cond) {
			throw new IllegalStateException(issue);
		}
	}

	/**
	 * @return true if changed
	 */
	protected <T extends IMergeableElement> boolean mergeList(
			IJavaElement parent, List<T> orig, List<T> other, MergeType type) {
		if (orig.isEmpty() && other.isEmpty()) {
			return false;
		}
		if (type != MergeType.LOCAL_TO_JSURE
				&& type != MergeType.JSURE_TO_LOCAL) {
			throw new IllegalStateException("Unexpected type: " + type);
		}
		// MERGE = take explicitly marked mods/deletes from other into orig
		if (type == MergeType.LOCAL_TO_JSURE) {
			if (other.isEmpty()) {
				/*
				 * Nothing to do, since there aren't any marked changes.
				 */
				return false;
			}
		}
		// UPDATE = take (implicit) changes from other unless there's a conflict
		else if (type == MergeType.JSURE_TO_LOCAL) {
			if (orig.isEmpty()) {
				/*
				 * Take everything in the other, since there's nothing to
				 * conflict with.
				 */
				copyList(parent, other, orig);
				return false;
			}
		}
		final List<T> baseline = handleNonconflictingChanges(orig, other, type);
		// A set of elements that are marked as being deleted
		final Set<T> deleted = type == MergeType.JSURE_TO_LOCAL ? new HashSet<T>()
				: Collections.<T> emptySet();
		boolean changed = false;
		for (int i = 0; i < baseline.size(); i++) {
			final T e = baseline.get(i);
			// TODO could be a slow lookup?
			final T o0, o2;
			final int i0 = orig.indexOf(e);
			if (i0 < 0) {
				if (type == MergeType.LOCAL_TO_JSURE) {
					changed = true;
				}
				continue;
			} else {
				o0 = orig.get(i0);
			}
			final int i2 = other.indexOf(e);
			if (i2 < 0) {
				if (type == MergeType.JSURE_TO_LOCAL && o0.isToBeDeleted()) {
					deleted.add(e);
					changed = true;
				}
				continue;
			} else {
				o2 = other.get(i2);
			}
			T syncd = merge(parent, o0, o2, type);
			if (syncd != e) {
				baseline.set(i, syncd);
				changed = true;
			}
		}
		// return baseline;
		orig.clear();
		if (deleted.isEmpty()) {
			orig.addAll(baseline);
		} else {
			// Keep everything that's not deleted
			for (T e : baseline) {
				if (deleted.contains(e)) {
					continue;
				}
				orig.add(e);
			}
		}
		for (T e : orig) {
			e.setParent(parent);
		}
		return changed;
	}

	/**
	 * Computes the baseline of which elements will be in the final list Deals
	 * with inserts/deletes
	 */
	protected <T extends IMergeableElement> List<T> handleNonconflictingChanges(
			List<T> orig, List<T> other, MergeType type) {
		// Compute which deltas don't conflict
		final Patch p = DiffUtils.diff(orig, other);
		final List<Delta> nonConflicts = new ArrayList<Delta>();
		deltas: for (final Delta d : p.getDeltas()) {
			/*
			 * final Chunk origC = d.getOriginal(); final Chunk otherC =
			 * d.getRevised(); System.out.println("Delta: "+d);
			 */
			if (d instanceof InsertDelta) {
				nonConflicts.add(d);
			} else if (d instanceof DeleteDelta) {
				checkAndFailIf(type == MergeType.LOCAL_TO_JSURE,
						"Deletes should be explicitly marked for " + type);

				// Check for conflicting changes in the original (client)
				for (Object o : d.getOriginal().getLines()) {
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
				System.out.println("Ignoring " + d);
			}
		}
		// Apply nonconflicting deltas
		final Patch filtered = new Patch();
		filtered.setDeltas(nonConflicts);
		try {
			final List<T> temp = new ArrayList<T>();
			for (Object o : DiffUtils.patch(orig, filtered)) {
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

	private <T extends IMergeableElement> void copyList(IJavaElement parent,
			List<T> src, List<T> dest) {
		for (T e : src) {
			@SuppressWarnings("unchecked")
			T c = (T) e.cloneMe(parent);
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
		return SLUtility.EMPTY_OBJECT_ARRAY;
	}

	protected void collectOtherChildren(List<Object> children) {
		// Nothing to do right now
	}

	/**
	 * NOTE: using this means that hasChildren() and other methods may not match
	 * what gets returned here
	 */
	protected static <T extends IMergeableElement> void filterDeleted(
			Collection<? super T> result, Collection<T> orig) {
		for (T e : orig) {
			if (e.isToBeDeleted()) {
				continue;
			}
			result.add(e);
		}
	}
	
	public boolean isStatic() {
	  return false;
	}

	@Override
	public String toString() {
		return super.toString() + "[" + getLabel() + "]";
	}
}
