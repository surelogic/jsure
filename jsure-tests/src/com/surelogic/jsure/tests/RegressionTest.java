package com.surelogic.jsure.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.logging.IErrorListener;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.dropsea.irfree.ISeaDiff;
import com.surelogic.dropsea.irfree.SeaSnapshotDiff;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.core.driver.ConsistencyListener;
import com.surelogic.jsure.core.driver.DoubleChecker;
import com.surelogic.jsure.core.driver.JavacBuild;
import com.surelogic.jsure.core.driver.JavacEclipse;
import com.surelogic.jsure.core.listeners.IAnalysisListener;
import com.surelogic.jsure.core.listeners.NotificationHub;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scripting.ScriptCommands;
import com.surelogic.jsure.core.scripting.ScriptReader;
import com.surelogic.test.ITest;
import com.surelogic.test.ITestOutput;
import com.surelogic.test.xml.JUnitXMLOutput;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.logging.XMLLogDiff;

public class RegressionTest extends TestCase implements IAnalysisListener {

	private boolean f_initNeeded = true;

	@Override
	protected void setUp() throws Exception {
		// TODO Set to property value, or default (~/.m2/repository)
		String repo = System.getProperty("user.home")+"/.m2/repository";
		JavaCore.setClasspathVariable("M2_REPO", new Path(repo), null);
		if (f_initNeeded) {
			f_initNeeded = true;
			Eclipse.getDefault().addTestOutputFactory(JUnitXMLOutput.factory);
			JavacEclipse.getDefault().addTestOutputFactory(
					JUnitXMLOutput.factory);
			System.out.println("Added JUnitXMLOutput.factory");

			final String testModulePath = System.getProperty("test.module");
			if (testModulePath == null)
				fail("'-Dtest.module=' was not set prior to invoking a regression test");

			final File testModule;
			// Needed to process scripted/zipped tests when run manually
			if (testModulePath.endsWith(".zip")) {
				// Clear out all old temporary directories
				final File tempDir = File.createTempFile("testModule", ".dir");
				for (File f : tempDir.getParentFile().listFiles()) {
					if (f.getName().startsWith("testModule")
							&& f.getName().endsWith(".dir")) {
						f.delete();
					}
				}
				final int lastSlash = testModulePath.lastIndexOf('/');
				final File tempMod = new File(tempDir, testModulePath.substring(
						lastSlash + 1, testModulePath.length() - 4));
				tempMod.mkdirs();
				FileUtility.unzipFile(new File(testModulePath), tempMod);
				testModule = tempMod;
			} else {
				testModule = new File(testModulePath);
			}
			if (!testModule.isDirectory())
				fail("'-Dtest.module=" + testModulePath
						+ "' does not reference a directory");
			
			setupClasspath(testModule);
			setupPreferences(testModule);
			importProject(testModule);
		}
	}

	static final String CLASSPATH_VARS = "classpath.properties";
	static final String JSURE_PREFS = "jsure.pref.properties";
	
	private Properties loadProperties(File root, String name) {
		File f = new File(root, root.getName()+'/'+name);
		if (!f.isFile()) {
			System.err.println("Couldn't find "+f);
			f = new File(root, name);
		}
		if (f.isFile()) {
			Properties p = new Properties();
			try {
				p.load(new FileInputStream(f));				
				return p;
			} catch (IOException e) {
				System.err.println("Couldn't get properties from "+f);
				e.printStackTrace();
			}
		} else {
			System.err.println("Couldn't find "+f);
		}
		return null;
	}
	
	private void setupClasspath(File testModule) {
		final Properties p = loadProperties(testModule, CLASSPATH_VARS);
		if (p != null) {
			for(Map.Entry<Object, Object> e : p.entrySet()) {
				final File path = new File(testModule, e.getValue().toString());
				try {			
					JavaCore.setClasspathVariable(e.getKey().toString(), new Path(path.getAbsolutePath()), null);
					System.out.println("Set classpath variable "+e.getKey()+" to "+path);
				} catch (JavaModelException e1) {
					System.err.println("Couldn't set "+e.getKey()+" to "+path);
					e1.printStackTrace();
				}
			}
		}
	}

	private void setupPreferences(File testModule) {
		final Properties p = loadProperties(testModule, JSURE_PREFS);
		if (p != null) {
			for(Map.Entry<Object, Object> e : p.entrySet()) {
				final String key = e.getKey().toString();
				final String value = e.getValue().toString();
				if ("true".equals(value) || "false".equals(value)) {
					EclipseUtility.setBooleanPreference(key, Boolean.parseBoolean(value));
				} else {
					try {
						int i = Integer.parseInt(value);
						EclipseUtility.setIntPreference(key, i);
					} catch(NumberFormatException ex) {
						EclipseUtility.setStringPreference(key, value);
					}					
				}
				System.out.println("Set pref "+key+" to "+value);
			}
		}
	}
	
