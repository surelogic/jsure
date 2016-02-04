package com.surelogic.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.surelogic.common.SLUtility;
import com.surelogic.common.util.FilterIterator;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ide.IDERoot;

/**
 * Describes what analyses are available, what order they should be run in, and 
 * whether they are on
 * 
 * @author edwin
 */
public final class AnalysisDefaults {
	private static final String NULLABLE = "com.surelogic.jsure.client.eclipse.Nullable";
	private static final String NULLABLE_PREP = "com.surelogic.jsure.client.eclipse.NullablePreprocessor";

	// Needs to be initialized before the Javac instance
	static final List<AnalysisInfo> analysisList = new ArrayList<>();
	static { 		
		// Assumed to be added in dependency order
		init("com.surelogic.analysis.concurrency.detector.ConcurrencyDetector",
				"com.surelogic.jsure.client.eclipse.IRConcurrencyDetector", true, "Concurrency detector");
		init("com.surelogic.analysis.threads.ThreadEffectsModule",
				"com.surelogic.jsure.client.eclipse.ThreadEffectAssurance2", true, "Thread effects");
		init("com.surelogic.analysis.structure.StructureAnalysis",
				"com.surelogic.jsure.client.eclipse.StructureAssurance", true, "Structure analysis");

		/* Checking of @ThreadSafe, etc., which is run by the lock policy and 
		 * equality analyses, depend on the results of annotation bounds checking.
		 */
		final AnalysisInfo annoBoundsChecking = 
				init("com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis",
						"com.surelogic.jsure.client.eclipse.ParameterizedType",
						true, "Annotation bounds");

		init("com.surelogic.analysis.equality.EqualityAnalysis",
				"com.surelogic.jsure.client.eclipse.EqualityAssurance", true,
				"Reference equality", annoBoundsChecking);		

		init("com.surelogic.analysis.layers.LayersAnalysis",
				"com.surelogic.jsure.client.eclipse.LayersAssurance", true, "Static structure");	

		// Using TopLevelAnalysisVisitor
		init("com.surelogic.analysis.utility.UtilityAnalysis", "com.surelogic.jsure.client.eclipse.Utility", true, "Utility class");
		init("com.surelogic.analysis.singleton.SingletonAnalysis", "com.surelogic.jsure.client.eclipse.Singleton", true, "Singleton class");

		// Lock and EffectAssurance need to be declared together because they share use of BindingContextAnalysis
		final AnalysisInfo lockBuilder =
		    init("com.surelogic.analysis.concurrency.driver.LockModelBuilder",
		        "com.surelogic.jsure.client.eclipse.LockModelBuilder", true,
		        "Lock Model Builder");
		init("com.surelogic.analysis.concurrency.driver.ThreadSafeAnalysis",
		    "com.surelogic.jsure.client.eclipse.ThreadSafe", true,
		    "ThreadSafe / Containable / Immutable", lockBuilder, annoBoundsChecking);
		
    init("com.surelogic.analysis.concurrency.driver.NewLockAnalysis",
        "com.surelogic.jsure.client.eclipse.LockAssurance4", true, "Lock policy",
        lockBuilder);
		init("com.surelogic.analysis.concurrency.driver.LockAnalysis",
		    "com.surelogic.jsure.client.eclipse.LockAssurance3", false, "Old Lock policy",
		    lockBuilder, annoBoundsChecking);
    init("com.surelogic.analysis.testing.EffectsAndLocksDumpModule",
        "com.surelogic.jsure.client.eclipse.EffectsAndLocks", false, "Effects and Locks Dump",
        lockBuilder);
		init("com.surelogic.analysis.effects.EffectsAnalysis",
				"com.surelogic.jsure.client.eclipse.EffectAssurance2", true, "Region effects",
				lockBuilder);

		init("com.surelogic.analysis.uniqueness.classic.sideeffecting.UniquenessAnalysisModule",
				"com.surelogic.jsure.client.eclipse.UniquenessAssuranceSE", true, "Uniqueness",
				lockBuilder);

		//    init(NonNullRawTypeModule.class, "com.surelogic.jsure.client.eclipse.NonNullRawTypes", false, "Combined NonNull & RawType (for reg tests only)");
		init("com.surelogic.analysis.testing.DefinitelyAssignedModule", "com.surelogic.jsure.client.eclipse.DefinitelyAssigned", false, "Definitely Assigned (for reg tests only)");

		init("com.surelogic.analysis.testing.LocalVariablesModule",
				"com.surelogic.jsure.client.eclipse.LV", false, "Local Variables (for reg tests only)");
	
		init("com.surelogic.analysis.testing.BCAModule", "com.surelogic.jsure.client.eclipse.BCA", false, "BCA (for reg tests only)");
		init("com.surelogic.analysis.testing.CollectMethodCallsModule",
				"com.surelogic.jsure.client.eclipse.CALLS", false, "Method Calls (for reg tests only)");
		init("com.surelogic.analysis.uniqueness.classic.sideeffecting.NewBenchmarkingUAM",
				"com.surelogic.jsure.client.eclipse.BenchmarkingUniquenessSE", false, "Uniqueness Benchmarking (SE)");
		init("com.surelogic.analysis.testing.TypeBasedAliasModule",
				"com.surelogic.jsure.cliend.eclipse.TypeBasedAlias", false, "Type-Based Alias Analysis (for reg tests only)");

		init("com.surelogic.analysis.testing.TypesModule", "com.surelogic.jsure.client.eclipse.Types", false, "Type Info (for reg tests only)");
		init("com.surelogic.analysis.testing.ConstantExpressionModule", "com.surelogic.jsure.client.eclipse.ConstantExpr", false, "Constant Expressions (for reg tests only)");
		init("com.surelogic.analysis.testing.BinderModule", "com.surelogic.jsure.client.eclipse.Binder", false, "Binder (for reg tests only)");
		final AnalysisInfo nullablePreprocessor =
				init("com.surelogic.analysis.nullable.NullablePreprocessorModule", NULLABLE_PREP, true, "Nullable Preprocessor");

		init("com.surelogic.analysis.nullable.NullableModule2", NULLABLE, true, "Nullable", nullablePreprocessor);
		init("com.surelogic.analysis.testing.NonNullRawTypeModule", "com.surelogic.jsure.client.eclipse.NonNullRawTypes", false, "Combined NonNull & RawType (for reg tests only)", nullablePreprocessor);
		init("com.surelogic.analysis.testing.FinalModule", "com.surelogic.jsure.client.eclipse.Final", false, "Final Declarations (for reg tests only)");

		init("com.surelogic.analysis.jtb.TestFunctionalInterfacePseudoAnalysis","com.surelogic.jsure.client.eclipse.TestIsFunctional", false, "Functional (for tests only)");
	}
	
