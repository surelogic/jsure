package com.surelogic.analysis;

import java.util.List;

import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public class GroupedAnalysis implements IIRAnalysis {
	private final Class<?> group;
	private final IIRAnalysis[] analyses;

	
	public GroupedAnalysis(Class<?> group, List<IIRAnalysis> grouped) {
		this.group = group;
		analyses = grouped.toArray(new IIRAnalysis[grouped.size()]);
	}

	@Override
  public Class<?> getGroup() {
		return group;
	}
	
	@Override
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

	@Override
  public ConcurrencyType runInParallel() {
		return analyses[0].runInParallel();
	}
	
	@Override
  public boolean analyzeAll() {
		return analyses[0].analyzeAll();
	}

	@Override
  public void init(IIRAnalysisEnvironment env) {
		for(IIRAnalysis a : analyses) {
			a.init(env);
		}
	}
	
	@Override
  public void analyzeBegin(IIRAnalysisEnvironment env, IIRProject p) {
		for(IIRAnalysis a : analyses) {
			a.analyzeBegin(env, p);
		}
	}

	@Override
  public boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud) {
		for(IIRAnalysis a : analyses) {
			a.doAnalysisOnAFile(env, cud);
		}
		return true;
	}


	@Override
  public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		for(IIRAnalysis a : analyses) {
			handleAnalyzeEnd(a, env, p);
		}
		return new EmptyIterator<IRNode>();
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

	@Override
  public void postAnalysis(IIRProject p) {
		for(IIRAnalysis a : analyses) {
			a.postAnalysis(p);
		}
	}

	@Override
  public void finish(IIRAnalysisEnvironment env) {
		for(IIRAnalysis a : analyses) {
			a.finish(env);
		}
	}
}
