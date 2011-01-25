package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FileFilter;

class RegressionTestFilter implements FileFilter {

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
