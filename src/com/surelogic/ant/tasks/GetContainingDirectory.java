package com.surelogic.ant.tasks;

import java.io.File;

import org.apache.tools.ant.Task;

public class GetContainingDirectory extends Task {
	private File file;
	private String property;

	public void execute() {
		if (file != null && file.isFile()) {
			/*
			 * Determine the directory that this file is contained within and
			 * set it as the outgoing property.
			 */
			getProject().setProperty(property, file.getParent());
		} else {
			log((file == null ? "null" : file.getAbsolutePath())
					+ " does not exist on your machine (or is a directory).");
		}
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}
}
