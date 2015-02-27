package com.surelogic.javac.jobs;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

import org.apache.commons.lang3.SystemUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.XUtil;
import com.surelogic.common.jobs.remote.*;

import edu.cmu.cs.fluid.parse.JJNode;

public class LocalJSureJob extends AbstractLocalSLJob<ILocalConfig> {
	@SuppressWarnings("unused")
  public static final int DEFAULT_PORT = true ? 0 : 20111;
	public static final AbstractLocalHandlerFactory<LocalJSureJob,ILocalConfig> factory = 
		new AbstractLocalHandlerFactory<LocalJSureJob,ILocalConfig>("jsure-console", LocalJSureJob.DEFAULT_PORT) {
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
		return super.getRemoteClassName();
	}
	
	@Override
	protected Class<? extends AbstractRemoteSLJob> getRemoteClass() {
		return RemoteJSureRun.class;
	}

	@Override
	protected void setupClassPath(final ConfigHelper util, CommandlineJava cmdj, Project proj, Path path) {
		// All unpacked
		util.addPluginAndJarsToPath(COMMON_PLUGIN_ID, "lib/runtime");
		util.addPluginAndJarsToPath(JSureConstants.JSURE_COMMON_PLUGIN_ID, "lib/runtime");
		util.addPluginAndJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, "lib/runtime");
		if (SystemUtils.IS_JAVA_1_8) {
			util.addPluginAndJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, "lib/runtime8");
		} else {
			util.addPluginAndJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, "lib/runtime7");
		}
		
		final boolean isMac = System.getProperty("sun.boot.class.path") != null || SystemUtils.IS_OS_MAC_OSX;
		if (XUtil.testing) {
			util.addPluginAndJarsToPath(JSureConstants.JSURE_TESTS_PLUGIN_ID, "lib");
			util.addPluginJarsToPath(JSureConstants.JUNIT_PLUGIN_ID, "junit.jar");
		}
		
		for(File jar : util.getPath()) {
			addToPath(proj, path, jar, true);
		}

		if (isMac) {
			// Add lib/javac.jar to the bootpath
			util.clear();
			if (SystemUtils.IS_JAVA_1_8) {
				util.addPluginJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, "lib/runtime8/javac_8.jar");
			} else {
				util.addPluginJarsToPath(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, "lib/runtime7/javac.jar");
			}
			final Path bootpath = cmdj.createBootclasspath(proj);		
			for(File jar : util.getPath()) {
				addToPath(proj, bootpath, jar, true);
			}
            if (usePrivateJRE) {
            	final File jre = new File(JRE_HOME);
             	for(String p : JRE_PATH) {
            		addToPath(proj, bootpath, new File(jre, p), false);
            	}
            } else {
            	String defaultPath = System.getProperty("sun.boot.class.path");
            	for(String p : defaultPath.split(File.pathSeparator)) {
            		addToPath(proj, bootpath, new File(p), false);
            	}
            }
			
			for(String s : bootpath.list()) {
				println("Boot classpath: "+s);
			}		
		}
	}

	private static final String[] JRE_PATH = {
		"lib/resources.jar",
		"lib/rt.jar",
		//"lib/sunrsasign.jar",
		"lib/jsse.jar",
		"lib/jce.jar",
		"lib/charsets.jar",
		//"lib/jfr.jar",
	};
	
	@Override
	protected void finishSetupJVM(boolean debug, CommandlineJava cmdj, Project proj) {			
		cmdj.createVmArgument().setValue("-Dfluid.ir.versioning=Versioning."+(JJNode.versioningIsOn ? "On" : "Off"));
		if (XUtil.testing) {
			cmdj.createVmArgument().setValue("-D"+AnnotationRules.XML_LOG_PROP+"=RemoteJSureRun.AnnotationRules");
			cmdj.createVmArgument().setValue("-D"+XUtil.testingProp+"="+XUtil.testing);
		}
		
		final ConfigHelper util = new ConfigHelper(debug, config);
		String location = util.getPluginDir(JSureConstants.JSURE_ANALYSIS_PLUGIN_ID, true);
		try {
			cmdj.createVmArgument().setValue("-D"+RemoteJSureRun.FLUID_DIRECTORY_URL+"="+new File(location).toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		try {
			for(Map.Entry<Object,Object> e : new Properties(System.getProperties()).entrySet()) {
				println(e.getKey()+" => "+e.getValue());
			}
		} catch(ConcurrentModificationException e) {
			// Ignore
		}
		
		// Only for debugging the remote JVM
		if (XUtil.debug) {
			cmdj.createVmArgument().setValue("-Xdebug");
			cmdj.createVmArgument().setValue("-Xrunjdwp:transport=dt_socket,address=8000,suspend=y"); // Connect to Eclipse		
		}
		if (XUtil.loadAllLibs) {
			cmdj.createVmArgument().setValue("-D"+XUtil.LOAD_ALL_LIBS+"=true");
		}
	}
}
