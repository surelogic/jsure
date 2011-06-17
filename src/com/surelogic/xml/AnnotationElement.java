package com.surelogic.xml;

import java.util.*;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.CommonImages;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class AnnotationElement extends CommentedJavaElement implements TestXMLParserConstants {
	private final String uid;
	private final String promise;
	private String contents;
	private final Map<String,String> attributes = new HashMap<String,String>(0);
	
	public AnnotationElement(final String id, final String tag, String text, Map<String,String> a) {
		final String name = AnnotationVisitor.capitalize(tag);
		final IPromiseDropStorage<?> storage = PromiseFramework.getInstance().findStorage(name);
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
		contents = text == null ? "" : text.trim();
		attributes.putAll(a);
		if (id == null && uid != name) {
			attributes.put(UID_ATTRB, uid);
		}
	}
	
	@Override
	public boolean canModify() {
		return true;
	}
	
	@Override
	public void modify(String value) {
		value = value.trim();
		
		final int paren = value.indexOf('(');
		String anno, text;
		if (paren < 0) {
			anno = value;
			text = "";
		} else {
			if (!value.endsWith(")")) {
				// Ignore, since the text doesn't have the right format
				return;
			}
			anno = value.substring(0, paren).trim();
			text = value.substring(paren+1, value.length()-1).trim();
		}			
		if (!promise.equals(anno)) {
			// Ignore, since the promise changed
			return;
		}
		if (!contents.equals(text)) {
			contents = text;		
			markAsDirty();
		}
	}
	
	public final String getUid() {
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
	
	public boolean isModified() {
		return "true".equals(attributes.get(DIRTY_ATTRB));
	}
	
	public void incrRevision() {
		if (!isModified()) {
			throw new IllegalStateException("Not dirty");
		}
		final int revision = getRevision();
		attributes.remove(DIRTY_ATTRB);
		attributes.put(REVISION_ATTRB, Integer.toString(revision+1));
	}
	
	public String getLabel() {
		if (contents == null || contents.length() == 0) {
			return promise;
		}
		return promise+'('+contents+')';
	}

	public final String getImageKey() {
		return CommonImages.IMG_ANNOTATION;
	}
}