	/**
	 * Change to check for a .project file If not present, try to import
	 * immediate sub-directories
	 */
	private void importProject(final File projectDir) {
		if (FileUtility.JSURE_DATA_PATH_FRAGMENT.equals(projectDir.getName())) {
			return; // It won't be in the jsure-data dir
		}
		
		// check for a .project file
		final File dotProjectFile = new File(projectDir, ".project");
		if (!dotProjectFile.exists()) {
			/*
			 * The .project file is not present so so assume it to be a
			 * multi-project container. We'll try to import each immediate
			 * sub-directories.
			 */
			System.out.println("No .project file found within " + projectDir
					+ "; trying to find subprojects...");
			for (File f : projectDir.listFiles()) {
				if (f.isDirectory()) {
					importProject(f);
				}
			}
			return;
		}
		System.out.println("Found .project file within " + projectDir
				+ "; trying to create an Eclipse project");

		initAnalyses(projectDir);

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription description = getProjectDescription(workspace,
				dotProjectFile);
		final IWorkspaceRoot wsRoot = workspace.getRoot();
		System.out.println("Workspace root: "
				+ wsRoot.getLocationURI().toString());

		if (description == null)
			fail("Unable to obtain a valid Eclipse project description for "
					+ projectDir);

		final IProject proj = wsRoot.getProject(description.getName());

		if (!proj.exists()) {
			try {
				// if (description == null) {
				// description = workspace.newProjectDescription(projectName);
				// description.setLocation(new Path(projectDir
				// .getAbsolutePath()));
				// }

				System.out
						.println("Creating Eclipse project:" + proj.getName());
				proj.create(description, null);
			} catch (CoreException e) {
				fail(e.getMessage());
			}
		}
		// Project should exist now
		if (proj.exists()) {
			final IResource res = proj.findMember(".projectStatus");
			if (res == null) {
				// do the default thing of opening the project
				try {
					System.out.println("Opening Eclipse project: "
							+ proj.getName());
					proj.open(null);
				} catch (CoreException e) {
					fail(e.getMessage());
				}
			}
		}
	}

