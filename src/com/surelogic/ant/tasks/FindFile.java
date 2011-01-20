/**
 * Provides a mechanism to find a file in a directory
 * Ant Usage:
 * <findfile basedir="directory" regex="file-regular-expression" property="property-to-set"/>
 * 
 * Required parameters:
 * basedir - The directory to search in
 * regex - A regular expression (Java regex) of the file to find
 * property - The property to set with the absolute path of the located file
 */
package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

import org.apache.tools.ant.*;

/**
 * @author Ethan.Urie
 * 
 */
public class FindFile extends Task {

	private String regex = null;

	private File basedir = null;

	private String property = null;
	
	private class RegexFilter implements FilenameFilter
	{
		public boolean accept(File arg0, String arg1) {
			if(Pattern.matches(regex, arg1))
			{
				return true;
			}
			return false;
		}
		
	}

	@Override
	public void execute() {
		if (basedir.isDirectory()) {
			File[] matches = basedir.listFiles(new RegexFilter());
			if(matches.length > 0)
			{
				//Set the desired property to the absolute path of the first file found
				getProject().setProperty(property, matches[0].getAbsolutePath());
			}
			else
			{
				log("No matches for " + regex + " found in " + basedir.getAbsolutePath(), Project.MSG_ERR);
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
