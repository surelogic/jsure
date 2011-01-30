package com.surelogic.ant.tasks;

import java.io.*;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.tools.ant.*;

/**
 * Generates our sl.test.properties file in the user's home directory with
 * detected/generated/guessed values
 * <p>
 * Ant usage: &lt;generatebuildproperties clearDeprecated="false"/&gt;
 * <p>
 * clearDeprecated - optional, defaults to false. If true, will remove all
 * properties in the user's sl.test.properties file that don't exist in the
 * default file.
 * <p>
 * No parameters required
 */
public class GenerateBuildProperties extends Task {

	private Properties properties; // user customizable properties

	private Properties defaults; // Default properties

	private boolean machineSpecific = false;

	private boolean clearDeprecated = false;

	private static final String PROP_FILENAME = "sl.test.properties";

	private static final String MACOSX_PROP_FILENAME = "lib/sl.macosx.test.properties";

	private static final String WIN_PROP_FILENAME = "lib/sl.win.test.properties";

	private static final String LINUX_PROP_FILENAME = "lib/sl.linux.test.properties";

	private static final File defaultsFile = new File(
			System.getProperty("user.dir") + "/lib", PROP_FILENAME);

	private static File propertiesFile;

	private static final String SYS_OS_KEY = "os.name";

	private static final String SYS_ARCH_KEY = "os.arch";

	private static final String ANT_OS_KEY = "baseos";

	private static final String ANT_ARCH_KEY = "basearch";

	private static final String ANT_WS_KEY = "basews";

	private static final String ROOT_KEY = "root";

	private static final String JAVAC_SOURCE_KEY = "javacSource";

	private static final String JAVAC_TARGET_KEY = "javacTarget";

	private static final String JAVA1_4 = "1.4";

	private static final String JAVA5 = "1.5";

	private static final String JAVA6 = "1.6";

	private static final String X86 = "x86";

	private static final String I386 = "i386";

	private static final String JAVA_PPC = "PowerPC";

	private static final String PPC = "ppc";

	private static final String VCS_USERNAME_KEY = "vcs.username";

	private static final String DEV_WORKSPACE_DIR_KEY = "dev.workspace.dir";

	private static final String BUILDER_DIR_KEY = "builder.project.dir";

	@Override
	public void execute() {
		FileInputStream fin = null;
		properties = new Properties();
		defaults = new Properties();
		try {
			setupPropertiesFile();
			fin = new FileInputStream(propertiesFile);
			properties.load(fin);

			defaults.load(new FileInputStream(defaultsFile));
		} catch (FileNotFoundException e) {
			throw new BuildException(e);
		} catch (IOException e) {
			throw new BuildException(e);
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (Exception e) {
					// Don't do anything
					e.printStackTrace();
				}
			}

		}

