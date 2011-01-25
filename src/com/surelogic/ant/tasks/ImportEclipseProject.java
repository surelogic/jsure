package com.surelogic.ant.tasks;

import java.io.File;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * This task will import all of the projects contained in a give directory into
 * eclipse workspaces.
 * <p>
 * Ant usage:
 * <p>
 * Required parameters:
 * <ul>
 * <li>projectparentdir - The directory containing the project directories to
 * import
 * <li>workspacedir - The directory where the projects' workspaces should be
 * created
 * </ul>
 * Optional parameters:
 * <ul>
 * <li>exceptions - A comma-separated list of project names to skip
 * </ul>
 */
public class ImportEclipseProject extends Task {

	private final static String TEST_DIR_FLAG = "-testProjectDir";

	private static String[] ignorable = null;

	private File projectsParentDir = null;

	private File workspacesDir;

	private String exceptions = null;

	private File startupJar = null;

	private String extraProjects = null;

	private List<String> extraProjs = new ArrayList<String>();

	/**
	 * Filters out directories
	 */
	private DirectoryFilter directoryFilter;

	@Override
	public void execute() throws BuildException {
		paramsAreValid();
		importTestProjects();
	}

	/**
	 * Checks to see if the user entered valid values for the parameters
	 */
	private void paramsAreValid() throws BuildException {
		if (projectsParentDir == null) {
			throw new BuildException(
					"The testProjectsParentDir parameter was not set.");
		} else if (!projectsParentDir.isDirectory()) {
			throw new BuildException(
					"The testProjectsParentDir is not a valid directory.");
		} else if (workspacesDir == null) {
			throw new BuildException(
					"The workspacesParentDir parameter was not set.");
		} else if (!workspacesDir.isDirectory()) {
			throw new BuildException(
					"The workspacesParentDir is not a valid directory.");
		} else if (startupJar == null || !startupJar.exists()
				|| !startupJar.isFile()) {
			throw new BuildException("The startupJar value is not valid");
		}
	}

	/**
	 * Import every project found in the testProjectsParentDir directory into
	 * Eclipse after making a workspace for it.
	 */
	private void importTestProjects() throws BuildException {
		List<String> args = new ArrayList<String>(9 + extraProjs.size());
		args.add("java");
		args.add("-jar");
		args.add(startupJar.getAbsolutePath());
		args.add("-data");
		// the location of the workspace that will contain the current project
		args.add("");
		args.add("-debug");
		args.add("-application");
		args.add("com.surelogic.ant.eclipse.plugin.ProjectImporter");
		args.add(TEST_DIR_FLAG);
		args.add("");
		args.addAll(extraProjs);

		File[] projects = getAllTestProjectDirectories(projectsParentDir
				.getAbsolutePath());
		try {
			for (File file : projects) {
				if (file.isDirectory()) {
					// Only perform the necessary tasks if the name of the
					// workspace is not
					// in the array of ignorable strings
					if (Arrays.binarySearch(ignorable, file.getName()) < 0) {
						File workspace = new File(workspacesDir, file.getName());
						getProject();
						log("Creating " + workspace.getAbsolutePath(),
								Project.MSG_VERBOSE);
						workspace.mkdirs();
						args.set(4, workspace.getAbsolutePath());
						args.set(9, file.getAbsolutePath());

						ProcessBuilder builder = new ProcessBuilder(args);
						builder.directory(new File(getProject().getProperty(
								"eclipse.dir")));
						// builder = builder.redirectErrorStream(true);

						getProject();
						log("Starting process in "
								+ builder.directory().getAbsolutePath(),
								Project.MSG_VERBOSE);

						Process proc = builder.start();
						getProject();
						log("Process started", Project.MSG_VERBOSE);
						getProject();
						/*
						 * BufferedInputStream procIn = new
						 * BufferedInputStream(proc.getInputStream()); byte[]
						 * buf = new byte[128]; StringBuilder sb = new
						 * StringBuilder(); int numRead = 0;
						 * while(procIn.available() > 0 && numRead != -1){
						 * log("Reading process output",
						 * getProject().MSG_VERBOSE); numRead =
						 * procIn.read(buf); sb.append(buf); }
						 */
						log("Done reading", Project.MSG_VERBOSE);
						// log(sb.toString(), getProject().MSG_VERBOSE);
						proc.waitFor();
					} else {
						getProject();
						log("Ignoring " + file.getAbsolutePath(),
								Project.MSG_VERBOSE);
					}
				}

			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new BuildException(
					"Error running ImportEclipseProject applicaton", e1);
		}
	}

	private File[] getAllTestProjectDirectories(String parent) {
		File parentDir = new File(parent);
		File[] files = null;

		if (parentDir.isDirectory()) {
			files = parentDir.listFiles(this.directoryFilter);
		}
		return files;
	}

	/**
	 * Getters and Setters per Ant's requirements
	 */
	public File getProjectsParentDir() {
		return projectsParentDir;
	}

	public void setProjectsParentDir(File testProjectsParentDir) {
		this.projectsParentDir = testProjectsParentDir;
	}

	public File getWorkspacesDir() {
		return workspacesDir;
	}

	public void setWorkspacesDir(File workspacesDir) {
		this.workspacesDir = workspacesDir;
	}

	public String getExceptions() {
		return exceptions;
	}

	public void setExceptions(String exceptions) {
		this.exceptions = exceptions;
		ignorable = null;

		StringBuffer exc = new StringBuffer("CVS,.svn,").append(exceptions);
		ignorable = exc.toString().split("\\s*,\\s*");
		Arrays.sort(ignorable);
	}

	/**
	 * @return the startupJar
	 */
	public final File getStartupJar() {
		return startupJar;
	}

	/**
	 * @param startupJar
	 *            the startupJar to set
	 */
	public final void setStartupJar(File startupJar) {
		this.startupJar = startupJar;
	}

	/**
	 * @return the extraProjects
	 */
	public final String getExtraProjects() {
		return extraProjects;
	}

	/**
	 * @param extraProjects
	 *            the extraProjects to set
	 */
	public final void setExtraProjects(String extraProjects) {
		this.extraProjects = extraProjects;
		String[] list = extraProjects.split("\\s*,\\s*");

		for (String string : list) {
			File tmp = new File(string);
			if (tmp.isDirectory()) {
				extraProjs.add(string);
			} else {
				throw new BuildException(
						string
								+ " is not a valid project directory. Make sure you enter the absolute classpath");
			}
		}
	}
}
