package com.surelogic.jsure.core.driver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;

import com.surelogic.analysis.JSureProperties;
import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.FileUtility;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.TextArchiver;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.java.AbstractAnalysisJob;
import com.surelogic.common.core.java.AbstractJavaScanner;
import com.surelogic.common.core.java.ProjectInfo;
import com.surelogic.common.core.jobs.EclipseLocalConfig;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.remote.AbstractRemoteSLJob;
import com.surelogic.common.jobs.remote.ILocalConfig;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.scan.JSureScanCrashReport;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.java.persistence.ScanProperty;
import com.surelogic.javac.jobs.LocalJSureJob;
import com.surelogic.jsure.core.listeners.NotificationHub;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ide.IDERoot;


public class JSureDriver<T extends JavaProject> extends AbstractJavaScanner<JavaProjectSet<T>, T> {
	private static final String CRASH_FILES = "crash.log.txt";
	
	private static JSureDriver<? extends JavaProject> prototype;

	public static JSureDriver<? extends JavaProject> getInstance() {
		return prototype;
	}
	
	public static synchronized void initInstance(JSureDriver<? extends JavaProject> driver) {
		if (driver == null) {
			prototype = new JSureDriver<JavaProject>(IJavaFactory.prototype); 
		} else {
			prototype = driver;
		}
	}
	
	protected JSureDriver(IJavaFactory<T> factory) {
		super(factory);
	}	
	
	/**
	 * Wait for a normal Eclipse build
	 */
	public static void waitForBuild() {
		waitForBuild(true);
		waitForBuild(false);
	}

	@Override
	protected final File getDataDirectory() {
		return JSurePreferencesUtility.getJSureDataDirectory();
	}
	  
	@Override
	protected final void markAsRunning(File runDir) {
		JSureScan.markAsRunning(runDir);
	}

	@Override
	public JavaProjectSet<T> configureBuild(File location, boolean isAuto /* IProject p */, boolean ignoreNature) {
		// System.out.println("Finished 'build' for "+p);
		/*
		 * //ProjectDrop.ensureDrop(p.getName(), p); final ProjectInfo info =
		 * projects.get(p); if (info == null) { return; // No info! }
		 */
		/*
		 * // Check if any projects are still building building.remove(p); if
		 * (!building.isEmpty()) {
		 * System.out.println("Still waiting for "+building); return; }
		 */

		// TODO this needs to be run after ALL the info is collected
		JavacEclipse.initialize();
		if (!XUtil.testing) {
			SLLogger.getLogger().fine("Configuring analyses for build");
			JavacEclipse.getDefault().synchronizeAnalysisPrefs();
		}
		return super.configureBuild(location, isAuto, ignoreNature);
	}

	@Override
	protected File prepForScan(JavaProjectSet<T> newProjects, SLProgressMonitor monitor, boolean useSeparateJVM) throws Exception {
		if (!XUtil.testing) {
			System.out.println("Configuring analyses for doBuild");
			JavacEclipse.getDefault().synchronizeAnalysisPrefs();
		}
		return super.prepForScan(newProjects, monitor, useSeparateJVM);
	}
	
	@Override
	protected AbstractAnalysisJob<JavaProjectSet<T>> makeAnalysisJob(
			JavaProjectSet<T> newProjects, File target, File zips,
			boolean useSeparateJVM) {
	     return new AnalysisJob(null, newProjects, target, zips, useSeparateJVM);
	}

	@Override 
	protected void scheduleScanForExecution(JavaProjectSet<T> newProjects, SLJob copy) throws Exception {
		if (XUtil.testing) {
			copy.run(new NullSLProgressMonitor());
		} else {
			super.scheduleScanForExecution(newProjects, copy);
		}
	}
	
	@Override
	protected ProjectInfo<T> finishRegisteringFullBuild(IProject project,
			List<Pair<IResource, Integer>> resources, List<ICompilationUnit> cus) {
		return new JSureProjectInfo(project, cus);
	}
	
	public volatile SLProgressMonitor lastMonitor = null;
	
	protected class JSureProjectInfo extends ProjectInfo<T> {

		public JSureProjectInfo(IProject project, List<ICompilationUnit> cus) {
			super(project, cus);
		}

		@Override
		protected void setProjectSpecificProperties(Config config) {
			// TODO obsolete?
			Properties props2 = JSureProperties.read(config.getLocation());
			if (props2 != null) {
				JSureProperties.handle(config.getProject(), props2);
			}
		}

		@Override
		protected ProjectInfo<T> getProjectInfo(IProject proj) {
			return JSureDriver.this.getProjectInfo(proj);
		}

		@Override
		protected void setDefaultJRE(String name) {
			JavacEclipse.getDefault().setPreference(IDEPreferences.DEFAULT_JRE, name);
		}
	}
	
	protected class AnalysisJob extends AbstractAnalysisJob<JavaProjectSet<T>> {
		protected final JavaProjectSet<T> oldProjects;

		protected AnalysisJob(JavaProjectSet<T> oldProjects, JavaProjectSet<T> projects, File target, File zips, boolean useSeparateJVM) {
			super(projects, target, zips, useSeparateJVM);
			if (useSeparateJVM) {
				this.oldProjects = null;
			} else {
				this.oldProjects = oldProjects;
			}
		}

		@Override
		public SLStatus run(SLProgressMonitor monitor) {
			lastMonitor = monitor;
			projects.setMonitor(monitor);
			/*
			 * if (XUtil.testingWorkspace) {
			 * System.out.println("Clearing state before running analysis");
			 * ClearProjectListener.clearJSureState(); }
			 */
			return super.run(monitor);
		}

