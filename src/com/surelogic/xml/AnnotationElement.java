package com.surelogic.xml;

import java.util.*;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class AnnotationElement implements TestXMLParserConstants {
	private final String uid;
	private final String promise;
	private final String contents;
	private final Map<String,String> attributes = new HashMap<String,String>(0);
	
	AnnotationElement(final String id, final String name, String text, Map<String,String> a) {
		final IPromiseDropStorage<?> storage = PromiseFramework.getInstance().findStorage(AnnotationVisitor.capitalize(name));
		if (storage == null) {
			System.err.println("Unknown annotation: "+name);
			uid = name;
		}
		else if (storage.type() != StorageType.SEQ) {
			if (id != null && !name.equals(id)) {
				System.err.println("Ignoring id for non-seq annotation: "+id);
			}
			uid = name;
		} else if (id == null) {
			System.err.println("Creating uid for seq annotation: "+name);
			UUID u = UUID.randomUUID();
			uid = u.toString();
		} else {
			uid = id;
		}
		promise = name;
		contents = text;
		attributes.putAll(a);
		if (id == null && uid != name) {
			attributes.put(UID_ATTRB, uid);
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
	
	public int getRevision() {
		String value = attributes.get(REVISION_ATTRB);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public boolean isDirty() {
		return "true".equals(attributes.get(DIRTY_ATTRB));
	}
	
	public void incrRevision() {
		if (!isDirty()) {
			throw new IllegalStateException("Not dirty");
		}
		final int revision = getRevision();
		attributes.remove(DIRTY_ATTRB);
		attributes.put(REVISION_ATTRB, Integer.toString(revision+1));
	}
}
