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
			Plugin.getDefault().elog(Plugin.getDefault(), IStatus.ERROR,
					"Unable to get build arguments", ce);
		}
		return null;
	}

	@Override
	protected void doJob(IProgressMonitor monitor) throws CoreException {
		for (String id : Plugin.getDefault().getIncludedExtensions()) {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Activated: " + id);
		}
		if (Plugin.testing) {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Skipping auto-build, due to dc.testing");
		} else {
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Starting first-time auto-build");
			m_project.build(IncrementalProjectBuilder.AUTO_BUILD,
					Nature.DOUBLE_CHECKER_BUILDER_ID, getArguments(), monitor);
			if (LOG.isLoggable(Level.FINE))
				LOG.fine("Ending first-time auto-build");
		}
	}
}