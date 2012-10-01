package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNOTATION_TYPE;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.ANNO_ATTRS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTENTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_INFO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_PROJECT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FROM_REF;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_ANNOTATION;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.ORIGIN;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROPOSED_PROMISE_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_ANNO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_ATTRS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.REPLACED_CONTENTS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TARGET_INFO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TARGET_PROJECT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.common.xml.XMLCreator.Builder;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.SeaSnapshot;
import com.surelogic.refactor.IRNodeUtil;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Represents a proposed promise in the sea. A proposed promise indicates a
 * missing portion of a model. Proposed promises are constructed by analyses and
 * used by the tool user interface to help the programmer annotate their code.
 * <p>
 * This drop implements value semantics so that duplicates can be removed by
 * placing them into a set.
 */
public final class ProposedPromiseDrop extends IRReferenceDrop implements IProposedPromiseDrop {

  public enum Origin {
    /**
     * This proposal was inferred from code with no model/annotation basis for
     * it whatsoever.
     */
    CODE,
    /**
     * This proposal was inferred from code and a model. It could be extending
     * or augmenting an existing model based upon the program's implementation.
     */
    MODEL,
    /**
     * This proposed promise was created to help fix a modeling problem.
     */
    PROBLEM
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

    if (XUtil.useExperimental()) {
    	if (contents == null) {
    		setMessage(18, annotation);
    	} else {
    		setMessage(10, annotation, contents);
    	}
    } else {
    	setMessage(10, annotation, contents == null ? "" : contents);
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

  /**
   * Gets the source reference of the fAST node this information references.
   * 
   * @return the source reference of the fAST node this information references.
   */
  public ISrcRef getAssumptionRef() {
    final ISrcRef ref = JavaNode.getSrcRef(f_requestedFrom);
    if (ref == null) {
      final IRNode parent = JavaPromise.getParentOrPromisedFor(f_requestedFrom);
      return JavaNode.getSrcRef(parent);
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

  public boolean isSameProposalAs(IProposedPromiseDrop other) {
    if (this == other)
      return true;
    if (other == null)
      return false;

    return isSame(f_annotation, other.getAnnotation()) && isSame(f_contents, other.getContents())
        && isSame(f_replacedContents, other.getReplacedContents()) && isSame(getSrcRef(), other.getSrcRef());
  }

  private static <T> boolean isSame(T o1, T o2) {
    if (o1 == null) {
      if (o2 != null) {
        return false;
      }
    } else if (!o1.equals(o2)) {
      return false;
    }
    return true;
  }

  public long computeHash() {
    long hash = 0;
    if (f_annotation != null) {
      hash += f_annotation.hashCode();
    }
    if (f_contents != null) {
      hash += f_contents.hashCode();
    }
    final ISrcRef ref = getSrcRef();
    if (ref != null) {
      hash += ref.getHash(); // Instead of hashCode()?
    }
    return hash;
  }

  /**
   * Filters out duplicate proposals so that they are not listed.
   * <p>
   * This doesn't handle proposed promises in binary files too well.
   * 
   * @param proposals
   *          the list of proposed promises.
   * @return the filtered list of proposals.
   */
  public static List<IProposedPromiseDrop> filterOutDuplicates(Collection<IProposedPromiseDrop> proposals) {
    List<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
    // Hash results
    MultiMap<Long, IProposedPromiseDrop> hashed = new MultiHashMap<Long, IProposedPromiseDrop>();
    for (IProposedPromiseDrop info : proposals) {
      long hash = info.computeHash();
      hashed.put(hash, info);
    }
    // Filter each list the old way
    for (Map.Entry<Long, Collection<IProposedPromiseDrop>> e : hashed.entrySet()) {
      result.addAll(filterOutDuplicates_slow(e.getValue()));
    }
    return result;
  }

  // n^2 comparisons
  private static List<IProposedPromiseDrop> filterOutDuplicates_slow(Collection<IProposedPromiseDrop> proposals) {
    List<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
    for (IProposedPromiseDrop h : proposals) {
      boolean addToResult = true;
      for (IProposedPromiseDrop i : result) {
        if (h.isSameProposalAs(i)) {
          addToResult = false;
          break;
        }
      }
      if (addToResult)
        result.add(h);
    }
    return result;
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
  }

  @Override
  public void snapshotRefs(SeaSnapshot s, Builder db) {
    super.snapshotRefs(s, db);
    s.addSrcRef(db, getAssumptionNode(), getAssumptionRef(), FROM_REF);
    s.addJavaDeclInfo(db, FROM_INFO, getFromInfo().snapshot());
    s.addJavaDeclInfo(db, TARGET_INFO, getTargetInfo().snapshot());
    s.addProperties(db, ANNO_ATTRS, f_attrs);
    s.addProperties(db, REPLACED_ATTRS, f_replacedAttrs);
  }

  public String getTargetProjectName() {
    return JavaProjects.getEnclosingProject(getNode()).getName();
  }

  public IJavaDeclaration getTargetInfo() {
    return makeJavaDecl(getNode());
  }

  public String getFromProjectName() {
    return JavaProjects.getEnclosingProject(f_requestedFrom).getName();
  }

  public IJavaDeclaration getFromInfo() {
    return makeJavaDecl(f_requestedFrom);
  }

  private static IJavaDeclaration makeJavaDecl(IRNode node) {
    final IIRProject proj = JavaProjects.getEnclosingProject(node);
    final IBinder b = proj.getTypeEnv().getBinder();
    return IRNodeUtil.convert(b, node);
  }
}
