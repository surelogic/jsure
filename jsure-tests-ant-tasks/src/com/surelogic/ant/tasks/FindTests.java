package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Returns a comma-separated list of directories or zip files contained at the
 * top-most level of the given directory. For use with the ant-contrib for and
 * foreach tasks (although it is not strictly required).
 * 
 * Usage: <findtests basedir="directory to search" fullpaths="true/false"
 * property="property to set"/>
 * 
 * Required parameters: basedir - The path to the directory to search for
 * directories fullpaths - Boolean indicating whether or not to return the
 * absolute paths, or just the names of the located directories property - The
 * property to set with the comma-separated list
 * 
 * @author Ethan.Urie
 * @author Edwin.Chan
 */
public class FindTests extends Task {

	private File basedir = null;
	private boolean fullpaths = false;
	private String property = null;

	private final static class RegressionTestFilter implements FileFilter {
		public boolean accept(File f) {
			boolean rv;
			if (f.isDirectory()) {
				rv = true;
			} else {
				rv = f.getName().endsWith(".zip");
			}
			return rv;
		}
	}

	@Override
  public void execute() {
		if (basedir.isDirectory()) {
			final File[] dirList = basedir
					.listFiles(new RegressionTestFilter());
			final List<String> resultList = new ArrayList<String>();

			for (int i = 0; i < dirList.length; i++) {
				final String path;
				if (fullpaths) {
					path = dirList[i].getAbsolutePath();
				} else {
					path = dirList[i].getName();
				}
				resultList.add(path);
			}

			/*
			 * Let's get things into alphabetical order
			 */
			Collections.sort(resultList);

			StringBuilder list = new StringBuilder();
			boolean first = true;
			for (String path : resultList) {
				if (first)
					first = false;
				else
					list.append(',');

				list.append(path);
			}
			getProject().setProperty(property, list.toString());
		} else {
			log(basedir.getAbsolutePath() + " is not a valid directory.",
					Project.MSG_ERR);
		}
	}

	public File getBasedir() {
		return basedir;
	}

	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public boolean isFullpaths() {
		return fullpaths;
	}

	public void setFullpaths(boolean fullpaths) {
		this.fullpaths = fullpaths;
	}
}
