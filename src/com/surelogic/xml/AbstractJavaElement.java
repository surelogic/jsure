package com.surelogic.xml;

import java.util.*;

public abstract class AbstractJavaElement {
	private final String name;
	private final Map<String, AnnotationElement> promises = new HashMap<String, AnnotationElement>();
	
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
}
