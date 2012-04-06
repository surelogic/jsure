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
	protected Class<?> getRemoteClass() {
		return RemoteJSureRun.class;
	}

	@Override
	protected void setupClassPath(boolean debug, CommandlineJava cmdj, Project proj, Path path) {
		final Set<File> jars    = new HashSet<File>();
		final ConfigHelper util = new ConfigHelper(config);
		// All unpacked
		util.addPluginToPath(debug, jars, JSureConstants.COMMON_PLUGIN_ID, true);
		util.addAllPluginJarsToPath(debug, jars, JSureConstants.COMMON_PLUGIN_ID, "lib/runtime");
		util.addPluginToPath(debug, jars, JSureConstants.FLUID_PLUGIN_ID, true);
		util.addAllPluginJarsToPath(debug, jars, JSureConstants.FLUID_PLUGIN_ID, "lib/runtime");
		//util.addPluginToPath(debug, jars, JSureConstants.FLUID_JAVAC_PLUGIN_ID, true);
		final boolean isMac = SystemUtils.IS_OS_MAC_OSX;
		/*
		if (!isMac) {
			util.addAllPluginJarsToPath(debug, jars, JSureConstants.FLUID_JAVAC_PLUGIN_ID, "lib");
		}
		*/
		if (XUtil.testing) {
			util.addPluginToPath(debug, jars, JSureConstants.JSURE_TESTS_PLUGIN_ID, true);
			util.addAllPluginJarsToPath(debug, jars, JSureConstants.JSURE_TESTS_PLUGIN_ID, "lib");
			util.addPluginJarsToPath(debug, jars, JSureConstants.JUNIT_PLUGIN_ID, "junit.jar");
		}
		
		for(File jar : jars) {
			addToPath(proj, path, jar, true);
		}

		if (isMac) {
			// Add lib/javac.jar to the bootpath
			jars.clear();
			//util.addAllPluginJarsToPath(debug, jars, JSureConstants.FLUID_JAVAC_PLUGIN_ID, "lib");		
			util.addPluginJarsToPath(debug, jars, JSureConstants.FLUID_PLUGIN_ID, "lib/runtime/javac.jar");
			
			final Path bootpath = cmdj.createBootclasspath(proj);		
			for(File jar : jars) {
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
		cmdj.createVmArgument().setValue("-D"+RemoteJSureRun.RUN_DIR_PROP+"="+config.getRunDirectory());
		
		final ConfigHelper util = new ConfigHelper(config);
		String location = util.getPluginDir(false, JSureConstants.FLUID_PLUGIN_ID, true);
		try {
			cmdj.createVmArgument().setValue("-D"+RemoteJSureRun.FLUID_DIRECTORY_URL+"="+new File(location).toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		for(Map.Entry<Object,Object> e : System.getProperties().entrySet()) {
		    println(e.getKey()+" => "+e.getValue());
		}
		
		if (SystemUtils.OS_ARCH.contains("64") && SystemUtils.JAVA_VENDOR.contains("Sun")) {
		    // TODO do I need to check if I'm running in 64-bit mode?
		    cmdj.createVmArgument().setValue("-XX:+UseCompressedOops");
		}		
		cmdj.createVmArgument().setValue("-verbosegc");
		
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
