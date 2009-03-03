package edu.cmu.cs.fluid.dc;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.eclipse.jobs.SLUIJob;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.LibResources;

/**
 * Management class for the double-checker nature. This class can configure and
 * unconfigure the nature for a project. The double-checker nature controls the
 * double-checker builder which controls assurance analysis. The double-checker
 * plugin manifest mandates that for a project to be allowed to have the
 * double-checker nature it <i>must</i> have a Java nature as well.
 */
public final class Nature implements IProjectNature {

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
			finishProjectSetup(project);
		}
	}
	
	private static final String[] dialogOptions = {
		IDialogConstants.OK_LABEL,
        "Browse",
        IDialogConstants.CANCEL_LABEL,
	};
	
	private static final String[] existsDialogOptions = {
	    "Browse",
        IDialogConstants.CANCEL_LABEL,
	};
	
	private static void finishProjectSetup(final IProject project) {
		final IJavaProject jp = checkForPromisesJar(project);
		SLUIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (jp != null) {
					IFile f = project.getFile("/promises.jar");
					
					final Shell shell = 
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

					final MessageDialog dialog = 
						new MessageDialog(shell, "Add Promises Jar?", null, 
						                  "Would you like to add promises.jar to the Java build path? "+
						                  "(defaults to the root of "+project.getName()+")", 						                  
						                  MessageDialog.QUESTION, 
						                  f.exists() ? existsDialogOptions : dialogOptions, 0); 
					final int choice = dialog.open();
					final boolean browse, cancel;
					if (f.exists()) {
						browse = (choice == 0);
						cancel = (choice == 1);	
					} else {
						browse = (choice == 1);
						cancel = (choice == 2);	
					}
					if (browse) {
						// Find a new directory to put promises.jar
						final String projDir = project.getLocation().toOSString();
						final DirectoryDialog dd = new DirectoryDialog(shell);
						dd.setText("Choose Directory for Promises Jar");
						dd.setMessage("Choose a directory to create promises.jar");						
						dd.setFilterPath(projDir);						    
						final String dir = dd.open();			
						if (dir.startsWith(projDir)) {
							f = project.getFile(dir.substring(projDir.length())+"/promises.jar");
						} else {
							MessageDialog.openInformation(shell, "Location Not In Project", 
									                      "Cancelling, because"+dir+" is not in "+projDir);
							f = null;
						}
					}
					else if (cancel) {
						f = null;
					}
					if (f != null) {			
						if (!f.exists()) {
							try {
								f.create(LibResources.getPromisesJar(), false, null);

								final IClasspathEntry[] orig = jp.getRawClasspath();
								List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
								for(IClasspathEntry e : orig) {
									entries.add(e);
								}
								entries.add(JavaCore.newLibraryEntry(f.getFullPath(), null, null));
								jp.setRawClasspath(entries.toArray(new IClasspathEntry[orig.length+1]), null);
							} catch (CoreException e) {
								SLLogger.getLogger().log(Level.WARNING, 
										                 "Error while setting up "+f.getFullPath().toOSString(), e);
								MessageDialog.openError(shell, "Error", e.getMessage());
							} catch (IOException e) {
								SLLogger.getLogger().log(Level.WARNING, 
										                 "Error while creating "+f.getFullPath().toOSString(), e);
								MessageDialog.openError(shell, "I/O Error", e.getMessage());
							}
						} else {
							MessageDialog.openInformation(shell, "Promise Jar Already Exists", 
									f.getFullPath().toPortableString()+" already exists");
						}
					}
				} 
				runAnalysis(project);
				return Status.OK_STATUS;
			}

		};
		job.schedule();
	}

	private static IJavaProject checkForPromisesJar(IProject project) {
		IJavaProject p = JavaCore.create(project);
		try {
			if (p.findType("com.surelogic.RegionLock") == null) {
				// Could add promises.jar
				return p;
			}
		} catch (JavaModelException e) {
			// Ignore any exception
		}
		return null;
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

	/**
	 * the Java project this nature is being managed for
	 */
	private IProject project;

	/**
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		if (LOG.isLoggable(Level.FINE))
			LOG.fine("configure() called");
		if (project == null) {
			LOG.log(Level.SEVERE,
					"the project is strangely null -- this should not happen");
			return;
		}
		addBuilderToProject(project);
	}

	/**
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		if (project == null) {
			LOG.log(Level.SEVERE,
					"the project is strangely null -- this should not happen");
			return;
		}
		removeBuilderFromProject(project);
	}

	/**
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("setProject() called for project " + project.getName());
		}
		this.project = project;
	}

	/**
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		if (LOG.isLoggable(Level.FINE))
			LOG.fine("getProject() called");
		return project;
	}

	/**
	 * Checks if a specific builder exists within a project's builder list.
	 * 
	 * @param commands
	 *            a list of builders which we need to check if
	 *            <code>builderId</code> is contained within
	 * @param builderId
	 *            the builder we want to look for within <code>commands</code>
	 * @return <code>true</code> if the builder is listed in
	 *         <code>commands</code>, <code>false</code> otherwise
	 */
	public boolean hasBuilder(ICommand[] commands, String builderId) {
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds the double-checker builder to a project.
	 * 
	 * @param project
	 *            the project to add the double-checker builder to
	 * @throws CoreException
	 *             if we are unable to get a {@link IProjectDescription} for the
	 *             project (which is how project builders are managed)
	 */
	private void addBuilderToProject(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		if (!hasBuilder(commands, DOUBLE_CHECKER_BUILDER_ID)) {
			// add builder to project
			ICommand command = desc.newCommand();
			command.setBuilderName(DOUBLE_CHECKER_BUILDER_ID);
			ICommand[] newCommands = new ICommand[commands.length + 1];
			// Add it at the end of all the other builders (e.g., after Java
			// builder)
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			newCommands[newCommands.length - 1] = command;
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}
	}

	/**
	 * Removes the double-checker builder from a project.
	 * 
	 * @param project
	 *            the project to remove the double-checker builder from
	 * @throws CoreException
	 *             if we are unable to get a {@link IProjectDescription} for the
	 *             project (which is how project builders are managed)
	 */
	private void removeBuilderFromProject(IProject project)
			throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		if (hasBuilder(commands, DOUBLE_CHECKER_BUILDER_ID)) {
			// remove builder from the project
			ICommand[] newCommands = new ICommand[commands.length - 1];
			int newCommandsIndex = 0;
			for (int i = 0; i < commands.length; ++i) {
				if (!commands[i].getBuilderName().equals(
						DOUBLE_CHECKER_BUILDER_ID)) {
					newCommands[newCommandsIndex++] = commands[i];
				}
			}
			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}
	}
}