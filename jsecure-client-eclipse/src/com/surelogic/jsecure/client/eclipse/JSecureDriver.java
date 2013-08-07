package com.surelogic.jsecure.client.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.ICompilationUnit;

import com.surelogic.common.*;
import com.surelogic.common.core.*;
import com.surelogic.common.core.java.*;
import com.surelogic.common.core.jobs.EclipseLocalConfig;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.remote.*;

public class JSecureDriver extends AbstractJavaScanner<JavaProjectSet<JavaProject>, JavaProject> {
	static final String RESULTS_XML = "jsecure.xml";
	
	public JSecureDriver() {
		super(new IJavaFactory<JavaProject>() {
			public JavaProject newProject(JavaProjectSet<JavaProject> projects,
					Config config, String name, SLProgressMonitor monitor) {
				return IJavaFactory.prototype.newProject(projects, config, name, monitor);
			}

			@Override
			public JavaProjectSet<JavaProject> newProjectSet(File loc,
					boolean isAuto, Date time, Map<String, Object> args) {
				final JavaProjectSet<JavaProject> rv = IJavaFactory.prototype.newProjectSet(loc, isAuto, time, args);
				rv.setArg(SrcEntry.ZIP_BINARIES, Boolean.TRUE);
				return rv;
			}
		});
	}

	@Override
	protected File getDataDirectory() {
		File result = EclipseUtility.getJSecureDataDirectory();
	    FileUtility.ensureDirectoryExists(result);
	    return result;
	}

	@Override
	protected void markAsRunning(File runDir) {
		final File location = getResultsFile(runDir);
		try {
			location.createNewFile();
		} catch (IOException e) {
			// Ignore
		}
	}

	public static File getResultsFile(File scanDir) {
		final File location = new File(scanDir, RemoteScanJob.TEMP_PREFIX+RESULTS_XML);
		return location;
	}
	
	@Override
	protected ProjectInfo<JavaProject> finishRegisteringFullBuild(
			IProject project, List<Pair<IResource, Integer>> resources,
			List<ICompilationUnit> cus) {
		return new ProjectInfo<JavaProject>(project, cus) {

			@Override
			protected ProjectInfo<JavaProject> getProjectInfo(IProject proj) {				
				return JSecureDriver.this.getProjectInfo(proj);
			}		
		};
	}
	
	@Override
	protected AbstractAnalysisJob<JavaProjectSet<JavaProject>> makeAnalysisJob(
			JavaProjectSet<JavaProject> projects, File target, File zips,
			boolean useSeparateJVM) {
		return new AbstractAnalysisJob<JavaProjectSet<JavaProject>>(projects, target, zips, useSeparateJVM) {			
			@Override
			protected void init(SLProgressMonitor monitor) throws Exception {
				System.out.println("JSecure started initializing: "+projects.getLocation());
			}
			
			@Override
			protected boolean analyzeInVM() throws Exception {
				System.out.println("JSecure running in same VM");
				System.setProperty(RemoteScanJob.RUN_DIR_PROP, projects.getRunDir().getAbsolutePath());
				RemoteJSecureJob.main();
				return true;
			}
			
			@Override
			protected AbstractLocalSLJob<?> makeLocalJob() throws Exception {
				return new LocalJSecureJob("JSecure", 100, new EclipseLocalConfig(4096, projects.getRunDir()));
			}
			
			@Override
			protected void handleSuccess() {
				System.out.println("JSecure analysis succeeded");			
			}
			
			@Override
			protected void handleFailure() {
				System.out.println("JSecure analysis failed");					
			}
			
			@Override
			protected void handleCrash(SLProgressMonitor monitor, SLStatus status) {
				System.out.println("JSecure analysis crashed: "+status.getMessage());
				if (status.getException() != null) {
					status.getException().printStackTrace(System.out);
				}
			}
			
			@Override
			protected void endAnalysis(SLProgressMonitor monitor) {
				System.out.println("JSecure ended analysis");		
			}
			
			@Override
			protected void finish(SLProgressMonitor monitor) {
				System.out.println("JSecure completed successfully.");			
			}
		};
	}
}