	private void initAnalyses(File project) {
		printActivatedAnalyses();

		final File analysisSettingsFile = findFile(project,
				ScriptCommands.ANALYSIS_SETTINGS, true);
		if (analysisSettingsFile != null && analysisSettingsFile.exists() && analysisSettingsFile.isFile()) {
			System.out.println("Found project-specific analysis settings.");
			JSureAnalysisXMLReader.readStateFrom(analysisSettingsFile);
			DoubleChecker.getDefault().initAnalyses();

			if (IDE.useJavac) {
				System.out
						.println("Configuring analyses from project-specific settings in "
								+ analysisSettingsFile.getAbsolutePath());
				JavacEclipse.initialize();
				((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs();
			}
		} else {
			System.out.println("No project-specific analysis settings.");
		}
	}

	private IProjectDescription getProjectDescription(IWorkspace workspace,
			File dotProjectFile) {
		IPath path = new Path(dotProjectFile.getPath());
		try {
			return workspace.loadProjectDescription(path);
		} catch (CoreException exception) {
			fail(exception.getMessage());
		}
		return null; // should never get here due to the fail above
	}

	@Override
	protected void tearDown() throws Exception {
		// nothing to do
	}

	void closeAllProjects(IWorkspaceRoot root) throws CoreException {
		for (IProject p : root.getProjects()) {
			if (p.isOpen()) {
				p.close(null);
			}
		}
	}

	void createAndOpenProject(IWorkspaceRoot root, String name)
			throws CoreException {
		IProject project = root.getProject(name);
		if (!project.exists()) {
			project.create(null);
			System.out.println("Creating a project: " + name);
		} else {
			System.out.println("Already created:    " + name);
		}

		// Open and then stop processing
		if (!project.isOpen()) {
			project.open(null);
			System.out.println("Opening a project:  " + name);
		} else {
			System.out.println("Already opened:     " + name);
		}
	}

	public void testMajordomo() throws Throwable {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		String workspacePath = root.getLocation().toOSString();
		File workspaceFile = new File(workspacePath);
		System.out.println("workspace = " + workspacePath);

		IProject[] projects = root.getProjects();

		// Just used to get events to see how the analysis is running
		NotificationHub.addAnalysisListener(this);

		// Assumes that there's only one project getting analyzed
		File project = null;
		for (int i = 0; i < projects.length; i++) {
			final IProject p = projects[i];
			if (p.getName().equals("promises")) {
				continue;
			}
			if (p.isOpen()) {
				final String projectPath = p.getLocation().toOSString();
				final File proj = new File(projectPath);
				File script = findFile(proj, ScriptCommands.NAME, false);
				if (script != null) {
					assertNull("More than one project to analyze!?!", project);
					project = proj;
				}
			}
		}
		
		if (project == null) {
			if (projects.length == 1) {	
				final String projectPath = projects[0].getLocation().toOSString();
				project = new File(projectPath);
			}
			else if (projects.length > 0) {
				// Check for script at parent of the project
				final String projectPath = projects[0].getLocation().toOSString();
				final File parent = new File(projectPath).getParentFile();
				File script = findFile(parent, ScriptCommands.NAME, false);
				if (script != null) {
					project = parent;
				} else {
					// TODO hack to get things to go when there's no script
					//project = new File(projectPath);
					project = new File(System.getProperty("test.module"));
				}
			}
		}
		if (project == null) {
			fail("No project");
		}
				
		// System.out.println("Setting up log for "+project.getName());
		output = IDE.getInstance().makeLog(project.getName());
		try {
			runAnalysis(workspaceFile, project, projects);
		} catch (AssertionFailedError e) {
			output.close();
			throw e; // pass-through
		} catch (Throwable ex) {
			ex.printStackTrace();
			output.reportError(currentTest.pop(), ex);
			output.close();
			throw ex;
			//fail(ex+" : "+ex.getMessage());
		}
		output.close();
	}

	private void start(final String tag) {
		System.out.println("RegressionTest: " + tag);
		ITest test = new ITest() {
			@Override
			public String getClassName() {
				return tag;
			}

			@Override
			public IRNode getNode() {
				return null;
			}

			@Override
			public String toString() {
				return "RegressionTest " + tag;
			}
			@Override
			public String identity() {
				return super.toString();
			} 
		};
		currentTest.push(output.reportStart(test));
	}

	private void end(String msg) {
		ITest t = currentTest.pop();
		System.out.println("Reporting success for "+t+" : "+msg);
		output.reportSuccess(t, msg);
		// currentTest = null;
	}

	private void endError(Throwable t) {
		ITest test = currentTest.pop();
		System.out.println("Reporting error for "+test+" : "+t);
		output.reportError(test, t);
		// currentTest = null;
	}

	private ITestOutput output = null;
	// private ITest currentTest = null;
	private final Stack<ITest> currentTest = new Stack<ITest>();

	/*
	private File findFile(final IProject project, final String file,
			boolean checkParent) {
		final String projectPath = project.getLocation().toOSString();
		final File proj = new File(projectPath);
		return findFile(proj, file, checkParent);
	}
	*/

	private File findFile(File proj, String file, boolean checkParent) {
		File result = lookForFile(proj, file);
		if (checkParent && result == null) {
			// Check for script in parent dir if multiple projects
			result = lookForFile(proj.getParentFile(), file);
		}
		return result;
	}

	private File lookForFile(final File project, final String file) {
		File result = new File(project, file);
		if (result.exists() && result.isFile()) {
			System.out.println("Found " + file + ": " + result);
			return result;
		}
		System.out.println("Couldn't find " + file + ": " + result);
		return null;
	}

	private void runAnalysis(final File workspaceFile, final File project,
			IProject[] projects) throws Throwable {
		final String projectPath = project.getAbsolutePath();
		start("Start logging to a file & refresh");
		final String logName = EclipseLogHandler.startFileLog(project.getName()
				+ ".log");
		ResourcesPlugin.getWorkspace().getRoot()
				.refreshLocal(IResource.DEPTH_INFINITE, null);

		printActivatedAnalyses();
		end("Done with refresh");

		/*
		 * Force a build of the workspace which does the analysis and updates
		 * the consistency proof.
		 */
		start("Build and analyze");
		//ScriptReader.waitForBuild(IncrementalProjectBuilder.AUTO_BUILD);
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, null);

		System.out.println("JSure data = "
				+ JSurePreferencesUtility.getJSureDataDirectory());
		List<IJavaProject> jprojects = new ArrayList<IJavaProject>(
				projects.length);
		for (IProject p : projects) {
			IJavaProject jp = JDTUtility.getJavaProject(p.getName());
			if (jp != null) {
				jprojects.add(jp);
			} else {
				System.out.println("Couldn't get java project for "+p);
			}
		}
		JavacBuild.analyze(jprojects, IErrorListener.throwListener);
		end("Done running JSure analysis...");

		final String projectName = project.getName();
		boolean resultsOk = true;

		// Check for script in the project to execute
		File script = findFile(project, ScriptCommands.NAME, true);
		final boolean logOk;
		try {
			if (script != null) {
				start("Run scripting");
				ScriptReader r = new ScriptReader(jprojects, false);
				resultsOk = r.execute(script);
				end("Done scripting");
			}
			// Checks consistency of TestResults
			// TODO is this right w/ the remote JVM?
			System.out.println("Updating consistency proof");
			ConsistencyListener.prototype.analysisCompleted();
		} finally {
			try {
				assertNotNull(projectName);
				final boolean ok = EclipseLogHandler.stopFileLog();
				System.out.println("logging is ok = "+ok);
				System.out.println("log = " + logName);
				logOk = compareLogs(projectPath, logName, projectName) && ok;

				AnnotationRules.XML_LOG.close();
			} catch (Throwable t) {
				throw t;
			}
		}
		/*
		final ProjectsDrop pd = ProjectsDrop.getDrop();
		if (pd == null) {
			throw new IllegalStateException("No results");
		}
		final Projects projs = (Projects) pd.getIIRProjects();
		*/
		final JSureScan run = JSureDataDirHub.getInstance().getCurrentScan();

		// Export the results from this run
		start("Exporting results");
		try {
			final File results = new File(workspaceFile, 
					RegressionUtility.computeScanName(projectName, run.getTimeOfScan()));
			FileUtility.recursiveCopy(run.getDir(), results);				
			end("Done exporting");

			start("comparing results");
			System.out
					.println("Try to compare these results to the results oracle");
			if (project.exists()) {
				resultsOk = compareResults(results, workspaceFile, project,
						projectName, resultsOk);
				end("Done comparing");
			}
		} catch (FileNotFoundException ex) {
			System.out.println("Problem while creating results:");
			endError(ex);
		} catch (IOException ex) {
			System.out.println("Problem while closing results:");
			endError(ex);
		} catch (Throwable ex) {
			System.out.println("Problem while exporting/comparing results: "
					+ ex + " -- " + ex.getMessage());
			ex.printStackTrace(System.out);
			endError(ex);
		}
		assertTrue("results = " + resultsOk + ", log = " + logOk, resultsOk
				&& logOk);
	}

	private boolean compareLogs(final String projectPath, final String logName,
			final String projectName) throws Throwable {
		final boolean logOk;
		start("comparing logs");
		System.out.println("Try to compare the log to the log oracle");
		if (projectPath != null) {
			final ITestOutput XML_LOG = IDE.getInstance().makeLog(
					"EclipseLogHandler");
			final File project = new File(projectPath);
			final File oracle = RegressionUtility.getOracleName(
					project, RegressionUtility.logOracleFilter);
			final String logDiffsName = projectName + ".log.diffs.xml";
			final File log = new File(logName);
			// final File diffs = new File(logDiffsName);
			if (!log.exists() && !oracle.exists()) {
				end("Done comparing logs");
				// TODO create diffs
				return true;
			}
			if (log.length() == 0 && oracle.length() == 0) {
				end("Done comparing logs");
				// TODO create diffs
				return true;
			}
			assert (oracle.exists());
			try {
				System.out.println("Starting log diffs");
				int numDiffs = XMLLogDiff.diff(XML_LOG, oracle.getAbsolutePath(), logName,
						logDiffsName);
				System.out.println("#diffs = " + numDiffs);
				logOk = (numDiffs == 0);
				System.out.println("log diffs = " + logDiffsName);
				end("Done comparing logs");
			} catch (Throwable e) {
				System.out.println("Problem while diffing the log: "
						+ oracle + ", " + logName + ", " + logDiffsName);
				endError(e);
				throw e;
			} finally {
				XML_LOG.close();
			}
		} else {
			logOk = false;
		}
		return logOk;
	}

	private boolean compareResults(final File resultsSnapshot,
			final File workspaceFile, final File project,
			final String projectName, boolean resultsOk) throws Exception {
		final File xmlLocation = RegressionUtility.findOracle(project);
		if (!xmlLocation.exists()) {
			return resultsOk;
		}	
		ISeaDiff diff = SeaSnapshotDiff.diff(UninterestingPackageFilterUtility.UNINTERESTING_PACKAGE_FILTER, 
				xmlLocation, resultsSnapshot);

		String diffPath = new File(workspaceFile, projectName
				+ RegressionUtility.JSURE_SNAPSHOT_DIFF_SUFFIX)
				.getAbsolutePath();
		if (!diff.isEmpty()) {
			diff.write(new File(diffPath));
		}
		return resultsOk && diff.isEmpty();
	}

	private void printActivatedAnalyses() {
		for (String id : DoubleChecker.getDefault().getIncludedExtensions()) {
			System.out.println("Activated: " + id);
		}
	}

	@Override
	public synchronized void analysisCompleted() {
		System.out.println("Analysis completed");
	}

	@Override
	public synchronized void analysisPostponed() {
		System.out.println("Analysis postponed");
		new Throwable("For stack trace").printStackTrace();
	}

	@Override
	public synchronized void analysisStarting() {
		System.out.println("Analysis starting");
	}
}
