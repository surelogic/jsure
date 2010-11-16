package edu.cmu.cs.fluid.dc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;

//import com.surelogic.jsure.client.eclipse.preferences.PreferenceConstants;

/**
 * Implements a context menu action for IProject and IJavaProject that causes those projects 
 * to be analyzed, if needed
 */
public final class AnalyzeNowAction implements IWorkbenchWindowActionDelegate {
	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void selectionChanged(IAction action, ISelection selection) {
		/* This only sets it the first time the action is shown
		 * 
 		boolean value = !PreferenceConstants.getAutoAnalyzeOnBuild();
		action.setEnabled(value);		
		System.out.println("Analyze now: "+(value ? "enabled" : "disabled"));
		*/
	} 

	public void run(IAction action) {
		Majordomo.analyzeNow(false);
	}
}
