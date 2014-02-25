package com.surelogic.analysis;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.analysis.granules.GranuleInType;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.common.Pair;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.EmptyIterator;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.MetricDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.javac.Javac;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;
import com.surelogic.javac.Util;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.util.IterableThreadLocal;
import extra166y.Ops.Procedure;

// Map groups to a linear ordering
// Deal with granulators
public class Analyses implements IAnalysisGroup<IAnalysisGranule> {
	/**
	 * Threshold to even consider doing a compact.
	 */
	static final int f_thresholdSize = 2000;	

	/**
	 * Threshold is one second.
	 */
	static final long f_thresholdNs = TimeUnit.SECONDS.toNanos(1);
	
	static final boolean compactBeforeCreation = false;
	
	private final List<AnalysisGroup<?>> groups = new ArrayList<AnalysisGroup<?>>();
	private long[] times;
	  
	public final IterableThreadLocal<AnalysisTimings> threadLocal = new IterableThreadLocal<AnalysisTimings>() {
		@Override
		protected AnalysisTimings makeInitialValue() {
			return new AnalysisTimings(Analyses.this);
		}
	};

	public void startTiming() {
		if (times != null) {
			throw new IllegalStateException("Already started timing");
		}
		this.times = new long[size()];
	}

	public long[] summarizeTiming() {
		if (times == null) {
			throw new IllegalStateException("Timing not started yet");
		}
		final Map<Pair<IRNode,IIRAnalysis<?>>, CUTime> timingsByCU = compactBeforeCreation ? new HashMap<Pair<IRNode,IIRAnalysis<?>>, CUTime>() : null;
		int numTimings = 0;
		for(AnalysisTimings at : threadLocal) {			
			for(int j=0; j<times.length; j++) {
				times[j] += at.times[j]; 
			}			  
			if (compactBeforeCreation) {
				for(Timing t : at.getTimings()) {
					final IRNode cu = t.granule.getCompUnit();
					final Pair<IRNode,IIRAnalysis<?>> pair = new Pair<IRNode,IIRAnalysis<?>>(cu, t.analysis);
					CUTime cut = timingsByCU.get(pair);
					if (cut == null) {
						cut = new CUTime(cu, t.analysis);
						timingsByCU.put(pair, cut);
					}
					cut.add(t);
					numTimings++;
				}
			} else {
				at.makeDrops();
			}
		}
		if (compactBeforeCreation) {
			final boolean verbose = numTimings < f_thresholdSize;
			for(CUTime t : timingsByCU.values()) {
				t.makeDrops(verbose);
			}
		}
		
		// Postprocess times to normalize to millis
		for(int i = 0; i<times.length; i++) {
			times[i] = times[i] / 1000000;
		}
		return times;
	}	  
	
	static class CUTime {
	    final IRNode cu;
	    final IIRAnalysis<?> analysis;
		long totalTimeInNs;
	    final List<Timing> timings = new ArrayList<Timing>();
		
		CUTime(IRNode cu, IIRAnalysis<?> a) {
			this.cu = cu;
			analysis = a;
		}

		void add(Timing t) {
			totalTimeInNs += t.t_in_ns;
			timings.add(t);
		}
		
