package com.surelogic.javac;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import com.surelogic.analysis.GroupedAnalysis;
import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.analysis.IIRAnalysis;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.concurrency.detector.ConcurrencyDetector;
import com.surelogic.analysis.concurrency.driver.LockAnalysis;
import com.surelogic.analysis.effects.EffectsAnalysis;
import com.surelogic.analysis.equality.EqualityAnalysis;
import com.surelogic.analysis.layers.LayersAnalysis;
import com.surelogic.analysis.singleton.SingletonAnalysis;
import com.surelogic.analysis.structure.StructureAnalysis;
import com.surelogic.analysis.testing.BCAModule;
import com.surelogic.analysis.testing.CollectMethodCallsModule;
import com.surelogic.analysis.testing.DefinitelyAssignedModule;
import com.surelogic.analysis.testing.LocalVariablesModule;
import com.surelogic.analysis.testing.NonNullModule;
import com.surelogic.analysis.testing.RawTypeModule;
import com.surelogic.analysis.testing.TypeBasedAliasModule;
import com.surelogic.analysis.testing.TypesModule;
import com.surelogic.analysis.threads.ThreadEffectsModule;
import com.surelogic.analysis.utility.UtilityAnalysis;
import com.surelogic.common.XUtil;
import com.surelogic.javac.jobs.RemoteJSureRun;

import edu.cmu.cs.fluid.ide.IClassPath;
import edu.cmu.cs.fluid.ide.IClassPathContext;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.IntegerTable;

public class Javac extends IDE {
	// Needs to be initialized before the Javac instance
	static final List<AnalysisInfo> analysisList = new ArrayList<AnalysisInfo>();
	
	static { 
		// Assumed to be added in dependency order
		init(ConcurrencyDetector.class,
				"com.surelogic.jsure.client.eclipse.IRConcurrencyDetector", true, "Concurrency detector");
		init(ThreadEffectsModule.class,
				"com.surelogic.jsure.client.eclipse.ThreadEffectAssurance2", true, "Thread effects");
		init(LayersAnalysis.class,
				"com.surelogic.jsure.client.eclipse.LayersAssurance", true, "Static structure");
		if (XUtil.useExperimental) {
			init(StructureAnalysis.class,
				"com.surelogic.jsure.client.eclipse.StructureAssurance", false, "Structure analysis");
		}
		init(EffectsAnalysis.class,
				"com.surelogic.jsure.client.eclipse.EffectAssurance2", true, "Region effects");
		
		/* Checking of @ThreadSafe, etc., which is run by the lock policy and 
		 * equality analyses, depend on the results of annotation bounds checking.
		 */
		final AnalysisInfo annoBoundsChecking = 
		    init(ParameterizedTypeAnalysis.class,
		        "com.surelogic.jsure.client.eclipse.ParameterizedType",
		        true, "Annotation Bounds");
    init(LockAnalysis.class,
        "com.surelogic.jsure.client.eclipse.LockAssurance3", true, "Lock policy",
        annoBoundsChecking);
    init(EqualityAnalysis.class,
        "com.surelogic.jsure.client.eclipse.EqualityAssurance", true,
        "Reference equality", annoBoundsChecking);

    init(com.surelogic.analysis.uniqueness.plusFrom.traditional.UniquenessAnalysisModule.class,
        "com.surelogic.jsure.client.eclipse.UniquenessAssuranceUWM", false, "Uniqueness + From");
    init(com.surelogic.analysis.uniqueness.plusFrom.traditional.UniquenessAnalysisAllModule.class,
        "com.surelogic.jsure.client.eclipse.UniquenessAssuranceUWM_ALL", false, "Uniqueness + From (All methods)");
    init(com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.UniquenessAnalysisModule.class,
        "com.surelogic.jsure.client.eclipse.UniquenessAssuranceUWM_SE", true, "Uniqueness + From (Side-effecting)");
		init(com.surelogic.analysis.uniqueness.classic.sideeffecting.UniquenessAnalysisModule.class,
				"com.surelogic.jsure.client.eclipse.UniquenessAssuranceSE", false, "Uniqueness (Obsolete)");

    init(NonNullModule.class, "com.surelogic.jsure.client.eclipse.NonNull", false, "NonNull");
    init(RawTypeModule.class, "com.surelogic.jsure.client.eclipse.RawTypes", false, "RawType");
    init(DefinitelyAssignedModule.class, "com.surelogic.jsure.client.eclipse.DefinitelyAssigned", false, "Definitely Assigned");
		
		init(LocalVariablesModule.class,
				"com.surelogic.jsure.client.eclipse.LV", false, "LV");
		init(BCAModule.class, "com.surelogic.jsure.client.eclipse.BCA", false, "BCA");
		init(CollectMethodCallsModule.class,
				"com.surelogic.jsure.client.eclipse.CALLS", false, "Method Calls");
    init(com.surelogic.analysis.uniqueness.plusFrom.traditional.NewBenchmarkingUAM.class,
        "com.surelogic.jsure.client.eclipse.BenchmarkingUniquenessNew", false, "Uniqueness Benchmarking (U+F)");
    init(com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.NewBenchmarkingUAM.class,
        "com.surelogic.jsure.client.eclipse.BenchmarkingUniquenessNew_SE", false, "Uniqueness Benchmarking (U+F SE)");
		init(com.surelogic.analysis.uniqueness.classic.sideeffecting.NewBenchmarkingUAM.class,
				"com.surelogic.jsure.client.eclipse.BenchmarkingUniquenessSE", false, "Uniqueness Benchmarking (SE)");
		init(TypeBasedAliasModule.class,
		    "com.surelogic.jsure.cliend.eclipse.TypeBasedAlias", false, "Test Type-Based Alias Analysis");
		
		init(TypesModule.class, "com.surelogic.jsure.client.eclipse.Types", false, "Type Info");
		
		init(UtilityAnalysis.class, "com.surelogic.jsure.client.eclipse.Utility", true, "Utility class");
    init(SingletonAnalysis.class, "com.surelogic.jsure.client.eclipse.Singleton", true, "Singleton class");
    
		/*
		AnalysisInfo[] deps = new AnalysisInfo[3];
		deps[0] = init(Module_IRAnalysis.class, 
				"com.surelogic.jsure.client.eclipse.ModuleAnalysis2", false, "");
		deps[1] = init(ThreadRoleZerothPass.class,
				"com.surelogic.jsure.client.eclipse.ThreadRoleZerothPass1", false, "", 
				deps[0]);
		deps[2] = init(ManageThreadRoleAnnos.class,
				"com.surelogic.jsure.client.eclipse.ManageThreadRoleAnnos1", false, "", 
				deps[0], deps[1]);
		init(ThreadRoleAssurance.class,
				"com.surelogic.jsure.client.eclipse.ThreadRoleAssurance1", false, "", deps);
				*/
	}
	
