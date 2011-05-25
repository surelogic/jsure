package com.surelogic.xml;

import java.util.*;

import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.util.UniqueID;

public class AnnotationElement {
	private final String uid;
	private final String promise;
	private final String contents;
	private final Map<String,String> attributes = new HashMap<String,String>(0);
	
	AnnotationElement(final String id, String name, String text, Map<String,String> a) {
		final IPromiseDropStorage<?> storage = PromiseFramework.getInstance().findStorage(name);
		if (storage.type() != StorageType.SEQ) {
			if (id != null && !name.equals(id)) {
				System.err.println("Ignoring id for non-seq annotation: "+id);
			}
			uid = name;
		} else if (id == null) {
			System.err.println("Creating uid for seq annotation: "+name);
			UniqueID u = new UniqueID();
			uid = u.toString();
		} else {
			uid = id;
		}
		promise = name;
		contents = text;
		attributes.putAll(a);
		if (id == null) {
			attributes.put(TestXMLParserConstants.UID_ATTRB, uid);
		}
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
