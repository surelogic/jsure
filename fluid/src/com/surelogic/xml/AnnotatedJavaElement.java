package com.surelogic.xml;

import java.util.*;

import com.surelogic.annotation.parse.AnnotationVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class AnnotatedJavaElement extends AbstractJavaElement {
	public enum Access {
		PUBLIC, PROTECTED, DEFAULT;
	}

	private final String name;
	private Access access;
	private boolean confirmed; // It exists

	// By uid
	private final Map<String, AnnotationElement> promises = new HashMap<String, AnnotationElement>(
			0);

	// By promise type
	private final Map<String, List<AnnotationElement>> order = new HashMap<String, List<AnnotationElement>>();

	AnnotatedJavaElement(boolean confirmed, String id, Access access) {
		this.confirmed = confirmed;
		name = id;
		this.access = access;
	}

	public abstract Operator getOperator();

	public final boolean isConfirmed() {
		return confirmed;
	}

	public final boolean isPublic() {
		return access == Access.PUBLIC;
	}

	public Access getAccessibility() {
		return access;
	}

	public final String getName() {
		return name;
	}

	public AnnotationElement addPromise(AnnotationElement a) {
		return addPromise(a, true);
	}

	/**
	 * @param replace
	 *            set to true if it can replace an existing annotation
	 * @return the old element/null if replace is true; the new element/null
	 *         otherwise
	 */
	public AnnotationElement addPromise(AnnotationElement a, boolean replace) {
		a.markAsDirty();
		a.setParent(this);
		if (!replace && promises.containsKey(a.getUid())) {
			return null;
		}
		AnnotationElement old = promises.put(a.getUid(), a);
		updateOrder(old, a);
		if (replace) {
			return old;
		}
		return a;
	}

	/**
	 * Does not mark this as modified, since the anno should be new
	 */
	void removePromise(AnnotationElement a) {
		AnnotationElement old = promises.remove(a.getUid());
		if (old == a) {
			List<AnnotationElement> l = order.get(a.getPromise());
			l.remove(a);
		}
	}

	AnnotationElement getPromise(String uid) {
		return promises.get(uid);
	}

	public Collection<AnnotationElement> getPromises() {
		return getPromises(false);
	}

	public Collection<AnnotationElement> getPromises(boolean all) {
		List<AnnotationElement> sorted;
		if (all) {
			sorted = new ArrayList<AnnotationElement>(promises.values());
		} else {
			sorted = new ArrayList<AnnotationElement>();

			// NOTE this means that hasChildren() and other methods may not
			// match what gets returned here
			filterDeleted(sorted, promises.values());
		}
		Collections.sort(sorted, new Comparator<AnnotationElement>() {
			public int compare(AnnotationElement o1, AnnotationElement o2) {
				int rv = o1.getPromise().compareTo(o2.getPromise());
				if (rv == 0) {
					// This break order for those with dependencies
					// rv = o1.getUid().compareTo(o2.getUid());
					rv = getOrdering(o1.getPromise(), o1, o2);
				}
				return rv;
			}
		});
		return sorted;
	}

	public boolean hasChildren() {
		return !promises.isEmpty()/* || super.hasChildren() */;
	}

	@Override
	protected void collectOtherChildren(List<Object> children) {
		children.addAll(getPromises());
	}

	@Override
	public boolean isDirty() {
		if (super.isDirty()) {
			return true;
		}
		for (AnnotationElement a : promises.values()) {
			if (a.isDirty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isModified() {
		if (super.isModified()) {
			return true;
		}
		for (AnnotationElement a : promises.values()) {
			if (a.isModified()) {
				return true;
			}
		}
		return false;
	}

	public void markAsClean() {
		super.markAsClean();
		for (AnnotationElement a : promises.values()) {
			a.markAsClean();
		}
	}

	boolean merge(AnnotatedJavaElement changed, MergeType type) {
		return mergeThis(changed, type);
	}

	/**
	 * Only merges the contents at this node (e.g. the annotations)
	 * 
	 * @return true if changed
	 */
	boolean mergeThis(AnnotatedJavaElement changed, MergeType type) {
		boolean modified = false;
		if (changed.confirmed) {
			this.confirmed = true;
			modified = true;
		}
		if (!changed.isPublic()) {
			this.access = changed.access;
			modified = true;
		}

		if (type == MergeType.JAVA) {
			return modified;
		}

		final Set<String> unhandledAnnos = new HashSet<String>(order.keySet());
		promises.clear();
		for (Map.Entry<String, List<AnnotationElement>> e : changed.order
				.entrySet()) {
			List<AnnotationElement> l = order.get(e.getKey());
			if (l == null) {
				l = new ArrayList<AnnotationElement>();
				order.put(e.getKey(), l);
			}
			modified |= mergeList(this, l, e.getValue(), type);
			if (l.isEmpty()) {
				// delete if empty
				order.remove(e.getKey());
			}

			for (AnnotationElement a : l) {
				promises.put(a.getUid(), a);
			}
			unhandledAnnos.remove(e.getKey());
		}
		for (String anno : unhandledAnnos) {
			for (AnnotationElement a : order.get(anno)) {
				promises.put(a.getUid(), a);
			}
		}

		return modified;
	}

	void copyToClone(AnnotatedJavaElement clone) {
		// super.copyToClone(clone);
		for (Map.Entry<String, List<AnnotationElement>> e : order.entrySet()) {
			// Reconstituted in the same order in the clone
			for (AnnotationElement a : e.getValue()) {
				clone.addPromise(a.cloneMe(clone));
			}
		}
	}

	void copyIfDirty(AnnotatedJavaElement copy) {
		for (final Map.Entry<String, List<AnnotationElement>> e : order
				.entrySet()) {
			// Check if one of the elements is dirty
			for (AnnotationElement a : e.getValue()) {
				if (a.isDirty()) {
					// Reconstituted in the same order in the clone
					for (AnnotationElement a2 : e.getValue()) {
						copy.addPromise(a2.isDirty() ? a2.cloneMe(copy) : a2
								.createRef());
					}
				}
			}

		}
	}

	private void updateOrder(AnnotationElement old, AnnotationElement a) {
		List<AnnotationElement> l = order.get(a.getPromise());
		if (l == null) {
			if (old != null) {
				throw new IllegalStateException("Couldn't find ordering for "
						+ old);
			}
			l = new ArrayList<AnnotationElement>();
			order.put(a.getPromise(), l);
		} else if (old != null) {
			l.remove(old);
		}
		l.add(a);
	}

	protected int getOrdering(String promise, AnnotationElement o1,
			AnnotationElement o2) {
		final List<AnnotationElement> l = order.get(promise);
		if (l == null) {
			throw new IllegalStateException("Couldn't get ordering for @"
					+ promise);
		}
		final int i1 = l.indexOf(o1);
		final int i2 = l.indexOf(o2);
		if (i1 < 0) {
			throw new IllegalStateException("Couldn't compute ordering for "
					+ o1);
		}
		if (i2 < 0) {
			throw new IllegalStateException("Couldn't compute ordering for "
					+ o2);
		}
		return i1 - i2;
	}

	/**
	 * @return The number of annotations added
	 */
	int applyPromises(final AnnotationVisitor v, final IRNode here) {
		if (here == null) {
			return 0;
		}
		int count = 0;
		for (Map.Entry<String, List<AnnotationElement>> e : order.entrySet()) {
			// Reconstituted in the same order in the clone
			for (AnnotationElement a : e.getValue()) {
				count += a.applyPromise(v, here);
			}
		}
		return count;
	}
}
