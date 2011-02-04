package edu.cmu.cs.fluid.dc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class ToggleAnalyzeAutoAction implements IWorkbenchWindowActionDelegate {
	public ToggleAnalyzeAutoAction() {
		super();
		// No changes here affect the real action
	}

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void selectionChanged(IAction action, ISelection selection) {
		final boolean autoAnalyzeOnBuild = EclipseUtility
				.getBooleanPreference(JSurePreferencesUtility.AUTO_ANALYZE_ON_BUILD);
		action.setChecked(autoAnalyzeOnBuild);
		// System.out.println("Auto-analyze: "+value);
	}

	public void run(IAction action) {
		final boolean autoAnalyzeOnBuild = EclipseUtility
				.getBooleanPreference(JSurePreferencesUtility.AUTO_ANALYZE_ON_BUILD);
		boolean newValue = !autoAnalyzeOnBuild;
		action.setChecked(newValue);
		EclipseUtility.setBooleanPreference(
				JSurePreferencesUtility.AUTO_ANALYZE_ON_BUILD, newValue);
		if (newValue) {
			Majordomo.analyzeNow(true);
		}
	}
}
