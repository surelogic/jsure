package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.surelogic.common.FileUtility;
import com.surelogic.common.SLUtility;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.javac.JavacTypeEnvironment;
import com.surelogic.javac.Projects;

public class JSureRun implements Comparable<JSureRun> {
	private final Date time;
	private final File dir;
	private Projects projs;
	private JSureRun lastRun;
	private final double sizeInMB;
	
	public JSureRun(File runDir) throws Exception {
		if (runDir == null || !runDir.isDirectory()) {
			throw new IllegalArgumentException();
		}	
		dir = runDir;
		
		//time = null;		
		// There should be at least 3 segments: label date time
		final String[] name = runDir.getName().split(" ");
		if (name.length < 3) {						
			throw new IllegalArgumentException();
		}
		time = SLUtility.fromStringHMS(name[name.length-2]+' '+(name[name.length-1].replace('-', ':')));
		sizeInMB = FileUtility.recursiveSizeInBytes(dir) / (1024*1024.0);
		
		// check the various files
		getProjects();
	}
	
	public File getDir() {
		return dir;
	}
	
	public String getName() {
		return dir.getName();
	}

	public double getSizeInMB() {
		return sizeInMB;
	}
	
	public Projects getProjects() throws Exception {
		if (projs != null) {
			return projs;
		}
		// Get info about projects
		JSureProjectsXMLReader reader = new JSureProjectsXMLReader();
		reader.read(new File(dir, PersistenceConstants.PROJECTS_XML));
		projs = reader.getProject();
		projs.setMonitor(NullSLProgressMonitor.getFactory().createSLProgressMonitor(""));
		return projs;
	}

	public void setLastRun(JSureRun last) {	
		if (lastRun != null && lastRun != last || last == null) {
			throw new IllegalArgumentException();
		}
		lastRun = last;
	}
	
	public JSureRun getLastRun() {
		return lastRun;
	}

	public int compareTo(JSureRun o) {
		return time.compareTo(o.time);
	}
	
	public String toString() {
		if (lastRun != null) {
			return "JSureRun: "+dir.getName()+" ->\n\t"+lastRun;
		}
		return "JSureRun: "+dir.getName();
	}
	
	public Map<String,JSureFileInfo> getLatestFilesForProject(String proj) throws IOException {
		if (proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
			return Collections.emptyMap();
		}
		final File srcZip = new File(dir, "zips/"+proj+".zip");
		if (!srcZip.exists()) {
			//throw new IllegalStateException("No sources: "+srcZip);
			System.err.println("No sources: "+srcZip);
			return Collections.emptyMap();
		}
		
		final Map<String,JSureFileInfo> info;
		final File resultsZip;
		if (lastRun != null) {
			resultsZip = new File(dir, PersistenceConstants.PARTIAL_RESULTS_ZIP);
			info = lastRun.getLatestFilesForProject(proj);
		} else {
			resultsZip = new File(dir, PersistenceConstants.RESULTS_ZIP);
			info = new HashMap<String, JSureFileInfo>();
		}
		if (!resultsZip.exists()) {
			//System.out.println("No results: "+resultsZip);
			return Collections.emptyMap();
		}
		// Overwrite any changes from results
		final JSureFileInfo thisInfo = new JSureFileInfo(srcZip, resultsZip);		
		ZipFile zip = new ZipFile(resultsZip);
		try {
			Enumeration<? extends ZipEntry> e = zip.entries();
			while (e.hasMoreElements()) {
				ZipEntry ze = e.nextElement();
				JSureFileInfo old = info.put(ze.getName(), thisInfo);
				if (old != null) {
					System.out.println("Replacing "+ze.getName()+" with entry from "+old);
				}
			}
		} finally {
			zip.close();
		}
		return info;
	}
}
