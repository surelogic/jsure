package com.surelogic.xml;

import java.util.*;

public abstract class AbstractJavaElement {
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
		return promises.put(a.getUid(), a);
	}
	
	AnnotationElement getPromise(String uid) {
		return promises.get(uid);
	}
	
	public Iterable<AnnotationElement> getPromises() {
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
	}

	Iterable<String> getComments() {
		return comments;
	}
	
	void setLastComments(Collection<String> c) {
		lastEnclosedComments.addAll(c);
	}
	
	Iterable<String> getLastComments() {
		return lastEnclosedComments;
	}
}
