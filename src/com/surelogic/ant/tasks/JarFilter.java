/**
 * 
 */
package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Ethan.Urie
 *
 */
public class JarFilter implements FileFilter
{

	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	public boolean accept(File pathname)
	{
		return pathname.getAbsolutePath().endsWith(".jar");
	}

}