	static final Javac instance = new Javac();
	{
		// Needs to be initialized before the locator
		IDE.prototype = this;
	}
	private final JavacFileLocator locator = JavacFileLocator.makeLocator();

	protected Javac() {
		// Nothing to do now
	}

	public static Javac getDefault() {
		return instance;
	}

	public static void initialize() {
		// Nothing to do right now, besides create the instance above
	}

	@Override
	public IJavaFileLocator<String, IIRProject> getJavaFileLocator() {
		return locator;
	}

	@Override
	public URL getResourceRoot() {
		// return Resources.findRoot("edu.cmu.cs.fluid");
	
		// Use a manually-set workspace
		try {
			String fluidLocation = System.getProperty(RemoteJSureRun.FLUID_DIRECTORY_URL);
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

	private final ConcurrentMap<String, Object> prefs = new ConcurrentHashMap<String, Object>();
	{
		setPreference(IDEPreferences.DEFAULT_JRE, "");
		setPreference(IDEPreferences.ANALYSIS_THREAD_COUNT, Runtime
				.getRuntime().availableProcessors());
		for (IAnalysisInfo analysis : getAnalysisInfo()) {
			setPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX+analysis.getUniqueIdentifier(), 
					analysis.isProduction());
		}
	}

	@Override
  public boolean getBooleanPreference(String key) {
		final Boolean val = (Boolean) prefs.get(key);
		return val == null ? false : val;
	}

  @Override
	public int getIntPreference(String key) {
		final Integer val = (Integer) prefs.get(key);
		return val == null ? 0 : val;
	}

  @Override
	public String getStringPreference(String key) {
		return (String) prefs.get(key);
	}

	public void setPreference(String key, boolean value) {
		// System.out.println("Setting "+key+" to "+value);
		if (XUtil.testing) {
			System.out.println(this.getClass().getSimpleName() + " set " + key
					+ " to " + (value ? "true" : "false"));
		}
		prefs.put(key, value);
	}

	public void setPreference(String key, Object value) {
		if (XUtil.testing) {
			System.out.println(this.getClass().getSimpleName() + " set " + key
					+ " to " + value);
		}
		prefs.put(key, value);
	}

	public static final String JAVAC_PROPS = "javac.properties";

	public synchronized void savePreferences(File runDir) throws IOException {
		final PrintWriter pw = new PrintWriter(new File(runDir, JAVAC_PROPS));
		try {
			for (Map.Entry<String, Object> e : prefs.entrySet()) {
				pw.println(e.getKey() + "=" + e.getValue().toString().replace("\\", "\\\\"));
			}
		} finally {
			pw.close();
		}
	}

	public synchronized void loadPreferences(File runDir) throws IOException {
		File f = new File(runDir, JAVAC_PROPS);
		if (!f.isFile()) {
			return; // No file to read
		}
		Properties p = new Properties();
		Reader r = new FileReader(f);
		try {
			p.load(r);
			prefs.clear();
			for (Map.Entry<Object, Object> e : p.entrySet()) {
				String key = e.getKey().toString();
				String val = e.getValue().toString();
				System.out.println("Loading "+key+" = "+val);
				
				// Check if boolean
				if ("true".equals(val) || "false".equals(val)) {
					prefs.put(key, Boolean.parseBoolean(val));
				} else {
					// Check if integer
					try {
						int i = Integer.parseInt(val);
						prefs.put(key, IntegerTable.newInteger(i));
					} catch (NumberFormatException ex) {
						// Otherwise use as String
						prefs.put(key, val);
					}
				}
			}
		} finally {
			r.close();
		}
	}
	
  @Override
	public IAnalysisInfo[] getAnalysisInfo() {		
		return analysisList.toArray(new IAnalysisInfo[analysisList.size()]);
	}
	
	private static class AnalysisInfo implements IAnalysisInfo {
		final Class<? extends IIRAnalysis> clazz;
		final String id;
		final List<AnalysisInfo> dependencies;
		final boolean isProduction;
		final String label;

		AnalysisInfo(Class<? extends IIRAnalysis> clazz, String id, boolean production, String label,
				AnalysisInfo... deps) {
			this.clazz = clazz;
			this.id = id;
			this.label = label;
			isProduction = production;
			if (deps.length == 0) {
				dependencies = Collections.emptyList();
			} else {
				dependencies = new ArrayList<AnalysisInfo>(deps.length);
				for (AnalysisInfo info : deps) {
					dependencies.add(info);
				}
			}
		}

		boolean isActive(List<AnalysisInfo> activeAnalyses) {
			boolean active = isIncluded();
			if (active) {
				// TODO use more principled way of identifying analyses
				if (!IDE.getInstance().getBooleanPreference(IDEPreferences.SCAN_MAY_RUN_UNIQUENESS) &&
					id.contains(".UniquenessAssurance")) {
					return false;
				}
				if (dependencies.size() == 0) {
					return true;
				}
				return activeAnalyses.containsAll(dependencies);
			}
			return false;
		}

		public Class<? extends IIRAnalysis> getAnalysisClass() {
			return clazz;
		}

		public String getCategory() {
			return null; // nothing's "required"
		}

		public String getLabel() {
			return label;
		}

		public String[] getPrerequisiteIds() {
			String[] result = new String[dependencies.size()];
			int i=0;
			for(AnalysisInfo ai : dependencies) {
				result[i] = ai.id;
			}
			return result;
		}

		public String getUniqueIdentifier() {
			return id;
		}

		public boolean isIncluded() {
			return IDE.getInstance().getBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX+id);
		}

		public boolean isProduction() {
			return isProduction;
		}
	}



