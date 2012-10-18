package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNOTATION_TYPE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNO_ATTRS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTENTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_PROJECT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_REF;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_ANNOTATION;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.ORIGIN;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROPOSED_PROMISE_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_ANNO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_ATTRS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_CONTENTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TARGET_PROJECT;

import java.util.Collections;
import java.util.Map;

import com.surelogic.analysis.JavaProjects;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;

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
   * Constructs a new proposed promise. Intended to be called from analysis
   * code.
   * 
   * @param annotation
   *          the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "Starts"}.
   * @param contents
   *          the contents of the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "nothing"}. For <code>@Borrowed</code>, which has no
   *          contents, the value of this string would be {@code null}. The
   *          contents placed into this string should not be escaped. Any
   *          embedded quotations or backward slashes will be escaped before
   *          output.
   * @param attrs
   *          TODO
   * @param replacedAnnotation
   *          the Java annotation being replaced.
   * @param replacedContents
   *          the contents of the Java annotation being replaced. For example,
   *          if the annotation <code>@Starts("nothing")</code> was being
   *          replaced the value of this string would be {@code "nothing"}.
   * @param replacedAttrs
   *          TODO
   * @param at
   *          the proposed location for the promise, a declaration.
   * @param from
   *          a node within the compilation unit where the analysis deems that
   *          this proposed promise is needed. This is used to remove this
   *          proposed promise if the compilation unit is reanalyzed.
   * @param origin
   *          where this proposed promise originated.
   */
  private ProposedPromiseDrop(final String annotation, final String contents, final Map<String, String> attrs,
      final String replacedAnnotation, final String replacedContents, final Map<String, String> replacedAttrs, final IRNode at,
      final IRNode from, final Origin origin) {
    super(at);
    if (from == null) {
      throw new IllegalArgumentException(I18N.err(44, "from"));
    }
    if (annotation == null) {
      throw new IllegalArgumentException(I18N.err(44, "annotation"));
    }

    // TODO can the rest be null?

    f_requestedFrom = from;
    f_annotation = annotation;
    f_contents = contents;
    f_attrs = attrs != null ? attrs : Collections.<String, String> emptyMap();
    f_replacedAnnotation = replacedAnnotation;
    f_replacedContents = replacedContents;
    f_replacedAttrs = replacedAttrs != null ? replacedAttrs : Collections.<String, String> emptyMap();
    f_origin = origin;

    if (contents == null) {
      setMessage(18, annotation);
    } else {
      setMessage(10, annotation, contents);
    }
  }

  /**
   * Constructs a new proposed promise. Intended to be called from analysis
   * code.
   * 
   * @param annotation
   *          the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "Starts"}.
   * @param contents
   *          the contents of the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "nothing"}. For <code>@Borrowed</code>, which has no
   *          contents, the value of this string would be {@code null}. The
   *          contents placed into this string should not be escaped. Any
   *          embedded quotations or backward slashes will be escaped before
   *          output.
   * @param replacedContents
   *          the contents of the Java annotation being replaced.
   * @param at
   *          the proposed location for the promise, a declaration.
   * @param from
   *          a node within the compilation unit where the analysis deems that
   *          this proposed promise is needed. This is used to remove this
   *          proposed promise if the compilation unit is reanalyzed.
   * @param origin
   *          where this proposed promise originated.
   */
  public ProposedPromiseDrop(final String annotation, final String contents, final String replacedContents, final IRNode at,
      final IRNode from, Origin origin) {
    this(annotation, contents, Collections.<String, String> emptyMap(), replacedContents != null ? annotation : null,
        replacedContents, Collections.<String, String> emptyMap(), at, from, origin);
  }

  /**
   * Constructs a new proposed promise. Intended to be called from analysis
   * code.
   * 
   * @param annotation
   *          the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "Starts"}.
   * @param contents
   *          the contents of the Java annotation being proposed. For
   *          <code>@Starts("nothing")</code> the value of this string would be
   *          {@code "nothing"}. For <code>@Borrowed</code>, which has no
   *          contents, the value of this string would be {@code null}. The
   *          contents placed into this string should not be escaped. Any
   *          embedded quotations or backward slashes will be escaped before
   *          output.
   * @param at
   *          the proposed location for the promise, a declaration.
   * @param from
   *          a node within the compilation unit where the analysis deems that
   *          this proposed promise is needed. This is used to remove this
   *          proposed promise if the compilation unit is reanalyzed.
   * @param origin
   *          where this proposed promise originated.
   */
  public ProposedPromiseDrop(final String annotation, final String contents, final IRNode at, final IRNode from, Origin origin) {
    this(annotation, contents, null, at, from, origin);
  }

  // TODO?

  private final Map<String, String> f_attrs;

  private final Map<String, String> f_replacedAttrs;

  /**
   * @return a non-null (possibly empty) map.
   */
  public Map<String, String> getAnnoAttributes() {
    return f_attrs;
  }

  /**
   * @return a non-null (possibly empty) map.
   */
  public Map<String, String> getReplacedAttributes() {
    return f_replacedAttrs;
  }

  /**
   * An indication of how this proposal was generated.
   */
  private final Origin f_origin;

  /**
   * Gets an indication of how this proposal was generated.
   * 
   * @return an indication of how this proposal was generated.
   */
  public Origin getOrigin() {
    return f_origin;
  }

  /**
   * Is this proposed promise inferred from an existing user annotation or
   * model.
   * 
   * @return {@code true} if this proposed promise inferred from an existing
   *         user annotation or model, {@code false} if this proposal was
   *         inferred from code with no model/annotation basis for it whatsoever
   */
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
  private final String f_annotation;

  /**
   * Gets the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "Starts"}.
   * 
   * @return the Java annotation being proposed.
   */
  public String getAnnotation() {
    return f_annotation;
  }

  /**
   * The Java annotation being replaced.
   */
  private final String f_replacedAnnotation;

  /**
   * Gets the Java annotation being replaced.
   * 
   * @return the Java annotation being replaced, may be null.
   */
  public String getReplacedAnnotation() {
    return f_replacedAnnotation;
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
  public IRNode getAssumptionNode() {
    return VisitUtil.getClosestType(f_requestedFrom);
  }

  public IJavaRef getAssumptionRef() {
    final IJavaRef ref = JavaNode.getJavaRef(f_requestedFrom);
    if (ref == null) {
      final IRNode parent = JavaPromise.getParentOrPromisedFor(f_requestedFrom);
      return JavaNode.getJavaRef(parent);
    }
    return ref;
  }

  /**
   * The contents of the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"}. For <code>@Borrowed</code>, which has no contents, the
   * value of this string would be {@code null}.
   * <p>
   * The contents placed into this string should not be escaped. Any embedded
   * quotations or backward slashes will be escaped before output.
   */
  private final String f_contents;

  /**
   * Checks if the proposed Java annotation has contents.
   * 
   * @return {@code true} if the proposed Java annotation has contents,
   *         {@code false} otherwise.
   */
  public boolean hasContents() {
    return f_contents != null;
  }

  /**
   * Gets the raw contents of the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"} (without quotation marks). For <code>@Borrowed</code>,
   * which has no contents, the value of this string would be {@code null}.
   * 
   * @return the contents of the Java annotation being proposed, or {code null}
   *         if none.
   */
  public String getContents() {
    return f_contents;
  }

  /**
   * Gets the escaped contents of the Java annotation being proposed. For
   * <code>@Starts("nothing")</code> the value of this string would be
   * {@code "nothing"}. For <code>@Borrowed</code>, which has no contents, the
   * value of this string would be {@code null}.
   * 
   * @return the contents of the Java annotation being proposed, or {code null}
   *         if none.
   * 
   * @see SLUtility#escapeJavaStringForQuoting(String)
   */
  public String getEscapedContents() {
    return SLUtility.escapeJavaStringForQuoting(f_contents);
  }

  public String getJavaAnnotationNoAtSign() {
    return f_annotation + (f_contents == null ? "" : "(\"" + getEscapedContents() + "\")");
  }

  public String getJavaAnnotation() {
    return "@" + getJavaAnnotationNoAtSign();
  }

  /**
   * The contents of the Java annotation being replaced&mdash;may be null.
   */
  private final String f_replacedContents;

  /**
   * Gets the contents of the Java annotation being replaced. For example, if
   * the annotation <code>@Starts("nothing")</code> was being replaced the value
   * of this string would be {@code "nothing"}.
   * 
   * @return the contents of the Java annotation being replaced&mdash;may be
   *         null.
   */
  public String getReplacedContents() {
    return f_replacedContents;
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
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(JAVA_ANNOTATION, getJavaAnnotation());
    s.addAttribute(ANNOTATION_TYPE, getAnnotation());
    s.addAttribute(CONTENTS, getContents());
    s.addAttribute(REPLACED_ANNO, getReplacedAnnotation());
    s.addAttribute(REPLACED_CONTENTS, getReplacedContents());
    s.addAttribute(ORIGIN, getOrigin().toString());
    s.addAttribute(TARGET_PROJECT, getTargetProjectName());
    s.addAttribute(FROM_PROJECT, getFromProjectName());

    final IJavaRef declRef = JavaNode.getJavaRef(getAssumptionNode());
    final IJavaRef assumeRef = getAssumptionRef();
    final IJavaRef javaRef = declRef != null && declRef.getDeclaration() != null ?
    // TODO to change enum?
    new JavaRef.Builder(assumeRef).setDeclaration(declRef.getDeclaration()).build()
        : assumeRef;
    if (javaRef != null) {
      final String encodedJavaRef = javaRef.encodeForPersistence();
      s.addAttribute(FROM_REF, encodedJavaRef);
    }
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    s.addProperties(db, ANNO_ATTRS, f_attrs);
    s.addProperties(db, REPLACED_ATTRS, f_replacedAttrs);
  }

  public String getTargetProjectName() {
    return JavaProjects.getEnclosingProject(getNode()).getName();
  }

  public String getFromProjectName() {
    return JavaProjects.getEnclosingProject(f_requestedFrom).getName();
  }
}
