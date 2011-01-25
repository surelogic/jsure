package com.surelogic.ant.tasks;

import java.io.File;
import java.io.FileFilter;

public class UberFileFilter implements FileFilter {
	private final String filename;
	private final String extension;

	/**
	 * @param filename
	 *            The name of the file, minus extension. May be null.
	 * @param extension
	 *            The extension on the file, minus the '.'. May be null.
	 */
	public UberFileFilter(String filename, String extension) {
		this.filename = filename;
		this.extension = extension;
	}

	public boolean accept(File arg0) {
		boolean accept = false;
		if (filename != null) {
			if (extension != null) {
				accept = arg0.getName().equals(filename + "." + extension);
			} else {
				String nameMinusExt = getNameOnly(arg0.getName());
				accept = nameMinusExt.equals(filename);
			}
		} else if (extension != null) {
			accept = arg0.getName().endsWith(extension);
		}
		return accept;
	}

	private String getNameOnly(String namePlusExt) {
		int off = namePlusExt.lastIndexOf('.');
		return namePlusExt.substring(0, off);
	}
}
