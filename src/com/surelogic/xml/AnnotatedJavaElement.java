package com.surelogic.xml;

import java.util.*;

import edu.cmu.cs.fluid.tree.Operator;

public abstract class AnnotatedJavaElement extends CommentedJavaElement {
	private final String name;
	
	// By uid
	private final Map<String, AnnotationElement> promises = new HashMap<String, AnnotationElement>(0);

	// By promise type
	private final Map<String,List<AnnotationElement>> order = new HashMap<String,List<AnnotationElement>>();
	
	AnnotatedJavaElement(String id) {
		name = id;
	}
	
	public abstract Operator getOperator();
	
	public final String getName() {
		return name;
	}
	
	public AnnotationElement addPromise(AnnotationElement a) {
		markAsDirty();
		a.setParent(this);
		AnnotationElement old = promises.put(a.getUid(), a);
		updateOrder(old, a);
		return old;
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
		
			// NOTE this means that hasChildren() and other methods may not match what gets returned here
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

	@Override
	public boolean hasChildren() {
		return !promises.isEmpty() || super.hasChildren();
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
		for(AnnotationElement a : promises.values()) {
			if (a.isDirty()) {
				return true;
			}
		}
		return false;
	}
	
	public void markAsClean() {
		super.markAsClean();
		for(AnnotationElement a : promises.values()) {
			a.markAsClean();
		}
	}
	
	boolean merge(AnnotatedJavaElement changed, MergeType type) {
		return mergeThis(changed, type);
	}
	
	/**
	 * Only merges the contents at this node
	 * (e.g. the annotations)
	 * 
	 * @return true if changed
	 */
	boolean mergeThis(AnnotatedJavaElement changed, MergeType type) {
		if (type == MergeType.JAVA) {
			return false;
		}
		boolean modified = super.mergeThis(changed, type);		
		
		// TODO this nukes all of the original annos
		promises.clear();
		for(Map.Entry<String, List<AnnotationElement>> e : changed.order.entrySet()) {
			List<AnnotationElement> l = order.get(e.getKey());
			if (l == null) {
				l = new ArrayList<AnnotationElement>();
				order.put(e.getKey(), l);
			}
			modified |= mergeList(l, e.getValue(), type);
			if (l.isEmpty()) {
				// delete if empty
				order.remove(e.getKey());
			}
			
			for(AnnotationElement a : l) {
				promises.put(a.getUid(), a);
			}
		}		
		/*
		for(Map.Entry<String,AnnotationElement> e : changed.promises.entrySet()) {
			// TODO how to merge the ordering?
			// right now, this puts any new elements at the end
			final AnnotationElement a = promises.get(e.getKey());
			if (a != null) {
				a.merge(e.getValue(), type);				
			} else {				
				addPromise(e.getValue().cloneMe());
			}
		}
		*/
		return modified;
	}
	
	void copyToClone(AnnotatedJavaElement clone) {
		super.copyToClone(clone);
		for(AnnotationElement a : promises.values()) {
			clone.addPromise(a.cloneMe());
		}
	}
	
	private void updateOrder(AnnotationElement old, AnnotationElement a) {
		List<AnnotationElement> l = order.get(a.getPromise());		
		if (l == null) {
			if (old != null) {
				throw new IllegalStateException("Couldn't find ordering for "+old);
			}
			l = new ArrayList<AnnotationElement>();
			order.put(a.getPromise(), l);
		} else if (old != null) {
			l.remove(old);
		}
		l.add(a);
	}
	
	protected int getOrdering(String promise, AnnotationElement o1,	AnnotationElement o2) {
		final List<AnnotationElement> l = order.get(promise);
		if (l == null) {
			throw new IllegalStateException("Couldn't get ordering for @"+promise);
		}
		final int i1 = l.indexOf(o1);
		final int i2 = l.indexOf(o2);
		if (i1 < 0) {
			throw new IllegalStateException("Couldn't compute ordering for "+o1);
		}
		if (i2 < 0) {
			throw new IllegalStateException("Couldn't compute ordering for "+o2);
		}
		return i1 - i2;
	}
}
