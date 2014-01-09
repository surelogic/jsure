package com.surelogic.analysis;

import java.util.List;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

public class GroupedAnalysis<Q extends IAnalysisGranule> implements IIRAnalysis<Q> {
	private final Class<?> group;
	private final IIRAnalysis<Q>[] analyses;

	
	public GroupedAnalysis(Class<?> group, List<IIRAnalysis<Q>> grouped) {
		this.group = group;
		analyses = grouped.toArray(new IIRAnalysis[grouped.size()]);
		
		// Check if all running the same way
		IIRAnalysis<Q> first = null;
		for(IIRAnalysis<Q> a : grouped) {
			if (first == null) {
				first = a;
			} else if (first.runInParallel() != a.runInParallel()) {
				SLLogger.getLogger().info(first.getClass().getCanonicalName()+": "+first.runInParallel()+" != "+a.runInParallel()+" :"+a);
			}
		}
	}

	@Override
  public Class<?> getGroup() {
		return group;
	}
	
	public IAnalysisGranulator<Q> getGranulator() {
		return analyses[0].getGranulator(); // TODO
	}
	
	@Override
  public String name() {
		StringBuilder sb = new StringBuilder();
		for(IIRAnalysis<Q> a : analyses) {
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
		for(IIRAnalysis<Q> a : analyses) {
			a.init(env);
		}
	}
	
	@Override
  public void analyzeBegin(IIRAnalysisEnvironment env, IIRProject p) {
		for(IIRAnalysis<Q> a : analyses) {
			a.analyzeBegin(env, p);
		}
	}

	@Override
  public boolean doAnalysisOnGranule(IIRAnalysisEnvironment env, Q cud) {
		for(IIRAnalysis<Q> a : analyses) {
			a.doAnalysisOnGranule(env, cud);
		}
		return true;
	}


	@Override
  public Iterable<Q> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		for(IIRAnalysis<Q> a : analyses) {
			handleAnalyzeEnd(a, env, p);
		}
		return new EmptyIterator<Q>();
	}
	
	public static <Q extends IAnalysisGranule> 
	void handleAnalyzeEnd(IIRAnalysis<Q> a, IIRAnalysisEnvironment env, IIRProject project) {
		boolean moreToAnalyze;
		do {
			moreToAnalyze = false;
			for(Q granule : a.analyzeEnd(env, project)) {
				moreToAnalyze = true;
	
				// TODO parallelize?
				a.doAnalysisOnGranule(env, granule);
			}
		} while (moreToAnalyze);
	}

	@Override
  public void postAnalysis(IIRProject p) {
		for(IIRAnalysis<Q> a : analyses) {
			a.postAnalysis(p);
		}
	}

	@Override
  public void finish(IIRAnalysisEnvironment env) {
		for(IIRAnalysis<Q> a : analyses) {
			a.finish(env);
		}
	}
}
