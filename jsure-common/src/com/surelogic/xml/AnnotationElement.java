package com.surelogic.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.surelogic.annotation.Attribute;
import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.IDecl;

public final class AnnotationElement extends AbstractJavaElement implements
		IMergeableElement, TestXMLParserConstants {
	private static final char DASH = '-';
	private static final String ORIG_PREFIX = "original-";
	private static final String ORIG_CONTENTS = ORIG_PREFIX + "contents";
	// Used to mark placeholder annotations that preserve the relative ordering
	// when merging diffs
	public static final String REF_SUFFIX = DASH + "ref";
//	static {
//		ParseUtil.init();
//	}

	private final String uid;
	private final String promise;
	private String contents;
	private final Map<String, String> attributes = new HashMap<String, String>(
			0);
	private final Map<String, Attribute> attrDefaults;
	private boolean isBad;
	private final boolean isReference;

	public AnnotationElement(IJavaElement parent, final String id,
			final String tag, String text, final Map<String, String> a) {
		if (parent != null) {
			setParent(parent);
		}
		final int dash = tag.indexOf(DASH);
		isReference = dash >= 0;

		final String name = capitalize(isReference ? tag.substring(0, dash) : tag);
		if (!allowsMultipleAnnos(name)) {
			if (id != null && !name.equals(id)) {
				System.err.println("Ignoring id for non-seq annotation: " + id);
			}
			uid = name;
		} else if (id == null) {
			// System.err.println("Creating uid for seq annotation: "+name);
			UUID u = UUID.randomUUID();
			uid = u.toString();
		} else {
			uid = id;
		}
		promise = name;
		contents = text == null ? "" : text.trim();

		attrDefaults = Collections.emptyMap();//AnnotationRules.getAttributes(promise);
		/*
		 * Handle "dirty" for backwards compatibility.
		 */
		final String obsoleteDirtyAttrb = "dirty";
		final String value = a.get(obsoleteDirtyAttrb);
		if (value != null) {
			a.remove(obsoleteDirtyAttrb);
			a.put(MODIFIED_BY_TOOL_USER_ATTRB, value);
		}
		attributes.putAll(a);
		if (id == null && uid != name) {
			attributes.put(UID_ATTRB, uid);
		}
		isBad = !parses(promise, contents);
	}

	public static boolean allowsMultipleAnnos(String name) {
		/*
		final IPromiseDropStorage<?> storage = PromiseFramework.getInstance()
				.findStorage(name);
		if (storage == null) {	
			// System.err.println("Unknown annotation: "+name);
			return false;
		}
		return storage.type() == StorageType.SEQ;
		*/
		return seqAnnos.contains(name);
	}
	
	public static Set<String> getAllowsMultipleAnnosSet() {
		return Collections.unmodifiableSet(seqAnnos);
	}
	
	static final Set<String> seqAnnos = new HashSet<String>();
	static {
		String[] annos = {
			// As of 2/26/15
			"Layer", "TypeSet", "RegionLock", "MapFields", "Region", "Promise", "Assume"
		};
		for(String a : annos) {
			seqAnnos.add(a);
		}
	}
	
	public static String capitalize(String tag) {
		if (tag.length() <= 0) {
			return tag;
		}
		char first = tag.charAt(0);
		if (Character.isLowerCase(first)) {
			return Character.toUpperCase(first) + tag.substring(1);
		}
		return tag;
	}
	
	public final String getName() {
		return promise;
	}

	public final IDecl.Kind getKind() {
		return null; // Not a declaration
	}
	
	@Override
  public boolean isReference() {
		return isReference;
	}

	@Override
	public boolean isBad() {
		return isBad;
	}

	@Override
  public boolean hasChildren() {
		return false;
	}

	@Override
  public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}

	public static boolean isIdentifier(String id) {
		boolean first = true;
		for (int i = 0; i < id.length(); i++) {
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
	public boolean isEditable() {
		return !attrDefaults.isEmpty()
				&& !/*ThreadEffectsRules.STARTS*/"starts".equals(promise);
	}

	public boolean modifyContents(String text) {
		if (!contents.equals(text)) {
			isBad = !parses(promise, text);
			contents = text;

			updateModifiedStatus();
			return true;
		} else {
			// l.reportError("Annotation unchanged",
			// "The contents of the promise were unchanged");
			return false;
		}
	}

	public void markAsModified() {
		super.markAsDirty();
		attributes.put(MODIFIED_BY_TOOL_USER_ATTRB, "true");
	}

	public void markAsUnmodified() {
		markAsClean();
		attributes.remove(MODIFIED_BY_TOOL_USER_ATTRB);
	}

	private void updateModifiedStatus() {
		final String origContents = attributes.get(ORIG_CONTENTS);
		boolean modified = origContents == null
				|| !origContents.equals(contents);
		if (!modified) {
			// Check if attributes are modified
			for (String attr : attrDefaults.keySet()) {
				if (AnnotationConstants.VALUE_ATTR.equals(attr)) {
					continue;
				}
				String value = attributes.get(attr);
				String orig = attributes.get(ORIG_PREFIX + attr);
				if (value != null) {
					// The attr has a value to check
					if (orig == null || !orig.equals(value)) {
						modified = true;
						break;
					}
				} else if (orig != null) { // value == null
					modified = true;
					break;
				}
			}
		}
		if (modified) {
			markAsModified();
		} else {
			markAsClean();
			attributes.remove(MODIFIED_BY_TOOL_USER_ATTRB);
		}
	}

	@Override
  public boolean delete() {
		markAsModified();
		attributes.put(DELETE_ATTRB, "true");
		if (!attributes.containsKey(ORIG_CONTENTS)) {
			// Delete from parent, since it was newly created
			removeFromParent();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Note that this does not set the modified bits in the parent
	 */
	void removeFromParent() {
		AnnotatedJavaElement parent = (AnnotatedJavaElement) getParent();
		parent.removePromise(this);
	}

	/**
	 * @return true if the promise parses
	 */
	private boolean parses(final String promise, final String text/*
																 * , final
																 * IErrorListener
																 * l
																 */) {
		// TODO Unable to check right now
		return true;
//		final IAnnotationParseRule<?, ?> rule = PromiseFramework.getInstance()
//				.getParseDropRule(promise);
//		final IAnnotationParsingContext context = new AbstractAnnotationParsingContext(
//				AnnotationSource.XML, AnnotationOrigin.DECL) {
//			@Override
//			public int mapToSource(int offset) {
//				return UNKNOWN;
//			}
//			
//			@Override
//			protected IRNode getAnnoNode() {
//				return null; // None to return
//			}
//
//			@Override
//      public Operator getOp() {
//				if (getParent() == null) {
//					// Might not be fully initialized
//					return PackageDeclaration.prototype;
//				}
//				return getParent().getOperator();
//			}
//
//			@Override
//			public String getAllText() {
//				return text;
//			}
//
//			@Override
//			public String getSelectedText(int start, int stop) {
//				return text.substring(start, stop);
//			}
//
//			@Override
//      public <T extends IAASTRootNode> void reportAAST(int offset,
//					AnnotationLocation loc, Object o, T ast) {
//				// Ignore this; we only care that it parses
//			}
//
//			@Override
//      public void reportErrorAndProposal(int offset, String msg, ProposedPromiseDrop.Builder proposal, String... info) {
//				// l.reportError("Problem parsing annotation", msg);
//			}
//
//			@Override
//      public void reportException(int offset, Exception e) {
//				SLLogger.getLogger().log(Level.WARNING,
//						"Problem parsing annotation", e);
//				// l.reportError("Problem parsing annotation",
//				// e.getMessage()+" at "+e.getStackTrace()[0]);
//			}
//		};
//		boolean ok = rule != null
//				&& rule.parse(context, text) == ParseResult.OK;
//		if (!ok && rule != null) {
//			System.out.print("Couldn't parse @" + promise + " " + text
//					+ " for " + context.getOp().name());
//			if (getParent() == null) {
//				System.out.println();
//			} else {
//				System.out.println(" : " + getParent().getLabel());
//			}
//			rule.parse(context, text);
//		}
//		return ok;
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

	public Iterable<Map.Entry<String, String>> getAttributes() {
		return PromisesXMLWriter.getSortedEntries(attributes);
	}

	public String getAttribute(String key) {
		return attributes.get(key);
	}

	public String setAttribute(String key, String value) {
		Attribute attr = attrDefaults.get(key);
		String defValue = attr == null ? null : attr.getDefaultValueOrNull();

		String old;
		if (defValue == null || !defValue.equals(value)) {
			old = attributes.put(key, value);
		} else {
			old = attributes.remove(key);
		}
		updateModifiedStatus();
		return old;
	}

	public Map<String,String> getAttributeMap() {
		return Collections.unmodifiableMap(attributes);
	}
	
	@Override
	public boolean isModified() {
		return "true".equals(attributes.get(MODIFIED_BY_TOOL_USER_ATTRB))
				|| isToBeDeleted();
	}

	@Override
  public boolean isToBeDeleted() {
		return "true".equals(attributes.get(DELETE_ATTRB));
	}

	@Override
  public String getLabel() {
		// Preprocess the attributes
		final Map<String, String> pairs;
		if (attrDefaults.isEmpty()) {
			pairs = Collections.emptyMap();
		} else {
			pairs = new HashMap<String, String>(4, 1.0f);
			for (Attribute attr : attrDefaults.values()) {
				if (AnnotationConstants.VALUE_ATTR.equals(attr.getName())) {
					continue;
				}
				String value = attributes.get(attr.getName());
				if (value != null) {
					pairs.put(attr.getName(),
							attr.isTypeString() ? '"' + value + '"' : value);
				}
			}
		}
		final boolean contentsIsEmpty = contents == null
				|| contents.length() == 0;
		if (contentsIsEmpty && pairs.isEmpty()) {
			return promise;
		}
		final StringBuilder sb = new StringBuilder(promise);
		sb.append('(');
		boolean first = contentsIsEmpty;
		if (!contentsIsEmpty) {
			sb.append('"');
			sb.append(contents);
			sb.append('"');
		}
		for (Map.Entry<String, String> e : pairs.entrySet()) {
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

	@Override
  public final String getImageKey() {
		return CommonImages.IMG_ANNOTATION;
	}

	@Override
  public void mergeAttached(IMergeableElement other) {
		// Merge the comments that are attached
		AnnotationElement a = (AnnotationElement) other;
		stashDiffState(a);
	}

	@Override
	public AnnotationElement cloneMe(IJavaElement parent) {
		AnnotationElement clone = new AnnotationElement(parent, uid, promise,
				contents, attributes);
		copyToClone(clone);
		return clone;
	}

	/**
	 * Make a copy of the current annotation,
	 * but make as if it was newly created by the user
	 */
	public AnnotationElement cloneAsNew(IJavaElement parent) {
		AnnotationElement clone = cloneMe(parent);
		// Clean out diff state
		for(String key : clone.attributes.keySet().toArray(new String[clone.attributes.size()])) {
			if (key.startsWith(ORIG_PREFIX)) {
				clone.attributes.remove(key);
			}
		}
		return clone;
	}
	
	@Override
	public int hashCode() {
		// return comment.hashCode();
		return uid.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AnnotationElement) {
			AnnotationElement other = (AnnotationElement) o;
			// return comment.equals(other.comment);
			return uid.equals(other.uid);
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getName() + '@'
				+ Integer.toHexString(super.hashCode());
	}

//	@Override
	@Override
  public boolean isEquivalent(IMergeableElement o) {
		if (o instanceof AnnotationElement) {
			AnnotationElement other = (AnnotationElement) o;
			return uid.equals(other.uid) &&
				contents.equals(other.contents) &&
				promise.equals(other.promise) &&
				hasSameAttributes(other);
		}
		return false;
	}
	
	private boolean hasSameAttributes(AnnotationElement other) {
		if (isToBeDeleted()) {
			return false; // These can't be the same
		}
		for(Map.Entry<String, String> e : other.attributes.entrySet()) {
			String val = getAttribute(e.getKey());
			if (!e.getValue().equals(val)) {
				return false;
			}
		}
		return true;
	}

	protected AnnotationElement createRef() {
		return new AnnotationElement(null, uid, promise + REF_SUFFIX, contents,
				Collections.<String, String> emptyMap());
	}

	protected void flushDiffState() {
		attributes.remove(ORIG_CONTENTS);
		for (String a : attrDefaults.keySet()) {
			if (AnnotationConstants.VALUE_ATTR.equals(a)) {
				continue;
			}
			final String origKey = ORIG_PREFIX + a;
			attributes.remove(origKey);
		}
	}

	protected void stashDiffState(AnnotationElement orig) {
		if (!attributes.containsKey(ORIG_CONTENTS)) {
			attributes.put(ORIG_CONTENTS, orig.contents);
		}
		for (String a : attrDefaults.keySet()) {
			if (AnnotationConstants.VALUE_ATTR.equals(a)) {
				continue;
			}
			final String origKey = ORIG_PREFIX + a;
			if (!attributes.containsKey(origKey)) {
				attributes.put(origKey, orig.getAttribute(a));
			}
		}
	}

	public boolean canRevert() {
		return isDirty() && attributes.containsKey(ORIG_CONTENTS);
	}

	public void revert() {
		if (!canRevert()) {
			return;
		}
		boolean hasValue = false;
		for (String a : attrDefaults.keySet()) {
			if (AnnotationConstants.VALUE_ATTR.equals(a)) {
				hasValue = true;
				continue;
			}
			final String origKey = ORIG_PREFIX + a;
			if (attributes.containsKey(origKey)) {
				attributes.put(a, attributes.get(origKey));
			} else {
				attributes.remove(a);
			}
		}
		if (hasValue) {
			String origContents = attributes.get(ORIG_CONTENTS);
			modifyContents(origContents);
		}
		markAsUnmodified();
	}

	public Map<String, Attribute> getAttributeDefaults() {
		return attrDefaults;
	}
}
