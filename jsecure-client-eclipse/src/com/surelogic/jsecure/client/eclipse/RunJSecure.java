package com.surelogic.jsecure.client.eclipse;

import java.util.*;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.java.*;
import com.surelogic.common.ui.BalloonUtility;
import com.surelogic.javac.*;
import com.surelogic.jsure.core.JSecureDriver;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class RunJSecure implements IViewActionDelegate {
	public void run(IAction action) {		
		try {
			Projects projects = JSureDataDirHub.getInstance().getCurrentScan().getProjects();
			List<IJavaProject> selectedProjects = new ArrayList<IJavaProject>();
			for(JavacProject o : projects) {
				IJavaProject jp = JDTUtility.getJavaProject(o.getName());
				if (jp != null) {
					selectedProjects.add(jp);
				}
			}
			JSecureDriver driver = new JSecureDriver();
			JavaBuild.analyze(driver, selectedProjects, BalloonUtility.errorListener);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	public void init(IViewPart view) {
		// TODO Auto-generated method stub

	}
}
