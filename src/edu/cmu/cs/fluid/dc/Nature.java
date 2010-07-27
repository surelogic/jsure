package edu.cmu.cs.fluid.dc;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.builder.AbstractNature;
import com.surelogic.common.jobs.*;
import com.surelogic.common.logging.SLLogger;

/**
 * Management class for the double-checker nature. This class can configure and
 * unconfigure the nature for a project. The double-checker nature controls the
 * double-checker builder which controls assurance analysis. The double-checker
 * plugin manifest mandates that for a project to be allowed to have the
 * double-checker nature it <i>must</i> have a Java nature as well.
 */
public final class Nature extends AbstractNature {

	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	/**
	 * The double-checker builder identifier (<i>must</i> match the plugin
	 * manifest)
	 */
	public static final String DOUBLE_CHECKER_BUILDER_ID = "com.surelogic.jsure.client.eclipse.dcBuilder";

	/**
	 * The double-checker nature identifier (<i>must</i> match the plugin
	 * manifest)
	 */
	public static final String DOUBLE_CHECKER_NATURE_ID = "com.surelogic.jsure.client.eclipse.dcNature";

	// private static final String PROMISES_JAR = "promises.jar";

	// private static final String PROMISES_JAVADOC_JAR =
	// "promises-javadoc.jar";

	// private static final boolean useSeparateJavadocJar = false;

	public Nature() {
		super(DOUBLE_CHECKER_BUILDER_ID);
	}

	/**
	 * Checks if the double-checker nature is set for a given project.
	 * 
	 * @param project
	 *            the project to check
	 */
	public static boolean hasNature(IProject project) {
		boolean result = false; // assume it doesn't
		try {
			result = project.hasNature(Nature.DOUBLE_CHECKER_NATURE_ID);
		} catch (CoreException e) {
			LOG.log(Level.WARNING,
					"check for double-checker nature on project "
							+ project.getName() + " failed", e);
		}
		return result;
	}

	/**
	 * Checks if any open project has the double-checker nature.
	 * 
	 * @return {@code true} if an open project has the double-checker nature,
	 *         {@code false} otherwise.
	 */
	public static boolean hasNatureAnyProject() {
		for (IJavaProject jp : JDTUtility.getJavaProjects()) {
		  final IProject project = jp.getProject();
			if (project.isOpen() && hasNature(project))
				return true;
		}
		return false;
	}

	/**
	 * Adds the double-checker nature to a project.
	 * 
	 * @param project
	 *            the project to add the double-checker nature to
	 * @throws CoreException
	 *             if we are unable to get a {@link IProjectDescription} for the
	 *             project (which is how project natures are managed)
	 */
	public static void addNatureToProject(final IProject project)
			throws CoreException {
		IProjectDescription description = project.getDescription();
		boolean hasQualityNature = description.hasNature(DOUBLE_CHECKER_NATURE_ID);
		if (!hasQualityNature) {
			PromisesJarUtility.finishProjectSetup(project, false, 
			  new AbstractSLJob("Adding JSure Nature to "+project.getName()) {
				public SLStatus run(SLProgressMonitor monitor) {
					try {
						onlyAddNatureToProject(project);
					} catch (CoreException e) {
						String msg = "Could not add JSure nature to "+project.getName();
						SLLogger.getLogger().log(Level.SEVERE, msg, e);
						return SLStatus.createErrorStatus(msg, e);
					}
					return SLStatus.OK_STATUS;
				}			
			});
		}
	}
	
	public static void onlyAddNatureToProject(final IProject project) throws CoreException {
		// add our nature to the project if it doesn't already exist
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		boolean hasQualityNature = description
				.hasNature(DOUBLE_CHECKER_NATURE_ID);
		if (!hasQualityNature) {
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = DOUBLE_CHECKER_NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);			
		}
	}

	public static void runAnalysis(IProject project) {
		// perform initial analysis
		new FirstTimeAnalysis(project).schedule();
	}

	/**
	 * Removes the double-checker nature from a project.
	 * 
	 * @param project
	 *            the project to remove the double-checker nature from
	 * @throws CoreException
	 *             if we are unable to get a {@link IProjectDescription} for the
	 *             project (which is how project natures are managed)
	 */
	static public void removeNatureFromProject(IProject project)
			throws CoreException {
		// remove our nature from the project if it exists
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		boolean hasQualityNature = description
				.hasNature(DOUBLE_CHECKER_NATURE_ID);
		if (hasQualityNature) {
			String[] newNatures = new String[natures.length - 1];
			int newNatureIndex = 0;
			for (int i = 0; i < natures.length; ++i) {
				if (!natures[i].equals(DOUBLE_CHECKER_NATURE_ID)) {
					newNatures[newNatureIndex++] = natures[i];
				}
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
	}
}