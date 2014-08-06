/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractIRAnalysis.java,v 1.4 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.IterableThreadLocal;

public abstract class AbstractIRAnalysis<T extends IBinderClient, Q extends IAnalysisGranule> extends ConcurrentAnalysis<Q> implements IIRAnalysis<Q> {
	//private IIRProject project;
	private String label;
	private IBinder binder;
	protected final ThreadLocalAnalyses analyses = new ThreadLocalAnalyses();
	
	protected AbstractIRAnalysis(boolean inParallel, Class<Q> type) {		
		super(inParallel, type);
	}

	@Override
  public String label() {
		return label;
	}
	
  @Override
	public void setLabel(String l) {
		if (l == null) {
			throw new IllegalArgumentException();
		}
		if (label != null) {
			throw new IllegalStateException();
		}
		label = l;
	}
			
	
	protected IBinder getBinder() {
		return binder;
	}
	
	protected T getAnalysis() {
		return analyses.getAnalysis();
	}
	
	protected boolean flushAnalysis() {
		return false;
	}
	
	@Override
  public Class<?> getGroup() {
		return null;
	}
	
  @Override
	public IAnalysisGranulator<Q> getGranulator() {
		return null;
	}
	
	@Override
  public String name() {
		return getClass().getSimpleName();
	}
	
	/**
	 * This doesn't affect other analyses, and so it can be called in analyzeEnd()
	 */
	protected final void finishBuild() {
		flushWorkQueue();
	}
		
	@Override
  public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}
    /*
	public void preAnalysis(IIRAnalysisEnvironment env) {
		// Nothing to do	
	}
	*/
	
	@Override
  public final void analyzeBegin(IIRAnalysisEnvironment env, final IIRProject p) {
		final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv(p);
		final IBinder binder        = tEnv.getBinder(); 
		//final IIRProject old        = project;
		//project = p;		
		if (binder == null) {
			throw new IllegalStateException();
		}
		this.binder = binder;
		
		startAnalyzeBegin(p, binder);
		if (flushAnalysis()) {
			analyses.resetAllAnalyses(binder);
		} else {
			analyses.updateAllAnalyses(binder);
		}
		/*
		if (!runInParallel() || singleThreaded) {
			setupAnalysis(old != p);
		} else {
			runInParallel(Void.class, nulls, new Procedure<Void>() {
				public void op(Void v) {
					//System.out.println(old+" ?= "+p);
					setupAnalysis(old != p);
				}				
			});
		}
		*/
		//finishAnalyzeBegin(p, binder);
	}
	
	/*
	final void setupAnalysis(boolean diffProject) {
		//System.out.println(Thread.currentThread()+" : "+flushAnalysis()+", "+diffProject);
		if (flushAnalysis() || diffProject || analysis.get() == null) {			
			runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
				public void run() {
					analysis.remove();
					T a = constructIRAnalysis(binder);
					analysis.set(a);
				}
			});
		}
	}
	*/
	
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	
	/**
	 * Called once per thread
	 */
	protected abstract T constructIRAnalysis(IBinder binder);
	
	@Override
	public final boolean doAnalysisOnGranule(final IIRAnalysisEnvironment env, final Q g) {
		// TODO move this to Util
		Object rv = runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			@Override
			public void run() {
				if (g instanceof CUDrop) {
					final CUDrop cud = (CUDrop) g;				
					result = doAnalysisOnAFile(env, cud, cud.getCompilationUnitIRNode());
				} else {
					result = doAnalysisOnGranule_wrapped(env, g);
				}
			}
		});
		return rv == Boolean.TRUE;
	}
	
	protected boolean doAnalysisOnGranule_wrapped(final IIRAnalysisEnvironment env, final Q g) {
		throw new NotImplemented();
	}
	
	public boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud) {
		return doAnalysisOnAFile(env, cud, cud.getCompilationUnitIRNode());
	}
		
	// Default implementation for non-CUDrop analyses
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, IRNode cu) {
		getGranulator().extractGranules(cud.getTypeEnv(), cu);
		for(Q granule : getGranulator().getGranules()) {
			doAnalysisOnGranule(env, granule);
		}
		return true;
	}
		
	@Override
  public Iterable<Q> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		return new EmptyIterator<Q>();
	}
	
	@Override
  public void postAnalysis(IIRProject p) {
		// Nothing to do	
	}
	
	@Override
  public void finish(IIRAnalysisEnvironment env) {
		// Nothing to do
	}
	
	protected static Object runVersioned(AbstractRunner r) {
		return IDE.runVersioned(r);
	}

	protected static Object runInVersion(AbstractRunner r) {
		return IDE.runAtMarker(r);
	}

	protected static Operator getOperator(final IRNode n) {
		return JJNode.tree.getOperator(n);
	}
	
	protected class ThreadLocalAnalyses {
		private final IterableThreadLocal<AtomicReference<T>> analysis = new IterableThreadLocal<AtomicReference<T>>() {
			@Override
			protected AtomicReference<T> makeInitialValue() {
				try {
					T a = constructIRAnalysis(binder);
					return new AtomicReference<T>(a);				
				} catch (Exception e) {
					return null;
				}
			}
		};
		
		T getAnalysis() {
			return analysis.get().get();
		}
		
		void updateAllAnalyses(IBinder binder) {
			for(AtomicReference<T> ref : analysis) {
				T old = ref.get();
				if (old.getBinder() != binder) {
					ref.set(constructIRAnalysis(binder));
				}
			}
		}
		
		void resetAllAnalyses(IBinder binder) {
			for(AtomicReference<T> ref : analysis) {
				ref.set(constructIRAnalysis(binder));
			}
		}
		
		public void clearCaches() {
			for(AtomicReference<T> ref : analysis) {
				ref.get().clearCaches();
			}
		}
	}
	
	protected void reportProblem(String msg) {
		reportProblem(msg, null);
	}
	
	protected void reportProblem(String msg, IRNode context) {
		final HintDrop d = HintDrop.newWarning(context);
		d.setMessage(msg);
	}
}
