package com.surelogic.javac;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.common.XUtil;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.jobs.*;

import edu.cmu.cs.fluid.ide.IClassPath;
import edu.cmu.cs.fluid.ide.IClassPathContext;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.java.IJavaFileLocator;

/**
 * Note: analyses now specified in AnalysisDefaults
 * @author edwin
 *
 */
public class Javac extends IDE {	
	private static Javac instance;
	{
		// Needs to be initialized before the locator
		IDE.initInstance(this);
	}
	private final JavacFileLocator locator = JavacFileLocator.makeLocator();

	protected Javac() {
		// Nothing to do now
	}

	public static synchronized Javac getDefault() {
		if (instance == null) {
			instance = new Javac();
		}
		return instance;
	}

	public static void initialize() {
		// Nothing to do right now, besides create the instance above
		getDefault();
	}

	@Override
	public IJavaFileLocator<String, IIRProject> getJavaFileLocator() {
		return locator;
	}

	@Override
	public URL getResourceRoot() {
		// Use a manually-set workspace
		try {
			String fluidLocation = System.getProperty(LocalJSureJob.FLUID_DIRECTORY_URL);
			if (fluidLocation != null) {
				return new URL(fluidLocation);
			}	
			return new File(Util.WORKSPACE + "/fluid").toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected IClassPathContext newContext(IClassPath path) {
		return (JavacProject) path;
	}
	
	/**
	 * Returns a filtered list of analyses, sorted properly to account for
	 * dependencies
	 */
	public static Analyses makeAnalyses() {
		final Analyses analyses = new Analyses();
		String test = XUtil.runTest();
//		if (test != null) {
//			Benchmark b = Benchmark.valueOf(test);
//			if (b != null) {
//				switch (b) {
//				case UAM:
//					analyses.addNewGroup(null,
//							new com.surelogic.analysis.uniqueness.plusFrom.traditional.NewBenchmarkingUAM());
//					return analyses;
//				default:
//				}
//			}
//		}
		List<IAnalysisInfo> active = new ArrayList<IAnalysisInfo>();
		// for(AnalysisInfo info : analysisMap.values()) {
		for (IAnalysisInfo info : AnalysisDefaults.getDefault().getAnalysisInfo()) {
			if (info.isActive(active)) {
				active.add(info);
			}
		}
		List<IIRAnalysis<?>> grouped = new ArrayList<IIRAnalysis<?>>();
		IAnalysisGranulator<?> granulator = null;
		for (IAnalysisInfo info : active) {
			try {
				IIRAnalysis<?> a = (IIRAnalysis<?>) Class.forName(info.getAnalysisClassName()).newInstance();
				a.setLabel(info.getLabel());
				System.out.println("Created " + info.getAnalysisClassName());
				/*
				if (a.getGroup() == null) {
					// Independent analysis
					handleGroup(analyses, group, grouped);
					analyses.add(a);		
				} else {
				*/
					// Potentially grouped analysis
					if (startNewGroup(a, grouped)) {
						// Different group
						System.out.println("Starting a new group for "+a.name());
						handleGroup(analyses, granulator, grouped);
						granulator = a.getGranulator();
					}					
					grouped.add(a);
				//}				
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		handleGroup(analyses, granulator, grouped);
		return analyses;
	}

	private static boolean startNewGroup(IIRAnalysis<?> a, List<IIRAnalysis<?>> grouped) {
		if (grouped.isEmpty()) {
			return true;
		}
		IIRAnalysis<?> first = grouped.get(0);
		if (Util.useNewDriver) {
			return a.getGranulator() != first.getGranulator() || a.runInParallel() != first.runInParallel();
		} else {
			return a.getGroup() != first.getGroup() || a.runInParallel() != first.runInParallel();
		}
	}

	/**
	 * @param grouped will be empty afterwards
	 */
	private static void handleGroup(Analyses analyses, IAnalysisGranulator<?> g, List<IIRAnalysis<?>> grouped) {
		if (grouped.isEmpty()) {
			return; // Nothing to do
		}
		final IIRAnalysis<?> first = grouped.get(0);
		System.out.println("Group: "+(g == null ? "" : g+", ")+
				                     (first.getGroup() == null ? "" : first.getGroup()+", ")+
				                     grouped.get(0).runInParallel());
		for(IIRAnalysis<?> a : grouped) {
			System.out.println("\t"+a.name());
		}
		analyses.addNewGroup(g, grouped.toArray(new IIRAnalysis<?>[grouped.size()]));
		grouped.clear();
		return;
	}

	public static void checkAnalysisInfo() {
		for (IAnalysisInfo info : AnalysisDefaults.getDefault().getAnalysisInfo()) {
			try {
				Class.forName(info.getAnalysisClassName());
			} catch (ClassNotFoundException e) {
				SLLogger.getLogger().severe("Unable to find class for "+info.getLabel()+" : "+info.getAnalysisClassName());
			}
		}
	}
}