	static AnalysisInfo init(Class<? extends IIRAnalysis> clazz, String id, 
			boolean isProduction, String label,
			AnalysisInfo... deps) {
		final AnalysisInfo info = new AnalysisInfo(clazz, id, isProduction, label, deps);
		// analysisMap.put(id, info);
		analysisList.add(info);
		return info;
	}

	/**
	 * Used to initialize Javac/JavacEclipse
	 */
	public static Iterable<String> getAvailableAnalyses() {
		// return analysisMap.keySet();
		return new FilterIterator<AnalysisInfo, String>(analysisList.iterator()) {
			@Override
			protected Object select(AnalysisInfo info) {
				return info.id;
			}

		};
	}

	public static int numAnalyses() {
		return analysisList.size();
	}
	
	/**
	 * Returns a filtered list of analyses, sorted properly to account for
	 * dependencies
	 */
	public static List<IIRAnalysis> makeAnalyses() {
		String test = XUtil.runTest();
		if (test != null) {
			Benchmark b = Benchmark.valueOf(test);
			if (b != null) {
				switch (b) {
				case UAM:
					return Collections
							.<IIRAnalysis> singletonList(new com.surelogic.analysis.uniqueness.plusFrom.traditional.NewBenchmarkingUAM());
				default:
				}
			}
		}
		List<AnalysisInfo> active = new ArrayList<AnalysisInfo>();
		// for(AnalysisInfo info : analysisMap.values()) {
		for (AnalysisInfo info : analysisList) {
			if (info.isActive(active)) {
				active.add(info);
			}
		}
		List<IIRAnalysis> analyses = new ArrayList<IIRAnalysis>();
		List<IIRAnalysis> grouped = new ArrayList<IIRAnalysis>();
		Class<?> group = null;
		for (AnalysisInfo info : active) {
			try {
				IIRAnalysis a = info.clazz.newInstance();
				System.out.println("Created " + info.clazz.getName());
				if (a.getGroup() == null) {
					// Independent analysis
					handleGroup(analyses, group, grouped);
					analyses.add(a);		
				} else {
					// Potentially grouped analysis
					if (a.getGroup() != group) {
						// Different group
						handleGroup(analyses, group, grouped);
						group = a.getGroup();
					}					
					grouped.add(a);
				}				
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		handleGroup(analyses, group, grouped);
		return analyses;
	}

	/**
	 * @param grouped will be empty afterwards
	 */
	private static void handleGroup(List<IIRAnalysis> analyses,	Class<?> group, List<IIRAnalysis> grouped) {
		if (grouped.isEmpty()) {
			return; // Nothing to do
		}
		if (grouped.size() == 1) {
			analyses.add(grouped.get(0));
		} else {
			analyses.add(new GroupedAnalysis(group, grouped));
		}
		grouped.clear();
		return;
	}
}
