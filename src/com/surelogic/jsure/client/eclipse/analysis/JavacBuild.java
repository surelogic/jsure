package com.surelogic.jsure.client.eclipse.analysis;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.eclipse.BalloonUtility;
import com.surelogic.common.eclipse.JDTUtility;

public class JavacBuild {
	public static void analyze(List<IJavaProject> selectedProjects) {
		try {
			for(IJavaProject p : selectedProjects) {			
				boolean noErrors = JDTUtility.noCompilationErrors(p, new NullProgressMonitor());		
				if (noErrors) {
					// TODO Collect resources and CUs
					JavacDriver.getInstance().registerBuild(p.getProject(), Collections.EMPTY_MAP, null, null);
				} else {
					// TODO what error to print?
					BalloonUtility.showMessage("Compile Errors in "+p.getElementName(), 
							"JSure is unable to analyze "+p.getElementName()+
							" due to some compilation errors.  Please fix (or do a clean build).");
					return;
				}
			}
			JavacEclipse.initialize();
			System.out.println("Configuring build");	
			JavacDriver.getInstance().configureBuild(Collections.EMPTY_MAP);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}

