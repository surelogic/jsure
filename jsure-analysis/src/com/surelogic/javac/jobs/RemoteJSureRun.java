package com.surelogic.javac.jobs;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.common.FileUtility;
import com.surelogic.common.java.*;
import com.surelogic.common.jobs.*;
import com.surelogic.common.jobs.remote.*;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.SeaSnapshot;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.javac.Javac;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;

public class RemoteJSureRun extends RemoteScanJob<Projects,JavacProject> {	
	public RemoteJSureRun() {
		super(Projects.javaFactory);
	}
	
	public static void main(String[] args) {
		RemoteJSureRun job = new RemoteJSureRun();
		job.run();
	}
	
	public static File snapshot(PrintStream out, String label, File scanDir) throws IOException {
		final File location = JSureScan.getResultsFile(scanDir);
		System.out.println("Creating snapshot: "+location);
		new SeaSnapshot(location).snapshot(label, Sea.getDefault());
		return location;
	}
	
	public static void renameToFinalName(PrintStream out, final File scanDir, final File tmpLocation) {
		if (tmpLocation == null) {
			return;
		}
 		final boolean compress = tmpLocation.getName().endsWith(FileUtility.GZIP_SUFFIX);
		final File location = new File(scanDir, compress ? JSureScan.RESULTS_XML_COMPRESSED : JSureScan.RESULTS_XML);
		System.out.println("Renaming snapshot: "+location);
		tmpLocation.renameTo(location);
	}
	
	@Override
	protected void init() {
		// Need to initialize things that are usually setup by Eclipse
		Javac.initialize();
		AnnotationRules.initialize();
	}
	
	@Override
	protected SLJob finishInit(final File runDir, final Projects projects) throws Throwable {
		// Finish initializing
		Javac.getDefault().loadPreferences(runDir);			
		out.println("JSure data dir = "+
				IDE.getInstance().getStringPreference(IDEPreferences.JSURE_DATA_DIRECTORY));
		out.println("XML diff dir   = "+
				IDE.getInstance().getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY));

		String defaultJRE = null;
		for(Config c : projects.getConfigs()) {
			out.println("Looking for JRE in "+c.getProject());
			if (c.getProject().startsWith(Config.JRE_NAME)) {
				defaultJRE = c.getProject();
				break;
			}
		}	
		out.println("default JRE = "+defaultJRE);
		if (defaultJRE != null) {
			Javac.getDefault().setPreference(IDEPreferences.DEFAULT_JRE, defaultJRE);
		}
		return new AbstractSLJob("Running JSure on "+projects.getLabel()) {			
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				final File tmpLocation; 
				try {
					projects.setMonitor(monitor);				
					tmpLocation = Util.openFiles(projects, true);

					// Already done
					// Sea.getDefault().updateConsistencyProof();

					// Previously done by ConsistencyListener 
					TestResult.checkConsistency();

					//tmpLocation = snapshot(out, projects.getLabel(), runDir);
					/*
						SeaStats.createSummaryZip(new File(runDir, SUMMARIES_ZIP), Sea.getDefault().getDrops(), 
								                  SeaStats.splitByProject, SeaStats.STANDARD_COUNTERS);
						out.println("Finished "+SUMMARIES_ZIP);
					 */
					monitor.done();
				} catch (Exception e) {
					return SLStatus.createErrorStatus(e);
				}
				renameToFinalName(out, runDir, tmpLocation);
				out.println("Done with JSure!");
				return SLStatus.OK_STATUS;
			}
		};
	}
}
