package com.surelogic.analysis;

import java.util.*;

import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.EmptyIterator;

// Map groups to a linear ordering
// Deal with granulators
public class Analyses implements IAnalysisGroup<IAnalysisGranule> {
	private final List<AnalysisGroup<?>> groups = new ArrayList<AnalysisGroup<?>>();

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
	
	public Iterable<AnalysisGroup<?>> getGroups() {
		return groups;
	}

	public void add(AnalysisGroup<?> g) {
		groups.add(g);
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
}
