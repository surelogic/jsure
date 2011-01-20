/**
 * 
 */
package com.surelogic.ant.tasks;

import java.io.File;

import org.apache.tools.ant.Task;

/**
 * @author Ethan.Urie
 *
 */
public class GetFileExtension extends Task
{
	private File file = null;
	private String property = null;

	public void execute()
	{
		if(file.exists())
		{
			if(file.isFile())
			{
				String path = file.getAbsolutePath();
				int index = path.lastIndexOf(".");
				getProject().setProperty(property, path.substring(index+1));
			}
			else
			{
				log("File: " + file.getAbsolutePath() + " is a directory");
			}
		}
		else
		{
			log("File: " + file.getAbsolutePath() + " does not exist.");
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
