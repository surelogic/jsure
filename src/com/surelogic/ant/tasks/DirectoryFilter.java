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
class DirectoryFilter implements FileFilter {

	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	public boolean accept(File arg0) {
		return arg0.isDirectory();
	}

}