		void makeDrops(boolean verbose) {
			if (!verbose && totalTimeInNs < f_thresholdNs) {
				// Compact
				recordTime(cu, analysis, totalTimeInNs);
			} else {
				for(Timing t : timings) {
					t.recordTime();
				}
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Iterator<IIRAnalysis<IAnalysisGranule>> iterator() {
		Iterator<IIRAnalysis<IAnalysisGranule>> rv = null;
		for(AnalysisGroup g : groups) {
			if (!g.isEmpty()) {
				if (rv == null) {
					rv = g.iterator();
				} else {
					rv = new AppendIterator<IIRAnalysis<IAnalysisGranule>>(rv, g.iterator());
				}
			}			
		}
		if (rv == null) {
			return EmptyIterator.prototype();
		} 
		return rv;
	}
	
	public int getOffset() {
		return 0;
	}
	
	public boolean runSingleThreaded() {
		return true; // to be conservative
	}
	
	public int size() {
		int size = 0;
		for(AnalysisGroup<?> g : groups) {
			size += g.size();
		}
		return size;
	}
	
	public int numGroups() {
		return groups.size();
	}
	
	public Iterable<AnalysisGroup<?>> getGroups() {
		return groups;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addNewGroup(IAnalysisGranulator<?> g, IIRAnalysis<?>... analyses) {
		if (times != null) {
			throw new IllegalStateException("Can't add analyses after starting timing");
		}
		groups.add(new AnalysisGroup(this, g, size(), analyses));
	}  
	
	public Set<IAnalysisGranulator<?>> getGranulators() {
		Set<IAnalysisGranulator<?>> rv = new HashSet<IAnalysisGranulator<?>>();
		for(AnalysisGroup<?> g : groups) {
			if (g.getGranulator() != null) {
				rv.add(g.getGranulator());
			}
		}
		return rv;
	}

	public IAnalysisGranulator<IAnalysisGranule> getGranulator() {		
		return null;
	}
	
	public static class AnalysisTimings {
		final long[] times;
		final List<Timing> timings = new ArrayList<Timing>();
		
		AnalysisTimings(Analyses analyses) {
			times = new long[analyses.size()];
		}

	    Iterable<Timing> getTimings() {
			return timings;
		}

		public void incrTime(int which, long time, IAnalysisGranule granule, IIRAnalysis<?> a) {
			times[which] += time;
			timings.add(new Timing(time, granule, a));
		}
		
		void makeDrops() {
			for(Timing t : timings) {
				t.recordTime();
			}
		}
	}

	private static class Timing {
		final IIRAnalysis<?> analysis;
		final IAnalysisGranule granule;
		final long t_in_ns;
		
		Timing(long t, IAnalysisGranule g, IIRAnalysis<?> a) {
			t_in_ns = t;
			granule = g;
			analysis = a;
		}		
		
		void recordTime() {
			Analyses.recordTime(granule.getNode(), analysis, t_in_ns);
		}
	}
	
	static void recordTime(IRNode n, IIRAnalysis<?> analysis, long t_in_ns) {
		final MetricDrop d = new MetricDrop(n, IMetricDrop.Metric.SCAN_TIME);

		final IKeyValue name = KeyValueUtility.getStringInstance(IMetricDrop.SCAN_TIME_ANALYSIS_NAME, analysis.label());
		d.addOrReplaceMetricInfo(name);

		final IKeyValue time = KeyValueUtility.getLongInstance(IMetricDrop.SCAN_TIME_DURATION_NS, t_in_ns);
		d.addOrReplaceMetricInfo(time);
	}
	
	public void incrTime(int i, long time) {
		times[i] += time;		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<IAnalysisGranule> getGranuleType() {
		return ((Class<IAnalysisGranule>) ((Class)CUDrop.class));
	}

	public Analyses getParent() {
		return this;
	}
	
	
	public interface Analyzer<P, Q extends IAnalysisGranule> {
		IIRAnalysisEnvironment getEnv();
		IAnalysisGroup<Q> getAnalyses();
		SLProgressMonitor getMonitor();
		boolean isSingleThreaded(IIRAnalysis<?> analysis);
		void process(Collection<P> fromProj);
	}
	
	public <P extends IAnalysisGranule, Q extends IAnalysisGranule>
	void analyzeProjects(final Projects projects, final Analyzer<P,Q> analyzer, Collection<? extends P> toAnalyze) {
		if (toAnalyze == null) {
			return;
		}
		final MultiMap<ITypeEnvironment, P> granules = new MultiHashMap<ITypeEnvironment, P>();
		for (P granule : toAnalyze) {
			granules.put(granule.getTypeEnv(), granule);
		}
		for (final JavacProject project : projects) {
			if (projects.getMonitor().isCanceled()) {
				throw new CancellationException();
			}
			analyzeAProject(analyzer, project, granules.get(project.getTypeEnv()));
		}
	}
	
	/**
	 * Handles all the analyzeBegin/End call, while abstracting how the analyses are run on the project
	 */
	public <P extends IAnalysisGranule, Q extends IAnalysisGranule>
	void analyzeAProject(final Analyzer<P,Q> analyzer, final JavacProject project, Collection<P> fromProj) {		  
		int i = analyzer.getAnalyses().getOffset();
		for (final IIRAnalysis<Q> a : analyzer.getAnalyses()) {
			//System.out.println(a.name()+" analyzing "+(a.analyzeAll() ? "all CUs" : "source CUs"));

			if (analyzer.getMonitor().isCanceled()) {
				throw new CancellationException();
			}
			final String inParallel = analyzer.isSingleThreaded(a) ? "" : "parallel ";
			Util.startSubTask(analyzer.getMonitor(), "Starting " + inParallel + a.name() + " [" + i + "]: " + (fromProj == null ? 0 : fromProj.size()) + 
					" for " + project.getName());
			final long start = System.nanoTime();
			try {
				a.analyzeBegin(analyzer.getEnv(), project);
			} finally {
				final long end = System.nanoTime();
				incrTime(i, end - start);
				i++;
			}
		}
		// Analyze granules
		if (fromProj != null) {
			analyzer.process(fromProj);
		}		

		// Finishing up loose ends (if any)
		i = analyzer.getAnalyses().getOffset();
		for (final IIRAnalysis<Q> a : analyzer.getAnalyses()) {
			final long start = System.nanoTime();
			AnalysisGroup.handleAnalyzeEnd(a, analyzer.getEnv(), project);
			final long end = System.nanoTime();
			incrTime(i, end - start);
			i++;	
		}

		// All analysis is done for the project
		i = analyzer.getAnalyses().getOffset();
		for (final IIRAnalysis<Q> a : analyzer.getAnalyses()) {
			final long start = System.nanoTime();
			a.postAnalysis(project);
			final long end = System.nanoTime();
			incrTime(i, end - start);
			i++;	
			Util.endSubTask(analyzer.getMonitor());
		}
	}
	
	public void finishAllAnalyses(IIRAnalysisEnvironment env) {
		int i = 0;
		for (final IIRAnalysis<?> a : this) {
			final long start = System.nanoTime();
			a.finish(env);
			final long end = System.nanoTime();
			incrTime(i, end - start);
			i++;
		}	
	}	
	
	public static void main(String... args) {
		Javac.getDefault();
		final ConcurrentAnalysis<TestGranule> a = new ConcurrentAnalysis<TestGranule>(true, TestGranule.class);
		final Set<String> names = new HashSet<String>();
		final List<TestGranule> c = new ArrayList<TestGranule>();
		for(int i=0; i<40; i++) {
			c.add(new TestGranule());
		}
		a.runAsTasks(c, new Procedure<TestGranule>() {
			@Override
			public void op(TestGranule g) {
				List<TestGranule> c2 = new ArrayList<TestGranule>();
				for(int i=0; i<40; i++) {
					c2.add(new TestGranule());
				}
				final Thread here = Thread.currentThread();
				names.add(here.getName());
				
				a.runAsTasks(c2, new Procedure<TestGranule>() {
					@Override
					public void op(TestGranule g) {
						//System.out.println(here.getName()+" => "+Thread.currentThread().getName());
						names.add(Thread.currentThread().getName());
					}
				});
			}
		});
		int i=1;
		for(String s : names) {
			System.out.println(i+". "+s);
			i++;
		}
	}
	
	private static class TestGranule extends GranuleInType {
		protected TestGranule() {
			super(null);
		}

		@Override
		public String getLabel() {
			return null;
		}

		@Override
		public IRNode getNode() {
			return null;
		}
	}
}
