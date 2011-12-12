package com.surelogic.xml;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.aast.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.IErrorListener;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.StorageType;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.Annotation;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public final class AnnotationElement extends AbstractJavaElement implements IMergeableElement, TestXMLParserConstants {
	private static final char DASH = '-';
	private static final String ORIG_CONTENTS = "original-contents";
	public static final String REF_SUFFIX = DASH+"ref";	
	
	private final String uid;
	private final String promise;
	private String contents;
	private final Map<String,String> attributes = new HashMap<String,String>(0);
	private boolean isBad;
	private final boolean isReference;
	
	public AnnotationElement(IJavaElement parent, final String id, final String tag, String text, Map<String,String> a) {
		if (parent != null) {
			setParent(parent);
		}
		final int dash = tag.indexOf(DASH);
		isReference = dash >= 0;
		
		final String name = AnnotationVisitor.capitalize(isReference ? tag.substring(0, dash) : tag);
		final IPromiseDropStorage<?> storage = PromiseFramework.getInstance().findStorage(name);
		if (storage == null) {
			//System.err.println("Unknown annotation: "+name);
			uid = name;
		}
		else if (storage.type() != StorageType.SEQ) {
			if (id != null && !name.equals(id)) {
				System.err.println("Ignoring id for non-seq annotation: "+id);
			}
			uid = name;
		} else if (id == null) {
			//System.err.println("Creating uid for seq annotation: "+name);
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
		isBad = !parses(promise, contents);
	}
	
	public boolean isReference() {
		return isReference;
	}
 	
	public boolean isBad() {
		return isBad;
	}
	
	public boolean hasChildren() {
		return false;
	}
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
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
	public boolean modify(String value, IErrorListener l) {
		value = value.trim();
				
		final int paren = value.indexOf('(');
		String anno, text;
		if (paren < 0) {
			if (!isIdentifier(value)) {
				// Ignore, since the promise type changed
				//
				//l.reportError("Annotation cannot be changed", "The promise type cannot be changed from "+promise+" to "+value);
				return false;
			}
			anno = value;
			text = "";
		} else {			
			final int end;
			if (!value.endsWith(")")) {
				// Ignore, since the text doesn't have the right format?
				/*
				l.reportError("Bad annotation syntax", "The annotation needs to use the general syntax:\n\tFoo()");
				return;
				*/
				// Act as if it ended with ')'
				end = value.length();
			} else {
				end = value.length()-1;
			}
			anno = value.substring(0, paren).trim();
			text = value.substring(paren+1, end).trim();
		}			
		/* Ignore changes to the promise
		 * 
		if (!promise.equals(anno)) {
			// Ignore, since the promise type changed
			l.reportError("Annotation cannot be changed", "The promise type cannot be changed from "+promise+" to "+anno);
			return;
		}
		*/
		if (!contents.equals(text)) {
			isBad = !parses(promise, text);
			contents = text;		
			
			final String origContents = attributes.get(ORIG_CONTENTS);
			if (origContents != null && origContents.equals(text)) {
				markAsUnmodified(); // TODO is this right with attributes?
			} else {
				markAsModified();
			}
			return true;
		} else {
			//l.reportError("Annotation unchanged", "The contents of the promise were unchanged");
			return false;
		}
	}

	public void markAsModified() {
		super.markAsDirty();
		attributes.put(DIRTY_ATTRB, "true");
	}
	
	void markAsUnmodified() {
		markAsClean();
		attributes.remove(DIRTY_ATTRB);
	}
	
	public boolean delete() {
		markAsModified();
		attributes.put(DELETE_ATTRB, "true");
		if (!attributes.containsKey(ORIG_CONTENTS)) {
			// Delete from parent, since it was newly created
			AnnotatedJavaElement parent = (AnnotatedJavaElement) getParent();
		    parent.removePromise(this);		        		
		    return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @return true if the promise parses
	 */
	private boolean parses(final String promise, final String text/*, final IErrorListener l*/) {
		final IAnnotationParseRule<?,?> rule = PromiseFramework.getInstance().getParseDropRule(promise);
		final IAnnotationParsingContext context = new AbstractAnnotationParsingContext(AnnotationSource.XML) {			
			@Override
			protected IRNode getNode() {
				return null; // None to return
			}
			public Operator getOp() {
				if (getParent() == null) {
					// Might not be fully initialized
					return PackageDeclaration.prototype;
				}
				return getParent().getOperator();
			}
			@Override
			public String getAllText() {
				return text;
			}
			@Override
			public String getSelectedText(int start, int stop) {
				return text.substring(start, stop);
			}			

			public <T extends IAASTRootNode> void reportAAST(int offset,
					AnnotationLocation loc, Object o, T ast) {				
				// Ignore this; we only care that it parses
			}			
			public void reportError(int offset, String msg) {
				//l.reportError("Problem parsing annotation", msg);
			}
			public void reportException(int offset, Exception e) {
				SLLogger.getLogger().log(Level.WARNING, "Problem parsing annotation", e);
				//l.reportError("Problem parsing annotation", e.getMessage()+" at "+e.getStackTrace()[0]);				
			}						
		};
		return rule != null && rule.parse(context, text) == ParseResult.OK;
	}
	
	public Operator getOperator() {
		return Annotation.prototype;
	}
	
	public final String getUid() {
		return uid;
	}
	
	public final String getPromise() {
		return promise;		
	}
	
	public final String getContents() {
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
	
	@Override
	public boolean isModified() {
		return "true".equals(attributes.get(DIRTY_ATTRB)) || isToBeDeleted();
	}
	
	public boolean isToBeDeleted() {
		return "true".equals(attributes.get(DELETE_ATTRB));
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
		final Map<String,String> pairs;	
		Collection<String> attrs = AnnotationRules.getAttributes(promise);
		if (attrs.isEmpty()) {
			pairs = Collections.emptyMap();
		} else {
			pairs = new HashMap<String,String>(4, 1.0f);
			for(String attr : AnnotationRules.getAttributes(promise)) {
				String value = attributes.get(attr);
				if (value != null) {
					pairs.put(attr, value);
				}
			}
		}		                                                          		
		final boolean contentsIsEmpty = contents == null || contents.length() == 0;
		if (contentsIsEmpty && pairs.isEmpty()) {
			return promise;
		}
		final StringBuilder sb = new StringBuilder(promise);
		sb.append('(');
		boolean first = contentsIsEmpty; 
		if (!contentsIsEmpty) {
			sb.append(contents);
		}
		for(Map.Entry<String,String> e : pairs.entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(e.getKey());
			sb.append('=');
			sb.append(e.getValue());
		}
		sb.append(')');
		return sb.toString(); // promise+'('+contents+')';
	}

	public final String getImageKey() {
		return CommonImages.IMG_ANNOTATION;
	}
	
	final AnnotationElement merge(AnnotationElement other, MergeType type) {		
		return merge(this, other, type);	
	}
	
	public void mergeAttached(IMergeableElement other) {
		// Merge the comments that are attached
		AnnotationElement a = (AnnotationElement) other;
		mergeThis(a, MergeType.MERGE);
		stashDiffState(a.getContents());
	}
	
	@Override
	public AnnotationElement cloneMe() {
		AnnotationElement clone = new AnnotationElement(getParent(), uid, promise, contents, attributes);
		copyToClone(clone);
		return clone;
	}
	
	@Override
	public int hashCode() {
		//return comment.hashCode();
		return uid.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof AnnotationElement) {
			AnnotationElement other = (AnnotationElement) o;			
			//return comment.equals(other.comment);
			return uid.equals(other.uid);
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getName()+'@'+Integer.toHexString(super.hashCode());
	}
	
	AnnotationElement createRef() {
		// TODO make factory method?
		return new AnnotationElement(null, uid, promise+REF_SUFFIX, contents, Collections.<String,String>emptyMap());
	}

	int applyPromise(final AnnotationVisitor v, final IRNode annotatedNode) {
		if (isBad || isToBeDeleted()) {
			return 0;
		}
		final boolean implOnly = "true".equals(attributes.get(AnnotationVisitor.IMPLEMENTATION_ONLY));
		final String rawVerify = attributes.get(AnnotationVisitor.VERIFY);
		final boolean verify   = rawVerify == null || "true".equals(rawVerify);
		final boolean allowReturn = "true".equals(attributes.get(AnnotationVisitor.ALLOW_RETURN));
		final boolean allowRead = "true".equals(attributes.get(AnnotationVisitor.ALLOW_READ));

		boolean added = v.handleXMLPromise(annotatedNode, promise, contents, 
				AnnotationVisitor.convertToModifiers(implOnly, verify, allowReturn, allowRead),
				attributes);
		return added ? 1 : 0;
	}

	void stashDiffState(String contents) {
		if (!attributes.containsKey(ORIG_CONTENTS)) {
			attributes.put(ORIG_CONTENTS, contents);
		}
	}
}
