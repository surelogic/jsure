package edu.cmu.cs.fluid.dc;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;

import com.surelogic.jsure.client.eclipse.preferences.PreferenceConstants;

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
		boolean value = PreferenceConstants.getAutoAnalyzeOnBuild();
		action.setChecked(value);
		//System.out.println("Auto-analyze: "+value);			
	} 
	
	public void run(IAction action) {
		  boolean newValue = !PreferenceConstants.getAutoAnalyzeOnBuild();
		  action.setChecked(newValue);
		  PreferenceConstants.setAutoAnalyzeOnBuild(newValue);
		  if (newValue) {
				Majordomo.analyzeNow(true);
		  }
	}
}
