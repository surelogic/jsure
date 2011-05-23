package com.surelogic.xml;

import java.util.*;

public class AnnotationElement {
	final String uid;
	final String promise;
	final String contents;
	final Map<String,String> attributes = new HashMap<String,String>(0);
	
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
}
