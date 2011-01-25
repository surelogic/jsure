package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FileFilter;

public class JarFilter implements FileFilter {
	public boolean accept(File pathname) {
		return pathname.getAbsolutePath().endsWith(".jar");
	}
}
