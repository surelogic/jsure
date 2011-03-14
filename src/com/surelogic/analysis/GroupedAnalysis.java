package com.surelogic.analysis;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.proxy.IDropBuilder;
import edu.cmu.cs.fluid.util.EmptyIterator;

public class GroupedAnalysis implements IIRAnalysis {
	private final Class<?> group;
	private final IIRAnalysis[] analyses;

	
	public GroupedAnalysis(Class<?> group, List<IIRAnalysis> grouped) {
		this.group = group;
		analyses = grouped.toArray(new IIRAnalysis[grouped.size()]);
	}

	public Class<?> getGroup() {
		return group;
	}
	
	public String name() {
		StringBuilder sb = new StringBuilder();
		for(IIRAnalysis a : analyses) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(a.name());
		}
		return sb.toString();
	}

	public boolean runInParallel() {
		return analyses[0].runInParallel();
	}
	
	public boolean analyzeAll() {
		return analyses[0].analyzeAll();
	}

	public void init(IIRAnalysisEnvironment env) {
		for(IIRAnalysis a : analyses) {
			a.init(env);
		}
	}
	
	public void analyzeBegin(IIRAnalysisEnvironment env, IIRProject p) {
		for(IIRAnalysis a : analyses) {
			a.analyzeBegin(env, p);
		}
	}

	public boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud) {
		for(IIRAnalysis a : analyses) {
			a.doAnalysisOnAFile(env, cud);
		}
		return true;
	}


	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		for(IIRAnalysis a : analyses) {
			handleAnalyzeEnd(a, env, p);
		}
		return EmptyIterator.prototype();
	}
	
	public static void handleAnalyzeEnd(IIRAnalysis a, IIRAnalysisEnvironment env, IIRProject project) {
		boolean moreToAnalyze;
		do {
			moreToAnalyze = false;
			for(IRNode n : a.analyzeEnd(env, project)) {
				moreToAnalyze = true;
	
				final CUDrop cud = CUDrop.queryCU(n);
				// TODO parallelize?
				a.doAnalysisOnAFile(env, cud);
			}
		} while (moreToAnalyze);
	}

	public void postAnalysis(IIRProject p) {
		for(IIRAnalysis a : analyses) {
			a.postAnalysis(p);
		}
	}

	public void finish(IIRAnalysisEnvironment env) {
		for(IIRAnalysis a : analyses) {
			a.finish(env);
		}
	}
	
	public void handleBuilder(IDropBuilder b) {
		throw new UnsupportedOperationException();
	}
}
