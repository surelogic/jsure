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

import java.util.Collections;
import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.RequiresLock;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.IJavaRef.Position;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.dropsea.irfree.XmlCreator;
import com.surelogic.dropsea.irfree.XmlCreator.Builder;

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

  /**
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
  public ProposedPromiseDrop(@NonNull String annotation, @Nullable String value,
      @Nullable Map<String, String> attributeNameToValue, @Nullable String replacedAnnotation, @Nullable String replacedValue,
      @Nullable Map<String, String> replacedAttributeNameToValue, @NonNull IRNode at, @NonNull IRNode from, @NonNull Origin origin) {
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

    if (value == null) {
      setMessage(18, annotation);
    } else {
      setMessage(10, annotation, value);
    }
  }

  /**
   * Constructs a new proposed promise to replace an existing promise that has
   * the same annotation.
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
   * @param replacedValue
   *          the value of the Java annotation being replaced. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "nothing"}. For <code>@Borrowed</code>, which has no
   *          contents, the value of this string would be {@code null}. If this
   *          is non-null <tt>annotation</tt> is assumed as the replaced
   *          promise.
   * @param at
   *          the proposed location for the promise, a declaration.
   * @param from
   *          a node within the compilation unit where the analysis deems that
   *          this proposed promise is needed. This is used to remove this
   *          proposed promise if the compilation unit is reanalyzed.
   * @param origin
   *          an indication of how this proposal was generated.
   */
  public ProposedPromiseDrop(@NonNull String annotation, @Nullable String value, @Nullable String replacedValue,
      @NonNull IRNode at, @NonNull IRNode from, @NonNull Origin origin) {
    this(annotation, value, Collections.<String, String> emptyMap(), replacedValue != null ? annotation : null, replacedValue,
        Collections.<String, String> emptyMap(), at, from, origin);
  }

  /**
   * Constructs a new proposed promise that does not replace an existing
   * promise.
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
   * @param at
   *          the proposed location for the promise, a declaration.
   * @param from
   *          a node within the compilation unit where the analysis deems that
   *          this proposed promise is needed. This is used to remove this
   *          proposed promise if the compilation unit is reanalyzed.
   * @param origin
   *          an indication of how this proposal was generated.
   */
  public ProposedPromiseDrop(@NonNull String annotation, @Nullable String value, @NonNull IRNode at, @NonNull final IRNode from,
      @NonNull Origin origin) {
    this(annotation, value, null, at, from, origin);
  }

  /**
   * An indication of how this proposal was generated.
   */
  @NonNull
  private final Origin f_origin;

  @NonNull
  public Origin getOrigin() {
    return f_origin;
  }

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

  @NonNull
  public Map<String, String> getAttributes() {
    return f_attributeNameToValue;
  }

  @NonNull
  public String getJavaAnnotationNoAtSign() {
    return f_annotation + (f_value == null ? "" : "(\"" + getEscapedValue() + "\")");
  }

  @NonNull
  public String getJavaAnnotation() {
    return "@" + getJavaAnnotationNoAtSign();
  }

  /**
   * The Java annotation being replaced. Similar to {@link #f_annotation}.
   */
  @Nullable
  private final String f_replacedAnnotation;

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
  public void snapshotRefs(SeaSnapshot s, Builder db) {
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
      return new Pair<IJavaRef, IRNode>(builder.build(), superRefAndNode.second());
    } else
      return superRefAndNode;
  }
}
