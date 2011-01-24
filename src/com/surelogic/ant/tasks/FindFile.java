package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import org.apache.tools.ant.*;

/**
 * Provides a mechanism to find a file in a directory.
 * <p>
 * Ant Usage:
 * <p>
 * &lt;findfile basedir="directory" regex="file-regular-expression"
 * property="property-to-set"/&gt;
 * <p>
 * Required Ant parameters:
 * <ul>
 * <li>basedir - The directory to search in
 * <li>regex - A regular expression (Java regex) of the file to find
 * <li>property - The property to set with the absolute path of the located file
 * </ul>
 */
public class FindFile extends Task {

	private String regex = null;

	private File basedir = null;

	private String property = null;

	private class RegexFilter implements FilenameFilter {
		public boolean accept(File directory, String fileName) {
			return Pattern.matches(regex, fileName);
		}
	}

	@Override
	public void execute() {
		if (basedir.isDirectory()) {
			File[] matches = basedir.listFiles(new RegexFilter());
			if (matches.length > 0) {
				// Set the desired property to the absolute path of the first
				// file found
				getProject()
						.setProperty(property, matches[0].getAbsolutePath());
			} else {
				log("No matches for " + regex + " found in "
						+ basedir.getAbsolutePath(), Project.MSG_ERR);
			}
		} else {
			throw new BuildException(basedir.getAbsolutePath()
					+ " is not a valid directory");
		}
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
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
}
