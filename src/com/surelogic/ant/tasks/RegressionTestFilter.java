/**
 * 
 */
package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Edwin.Chan
 */
class RegressionTestFilter implements FileFilter {

	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	public boolean accept(File f) {
		boolean rv;
		if (f.isDirectory()) {
			rv = true;
		} else {
			rv = f.getName().endsWith(".zip");
		}
		//System.out.println(f.getName()+" => "+rv);
		return rv;
	}
}
