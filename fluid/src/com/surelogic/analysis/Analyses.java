package com.surelogic.analysis;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.EmptyIterator;
import com.surelogic.dropsea.ir.drops.CUDrop;

// Map groups to a linear ordering
// Deal with granulators
public class Analyses implements IAnalysisGroup<IAnalysisGranule> {
	private final List<AnalysisGroup<?>> groups = new ArrayList<AnalysisGroup<?>>();
	private long[] times;
	  
	final List<AnalysisTimings> allTimings = new CopyOnWriteArrayList<AnalysisTimings>();
	public final ThreadLocal<AnalysisTimings> threadLocal = new ThreadLocal<AnalysisTimings>() {
		@Override
		protected AnalysisTimings initialValue() {
			AnalysisTimings rv = new AnalysisTimings(Analyses.this);
			allTimings.add(rv);
			return rv;
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
		for(AnalysisTimings t : allTimings) {
			for(int j=0; j<times.length; j++) {
				times[j] += t.times[j]; 
			}			  
		}

		// Postprocess times to normalize to millis
		for(int i = 0; i<times.length; i++) {
			times[i] = times[i] / 1000000;
		}
		return times;
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
		
		AnalysisTimings(Analyses analyses) {
			times = new long[analyses.size()];
		}

		public void incrTime(int which, long time) {
			times[which] += time;
		}
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
}
