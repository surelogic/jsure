package edu.cmu.cs.fluid.dc;

import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.surelogic.jsure.client.eclipse.Activator;

/**
 * This class invokes an initial build on the project when the double-checker
 * nature and builder are added to a project, during workbench startup, and when
 * a project is opened within the workbench.
 * 
 * implements IRunnableWithProgress
 */
public final class FirstTimeAnalysis extends FirstTimeJob {
	public FirstTimeAnalysis(IProject project) {
		super("JSure Assurance Tool", project);
		// new Throwable("For stack trace").printStackTrace();
	}

	@SuppressWarnings("unchecked")
	private Map getArguments() {
		try {
			IProjectDescription desc = m_project.getDescription();
			ICommand[] commands = desc.getBuildSpec();

			for (int i = 0; i < commands.length; i++) {
				if (commands[i].getBuilderName().equals(
						Nature.DOUBLE_CHECKER_BUILDER_ID)) {
					return commands[i].getArguments();
				}
			}
		} catch (CoreException ce) {
			Activator.getDefault().getDoubleChecker().elog(Activator.getDefault(), IStatus.ERROR,
					"Unable to get build arguments", ce);
		}
		return null;
	}

	@Override
	protected void doJob(IProgressMonitor monitor) throws CoreException {
		for (String id : Activator.getDefault().getDoubleChecker().getIncludedExtensions()) {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Activated: " + id);
		}
		if (Plugin.testing) {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Skipping auto-build, due to dc.testing");
		} else {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Starting first-time auto-build");
			
			final IJavaProject javaProject = JavaCore.create(m_project);
			final int flag;
			if (Majordomo.noCompilationErrors(javaProject)) {
				flag = IncrementalProjectBuilder.AUTO_BUILD;
			} else {
				flag = IncrementalProjectBuilder.CLEAN_BUILD;
				LOG.info("Trying to do clean build");
			}
			m_project.build(flag, Nature.DOUBLE_CHECKER_BUILDER_ID, getArguments(), monitor);
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Ending first-time auto-build");
		}
	}
}