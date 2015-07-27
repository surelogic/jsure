package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ANNOTATION_TYPE;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ANNO_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CONTENTS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FROM_REF;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.JAVA_ANNOTATION;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.NO_ANNO_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.NO_REPLACED_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ORIGIN;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PROPOSED_PROMISE_DROP;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_ANNO;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_ATTRS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.REPLACED_CONTENTS;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.NotThreadSafe;
import com.surelogic.Nullable;
import com.surelogic.RequiresLock;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.IJavaRef.Position;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ir.SeaSnapshot;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Represents a proposed promise in the sea. A proposed promise indicates a
 * missing portion of a model. Proposed promises are constructed by analyses and
 * used by the tool user interface to help the programmer annotate their code.
 * <p>
 * This drop implements value semantics so that duplicates can be removed by
 * placing them into a set.
 */
public final class ProposedPromiseDrop extends Drop implements IProposedPromiseDrop {

  @NotThreadSafe
  public static final class Builder {

    /**
     * Where I want to create the annotation
     */
    @NonNull
    private final IRNode f_at;

    /**
     * Where the request came from, in case we have to create an assume rather
     * than an actual annotation.
     */
    @NonNull
    private final IRNode f_from;

    /**
     * An indication of how this proposal was generated.
     */
    @Nullable
    private Origin f_origin = null;

    /**
     * the annotation being proposed. For <code>@Starts("nothing")</code> the
     * value of this string would be {@code "Starts"}.
     */
    @NonNull
    private final String f_annotation;

    /**
     * The value of the annotation being proposed. For
     * <code>@Starts("nothing")</code> the value of this string would be
     * {@code "nothing"}. For <code>@Borrowed</code>, which has no contents, the
     * value of this string would be {@code null}.
     */
    @Nullable
    private String f_value = null;

    /**
     * The non-value attributes for the annotation being proposed.
     */
    @NonNull
    private final Map<String, String> f_attributeNameToValue = new HashMap<>();

    /**
     * The annotation being replaced, similar to {@link #f_annotation}.
     */
    @Nullable
    private String f_replacedAnnotation = null;
    /**
     * The value being replaced, similar to {@link #f_value}.
     */
    @Nullable
    private String f_replacedValue = null;

    /**
     * The non-value attributes for the annotation being replaced, similar to
     * {@link #f_attributeNameToValue}.
     */
    @NonNull
    private final Map<String, String> f_replacedAttributeNameToValue = new HashMap<>();

    @Nullable
    private Drop f_forDrop = null;

    @Nullable
    private ProofDrop f_forDropOnlyIfNotProvedConsistent = null;

    /**
     * Constructs a builder to propose the passed annotation.
     * 
     * @param annotation
     *          an annotation in <tt>com.surelogic</tt>.
     * @param at
     *          where to put the proposal.
     * @param from
     *          where the request came from, in case we have to create an assume
     *          rather than an actual annotation.
     * @throws IllegalArgumentException
     *           if any of the parameters is {@code null} or the passed
     *           annotation is not in the package <tt>com.surelogic</tt>.
     */
    public Builder(@NonNull Class<? extends Annotation> annotation, @NonNull IRNode at, @NonNull IRNode from) {
      if (annotation == null)
        throw new IllegalArgumentException(I18N.err(44, "annotation"));
      if (at == null)
        throw new IllegalArgumentException(I18N.err(44, "at"));
      if (from == null)
        throw new IllegalArgumentException(I18N.err(44, "from"));
      if (!SLUtility.SURELOGIC_ANNOTATION_PACKAGE.equals(annotation.getPackage().getName()))
        throw new IllegalArgumentException(I18N.err(299, annotation.getName()));
      f_annotation = annotation.getSimpleName();
      f_at = at;
      f_from = from;
    }

    /**
     * Constructs a builder to propose the passed annotation.
     * <p>
     * <b>If possible use {@link Builder#Builder(Class, IRNode, IRNode)} which
     * has better error checking&mdash;this method should only be invoked by XML
     * reading code and its ilk.</b>
     * 
     * @param annotation
     *          the simple name of an annotation in <tt>com.surelogic</tt>.
     * @param at
     *          where to put the proposal.
     * @param from
     *          where the request came from, in case we have to create an assume
     *          rather than an actual annotation.
     * @throws IllegalArgumentException
     *           if any of the parameters is {@code null}.
     */
    public Builder(@NonNull String annotation, @NonNull IRNode at, @NonNull IRNode from) {
      if (annotation == null)
        throw new IllegalArgumentException(I18N.err(44, "annotation"));
      if (at == null)
        throw new IllegalArgumentException(I18N.err(44, "at"));
      if (from == null)
        throw new IllegalArgumentException(I18N.err(44, "from"));
      f_annotation = annotation;
      f_at = at;
      f_from = from;
    }

