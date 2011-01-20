/**
 * 
 */
package com.surelogic.ant.tasks;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.tools.ant.*;

/**
 * @author ethan
 * 
 */
public class GetEclipseVersion extends Task {
	private String property = null;
	private File file = null;
	private final static String regex = "\\d\\.\\d(\\.\\d)*";

	public void execute() {
		if (validParams()) {
			String name = file.getName();
			String[] parts = name.split("-");
			for (String string : parts) {
				if (Pattern.matches(regex, string)) {
					getProject().setProperty(property, string);
					break;
				}
			}
		}
		else{
			throw new BuildException("Parameters are invalid.");
		}
	}

	private boolean validParams() {
		boolean ret = true;
		if (!file.exists()) {
			ret = false;
			log("File referenced by 'file' does not exist.", Project.MSG_ERR);
		}
		else if (property == null || "".equals(property.trim())) {
			ret = false;
			log("Parameter 'property' is required and must not be an empty string.", Project.MSG_ERR);
		}
		return ret;
	}

	/**
	 * @return the property
	 */
	public final String getProperty() {
		return property;
	}

	/**
	 * @param property
	 *            the property to set
	 */
	public final void setProperty(String property) {
		this.property = property;
	}

	/**
	 * @return the file
	 */
	public final File getFile() {
		return file;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public final void setFile(File file) {
		this.file = file;
	}
}
