package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * Implements a context menu action for IProject and IJavaProject that turns off
 * automatic analysis
 */
public final class AnalyzeManualAction extends SelectedProjectsAction {
	@Override
	protected boolean doRun(IProject current) {
		EclipseUtility.setBooleanPreference(
				JSurePreferencesUtility.AUTO_ANALYZE_ON_BUILD, false);
		return true;
	}
}
