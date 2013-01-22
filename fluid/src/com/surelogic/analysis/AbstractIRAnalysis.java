/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractIRAnalysis.java,v 1.4 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.EmptyIterator;

public abstract class AbstractIRAnalysis<T extends IBinderClient, Q extends ICompUnitContext> extends ConcurrentAnalysis<Q> implements IIRAnalysis {
	//private IIRProject project;
	private IBinder binder;
	protected final ThreadLocalAnalyses analyses = new ThreadLocalAnalyses();
	
	protected AbstractIRAnalysis(boolean inParallel, Class<Q> type) {		
		super(inParallel, type);
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
	
	/*
	protected final void finishAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	*/
	
	@Override
  public final boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env, final CUDrop cud) {
		/*
		T analysis = getAnalysis();
		if (analysis == null) {
			setupAnalysis();
		}
		*/
		Object rv = runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			@Override
      public void run() {
				result = doAnalysisOnAFile(env, cud, cud.getCompilationUnitIRNode());
			}
		});
		return rv == Boolean.TRUE;
	}
	protected abstract boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, IRNode cu);
		
	@Override
  public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		return new EmptyIterator<IRNode>();
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
		private final List<AtomicReference<T>> analysisRefs = new Vector<AtomicReference<T>>();
		private final ThreadLocal<AtomicReference<T>> analysis = new ThreadLocal<AtomicReference<T>>() {
			@Override
			protected AtomicReference<T> initialValue() {
				try {
					T a = constructIRAnalysis(binder);
					AtomicReference<T> ref = new AtomicReference<T>(a);				
					analysisRefs.add(ref);
					return ref;
				} catch (Exception e) {
					return null;
				}
			}
		};
		
		T getAnalysis() {
			return analysis.get().get();
		}
		
		void updateAllAnalyses(IBinder binder) {
			for(AtomicReference<T> ref : analysisRefs) {
				T old = ref.get();
				if (old.getBinder() != binder) {
					ref.set(constructIRAnalysis(binder));
				}
			}
		}
		
		void resetAllAnalyses(IBinder binder) {
			for(AtomicReference<T> ref : analysisRefs) {
				ref.set(constructIRAnalysis(binder));
			}
		}
		
		public void clearCaches() {
			for(AtomicReference<T> ref : analysisRefs) {
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