    /**
     * Sets the value of the Java annotation being proposed. For
     * <code>@Starts("nothing")</code> the value of this string would be
     * {@code "nothing"}. For <code>@Borrowed</code>, which has no contents, the
     * value of this string would be {@code null}.
     * <p>
     * The contents placed into this string should not be escaped. Any embedded
     * quotations or backward slashes will be escaped before output using
     * {@link SLUtility#escapeJavaStringForQuoting(String)}.
     * 
     * @param value
     *          the value of the Java annotation being proposed.
     * @return this builder.
     */
    public Builder setValue(@Nullable String value) {
      f_value = value;
      return this;
    }

    public Builder addAttribute(@NonNull String name, @NonNull String value) {
      if (name == null)
        throw new IllegalArgumentException(I18N.err(44, "name"));
      if (value == null)
        throw new IllegalArgumentException(I18N.err(44, "value"));
      f_attributeNameToValue.put(name, value);
      return this;
    }

    public Builder setAttributes(@Nullable Map<String, String> nameToValue) {
      if (nameToValue != null) {
        f_attributeNameToValue.clear();
        f_attributeNameToValue.putAll(nameToValue);
      }
      return this;
    }

    /**
     * An indication of how this proposal was generated.
     */
    public Builder setOrigin(@Nullable Origin value) {
      f_origin = value;
      return this;
    }

    public Builder forDrop(@Nullable Drop value) {
      f_forDrop = value;
      return this;
    }

    public Builder forDropOnlyIfNotProvedConsistent(@Nullable ProofDrop value) {
      f_forDropOnlyIfNotProvedConsistent = value;
      return this;
    }

    public Builder replaceExisting(@NonNull Class<? extends Annotation> annotation) {
      return replaceExisting(annotation, null, null);
    }

    public Builder replaceExisting(@NonNull Class<? extends Annotation> annotation, @Nullable String value) {
      return replaceExisting(annotation, value, null);
    }

    public Builder replaceExisting(@NonNull Class<? extends Annotation> annotation, @Nullable String value,
        @Nullable Map<String, String> nameToValue) {
      if (annotation == null)
        throw new IllegalArgumentException(I18N.err(44, "annotation"));
      if (!SLUtility.SURELOGIC_ANNOTATION_PACKAGE.equals(annotation.getPackage().getName()))
        throw new IllegalArgumentException(I18N.err(299, annotation.getName()));
      f_replacedAnnotation = annotation.getSimpleName();
      f_replacedValue = value;
      if (nameToValue != null) {
        f_replacedAttributeNameToValue.clear();
        f_replacedAttributeNameToValue.putAll(nameToValue);
      }
      return this;
    }

    public Builder replaceSameExisting(@Nullable String value) {
      return replaceSameExisting(value, null);
    }

    public Builder replaceSameExisting(@Nullable String value, @Nullable Map<String, String> nameToValue) {
      f_replacedAnnotation = f_annotation;
      f_replacedValue = value;
      if (nameToValue != null) {
        f_replacedAttributeNameToValue.clear();
        f_replacedAttributeNameToValue.putAll(nameToValue);
      }
      return this;
    }

    public ProposedPromiseDrop build() {
      /*
       * Set the default for origin if it is still null. If we are going to be
       * attached to a promise, even conditional upon its consistency result, we
       * go with MODEL. We go with PROBLEM otherwise.
       */
      if (f_origin == null) {
        if (f_forDrop == null && f_forDropOnlyIfNotProvedConsistent == null)
          f_origin = Origin.PROBLEM;
        else
          f_origin = Origin.MODEL;
      }

      final ProposedPromiseDrop result = new ProposedPromiseDrop(f_annotation, f_value, f_attributeNameToValue,
          f_replacedAnnotation, f_replacedValue, f_replacedAttributeNameToValue, f_at, f_from, f_origin);

      if (f_forDrop != null) {
        f_forDrop.addProposal(result);
      }
      if (f_forDropOnlyIfNotProvedConsistent != null) {
        f_forDropOnlyIfNotProvedConsistent.addProposalNotProvedConsistent(result);
      }
      return result;
    }
  }

