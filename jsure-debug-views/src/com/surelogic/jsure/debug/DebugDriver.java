package com.surelogic.jsure.debug;

import java.io.File;

import com.surelogic.common.java.JavaProjectSet;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.dropsea.ir.utility.ClearStateUtility;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.jsure.core.driver.JavacDriver;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDERoot;

public class DebugDriver extends JavacDriver<JavacProject> {
	public DebugDriver() {
		super(Projects.javaFactory);
	}

	@Override
	protected AnalysisJob makeAnalysisJob(JavaProjectSet<JavacProject> newProjects, File target, File zips, boolean useSeparateJVM) {
		return new AnalysisJob(null, newProjects, target, zips, useSeparateJVM) {
			@Override
			protected boolean analyzeInVM(SLProgressMonitor monitor) throws Exception {
				File tmpLocation;
				/*
				if (clearBeforeAnalysis || oldProjects == null) {
					// ClearProjectListener.clearJSureState();

					tmpLocation = Util.openFiles(projects, true);
				} else {
					tmpLocation = Util.openFiles(oldProjects, projects, true);
				}
				*/
				if (oldProjects != null) {
					throw new IllegalStateException("Can't delta off another scan");
				}
				IDE.getInstance().loadPreferences(projects.getRunDir());
				tmpLocation = Util.openFiles((Projects) projects, true);
				boolean ok = tmpLocation != null;
				// Persist the Sea
				//final File tmpLocation = RemoteJSureRun.snapshot(System.out, projects.getLabel(), projects.getRunDir());
				/*
		    SeaStats.createSummaryZip(new File(projects.getRunDir(), RemoteJSureRun.SUMMARIES_ZIP), Sea.getDefault().getDrops(),
		        SeaStats.splitByProject, SeaStats.STANDARD_COUNTERS);
		    System.out.println("Finished " + RemoteJSureRun.SUMMARIES_ZIP);
				 */

				// Create empty files
				/*
		    final File log = new File(projects.getRunDir(), RemoteJSureRun.LOG_NAME);
		    log.createNewFile();
				 */
				ClearStateUtility.clearAllState();
				RemoteJSureRun.renameToFinalName(System.out, projects.getRunDir(), tmpLocation);
				return ok;
			}
		};
	}
}
