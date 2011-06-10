package com.surelogic.xml;

import java.util.*;

public abstract class AnnotatedJavaElement extends CommentedJavaElement {
	private final String name;
	private final Map<String, AnnotationElement> promises = new HashMap<String, AnnotationElement>(0);
	
	AnnotatedJavaElement(String id) {
		name = id;
	}
	
	public final String getName() {
		return name;
	}
	
	AnnotationElement addPromise(AnnotationElement a) {
		markAsDirty();
		a.setParent(this);
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
}
