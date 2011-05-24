package com.surelogic.xml;

import java.util.*;

public class AnnotationElement {
	private final String uid;
	private final String promise;
	private final String contents;
	private final Map<String,String> attributes = new HashMap<String,String>(0);
	
	AnnotationElement(String id, String name, String text, Map<String,String> a) {
		uid = id;
		promise = name;
		contents = text;
		attributes.putAll(a);
	}
	
	final String getUid() {
		return uid;
	}
	
	final String getPromise() {
		return promise;		
	}
	
	final String getContents() {
		return contents;
	}

	public boolean isEmpty() {
		return contents == null || contents.length() == 0;
	}

	public Iterable<Map.Entry<String,String>> getAttributes() {
		return PromisesXMLWriter.getSortedEntries(attributes);
	}
}
