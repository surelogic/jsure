package com.surelogic.javac.jobs;

import java.io.BufferedReader;
import java.io.File;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.jobs.remote.AbstractRemoteSLJob;
import com.surelogic.javac.Config;
import com.surelogic.javac.Javac;
import com.surelogic.javac.JavacTypeEnvironment;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;
import com.surelogic.javac.persistence.JSureScan;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.SeaStats;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

public class RemoteJSureRun extends AbstractRemoteSLJob {
	public static final String RUN_DIR_PROP = "jsure.run.dir";
	public static final String FLUID_DIRECTORY_URL = "fluid.directory.url";
	public static final String RESULTS_XML  = "results"+SeaSnapshot.SUFFIX;
	public static final String LOG_TXT = "remote"+LOG_SUFFIX;
	public static final String SUMMARIES_ZIP = "summaries.zip";
	
	public static void main(String[] args) {
		RemoteJSureRun job = new RemoteJSureRun();
		job.run();
	}
	
	@Override
	protected SLJob init(BufferedReader br, Monitor mon) throws Throwable {
		out.println("Running "+getClass().getSimpleName());
		
		// Need to initialize things that are usually setup by Eclipse
		Javac.initialize();
		AnnotationRules.initialize();
		
		final String runPath = System.getProperty(RUN_DIR_PROP, System.getProperty("user.dir"));
		if (runPath == null) {
			throw new IllegalStateException("No run directory");
		}
		out.println("runPath = "+runPath);
		
		// Load up projects
		final File runDir       = new File(runPath);		
		/*
		final String[] name = runDir.getName().split(" ");
		final String time   = name[name.length-2]+' '+(name[name.length-1].replace('-', ':'));
		SLUtility.fromStringHMS(time);
		out.println("time = "+time);
		*/
		try {
			out.println("Creating run");
			final JSureScan run = new JSureScan(runDir);//JSureDataDirScanner.findRunDirectory(runDir);		
			out.println("run = "+run.getDirName());
			/*
			if (false) {				
				out.println("Doing nothing");
				return new AbstractSLJob("Does nothing") {					
					public SLStatus run(SLProgressMonitor monitor) {
						return SLStatus.OK_STATUS;
					}
				};
			} else {
				JSureProjectsXMLReader reader = new JSureProjectsXMLReader();
				out.println("reader = "+reader);
				try {
					reader.read(new File(runDir, PersistenceConstants.PROJECTS_XML));
				} catch(Exception e) {
					e.printStackTrace(out);
				}
				out.println("Read "+PersistenceConstants.PROJECTS_XML);
				Projects projs = reader.getProject();
				out.println("projs = "+projs.getLabel());
				projs.setMonitor(NullSLProgressMonitor.getFactory().createSLProgressMonitor(""));
				out.println("Set monitor");
			}
			*/	
			final Projects projects = run.getProjects();
			out.println("projects = "+projects.getLabel());
			
			// Finish initializing
			Javac.getDefault().loadPreferences(runDir);			
			out.println("JSure data dir = "+
					IDE.getInstance().getStringPreference(IDEPreferences.JSURE_DATA_DIRECTORY));
			out.println("XML diff dir   = "+
					IDE.getInstance().getStringPreference(IDEPreferences.JSURE_XML_DIFF_DIRECTORY));
			
			String defaultJRE = null;
			for(Config c : projects.getConfigs()) {
				out.println("Looking for JRE in "+c.getProject());
				if (c.getProject().startsWith(JavacTypeEnvironment.JRE_NAME)) {
					defaultJRE = c.getProject();
					break;
				}
			}	
			out.println("default JRE = "+defaultJRE);
			if (defaultJRE != null) {
				Javac.getDefault().setPreference(IDEPreferences.DEFAULT_JRE, defaultJRE);
			}
			return new AbstractSLJob("Running JSure on "+projects.getLabel()) {			
				public SLStatus run(SLProgressMonitor monitor) {
					try {
						projects.setMonitor(monitor);				
						Util.openFiles(projects, true);

						// Already done
						// Sea.getDefault().updateConsistencyProof();
						
						// Previously done by ConsistencyListener 
						TestResult.checkConsistency();
						
						new SeaSnapshot(new File(runDir, RESULTS_XML)).snapshot(projects.getLabel(), Sea.getDefault());
						SeaStats.createSummaryZip(new File(runDir, SUMMARIES_ZIP), Sea.getDefault().getDrops(), 
								                  SeaStats.splitByProject, SeaStats.STANDARD_COUNTERS);
						out.println("Finished "+SUMMARIES_ZIP);
						monitor.done();
					} catch (Exception e) {
						return SLStatus.createErrorStatus(e);
					}
					return SLStatus.OK_STATUS;
				}
			};
		} catch(Throwable t) {
			mon.failed("Unable to create JSure job", t);		
			return null;
		}
	}
}