		// printAllProperties(System.getProperties());
		generateProperties();
		verifyAndSetProperties();
		if (clearDeprecated) {
			clearDeprecatedProps();
		}
		// printAllProperties(properties);
		generatePropertyFile();
	}

	private void setupPropertiesFile() {
		if (propertiesFile == null) {
			final File userHome = new File(System.getProperty("user.home"));
			final String file = getProject().getProperty("test.properties");
			propertiesFile = new File(userHome, file == null ? PROP_FILENAME
					: file);
		}
		if (propertiesFile.isFile()) {
			log("Modifying existing properties file "
					+ propertiesFile.getAbsolutePath());
		} else {
			boolean success = false;
			try {
				success = propertiesFile.createNewFile();
			} catch (IOException e) {
				// nothing to do
			}
			if (success) {
				log("Created properties file "
						+ propertiesFile.getAbsolutePath());
			} else {
				throw new BuildException("Properties file, "
						+ propertiesFile.getAbsolutePath()
						+ " is not a valid file and cannot be created.");
			}
		}
	}

	/**
	 * Detects and sets those properties that can be logically deduced.
	 */
	private void generateProperties() {
		setOSProperties();
		setArchitectureProperties();
		setJavaProperties();
		setMiscProperties();
	}

	/**
	 * Removes properties that are not in the default properties file anymore
	 * Has to be run after loading the user's properties.
	 */
	private void clearDeprecatedProps() {
		Enumeration<?> props = properties.propertyNames();
		while (props.hasMoreElements()) {
			String prop = (String) props.nextElement();
			if (!defaults.containsKey(prop)) {
				properties.remove(prop);
				log("Removed " + prop);
			}
		}
	}

	private void setMiscProperties() {
		defaults.setProperty(VCS_USERNAME_KEY, System.getProperty("user.name"));
		File currentDirectory = new File(System.getProperty("user.dir"));
		getProject().setProperty(DEV_WORKSPACE_DIR_KEY,
				currentDirectory.getParent());
		getProject().setProperty(BUILDER_DIR_KEY,
				currentDirectory.getAbsolutePath());
	}

	/**
	 * Sets OS-specific properties.
	 */
	private void setOSProperties() {
		// Take care of the special cases
		String OS = System.getProperty(SYS_OS_KEY);

		// Want to capture all windows versions
		try {
			if (OS.startsWith("Windows")) {
				setWindowsProperties();
			} else if ("Mac OS X".equals(OS)) {
				setMacOSXProperties();
			} else if ("Linux".equals(OS)) {
				setLinuxProperties();
			} else {
				log("Unrecognized OS: " + OS, Project.MSG_ERR);
			}
		} catch (IOException e) {
			log("Could not set OS-specific settings for OS: " + OS, e,
					Project.MSG_ERR);
		}

	}

	/**
	 * Sets architecture-specific properties.
	 */
	private void setArchitectureProperties() {
		String arch = System.getProperty(SYS_ARCH_KEY);
		if (machineSpecific) {
			if (X86.equals(arch) || I386.equals(arch)) {
				defaults.setProperty(ANT_ARCH_KEY, X86);
			} else if (JAVA_PPC.equals(arch)) {
				defaults.setProperty(ANT_ARCH_KEY, PPC);
			} else {
				log("Unknown CPU architecture: " + arch, Project.MSG_ERR);
			}
		}
	}

	/**
	 * Sets Java-specific properties.
	 */
	private void setJavaProperties() {
		String javaVersion = System.getProperty("java.specification.version");
		if (javaVersion.equals(JAVA1_4)) {
			defaults.setProperty(JAVAC_SOURCE_KEY, JAVA1_4);
			defaults.setProperty(JAVAC_TARGET_KEY, JAVA1_4);
		} else if (javaVersion.equals(JAVA5)) {
			defaults.setProperty(JAVAC_SOURCE_KEY, JAVA5);
			defaults.setProperty(JAVAC_TARGET_KEY, JAVA5);
		} else if (javaVersion.equals(JAVA6)) {
			defaults.setProperty(JAVAC_SOURCE_KEY, JAVA6);
			defaults.setProperty(JAVAC_TARGET_KEY, JAVA6);
		} else {
			log("Unknown Java version: " + javaVersion, Project.MSG_ERR);
		}
	}

	/**
	 * Sets Linux-specific settings.
	 */
	private void setLinuxProperties() throws IOException {
		FileInputStream fin = new FileInputStream(LINUX_PROP_FILENAME);
		defaults.load(fin);
		if (machineSpecific) {
			defaults.setProperty(ANT_OS_KEY, "linux");
			defaults.setProperty(ANT_WS_KEY, "gtk");// this may be 'motif'
		}
		defaults.setProperty(ROOT_KEY, "");
		fin.close();
	}

	/**
	 * Sets Mac OS X-specific settings.
	 */
	private void setMacOSXProperties() throws IOException {
		FileInputStream fin = new FileInputStream(MACOSX_PROP_FILENAME);
		defaults.load(fin);
		if (machineSpecific) {
			defaults.setProperty(ANT_OS_KEY, "macosx");
			defaults.setProperty(ANT_WS_KEY, "carbon");
		}
		defaults.setProperty(ROOT_KEY, "");

		fin.close();
	}

	/**
	 * Sets Windows-specific settings.
	 */
	private void setWindowsProperties() throws IOException {
		FileInputStream fin = new FileInputStream(WIN_PROP_FILENAME);
		defaults.load(fin);
		if (machineSpecific) {
			defaults.setProperty(ANT_OS_KEY, "win32");
			defaults.setProperty(ANT_WS_KEY, "win32");
		}
		defaults.setProperty(ROOT_KEY, "C:");
		fin.close();
	}

	/**
	 * Takes the properties from the default properties and adds them to the
	 * properties file if they don't exist.
	 */
	private void verifyAndSetProperties() {
		if (properties.size() > 0) {
			/*
			 * remove plug-in and feature project properties so that they will
			 * be reset.
			 */
			properties.remove("plugin.projects");
			properties.remove("feature.projects");

			Enumeration<Object> keys = defaults.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				// Don't overwrite existing properties
				if (!properties.containsKey(key)) {
					properties.put(key, defaults.getProperty(key));
				}
			}
		}
		// If the sl.test.properties file is blank, put all our defaults there
		else {
			properties.putAll(defaults);
		}
	}

	/**
	 * Writes out the sl.test.properties file.
	 */
	private void generatePropertyFile() throws BuildException {
		try {
			properties.store(new FileOutputStream(propertiesFile),
					"Build properties for SureLogic automated testing suite.");

		} catch (FileNotFoundException e) {
			throw new BuildException(e);
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	public boolean isMachineSpecific() {
		return machineSpecific;
	}

	public void setMachineSpecific(boolean genMachineSpecifics) {
		this.machineSpecific = genMachineSpecifics;
	}

	public final boolean isClearDeprecated() {
		return clearDeprecated;
	}

	public final void setClearDeprecated(boolean removeUnused) {
		this.clearDeprecated = removeUnused;
	}

	public static final void setPropertiesFile(File propertiesFile) {
		GenerateBuildProperties.propertiesFile = propertiesFile;
	}

}
