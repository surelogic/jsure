package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

public abstract class AbstractResultsViewContentProvider implements
		IResultsViewContentProvider {
	protected static final Logger LOG = SLLogger
			.getLogger("ResultsViewContentProvider");

	protected boolean m_showInferences = true;

	private static String f_modelingProblemsHintMessage = "";

	/**
	 * @return Returns the showInferences.
	 */
	public final boolean isShowInferences() {
		return m_showInferences;
	}

	/**
	 * @param showInferences
	 *            The showInferences to set.
	 */
	public final void setShowInferences(boolean showInferences) {
		this.m_showInferences = showInferences;
	}

	/**
	 * Gets the hint if the user needs to look at modeling problems or not.
	 * 
	 * @return the non-null hint if the user needs to look at modeling problems
	 *         or not.
	 */
	public final String getModelingProblemsHintMessage() {
		return f_modelingProblemsHintMessage;
	}

	/**
	 * Sets the hint if the user needs to look at modeling problems or not.
	 * 
	 * @param value
	 *            a hint if the user needs to look at modeling problems or not.
	 *            If this parameter is {@code null} the hint will be set to the
	 *            empty string.
	 */
	protected final void setModelingProblemsHintMessage(final String value) {
		if (value != null) {
			f_modelingProblemsHintMessage = value;
		} else {
			f_modelingProblemsHintMessage = ""; // none
		}
	}
}