  /**
   * <b>Do Not Invoke&mdash;Use the {@link Builder}</b>.
   * <p>
   * Constructs a new proposed promise. Optionally this promise may replace an
   * existing promise.
   * 
   * @param annotation
   *          the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "Starts"}.
   * @param value
   *          the value of the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "nothing"}. For <code>@Borrowed</code>, which has no
   *          contents, the value of this string would be {@code null}.
   * @param attributeNameToValue
   *          the non-value attributes for the Java annotation being proposed
   * @param replacedAnnotation
   *          the Java annotation being replaced.
   * @param replacedValue
   *          the value of the Java annotation being replaced. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "nothing"}. For <code>@Borrowed</code>, which has no
   *          contents, the value of this string would be {@code null}.
   * @param replacedAttributeNameToValue
   *          the non-value attributes for the Java annotation being replaced.
   * @param at
   *          the proposed location for the promise, a declaration.
   * @param from
   *          a node within the compilation unit where the analysis deems that
   *          this proposed promise is needed. This is used to remove this
   *          proposed promise if the compilation unit is reanalyzed.
   * @param origin
   *          an indication of how this proposal was generated.
   */
  ProposedPromiseDrop(@NonNull String annotation, @Nullable String value, @NonNull Map<String, String> attributeNameToValue,
      @Nullable String replacedAnnotation, @Nullable String replacedValue,
      @NonNull Map<String, String> replacedAttributeNameToValue, @NonNull IRNode at, @NonNull IRNode from, @NonNull Origin origin) {
    super(at);
    if (from == null)
      throw new IllegalArgumentException(I18N.err(44, "from"));
    f_requestedFrom = from;
    if (annotation == null)
      throw new IllegalArgumentException(I18N.err(44, "annotation"));
    f_annotation = annotation;
    if (origin == null)
      throw new IllegalArgumentException(I18N.err(44, "origin"));
    f_origin = origin;

    // The rest be null

    f_value = value;
    f_attributeNameToValue = attributeNameToValue != null ? attributeNameToValue : Collections.<String, String> emptyMap();
    f_replacedAnnotation = replacedAnnotation;
    f_replacedValue = replacedValue;
    f_replacedAttributeNameToValue = replacedAttributeNameToValue != null ? replacedAttributeNameToValue : Collections
        .<String, String> emptyMap();

    String contents = computeActualContents(value, f_attributeNameToValue, false);
    if (contents == null) {
    	setMessageHelper(18, annotation);
    } else {
    	setMessageHelper(10, annotation, contents);
    }
  }

  private static String computeActualContents(String value, Map<String, String> attrs, boolean escape) {
	  if (attrs.isEmpty()) {
		  if (escape) {
			  if (value == null) {
				  return null;
			  }
			  return '"'+SLUtility.escapeJavaStringForQuoting(value)+'"';
		  }
		  return value;
	  }
      StringBuilder sb = new StringBuilder();
      if (value != null) {
      	String v = escape ? SLUtility.escapeJavaStringForQuoting(value) : value;
        sb.append("value=").append('"').append(v).append('"');
      }
      for (Map.Entry<String, String> e : attrs.entrySet()) {
    	String v = escape ? SLUtility.escapeJavaStringForQuoting(e.getValue()) : e.getValue();
        sb.append(e.getKey()).append('=').append('"').append(v).append('"');
      }
      return sb.toString();
  }
  
  /**
   * An indication of how this proposal was generated.
   */
  @NonNull
  private final Origin f_origin;

  @Override
  @NonNull
  public Origin getOrigin() {
    return f_origin;
  }

  public final DropType getDropType() {
	return DropType.PROPOSAL;
  }
  
  @Override
  public boolean isAbductivelyInferred() {
    /*
     * This could change but we take problem and model for now.
     */
    return f_origin != Origin.CODE;
  }

  /**
   * The Java annotation being proposed. For <code>@Starts("nothing")</code> the
   * value of this string would be {@code "Starts"}.
   */
  @NonNull
  private final String f_annotation;

  @Override
  @NonNull
  public String getAnnotation() {
    return f_annotation;
  }

  /**
   * The value of the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"}. For <code>@Borrowed</code>, which has no contents, the
   * value of this string would be {@code null}.
   * <p>
   * The contents placed into this string should not be escaped. Any embedded
   * quotations or backward slashes will be escaped before output.
   */
  @Nullable
  private final String f_value;

  @Override
  @Nullable
  public String getValue() {
    return f_value;
  }

  /**
   * Gets the escaped value of the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"}. For <code>@Borrowed</code>, which has no contents, the
   * value of this string would be {@code null}.
   * 
   * @return the contents of the Java annotation being proposed, or {code null}
   *         if none.
   * 
   * @see SLUtility#escapeJavaStringForQuoting(String)
   */
  public String getEscapedValue() {
    return SLUtility.escapeJavaStringForQuoting(f_value);
  }

  /**
   * Gets the non-value attributes for the Java annotation being proposed. The
   * map is from the name of the attribute to the value.
   */
  @NonNull
  private final Map<String, String> f_attributeNameToValue;

  @Override
  @NonNull
  public Map<String, String> getAttributes() {
    return f_attributeNameToValue;
  }

  @NonNull
  public String getJavaAnnotationNoAtSign() {
	String contents = computeActualContents(f_value, f_attributeNameToValue, true);
    return f_annotation + (contents == null ? "" : "(" + contents + ")");
  }

