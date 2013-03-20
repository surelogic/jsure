package com.surelogic.ant.jsure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.StringUtils;

import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.remote.*;
import com.surelogic.common.java.*;
import com.surelogic.javac.ConfigZip;
import com.surelogic.javac.Javac;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;
import com.surelogic.javac.jobs.JSureConstants;
import com.surelogic.javac.jobs.LocalJSureJob;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JSureJavacAdapter extends DefaultCompilerAdapter {
	boolean keepRunning = true;

	Path sourcepath = null;
	final JSureScan scan;

	public JSureJavacAdapter(JSureScan s) {
		scan = s;
	}

	@Override
  public boolean execute() throws BuildException {
		// TODO Check if home, document, projectname are defined and valid
		/*
		 * for(Object key : System.getProperties().keySet()) {
		 * System.out.println("Key: "+key); }
		 
		if (false) {
			checkClassPath("sun.boot.class.path");
			checkClassPath("java.class.path");
		}
		*/
		try {
			// Set up prefs
			System.out.println("project = " + scan.getProjectName());
			Javac.initialize();
			Javac.getDefault().setPreference(
					IDEPreferences.JSURE_DATA_DIRECTORY, scan.getDataDir());
			
			/*
			 * TODO: This is BROKEN because it points the user's DIFF directory to the release directory
			 * This needs to set -D RemoteJSureRun.FLUID_DIRECTORY_URL to the right place
			 * See LocalJSureJob class (I think)
			 * 
			 * BROKEN BROKEN TODO TODO BROKEN BROKEN
			 */
			Javac.getDefault().setPreference(
					IDEPreferences.JSURE_XML_DIFF_DIRECTORY,
					scan.getHome() + "/lib/fluid/"
							+ TestXMLParserConstants.PROMISES_XML_REL_PATH);

			Config config = createConfig();
			System.out.println("config = " + config.getProject());

			final Projects projects = new Projects(config,
					new NullSLProgressMonitor());
			System.out.println("projects = " + projects.getLabel());
			System.out.println("data-dir = " + scan.getDataDir());
			final File dataDir = new File(scan.getDataDir());
			projects.computeScan(dataDir, null);

			// Zip up the sources for future reference
			final File zips = new File(projects.getRunDir(),
					PersistenceConstants.ZIPS_DIR);
			if (zips.mkdirs()) {
				ConfigZip zip = new ConfigZip(config);
				FileOutputStream o = new FileOutputStream(new File(zips,
						config.getProject() + ".zip"));
				ZipOutputStream out = new ZipOutputStream(o);
				try {
					zip.generateSourceZipContents(out);
				} finally {
					out.close();
				}
			}

			// Note: this doesn't work if we don't have our javac library
			// installed
			//
			// Util.openFiles(projects, true);
			//
			// Run as a separate JVM?
			System.out.println("run dir = " + projects.getRunDir());
			Javac.getDefault().savePreferences(projects.getRunDir());

			final String msg = "Running JSure for " + projects.getLabel();
			LocalJSureJob.factory.newJob(msg, 100, makeJSureConfig(projects))
					.run(new NullSLProgressMonitor());
		} catch (Throwable t) {
			t.printStackTrace();
			throw new BuildException("Exception while scanning", t);
		}
		return true;
	}

	private ILocalConfig makeJSureConfig(final Projects projects) {
		final int memSize = parseMemorySize(memoryMaximumSize);
		return new AbstractLocalConfig(memSize, projects.getRunDir()) {
			@Override
      public boolean isVerbose() {
				return verbose;
			}

			@Override
      public String getPluginDir(String id, boolean required) {
				File home = new File(scan.getHome());
				if (JSureConstants.COMMON_PLUGIN_ID.equals(id)) {
					return new File(home, "lib/common").getAbsolutePath();
				} else if (JSureConstants.FLUID_PLUGIN_ID.equals(id)) {
					return new File(home, "lib/fluid").getAbsolutePath();
				}
				throw new IllegalStateException("Unknown plugin id requested: "
						+ id);
			}
		};
	}

	@SuppressWarnings("unused")
	private void checkClassPath(String key) {
		StringTokenizer st = new StringTokenizer(System.getProperty(key),
				File.pathSeparator);
		while (st.hasMoreTokens()) {
			System.out.println(key + ": " + st.nextToken());
		}
	}

	private Config createConfig() throws IOException {
		Config config = new Config(scan.getProjectName(), null, false, false); // TODO
																		// what
																		// location?
		config.setOption(Config.AS_SOURCE, Boolean.TRUE);
		final String srcLevel = scan.getSource();
		if (srcLevel.startsWith("1.")) {
			config.setOption(Config.SOURCE_LEVEL,
					Integer.parseInt(srcLevel.substring(2)));
		}
		setupConfig(config, false);
		logAndAddFilesToCompile(config);
		/*
		 * if (verbose) { System.out.println("verbose = "+verbose); }
		 * config.setVerbose(verbose); setMemorySize(config);
		 * config.setJavaVendor(System.getProperty("java.vendor"));
		 * config.setJavaVersion(System.getProperty("java.version"));
		 * 
		 * if (scan.getHome() == null) { throw new
		 * BuildException("No value for home"); } //
		 * C:/work/workspace/sierra-ant final String libHome =
		 * scan.getHome()+"/lib/"; if (!new File(libHome).exists()) { throw new
		 * BuildException("No lib subdirectory under "+libHome); }
		 * System.setProperty(ToolUtil.TOOLS_PATH_PROP_NAME, libHome); final
		 * String toolsDir = libHome+"tools/";
		 * config.setExcludedToolsList("Checkstyle"); for (IToolFactory f :
		 * ToolUtil.findToolFactories()) { if (!"Checkstyle".equals(f.getId()))
		 * { for(final IToolExtension t : f.getExtensions()) { if (t.isCore()) {
		 * // Implied by the above continue; } final ToolExtension ext = new
		 * ToolExtension(); ext.setTool(f.getId()); ext.setId(t.getId());
		 * ext.setVersion(t.getVersion()); config.addExtension(ext); } } }
		 * config.setToolsDirectory(new File(toolsDir+"reckoner"));
		 * config.putPluginDir(SierraToolConstants.COMMON_PLUGIN_ID,
		 * libHome+"common.jar");
		 * config.putPluginDir(SierraToolConstants.MESSAGE_PLUGIN_ID,
		 * libHome+"sierra-message.jar");
		 * config.putPluginDir(SierraToolConstants.PMD_PLUGIN_ID,
		 * toolsDir+"pmd");
		 * config.putPluginDir(SierraToolConstants.FB_PLUGIN_ID,
		 * toolsDir+"findbugs");
		 * config.putPluginDir(SierraToolConstants.TOOL_PLUGIN_ID,
		 * libHome+"sierra-tool.jar");
		 * config.putPluginDir(SierraToolConstants.JUNIT4_PLUGIN_ID,
		 * libHome+"junit"); if (SystemUtils.IS_JAVA_1_5) {
		 * System.out.println("Home: "+scan.getHome());
		 * config.putPluginDir(SierraToolConstants.JAVA5_PLUGIN_ID,
		 * scan.getHome()); }
		 * //System.out.println("Using source level "+scan.getSource());
		 * config.setSourceLevel(scan.getSource());
		 * 
		 * File scanDocument = new File(scan.getDocument() + (USE_ZIP ?
		 * PARSED_ZIP_FILE_SUFFIX : PARSED_FILE_SUFFIX));
		 * config.setScanDocument(scanDocument);
		 */
		return config;
	}

	/*
	 * private void setMemorySize(Config config) { int max =
	 * parseMemorySize(scan.getMemoryMaximumSize()); int init =
	 * parseMemorySize(scan.getMemoryInitialSize()); config.setMemorySize(max >
	 * init ? max : init); }
	 */

	private int parseMemorySize(String memSize) {
		if (memSize != null && !"".equals(memSize)) {
			int last = memSize.length() - 1;
			char lastChar = memSize.charAt(last);
			int size, mb = 1024;
			switch (lastChar) {
			case 'm':
			case 'M':
				mb = Integer.parseInt(memSize.substring(0, last));
				break;
			case 'g':
			case 'G':
				size = Integer.parseInt(memSize.substring(0, last));
				mb = size * 1024;
				break;
			case 'k':
			case 'K':
				size = Integer.parseInt(memSize.substring(0, last));
				mb = (int) Math.ceil(size / 1024.0);
				break;
			default:
				// in bytes
				size = Integer.parseInt(memSize);
				mb = (int) Math.ceil(size / (1024 * 1024.0));
			}
			return mb;
		}
		return 1024;
	}

	private void addPath(Config config, Path path) {
		for (String elt : path.list()) {
			File f = new File(elt);
			if (f.exists()) {
				// System.out.println("Adding "+elt);
				if (f.isDirectory()) {
					Util.addJavaFiles(f, config);
				} else {
					config.addJar(elt);
				}
			}
		}
	}

	/**
	 * Originally based on
	 * DefaultCompilerAdapter.setupJavacCommandlineSwitches()
	 */
	protected Config setupConfig(Config cmd, boolean useDebugLevel) {
		Path classpath = getCompileClasspath();
		Path bootClasspath = getBootClassPath();
		// For -sourcepath, use the "sourcepath" value if present.
		// Otherwise default to the "srcdir" value.
		Path sourcepath;
		if (compileSourcepath != null) {
			sourcepath = compileSourcepath;
		} else {
			sourcepath = src;
		}

		/*
		 * if (memoryMaximumSize != null) { if (!attributes.isForkedJavac()) {
		 * attributes.log("Since fork is false, ignoring " + "memoryMaximumSize
		 * setting.", Project.MSG_WARN); } else {
		 * cmd.createArgument().setValue(memoryParameterPrefix + "mx" +
		 * memoryMaximumSize); } }
		 */

		// if (destDir != null) {
		// cmd.addTarget(new FullDirectoryTarget(Type.BINARY, destDir
		// .toURI()));
		// }
		if (bootClasspath.size() == 0) {
			// try to use sun.boot.classpath
			bootClasspath = new Path(getProject());
			StringTokenizer st = new StringTokenizer(
					System.getProperty("sun.boot.class.path"),
					File.pathSeparator);
			while (st.hasMoreTokens()) {
				bootClasspath.add(new Path(getProject(), st.nextToken()));
			}
		}
		/*
		 * TODO add directly to projects Config binaries = new
		 * Config("Dependencies", null, true); // TODO what location?
		 * addPath(binaries, bootClasspath); addPath(binaries, classpath);
		 * cmd.addToClassPath(binaries);
		 */
		addPath(cmd, bootClasspath);
		addPath(cmd, classpath);

		// If the buildfile specifies sourcepath="", then don't
		// output any sourcepath.
		if (sourcepath.size() > 0) {
			// addPath(cmd, Type.SOURCE, sourcepath);
			this.sourcepath = sourcepath;
		}

		/*
		 * Path bp = getBootClassPath(); if (bp.size() > 0) { addPath(cmd,
		 * Type.AUX, bp); }
		 */

		/*
		 * if (verbose) { cmd.createArgument().setValue("-verbose"); }
		 */

		return cmd;
	}

	/**
	 * Based on DefaultCompilerAdapter.logAndAddFilesToCompile()
	 */
	protected void logAndAddFilesToCompile(Config config) {
		attributes.log("Compilation for " + config.getProject(),
				Project.MSG_VERBOSE);

		StringBuffer niceSourceList = new StringBuffer("File");
		if (compileList.length != 1) {
			niceSourceList.append('s');
		}
		niceSourceList.append(" to be compiled:");

		niceSourceList.append(StringUtils.LINE_SEP);

		for (int i = 0; i < compileList.length; i++) {
			addJavaFile(config, compileList[i]);
			niceSourceList.append("    ");
			niceSourceList.append(compileList[i].getAbsolutePath());
			niceSourceList.append(StringUtils.LINE_SEP);
		}
		/*
		 * 
		 * if (attributes.getSourcepath() != null) { addPath(config,
		 * Type.SOURCE, attributes.getSourcepath()); } else { addPath(config,
		 * Type.SOURCE, attributes.getSrcdir()); } addPath(config, Type.AUX,
		 * attributes.getClasspath());
		 */

		attributes.log(niceSourceList.toString(), Project.MSG_VERBOSE);
	}

	private void addJavaFile(Config config, File file) {
		if (!file.getName().endsWith(".java")) {
			return;
		}
		String path = file.getAbsolutePath();
		String srcPath = findSrcDir(path);
		if (srcPath != null) {
			String relPath = path.substring(srcPath.length() + 1);
			String qname = computeQualifiedName(relPath);
			config.addFile(new JavaSourceFile(qname, file, relPath, false));
			// System.out.println(qname+": "+file.getAbsolutePath());

			final int lastDot = qname.lastIndexOf('.');
			config.addPackage(lastDot < 0 ? "" : qname.substring(0, lastDot));
		}
	}

	private String computeQualifiedName(String path) {
		String noSuffix = path.substring(0, path.length() - 5);
		return noSuffix.replace(File.separatorChar, '.');
	}

	private String findSrcDir(String arg) {
		for (String src : sourcepath.list()) {
			if (arg.startsWith(src)) {
				return src;
			}
		}
		return null;
	}
	/*
	 * class Monitor extends NullSLProgressMonitor { public void failed(String
	 * msg) { System.err.println(msg); }
	 * 
	 * public void failed(String msg, Throwable t) { System.err.println(msg);
	 * t.printStackTrace(System.err); }
	 * 
	 * public boolean isCanceled() { return !keepRunning; }
	 * 
	 * public void setCanceled(boolean value) { keepRunning = false; } }
	 */
}
