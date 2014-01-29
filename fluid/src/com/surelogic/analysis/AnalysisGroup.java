package com.surelogic.analysis;

import java.util.*;

import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.dropsea.ir.drops.CUDrop;

/**
 * A group of analyses sharing the same IAnalysisGranulator
 * 
 * @author edwin
 *
 */
public final class AnalysisGroup<Q extends IAnalysisGranule> extends ArrayList<IIRAnalysis<Q>> implements IAnalysisGroup<Q> {
	private static final long serialVersionUID = 1L;
	
	final IAnalysisGranulator<Q> granulator;
	final int offset; // Into the linear ordering
	
	public AnalysisGroup(IAnalysisGranulator<Q> g, int index, IIRAnalysis<Q>... analyses) {
		granulator = g;
		offset = index;
		for(IIRAnalysis<Q> a : analyses) {
			this.add(a);
		}
	}

	public int getOffset() {
		return offset;
	}
	
	public boolean runSingleThreaded() {
		// internal or never
		return get(0).runInParallel() != ConcurrencyType.EXTERNALLY;
	}
	
	@SuppressWarnings("unchecked")
	public Class<Q> getGranuleType() {
		if (granulator == null) {
			return (Class<Q>) CUDrop.class;
		}
		return granulator.getType();
	}
	
	public IAnalysisGranulator<Q> getGranulator() {
		return granulator;
	}

	public String getLabel() {
		StringBuilder sb = new StringBuilder();
		for(IIRAnalysis<Q> a : this) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(a.name());
		}
		return sb.toString();
	}
}
