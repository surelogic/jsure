package com.surelogic.xml;

import java.util.*;

import edu.cmu.cs.fluid.util.ArrayUtil;

public abstract class AbstractJavaElement implements IJavaElement {
	private boolean isDirty;
	private final String name;
	/**
	 * These come before the element at the same level
	 */
	private final List<String> comments = new ArrayList<String>(0);
	private final Map<String, AnnotationElement> promises = new HashMap<String, AnnotationElement>(0);
	/**
	 * These come after the last enclosing element at the next indent
	 */
	private final List<String> lastEnclosedComments = new ArrayList<String>(0);
	
	AbstractJavaElement(String id) {
		name = id;
	}
	
	public final String getName() {
		return name;
	}
	
	AnnotationElement addPromise(AnnotationElement a) {
		isDirty = true;
		return promises.put(a.getUid(), a);
	}
	
	AnnotationElement getPromise(String uid) {
		return promises.get(uid);
	}
	
	public Collection<AnnotationElement> getPromises() {
		List<AnnotationElement> sorted = new ArrayList<AnnotationElement>(promises.values());
		Collections.sort(sorted, new Comparator<AnnotationElement>() {
			public int compare(AnnotationElement o1, AnnotationElement o2) {
				int rv = o1.getPromise().compareTo(o2.getPromise()); 
				if (rv == 0) {
					rv = o1.getUid().compareTo(o2.getUid());
				}
				return rv;
			}
		});
		return sorted;
	}
	
	public final void addComments(Collection<String> c) {
		comments.addAll(c);
		isDirty = true;
	}

	Iterable<String> getComments() {
		return comments;
	}
	
	void setLastComments(Collection<String> c) {
		lastEnclosedComments.addAll(c);
		isDirty = true;
	}
	
	Iterable<String> getLastComments() {
		return lastEnclosedComments;
	}
	
	public boolean hasChildren() {
		return !comments.isEmpty() || !promises.isEmpty() || !lastEnclosedComments.isEmpty();
	}
	
	public final Object[] getChildren() {
		if (hasChildren()) {
			List<Object> children = new ArrayList<Object>();
			children.addAll(comments);
			children.addAll(getPromises());
			collectOtherChildren(children);
			children.addAll(lastEnclosedComments);
			return children.toArray();
		}
		return ArrayUtil.empty;
	}

	protected void collectOtherChildren(List<Object> children) {
		// Nothing to do right now
	}
	
	public boolean isDirty() {
		if (isDirty) {
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
		isDirty = false;
		for(AnnotationElement a : promises.values()) {
			a.markAsClean();
		}
	}
}