  @Override
  @NonNull
  public String getJavaAnnotation() {
    return "@" + getJavaAnnotationNoAtSign();
  }

  /**
   * The Java annotation being replaced. Similar to {@link #f_annotation}.
   */
  @Nullable
  private final String f_replacedAnnotation;

  @Override
  @Nullable
  public String getReplacedAnnotation() {
    return f_replacedAnnotation;
  }

  /**
   * The contents of the Java annotation being replaced. Similar to
   * {@link #f_value}.
   */
  @Nullable
  private final String f_replacedValue;

  @Override
  @Nullable
  public String getReplacedValue() {
    return f_replacedValue;
  }

  /**
   * Gets the non-value attributes for the Java annotation being replaced. The
   * map is from the name of the attribute to the value. Similar to
   * {@link #f_attributeNameToValue}.
   */
  @NonNull
  private final Map<String, String> f_replacedAttributeNameToValue;

  @Override
  @NonNull
  public Map<String, String> getReplacedAttributes() {
    return f_replacedAttributeNameToValue;
  }

  private final IRNode f_requestedFrom;

  /**
   * A node within the compilation unit where the analysis deems that this
   * proposed promise is needed. This is used to remove this proposed promise if
   * the compilation unit is reanalyzed.
   * 
   */
  public IRNode getRequestedFrom() {
    return f_requestedFrom;
  }

  /**
   * The enclosing type of the node where the analysis deems that this proposed
   * promise is needed. This is used to add an {@code Assume} promise if the
   * SrcRef is not in this project.
   * 
   * @return the node where the analysis deems that this proposed promise is
   *         needed.
   */
  @NonNull
  public IRNode getAssumptionNode() {
    return VisitUtil.getClosestType(f_requestedFrom);
  }

  @Override
  @NonNull
  public IJavaRef getAssumptionRef() {
    IJavaRef result = JavaNode.getJavaRef(f_requestedFrom);
    if (result == null) {
      final IRNode parent = JavaPromise.getParentOrPromisedFor(f_requestedFrom);
      result = JavaNode.getJavaRef(parent);
    }
    if (result != null) {
      // check position
      Position position = result.getPositionRelativeToDeclaration();
      if (position == Position.IS_DECL) {
        position = Position.ON_DECL;
        final JavaRef.Builder builder = new JavaRef.Builder(result);
        builder.setPositionRelativeToDeclaration(position);
        result = builder.build();
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return getJavaAnnotation();
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return PROPOSED_PROMISE_DROP;
  }

  @Override
  @RequiresLock("SeaLock")
  public void snapshotAttrs(XmlCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(JAVA_ANNOTATION, getJavaAnnotation());
    s.addAttribute(ANNOTATION_TYPE, getAnnotation());
    s.addAttribute(CONTENTS, getValue());
    s.addAttribute(REPLACED_ANNO, getReplacedAnnotation());
    s.addAttribute(REPLACED_CONTENTS, getReplacedValue());
    s.addAttribute(ORIGIN, getOrigin().toString());
    s.addAttribute(NO_ANNO_ATTRS, f_attributeNameToValue.isEmpty());
    s.addAttribute(NO_REPLACED_ATTRS, f_replacedAttributeNameToValue.isEmpty());

    final @Nullable
    IJavaRef declRef = JavaNode.getJavaRef(getAssumptionNode());
    final @NonNull
    IJavaRef assumeRef = getAssumptionRef();
    final IJavaRef javaRef = declRef != null && declRef.getDeclaration() != null ? new JavaRef.Builder(assumeRef).setDeclaration(
        declRef.getDeclaration()).build() : assumeRef;
    final String encodedJavaRef = javaRef.encodeForPersistence();
    s.addAttribute(FROM_REF, encodedJavaRef);
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, XmlCreator.Builder db) {
    super.snapshotRefs(s, db);
    s.addProperties(db, ANNO_ATTRS, f_attributeNameToValue);
    s.addProperties(db, REPLACED_ATTRS, f_replacedAttributeNameToValue);
  }

  @Override
  @NonNull
  protected Pair<IJavaRef, IRNode> getJavaRefAndCorrespondingNode() {
    final Pair<IJavaRef, IRNode> superRefAndNode = super.getJavaRefAndCorrespondingNode();
    if (superRefAndNode == null) {
      // throw new IllegalStateException(I18N.err(293, getMessage()));
      return null;
    }
    Position position = superRefAndNode.first().getPositionRelativeToDeclaration();
    if (position == Position.IS_DECL) {
      position = Position.ON_DECL;
      final JavaRef.Builder builder = new JavaRef.Builder(superRefAndNode.first());
      builder.setPositionRelativeToDeclaration(position);
      return new Pair<>(builder.build(), superRefAndNode.second());
    } else
      return superRefAndNode;
  }
}
