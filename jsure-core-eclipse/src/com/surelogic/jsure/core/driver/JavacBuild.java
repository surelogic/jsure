package com.surelogic.jsure.core.driver;

import java.util.*;
import java.util.logging.Level;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.Pair;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.*;
import com.surelogic.common.logging.IErrorListener;
import com.surelogic.common.logging.SLLogger;


public class JavacBuild {
	private static List<Pair<IResource, Integer>> pairUpResources(
			List<IResource> resources, Integer kind) {
		if (resources.isEmpty()) {
			return Collections.emptyList();
		}
		List<Pair<IResource, Integer>> result = new ArrayList<Pair<IResource, Integer>>(
				resources.size());
		for (IResource r : resources) {
			result.add(new Pair<IResource, Integer>(r, kind));
		}
		return result;
	}

	private static final Map<?, ?> buildArgs = Collections.singletonMap(
			DriverConstants.BUILD_KIND,
			Integer.toString(IncrementalProjectBuilder.FULL_BUILD));

	public static boolean analyze(List<IJavaProject> selectedProjects,
			IErrorListener l) {
		if (selectedProjects == null || selectedProjects.isEmpty())
			return false;
		try {
			JavacDriver.getInstance().clearProjectInfo();
			
			// Check for errors in the selected projects and dependencies
			try {
				for (IJavaProject p : JDTUtility.getAllRequiredProjects(selectedProjects)) {
					boolean noErrors = XUtil.testing
					|| JDTUtility.noCompilationErrors(p, new NullProgressMonitor());
					if (!noErrors) {
						l.reportError(
								"Compile Errors in " + p.getElementName(),
								"JSure is unable to analyze "
								+ p.getElementName()
								+ " due to some compilation errors.  Please fix (or do a clean build).");
						return false;
					}
				}
			} catch(IllegalStateException e) {
				l.reportError("Error within Eclipse", 
						"JSure is unable to determine if there are any compilation errors, due to problems within Eclipse:\n\t"+e.getMessage());
				return false;
			}
			// Setup project info
			for (IJavaProject p : selectedProjects) {
				// Collect resources and CUs for build
				JavaProjectResources jpr = JDTUtility.collectAllResources(p, null);
				JavacDriver.getInstance().registerBuild(
						p.getProject(),
						buildArgs,
						pairUpResources(jpr.resources,
								IResourceDelta.ADDED), jpr.cus);

			}
			JavacEclipse.initialize();
			SLLogger.getLogger().fine("Configuring explicit build");
			JavacDriver.getInstance().doExplicitBuild(buildArgs, true);
			return true;
		} catch (CoreException e) {
			SLLogger.getLogger().log(Level.SEVERE,
					"Failure setting up to analyze: " + selectedProjects, e);
			return false;
		}
	}
}
