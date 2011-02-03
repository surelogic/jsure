package edu.cmu.cs.fluid.dc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

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
		boolean value = JSurePreferencesUtility.getAutoAnalyzeOnBuild();
		action.setChecked(value);
		// System.out.println("Auto-analyze: "+value);
	}

	public void run(IAction action) {
		boolean newValue = !JSurePreferencesUtility.getAutoAnalyzeOnBuild();
		action.setChecked(newValue);
		JSurePreferencesUtility.setAutoAnalyzeOnBuild(newValue);
		if (newValue) {
			Majordomo.analyzeNow(true);
		}
	}
}
