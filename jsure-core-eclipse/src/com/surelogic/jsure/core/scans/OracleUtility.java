package com.surelogic.jsure.core.scans;

import java.io.File;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.javac.*;
import com.surelogic.javac.persistence.*;

public class OracleUtility {
	public static JSureScan findOracle(final JSureScan scan) {
		File sharedParent = null;
		boolean setParent = false;
		try {
			final Projects projects = scan.getProjects();
			final boolean hasMultipleProjects = projects.size() > 2;
			for(final JavacProject jp : projects) {
				final IProject p = EclipseUtility.getProject(jp.getName());
				if (p == null || !p.exists()) {
					continue;
				}
				final File pFile = p.getLocation().toFile();
				if (hasMultipleProjects) {
					// Multiple projects so try to find a shared parent
					final File parent = pFile.getParentFile();
					if (setParent) {		
						if (!parent.equals(sharedParent)) {
							sharedParent = null;
						}
					} else {
						sharedParent = parent;
						setParent = true;
					}
				} else {
					// One is the JRE, so use an oracle if it's in the project)
					JSureScan s = findOracle(pFile);
					if (s != null) {
						return s;
					}
				}
			}
			if (setParent && sharedParent != null) {
				return findOracle(sharedParent);
			}
		} catch(Exception e) {
			SLLogger.getLogger().log(Level.WARNING, "Unable to determine projects for "+scan.getDirName(), e);
		}
		return null;
	}

	private static JSureScan findOracle(File dir) {
		File file = RegressionUtility.findOracle(dir);				
		if (file != null && file.isDirectory()) {
			try {
				return new JSureScan(file);						
			} catch(Exception e) {
				// ignore
			}									
		}
		return null;
	}
}