		@Override
		protected void init(SLProgressMonitor monitor) throws IOException {
			JavacEclipse.initialize();
			System.out.println("JSure data dir  = " + IDERoot.getInstance().getStringPreference(IDEPreferences.JSURE_DATA_DIRECTORY));
			System.out.println("XML diff dir    = " + IDERoot.getInstance().getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY));
			NotificationHub.notifyAnalysisStarting();
			JavacEclipse.getDefault().savePreferences(projects.getRunDir());
		}

		@Override
		protected LocalJSureJob makeLocalJob() throws Exception {
			// Normally done by Javac, but needs to be repeated locally
			final boolean noConflict;
			if (oldProjects != null) {
				/*
				  noConflict = !projects.conflictsWith(oldProjects);
				  if (noConflict) {
					  projects.init(oldProjects);
				  } else {
					  System.out.println("Detected a conflict between projects");
				  }
				 */
				throw new UnsupportedOperationException("Can't support delta runs");
			} else {
				noConflict = true;
			}

			System.out.println("run = " + projects.getRun());
			final String msg = "Running JSure for " + projects.getLabel();
			ILocalConfig cfg = new EclipseLocalConfig(JSurePreferencesUtility.getMaxMemorySize(), projects.getRunDir());
			return LocalJSureJob.factory.newJob(msg, 100, cfg);
		}

		@Override
		protected boolean analyzeInVM(SLProgressMonitor monitor) throws Exception {
			return runAsLocalJob(monitor);
		}

		@Override
		protected void handleSuccess() {
			final File runDir = projects.getRunDir();
			// Unneeded after running the scan
			FileUtility.recursiveDelete(new File(runDir, PersistenceConstants.SRCS_DIR), false);

			JSureDataDirHub.getInstance().scanDirectoryAdded(projects.getRunDir());
		}

		@Override 
		protected void handleFailure() {
			NotificationHub.notifyAnalysisPostponed();
		}

		@Override
		protected void handleCrash(SLProgressMonitor monitor, SLStatus status) {
			if (monitor.isCanceled()) {				
				return; // Ignore this 
			}
			if (XUtil.testing && status.getException() != null) { 			  
				throw new RuntimeException(status.getException());
			}
			/*
			 * Collect information and report this scan crash to SureLogic.
			 */
			final File rollup = collectCrashFiles(projects);
			if (XUtil.testing) {
				if (status.getException() != null) {
					status.getException().printStackTrace();
					throw new RuntimeException(status.getException());
				} else {
					System.err.println("CRASH: " + status.getMessage());
					throw new RuntimeException(status.getMessage());
				}
			} else {
				JSureScanCrashReport.getInstance().getReporter().reportScanCrash(status, rollup);
			}
			/*
			 * Because we already opened a dialog above about the crash, log it and
			 * bail out of the job.
			 */
			status.logTo(SLLogger.getLogger());
		}

		@Override
		protected void handleCancel(SLStatus s) {
			if (s.getException() != null) {
				JSureScanCrashReport.getInstance().getReporter().reportScanCancellation(s.getException().getMessage());
			}
		}

		protected void endAnalysis(SLProgressMonitor monitor) {
			if (lastMonitor == monitor) {
				lastMonitor = null;
			}
		}

		protected void finish(SLProgressMonitor monitor) {
			NotificationHub.notifyAnalysisCompleted();
			// recordViewUpdate();

			// Cleared here after notifications are processed
			// to prevent redoing some (binder) work
			//IDE.getInstance().clearCaches();
		}
	}	
	
	private static <T extends JavaProject> File collectCrashFiles(JavaProjectSet<T> projects) {
		final File crash = new File(projects.getRunDir(), CRASH_FILES);
		try {
			PromisesXMLArchiver out = new PromisesXMLArchiver(crash);
			try {
				out.archive(ScanProperty.SCAN_PROPERTIES, 
						new File(projects.getRunDir(), ScanProperty.SCAN_PROPERTIES));
				out.archive(IDERoot.JAVAC_PROPS, new File(projects.getRunDir(), IDERoot.JAVAC_PROPS));

				// Get project-specific config
				for (String name : projects.getProjectNames()) {
					IProject proj = EclipseUtility.getProject(name);
					if (proj == null) {
						out.outputWarning("Project does not exist: " + name);
						continue;
					}
					IPath projLocation = proj.getLocation();
					if (projLocation != null) {
						File projFile = projLocation.toFile();
						if (projFile != null && projFile.isDirectory()) {
							for (String config : AbstractJavaZip.CONFIG_FILES) {
								out.archive(projFile.getName() + '/' + config, new File(projFile, config));
							}
						} else {
							out.outputWarning("File could not be created for project location: " + projLocation);
							continue;
						}
					} else {
						out.outputWarning("Project location could not be retrieved: " + name);
						continue;
					}
				}
				out.archive(PersistenceConstants.PROJECTS_XML, new File(projects.getRunDir(), PersistenceConstants.PROJECTS_XML));
				out.archive(SLUtility.LOG_NAME, new File(projects.getRunDir(), SLUtility.LOG_NAME));

				final File libDir = JSurePreferencesUtility.getJSureXMLDirectory();
				FileUtility.recursiveIterate(out, libDir);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			// Couldn't create the new file for some reason
			return new File(projects.getRunDir(), SLUtility.LOG_NAME);
		}
		return crash;
	}	

	private static class PromisesXMLArchiver extends TextArchiver {
		public PromisesXMLArchiver(File target) throws IOException {
			super(target);
		}

		@Override
		public boolean accept(File pathname) {
			return TestXMLParserConstants.XML_FILTER.accept(pathname);
		}
	}
}
