package com.surelogic.javac.jobs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.jobs.remote.AbstractLocalHandlerFactory;
import com.surelogic.common.jobs.remote.AbstractLocalSLJob;
import com.surelogic.common.jobs.remote.AbstractRemoteSLJob;
import com.surelogic.common.jobs.remote.ConfigHelper;
import com.surelogic.common.jobs.remote.Console;
import com.surelogic.common.jobs.remote.ILocalConfig;

public class LocalJSureJob extends AbstractLocalSLJob<ILocalConfig> {
  public static final String JSURE_ANALYSIS_DIRECTORY_URL = "jsure.analysis.directory.url";

  @SuppressWarnings("unused")
  public static final int DEFAULT_PORT = true ? 0 : 20111;
  public static final AbstractLocalHandlerFactory<LocalJSureJob, ILocalConfig> factory = new AbstractLocalHandlerFactory<LocalJSureJob, ILocalConfig>(
      "jsure-console", LocalJSureJob.DEFAULT_PORT) {
    @Override
    protected LocalJSureJob createJob(String name, int work, ILocalConfig config, Console console) {
      return new LocalJSureJob(name, work, config, console);
    }
  };

  /**
   * Assuming that everything's already persisted
   */
  LocalJSureJob(String name, int work, ILocalConfig config, Console console) {
    super(name, work, config, console);
  }

  @Override
  protected String getRemoteClassName() {
    if (XUtil.testing) {
      return "com.surelogic.jsure.tests.RemoteJSureRunTest";
    }
    return "com.surelogic.javac.jobs.RemoteJSureRun";
  }

  @Override
  protected Class<? extends AbstractRemoteSLJob> getRemoteClass() {
    throw new IllegalStateException();
  }

  @Override
  protected void setupClassPath(final ConfigHelper util, CommandlineJava cmdj, Project proj, Path path) {
    // All unpacked
    util.addPluginAndJarsToPath(SLUtility.COMMON_PLUGIN_ID, "lib/runtime");
    util.addPluginAndJarsToPath(JSureConstants.JSURE_COMMON_PLUGIN_ID, "lib/runtime");
    util.addPluginAndJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, "lib/runtime");
    /*
     * if (SystemUtils.IS_JAVA_1_7) {
     * util.addPluginAndJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID,
     * "lib/runtime8"); } else {
     * util.addPluginAndJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID,
     * "lib/runtime7"); }
     */
    // final boolean isMac = System.getProperty("sun.boot.class.path") != null
    // || SystemUtils.IS_OS_MAC_OSX;
    if (XUtil.testing) {
      util.addPluginAndJarsToPath(JSureConstants.JSURE_TESTS_PLUGIN_ID, "lib");
      util.addPluginJarsToPath(JSureConstants.JUNIT_PLUGIN_ID, "junit.jar");
    }

    for (File jar : util.getPath()) {
      addToPath(proj, path, jar, true);
    }
    /*
     * if (isMac) { // Add lib/javac.jar to the bootpath util.clear(); if
     * (SystemUtils.IS_JAVA_1_7) {
     * util.addPluginJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID,
     * "lib/runtime8/javac_8.jar"); } else {
     * util.addPluginJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID,
     * "lib/runtime7/javac.jar"); } final Path bootpath =
     * cmdj.createBootclasspath(proj); for(File jar : util.getPath()) {
     * addToPath(proj, bootpath, jar, true); } if (usePrivateJRE) { final File
     * jre = new File(JRE_HOME); for(String p : JRE_PATH) { addToPath(proj,
     * bootpath, new File(jre, p), false); } } else { String defaultPath =
     * System.getProperty("sun.boot.class.path"); for(String p :
     * defaultPath.split(File.pathSeparator)) { addToPath(proj, bootpath, new
     * File(p), false); } }
     * 
     * for(String s : bootpath.list()) { println("Boot classpath: "+s); } }
     */
  }

  @Override
  protected void finishSetupJVM(boolean debug, CommandlineJava cmdj, Project proj) {
    cmdj.createVmArgument().setValue("-Dfluid.ir.versioning=Versioning." + (JSureConstants.versioningIsOn ? "On" : "Off"));
    if (XUtil.testing) {
      cmdj.createVmArgument().setValue("-D" + AnnotationConstants.XML_LOG_PROP + "=RemoteJSureRun.AnnotationRules");
      cmdj.createVmArgument().setValue("-D" + XUtil.testingProp + "=" + XUtil.testing);
    }
    try {
      final URL jsureAnalysisURL = config.getPluginDirectory(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID).toURI().toURL();
      cmdj.createVmArgument().setValue("-D" + JSURE_ANALYSIS_DIRECTORY_URL + "=" + jsureAnalysisURL);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    try {
      for (Map.Entry<Object, Object> e : new Properties(System.getProperties()).entrySet()) {
        println(e.getKey() + " => " + e.getValue());
      }
    } catch (ConcurrentModificationException e) {
      // Ignore
    }

    // Only for debugging the remote JVM
    if (XUtil.debug) {
      cmdj.createVmArgument().setValue("-Xdebug");
      cmdj.createVmArgument().setValue("-Xrunjdwp:transport=dt_socket,address=8000,suspend=y"); // Connect
                                                                                                // to
                                                                                                // Eclipse
    }
    if (XUtil.loadAllLibs) {
      cmdj.createVmArgument().setValue("-D" + XUtil.LOAD_ALL_LIBS + "=true");
    }
  }
}
