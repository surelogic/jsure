package edu.cmu.cs.fluid.dc;

import java.util.*;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.eclipse.actions.AbstractProjectSelectedMenuAction;
import com.surelogic.common.eclipse.dialogs.JavaProjectSelectionDialog;
import com.surelogic.common.eclipse.preferences.IPreferenceAccessor;
import com.surelogic.jsure.client.eclipse.analysis.*;

public class AnalyzeNow2Action extends AbstractProjectSelectedMenuAction {
	protected JavaProjectSelectionDialog.Config getDialogInfo(List<IJavaProject> selectedProjects) {
		return new JavaProjectSelectionDialog.Config("Select project(s) to analyze", "Analyze Project", 
				null, selectedProjects, new IPreferenceAccessor<Boolean>() {					
					public void set(Boolean newValue) {
						// TODO Auto-generated method stub						
					}					
					public Boolean get() {
						// TODO Auto-generated method stub
						return true;
					}
				});
	}
	
	@Override
	protected void run(List<IJavaProject> selectedProjects,	List<String> projectNames) {
		// TODO Check if this includes dependencies
		// If not, show confirmation dialog
		JavacBuild.analyze(selectedProjects);
	}
}
