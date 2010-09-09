package edu.cmu.cs.fluid.sea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
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
public final class ProposedPromiseDrop extends IRReferenceDrop {

	/**
	 * Constructs a new proposed promise. Intended to be called from analysis
	 * code.
	 * 
	 * @param annotation
	 *            the Java annotation being proposed. For
	 *            <code>@Starts("nothing")</code> the value of this string would
	 *            be {@code "Starts"}.
	 * @param contents
	 *            the contents of the Java annotation being proposed. For
	 *            <code>@Starts("nothing")</code> the value of this string would
	 *            be {@code "nothing"}. For <code>@Borrowed</code>, which has no
	 *            contents, the value of this string would be {@code null}. The
	 *            contents placed into this string should not be escaped. Any
	 *            embedded quotations or backward slashes will be escaped before
	 *            output.
	 * @param at
	 *            the proposed location for the promise, a declaration.
	 * @param from
	 *            a node within the compilation unit where the analysis deems
	 *            that this proposed promise is needed. This is used to remove
	 *            this proposed promise if the compilation unit is reanalyzed.
	 */
	public ProposedPromiseDrop(final String annotation, final String contents,
			final IRNode at, final IRNode from) {
		if (at == null) {
			throw new IllegalArgumentException(I18N.err(44, "at"));
		}
		if (from == null) {
			throw new IllegalArgumentException(I18N.err(44, "from"));
		}
		if (annotation == null) {
			throw new IllegalArgumentException(I18N.err(44, "annotation"));
		}
		f_requestedFrom = from;
		f_annotation = annotation;
		f_contents = contents;
		setNode(at);
		dependUponCompilationUnitOf(from);
		//setMessage("Proposal: @"+annotation+'('+contents+')');
	}

	/**
	 * The Java annotation being proposed. For <code>@Starts("nothing")</code>
	 * the value of this string would be {@code "Starts"}.
	 */
	private final String f_annotation;

	/**
	 * Gets the Java annotation being proposed. For
	 * <code>@Starts("nothing")</code> the value of this string would be {@code
	 * "Starts"}.
	 * 
	 * @return the Java annotation being proposed.
	 */
	public String getAnnotation() {
		return f_annotation;
	}

	private final IRNode f_requestedFrom;

	/**
	 * A node within the compilation unit where the analysis deems that this
	 * proposed promise is needed. This is used to remove this proposed promise
	 * if the compilation unit is reanalyzed.
	 * 
	 */
	public IRNode getRequestedFrom() {
		return f_requestedFrom;
	}

	/**
	 * The enclosing type of the node where the analysis deems that this
	 * proposed promise is needed. This is used to add an {@code Assume} promise
	 * if the SrcRef is not in this project.
	 * 
	 * @return the node where the analysis deems that this proposed promise is
	 *         needed.
	 */
	public IRNode getAssumptionNode() {
		return VisitUtil.getEnclosingType(f_requestedFrom);
	}

	/**
	 * Gets the source reference of the fAST node this information references.
	 * 
	 * @return the source reference of the fAST node this information
	 *         references.
	 */
	public ISrcRef getAssumptionRef() {
		final ISrcRef ref = JavaNode.getSrcRef(f_requestedFrom);
		if (ref == null) {
			final IRNode parent = JavaPromise
					.getParentOrPromisedFor(f_requestedFrom);
			return JavaNode.getSrcRef(parent);
		}
		return ref;
	}

	/**
	 * The contents of the Java annotation being proposed. For
	 * <code>@Starts("nothing")</code> the value of this string would be {@code
	 * "nothing"}. For <code>@Borrowed</code>, which has no contents, the value
	 * of this string would be {@code null}.
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
	 * <code>@Starts("nothing")</code> the value of this string would be {@code
	 * "nothing"} (without quotation marks). For <code>@Borrowed</code>, which
	 * has no contents, the value of this string would be {@code null}.
	 * 
	 * @return the contents of the Java annotation being proposed, or {code
	 *         null} if none.
	 */
	public String getContents() {
		return f_contents;
	}

	/**
	 * Gets the escaped contents of the Java annotation being proposed. For
	 * <code>@Starts("nothing")</code> the value of this string would be {@code
	 * "nothing"}. For <code>@Borrowed</code>, which has no contents, the value
	 * of this string would be {@code null}.
	 * 
	 * @return the contents of the Java annotation being proposed, or {code
	 *         null} if none.
	 * 
	 * @see SLUtility#escapeJavaStringForQuoting(String)
	 */
	public String getEscapedContents() {
		return SLUtility.escapeJavaStringForQuoting(f_contents);
	}

	public String getJavaAnnotationNoAtSign() {
		return f_annotation
				+ (f_contents == null ? "" : "(\"" + getEscapedContents()
						+ "\")");
	}

	public String getJavaAnnotation() {
		return "@" + getJavaAnnotationNoAtSign();
	}

	@Override
	public String toString() {
		return getJavaAnnotation();
	}

	public boolean isSameProposalAs(ProposedPromiseDrop other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (f_annotation == null) {
			if (other.f_annotation != null)
				return false;
		} else if (!f_annotation.equals(other.f_annotation))
			return false;
		if (f_contents == null) {
			if (other.f_contents != null)
				return false;
		} else if (!f_contents.equals(other.f_contents))
			return false;
		if (getNode() == null) {
			if (other.getNode() != null)
				return false;
		} else if (!getNode().equals(other.getNode()))
			return false;
		return true;
	}

	/**
	 * Filters out duplicate proposals so that they are not listed.
	 * <p>
	 * This doesn't handle proposed promises in binary files too well.
	 * 
	 * @param proposals
	 *            the list of proposed promises.
	 * @return the filtered list of proposals.
	 */
	public static List<ProposedPromiseDrop> filterOutDuplicates(
			Collection<ProposedPromiseDrop> proposals) {
		List<ProposedPromiseDrop> result = new ArrayList<ProposedPromiseDrop>();
		for (ProposedPromiseDrop h : proposals) {
			boolean addToResult = true;
			for (ProposedPromiseDrop i : result) {
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
}
