package com.surelogic.javac.persistence;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.surelogic.common.FileUtility;
import com.surelogic.common.SLUtility;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.javac.JavacTypeEnvironment;
import com.surelogic.javac.Projects;

public class JSureScan implements Comparable<JSureScan> {

	public static JSureScan findByDirName(List<JSureScan> in, String dirName) {
		for (JSureScan scan : in) {
			if (scan.getDirName().equals(dirName))
				return scan;
		}
		return null;
	}

	private final Date f_timeOfScan;
	private final File f_scanDir; // non-null
	private Projects f_projectsScanned;
	private JSureScan f_lastPartialScan;
	private final double f_sizeInMB;

	public JSureScan(File scanDir) throws Exception {
		if (scanDir == null || !scanDir.isDirectory()) {
			throw new IllegalArgumentException();
		}
		f_scanDir = scanDir;

		// time = null;
		// There should be at least 3 segments: label date time
		final String[] name = scanDir.getName().split(" ");
		if (name.length < 3) {
			throw new IllegalArgumentException();
		}
		f_timeOfScan = SLUtility.fromStringHMS(name[name.length - 2] + ' '
				+ (name[name.length - 1].replace('-', ':')));
		f_sizeInMB = FileUtility.recursiveSizeInBytes(f_scanDir)
				/ (1024 * 1024.0);

		// check the various files
		getProjects();
	}

	public File getDir() {
		return f_scanDir;
	}

	public String getDirName() {
		return f_scanDir.getName();
	}

	public double getSizeInMB() {
		return f_sizeInMB;
	}

	public Projects getProjects() throws Exception {
		if (f_projectsScanned != null) {
			return f_projectsScanned;
		}
		// Get info about projects
		JSureProjectsXMLReader reader = new JSureProjectsXMLReader();
		reader.read(new File(f_scanDir, PersistenceConstants.PROJECTS_XML));
		f_projectsScanned = reader.getProject();
		f_projectsScanned.setMonitor(NullSLProgressMonitor.getFactory()
				.createSLProgressMonitor(""));
		return f_projectsScanned;
	}

	public void setLastPartialScan(JSureScan last) {
		if (f_lastPartialScan != null && f_lastPartialScan != last
				|| last == null) {
			throw new IllegalArgumentException();
		}
		f_lastPartialScan = last;
	}

	public JSureScan getLastPartialScan() {
		return f_lastPartialScan;
	}

	public int compareTo(JSureScan o) {
		return f_timeOfScan.compareTo(o.f_timeOfScan);
	}

	public String toString() {
		if (f_lastPartialScan != null) {
			return "JSureScan: " + f_scanDir.getName() + " ->\n\t"
					+ f_lastPartialScan;
		}
		return "JSureScan: " + f_scanDir.getName();
	}

	public Map<String, JSureFileInfo> getLatestFilesForProject(String proj)
			throws IOException {
		if (proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
			return Collections.emptyMap();
		}
		final File srcZip = new File(f_scanDir, "zips/" + proj + ".zip");
		if (!srcZip.exists()) {
			// throw new IllegalStateException("No sources: "+srcZip);
			System.err.println("No sources: " + srcZip);
			return Collections.emptyMap();
		}

		final Map<String, JSureFileInfo> info;
		final File resultsZip;
		if (f_lastPartialScan != null) {
			resultsZip = new File(f_scanDir,
					PersistenceConstants.PARTIAL_RESULTS_ZIP);
			info = f_lastPartialScan.getLatestFilesForProject(proj);
		} else {
			resultsZip = new File(f_scanDir, PersistenceConstants.RESULTS_ZIP);
			info = new HashMap<String, JSureFileInfo>();
		}
		if (!resultsZip.exists()) {
			// System.out.println("No results: "+resultsZip);
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
					System.out.println("Replacing " + ze.getName()
							+ " with entry from " + old);
				}
			}
		} finally {
			zip.close();
		}
		return info;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((f_scanDir == null) ? 0 : f_scanDir.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JSureScan other = (JSureScan) obj;
		if (f_scanDir == null) {
			if (other.f_scanDir != null)
				return false;
		} else if (!f_scanDir.equals(other.f_scanDir))
			return false;
		return true;
	}
}
