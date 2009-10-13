package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

public abstract class AbstractResultsViewContentProvider implements
		IResultsViewContentProvider {
	protected static final Logger LOG = SLLogger
			.getLogger("ResultsViewContentProvider");

	private boolean m_showInferences = true;

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
}
