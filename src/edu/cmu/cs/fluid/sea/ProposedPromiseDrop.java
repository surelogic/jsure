package edu.cmu.cs.fluid.sea;

import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents a proposed promise in the sea. A proposed promise indicates a
 * missing portion of a model. Proposed promises are constructed by analyses and
 * used by the tool user interface to help the programmer annotate their code.
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
	 *            contents, the value of this string would be {@code null}.
	 * @param at
	 *            the proposed location for the promise, a declaration.
	 * @param from
	 *            a node within the compilation unit where the analysis deems
	 *            that this proposed promise is needed. This is used to remove
	 *            this proposed promise if the compilation unit is reanalyzed.
	 */
	public ProposedPromiseDrop(String annotation, String contents, IRNode at,
			IRNode from) {
		if (at == null)
			throw new IllegalArgumentException(I18N.err(44, "at"));
		if (from == null)
			throw new IllegalArgumentException(I18N.err(44, "from"));
		if (annotation == null)
			throw new IllegalArgumentException(I18N.err(44, "annotation"));
		f_annotation = annotation;
		f_contents = contents;
		setNode(at);
		dependUponCompilationUnitOf(from);
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

	/**
	 * The contents of the Java annotation being proposed. For
	 * <code>@Starts("nothing")</code> the value of this string would be {@code
	 * "nothing"}. For <code>@Borrowed</code>, which has no contents, the value
	 * of this string would be {@code null}.
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
	 * Gets the contents of the Java annotation being proposed. For
	 * <code>@Starts("nothing")</code> the value of this string would be {@code
	 * "nothing"}. For <code>@Borrowed</code>, which has no contents, the value
	 * of this string would be {@code null}.
	 * 
	 * @return the contents of the Java annotation being proposed, or {code
	 *         null} if none.
	 */
	public String getContents() {
		return f_contents;
	}
}
