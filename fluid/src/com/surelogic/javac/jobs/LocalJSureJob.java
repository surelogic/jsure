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

public class LocalJSureJob extends AbstractLocalSLJob {
	@SuppressWarnings("unused")
  public static final int DEFAULT_PORT = true ? 0 : 20111;
	public static final AbstractLocalHandlerFactory<LocalJSureJob,ILocalJSureConfig> factory = 
		new AbstractLocalHandlerFactory<LocalJSureJob,ILocalJSureConfig>("jsure-console", LocalJSureJob.DEFAULT_PORT) {
		@Override
		protected LocalJSureJob createJob(String name, int work, ILocalJSureConfig config, Console console) {
			return new LocalJSureJob(name, work, config, console);
		}
	};
	
	private final ILocalJSureConfig config;
	
	/**
	 * Assuming that everything's already persisted
	 */
	LocalJSureJob(String name, int work, ILocalJSureConfig config, Console console) {
		super(name, work, config, console);
		this.config = config;
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
	protected void setupClassPath(boolean debug, CommandlineJava cmdj, Project proj, Path path) {
		final ConfigHelper util = new ConfigHelper(debug, config);
		// All unpacked
		util.addPluginAndJarsToPath(JSureConstants.COMMON_PLUGIN_ID, "lib/runtime");
		util.addPluginAndJarsToPath(JSureConstants.FLUID_PLUGIN_ID, "lib/runtime");
		final boolean isMac = SystemUtils.IS_OS_MAC_OSX;
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
			util.addPluginJarsToPath(JSureConstants.FLUID_PLUGIN_ID, "lib/runtime/javac.jar");
			
			final Path bootpath = cmdj.createBootclasspath(proj);		
			for(File jar : util.getPath()) {
				addToPath(proj, bootpath, jar, true);
			}
			String defaultPath = System.getProperty("sun.boot.class.path");
			for(String p : defaultPath.split(File.pathSeparator)) {
				addToPath(proj, bootpath, new File(p), false);
			}
			
			for(String s : bootpath.list()) {
				println("Boot classpath: "+s);
			}		
		}
	}
	
	@Override
	protected void finishSetupJVM(boolean debug, CommandlineJava cmdj, Project proj) {			
		cmdj.createVmArgument().setValue("-Dfluid.ir.versioning=Versioning."+(JJNode.versioningIsOn ? "On" : "Off"));
		if (XUtil.testing) {
			cmdj.createVmArgument().setValue("-D"+AnnotationRules.XML_LOG_PROP+"=RemoteJSureRun.AnnotationRules");
			cmdj.createVmArgument().setValue("-D"+XUtil.testingProp+"="+XUtil.testing);
		}
		cmdj.createVmArgument().setValue("-D"+RemoteScanJob.RUN_DIR_PROP+"="+config.getRunDirectory());
		
		final ConfigHelper util = new ConfigHelper(debug, config);
		String location = util.getPluginDir(JSureConstants.FLUID_PLUGIN_ID, true);
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
