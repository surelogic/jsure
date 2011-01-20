/**
 * Returns a comma-separated list of the directories contained at the top-most level of the given directory. For use with 
 * the ant-contrib for and foreach tasks (although it is not strictly required).
 * 
 * Usage:
 * <directorylist basedir="directory to search" fullpaths="true/false" property="property to set"/>
 * 
 * Required parameters:
 * basedir - The path to the directory to search for directories
 * fullpaths - Boolean indicating whether or not to return the absolute paths, or just the names of the located directories
 * property - The property to set with the comma-separated list
 */
package com.surelogic.ant.tasks;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * @author Ethan.Urie
 *
 */
public class DirectoryList extends Task {
	
	private File basedir = null;
	private boolean fullpaths = false;
	private String property = null;
	
	public void execute()
	{
		if(basedir.isDirectory())
		{
			File[] dirList = basedir.listFiles(new DirectoryFilter());
			StringBuffer list = new StringBuffer();
			String file = null;
			
			for (int i = 0; i < dirList.length; i++) {
				if(fullpaths)
				{
					file =  dirList[i].getAbsolutePath();
				}
				else
				{
					file =  dirList[i].getName();
				}
				list.append(file);
				if(i != dirList.length - 1)
				{
					list.append(",");
				}
			}
			getProject().setProperty(property, list.toString());
		}
		else
		{
			log(basedir.getAbsolutePath() + " is not a valid directory.", Project.MSG_ERR);
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