	private AnalysisDefaults() {
		// Nothing to do here
	}
	
	private static final AnalysisDefaults prototype = new AnalysisDefaults();
	
	public static AnalysisDefaults getDefault() {
		return prototype;
	}

	//@Override
	public Collection<? extends IAnalysisInfo> getAnalysisInfo() {		
		return analysisList;
	}
  
	static AnalysisInfo init(String clazzName, String id, boolean isProduction, String label,
			AnalysisInfo... deps) {
		final AnalysisInfo info = new AnalysisInfo(clazzName, id, isProduction, label, deps);
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

	public static String[] getOtherAnalysesToActivate(String id) {
		if (NULLABLE.equals(id)) {
			return new String[] { NULLABLE_PREP };
		}
		return SLUtility.EMPTY_STRING_ARRAY;
	}

	private static class AnalysisInfo implements IAnalysisInfo {
		final String clazz;
		final String id;
		final List<AnalysisInfo> dependencies;
		final boolean isProduction;
		final String label;

		AnalysisInfo(String analysis, String id, boolean production, String label,
				AnalysisInfo... deps) {
			this.clazz = analysis;
			this.id = id;
			this.label = label;
			isProduction = production;
			if (deps.length == 0) {
				dependencies = Collections.emptyList();
			} else {
				dependencies = new ArrayList<>(deps.length);
				for (AnalysisInfo info : deps) {
					dependencies.add(info);
				}
			}
		}

		@Override
		public boolean runsUniqueness() {
			return id.contains(".UniquenessAssurance");
		}

		@Override
		public boolean isActive(List<IAnalysisInfo> activeAnalyses) {
			boolean active = isIncluded();
			if (active) {
				if (dependencies.size() == 0) {
					return true;
				}
				return activeAnalyses.containsAll(dependencies);
			}
			return false;
		}

		@Override
		public String getAnalysisClassName() {
			return clazz;
		}

		@Override
		public String getCategory() {
			return null; // nothing's "required"
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public String[] getPrerequisiteIds() {
			String[] result = new String[dependencies.size()];
			int i=0;
			for(AnalysisInfo ai : dependencies) {
				result[i] = ai.id;
			}
			return result;
		}

		@Override
		public String getUniqueIdentifier() {
			return id;
		}

		@Override
		public boolean isIncluded() {
			return IDERoot.getInstance().getBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX+id);
		}

		@Override
		public boolean isProduction() {
			return isProduction;
		}
	}
}
