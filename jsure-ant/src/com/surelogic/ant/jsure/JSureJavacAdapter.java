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

import com.surelogic.Nullable;
import com.surelogic.common.CommonJVMPrefs;
import com.surelogic.common.FileUtility;
import com.surelogic.common.FileUtility.TempFileFilter;
import com.surelogic.common.java.Config;
import com.surelogic.common.java.JavaSourceFile;
import com.surelogic.common.java.PersistenceConstants;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.remote.AbstractLocalConfig;
import com.surelogic.common.jobs.remote.AbstractLocalSLJob;
import com.surelogic.common.jobs.remote.ILocalConfig;
import com.surelogic.javac.ConfigZip;
import com.surelogic.javac.Javac;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;
import com.surelogic.javac.jobs.JSureConstants;
import com.surelogic.javac.jobs.LocalJSureJob;

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
    try {
      System.out.println("Project name to scan w/JSure = " + scan.getJSureProjectName());

      // temp output location for scan
      final TempFileFilter scanDirFileFilter = new TempFileFilter("jsureAnt", ".scandir");
      final File tempDir = scanDirFileFilter.createTempFolder();

      // surelogic-tools.properties file
      final File surelogicToolsProperties = scan.getSurelogicToolsPropertiesAsFile();
      if (surelogicToolsProperties != null)
        System.out.println("Using properties in          = " + surelogicToolsProperties.getAbsolutePath());

      System.out.println("Directory to write scan      = " + scan.getJSureScanDirAsFile().getAbsolutePath());

      Javac.initialize();
      Javac.getDefault().setPreference(IDEPreferences.JSURE_DATA_DIRECTORY, tempDir.getAbsolutePath());

      System.setProperty(LocalJSureJob.FLUID_DIRECTORY_URL, new File(scan.getJSureAntHomeAsFile(), "lib/jsure-analysis").toURI()
          .toURL().toString());
      System.setProperty(CommonJVMPrefs.PROP, new File(scan.getJSureAntHomeAsFile(), "lib/common" + CommonJVMPrefs.PATH).toURI()
          .toURL().toString());

      final Config config = createConfig(surelogicToolsProperties);
      final Projects projects = new Projects(config, new NullSLProgressMonitor());

      projects.computeScan(tempDir, null);

      // Zip up the sources for future reference
      final File zips = new File(projects.getRunDir(), PersistenceConstants.ZIPS_DIR);
      if (zips.mkdirs()) {
        ConfigZip zip = new ConfigZip(config);
        FileOutputStream o = new FileOutputStream(new File(zips, config.getProject() + ".zip"));
        ZipOutputStream out = new ZipOutputStream(o);
        try {
          zip.generateSourceZipContents(out);
        } finally {
          out.close();
        }
      }

      final File outputDir = projects.getRunDir();
      Javac.getDefault().savePreferences(outputDir);

      final String scanName = outputDir.getName();
      final File zipFile = new File(scanName + ".zip");
      System.out.println("Scan " + scanName + " running...");
      final String msg = "Running JSure for " + projects.getLabel();
      LocalJSureJob.factory.newJob(msg, 100, makeJSureConfig(projects)).run(new NullSLProgressMonitor());
      FileUtility.zipDir(outputDir, zipFile);
      if (!FileUtility.recursiveDelete(tempDir)) {
        System.out.println("Error unable to delete temp dir " + tempDir.getAbsolutePath());
      }
    } catch (Throwable t) {
      t.printStackTrace();
      throw new BuildException("Exception while scanning " + scan.getJSureProjectName(), t);
    }
    return true;
  }

  private ILocalConfig makeJSureConfig(final Projects projects) {
    final int memSize = parseMemorySize(memoryMaximumSize);
    return new AbstractLocalConfig(memSize, projects.getRunDir()) {
      @Override
      @SuppressWarnings("synthetic-access")
      public boolean isVerbose() {
        return verbose;
      }

      @Override
      public String getPluginDir(String id, boolean required) {
        final File home = scan.getJSureAntHomeAsFile();
        if (AbstractLocalSLJob.COMMON_PLUGIN_ID.equals(id)) {
          return new File(home, "lib/common").getAbsolutePath();
        } else if (JSureConstants.JSURE_COMMON_PLUGIN_ID.equals(id)) {
          return new File(home, "lib/jsure-common").getAbsolutePath();
        } else if (JSureConstants.JSURE_ANALYSIS_PLUGIN_ID.equals(id)) {
          return new File(home, "lib/jsure-analysis").getAbsolutePath();
        }
        throw new IllegalStateException("Unknown plugin id requested: " + id);
      }
    };
  }

  @SuppressWarnings("unused")
  private void checkClassPath(String key) {
    StringTokenizer st = new StringTokenizer(System.getProperty(key), File.pathSeparator);
    while (st.hasMoreTokens()) {
      System.out.println(key + ": " + st.nextToken());
    }
  }

  private Config createConfig(@Nullable File surelogicToolsPropertyFile) throws IOException {
    Config config = new Config(scan.getJSureProjectName(), null, false, false);
    config.setOption(Config.AS_SOURCE, Boolean.TRUE);
    config.initFromSureLogicToolsProps(surelogicToolsPropertyFile);
    final String srcLevel = scan.getSource();
    if (srcLevel.startsWith("1.")) {
      config.setOption(Config.SOURCE_LEVEL, Integer.parseInt(srcLevel.substring(2)));
    }
    setupConfig(config, false);
    logAndAddFilesToCompile(config);
    return config;
  }

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
   * Originally based on DefaultCompilerAdapter.setupJavacCommandlineSwitches()
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
      StringTokenizer st = new StringTokenizer(System.getProperty("sun.boot.class.path"), File.pathSeparator);
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
     * Path bp = getBootClassPath(); if (bp.size() > 0) { addPath(cmd, Type.AUX,
     * bp); }
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
    attributes.log("Compilation for " + config.getProject(), Project.MSG_VERBOSE);

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
     * if (attributes.getSourcepath() != null) { addPath(config, Type.SOURCE,
     * attributes.getSourcepath()); } else { addPath(config, Type.SOURCE,
     * attributes.getSrcdir()); } addPath(config, Type.AUX,
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
      config.addFile(new JavaSourceFile(qname, file, relPath, false, config.getProject()));
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
