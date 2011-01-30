package com.surelogic.jsure.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 * Should be changed to IApplication for Eclipse 3.3
 */
public class ImportProjectsPlatformRunnable implements IPlatformRunnable,
		ILogListener {

	/**
	 * Logger for this class
	 */
	private static BufferedWriter out = null;
	private static ILog ilog;
	private static final File logfile = new File(
			System.getProperty("user.home"), "SLRegressionTestEPlugin.log");

	private final static String TEST_DIR_FLAG = "-testProjectDir";

	private final static String PR_NAME = "ImportProjectsPlatformRunnable";

	private IProjectDescription description = null;

	private IWorkspace workspace = null;

	private FileFilter projectFilter = new FileFilter() {
		// Only accept those files that are .project
		public boolean accept(File pathName) {
			return pathName.getName().equals(
					IProjectDescription.DESCRIPTION_FILE_NAME);
		}
	};

	public ImportProjectsPlatformRunnable() {
		ilog = Activator.getILog();
		ilog.addLogListener(this);
		try {
			logfile.createNewFile();
			ilog.log(new Status(IStatus.ERROR, "SL Eclipse Ant",
					"SL Plugin starting..."));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Object run(Object args) throws Exception {

		String[] strArgs = (String[]) args;

		System.err.println("# of args: " + strArgs.length);
		for (int i = 0, len = strArgs.length; i < len; i++) {
			String string = strArgs[i];
			System.err.println("i= " + strArgs[i]);
			ilog.log(new Status(IStatus.WARNING, PR_NAME, "i= " + strArgs[i]));
			// create workspace
			if (TEST_DIR_FLAG.equalsIgnoreCase(string)
					&& strArgs.length >= i + 1) {
				for (int j = i + 1; j < len; j++) {
					System.err.println("j= " + strArgs[j]);
					ilog.log(new Status(IStatus.WARNING, PR_NAME, "j= "
							+ strArgs[j]));
					try {
						if (importProject(strArgs[j]) == EXIT_RESTART) {
							out.close();
							return EXIT_RESTART;
						}
					} catch (FileNotFoundException e) {
						out.close();
						return EXIT_RESTART;
					}
				}
				break;
			}
		}
		out.close();
		return EXIT_OK;
	}

	private Object importProject(String projectDir) throws IOException {

		ilog.log(new Status(IStatus.WARNING, "SL Eclipse Ant", "importing: "
				+ projectDir));

		// The parent directory of where we want to store our workspaces
		String project = null;
		IWorkspaceRoot wsRoot = null;
		IProject proj = null;
		String path = null;
		File file = null;
		// Change to that workspace and import the respective project
		if (projectDir != null) {
			ilog.log(new Status(IStatus.WARNING, PR_NAME, "Testing DIR: "
					+ projectDir));

			System.out.println("Testing DIR: " + projectDir);
			file = new File(projectDir);

			project = file.getName();

			workspace = ResourcesPlugin.getWorkspace();
			wsRoot = workspace.getRoot();

			setProjectName(projectFile(file));
			System.out.println("description: "
					+ description.getLocationURI().getPath());
			ilog.log(new Status(IStatus.WARNING, PR_NAME, "description: "
					+ description.getLocationURI().getPath()));
			if (description != null) {
				proj = wsRoot.getProject(description.getName());
			} else {
				proj = wsRoot.getProject(project);
			}

			if (!proj.exists()) {
				try {
					if (description == null) {
						System.out.println("creating new description: " + path);
						ilog.log(new Status(IStatus.WARNING, PR_NAME,
								"creating new description @: " + path));
						description = workspace.newProjectDescription(project);
						description.setLocation(new Path(path));
					}

					ilog.log(new Status(IStatus.WARNING, PR_NAME,
							"creating project"));
					System.out.println("creating project: ");
					proj.create(description, null);
					ilog.log(new Status(IStatus.WARNING, PR_NAME,
							"opening project"));
					System.out.println("opening project: ");
					proj.open(null);
				} catch (CoreException e) {
					ilog.log(new Status(IStatus.ERROR, PR_NAME,
							"The test directory '" + projectDir
									+ "' does not contain a project", e));
				}
			}
		} else {
			ilog.log(new Status(IStatus.WARNING, PR_NAME,
					"The test directory '" + projectDir
							+ "' does not contain a project"));
			return EXIT_RESTART;
		}
		return EXIT_OK;
	}

	/**
	 * Return a .project file from the specified location. If there isn't one
	 * return null.
	 */
	private File projectFile(File directory) {
		System.out.println("projectFile(File): " + directory.getAbsolutePath());
		File ret = null;
		if (directory.isDirectory()) {
			File[] files = directory.listFiles(this.projectFilter);
			if (files != null && files.length == 1) {
				ret = files[0];
			}
		}
		System.out.println("Found .project File: " + ret.getAbsolutePath());
		return ret;
	}

	/**
	 * Set the project name using either the name of the parent of the file or
	 * the name entry in the XML for the file.
	 */
	private void setProjectName(File projectFile) {
		System.out
				.println("setProjectName(File) - ProjectFile:  - projectFile="
						+ projectFile.getAbsolutePath());

		// If there is no file or the user has already specified forget it
		if (projectFile != null) {
			IPath path = new Path(projectFile.getPath());

			IProjectDescription newDescription = null;

			try {
				newDescription = workspace.loadProjectDescription(path);
			} catch (CoreException exception) {
				// no good couldn't get the name
			}

			if (newDescription == null) {
				this.description = null;
			} else {
				this.description = newDescription;
			}
		}
	}

	public void logging(IStatus status, String plugin) {
		try {
			if (out == null) {
				out = new BufferedWriter(new FileWriter(logfile, true));
			}
			out.append(status.getMessage());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
