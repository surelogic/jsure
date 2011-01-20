/**
 * 
 */
package com.surelogic.ant.tasks;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * @author Ethan.Urie
 *
 */
public class FilenameFromPath extends Task
{
	private String property = null;
	private File file = null;

	public void execute()
	{
		if(file.exists())
		{
			getProject().setProperty(property, file.getName());
		}
		else
		{
			log("Error: " + file.getAbsolutePath() + " does not exist.", Project.MSG_WARN);
		}
	}

	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
	}

	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}

}
