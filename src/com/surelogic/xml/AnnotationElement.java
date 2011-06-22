package com.surelogic.xml;

import java.util.*;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.IErrorListener;
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
		
	public static boolean isIdentifier(String id) {
		boolean first = true;
		for(int i=0; i<id.length(); i++) {
			char c = id.charAt(i);
			if (first) {
				first = false;
				if (!Character.isJavaIdentifierStart(c)) {
					return false;
				}
			} else {
				if (!Character.isJavaIdentifierPart(c)) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public boolean canModify() {
		return true;
	}
	
	@Override
	public void modify(String value, IErrorListener l) {
		value = value.trim();
				
		final int paren = value.indexOf('(');
		String anno, text;
		if (paren < 0) {
			if (!isIdentifier(value)) {
				// Ignore, since the promise type changed
				l.reportError("Annotation cannot be changed", "The promise type cannot be changed from "+promise+" to "+value);
				return;
			}
			anno = value;
			text = "";
		} else {
			if (!value.endsWith(")")) {
				// Ignore, since the text doesn't have the right format
				l.reportError("Bad annotation syntax", "The annotation needs to use the general syntax:\n\tFoo()");
				return;
			}
			anno = value.substring(0, paren).trim();
			text = value.substring(paren+1, value.length()-1).trim();
		}			
		if (!promise.equals(anno)) {
			// Ignore, since the promise type changed
			l.reportError("Annotation cannot be changed", "The promise type cannot be changed from "+promise+" to "+anno);
			return;
		}
		if (!contents.equals(text)) {
			contents = text;		
			markAsDirty();
			attributes.put(DIRTY_ATTRB, "true");
		} else {
			l.reportError("Annotation unchanged", "The contents of the promise were unchanged");
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
	
	void merge(AnnotationElement other) {		
		if (isModified()) {
		 	return; // Keep what we've edited
		}
		final int thisRev = getRevision();
		final int otherRev = other.getRevision();
		MergeType type;
		if (thisRev == otherRev) {
			if (!other.isModified()) {
				return; // These should be the same
			}
			// Overwrite things in common
			contents = other.contents;
			attributes.putAll(other.attributes);
			incrRevision();			
			type = MergeType.USE_OTHER;
		} else if (otherRev > thisRev) {
			// Overwrite this completely
			attributes.clear();
			contents = other.contents;
			attributes.putAll(other.attributes);
			type = MergeType.USE_OTHER;
		} else {
			// Ignore the other, since it's an older rev
			return;
		}
		super.mergeThis(other, type);		
	}
	
	AnnotationElement cloneMe() {
		AnnotationElement clone = new AnnotationElement(uid, promise, contents, attributes);
		copyToClone(clone);
		return clone;
	}
}
