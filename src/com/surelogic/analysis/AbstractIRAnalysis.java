/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractIRAnalysis.java,v 1.4 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.*;
import jsr166y.forkjoin.Ops.*;

import org.apache.commons.lang.SystemUtils;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.proxy.AbstractDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;

public abstract class AbstractIRAnalysis<T extends IBinderClient, Q> implements IIRAnalysis {
	private IIRProject project;
	private IBinder binder;
	protected final ThreadLocalAnalyses analyses = new ThreadLocalAnalyses();
	
    public static final boolean singleThreaded  = false || SystemUtils.IS_JAVA_1_5;
    private static final int threadCount = 
    	IDE.getInstance().getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT);
	private static final ForkJoinExecutor pool   = singleThreaded ? null : new ForkJoinPool(threadCount);  
	
	// TODO use ThreadLocal trick to collect all the builders
	private final List<AbstractDropBuilder> builders = new Vector<AbstractDropBuilder>();
	/**
	 * Used to queue up work across comp units before running in parallel
	 */
	private final IParallelArray<Q> workQueue;
	private Procedure<Q> workProc;
	private static final int FLUSH_SIZE = 20*threadCount;
	
	protected AbstractIRAnalysis(Class<Q> type) {		
		if (type != null) {
			System.out.println("Threads: "+threadCount);
			System.out.println("Singlethreaded? "+singleThreaded);
			workQueue = createIParallelArray(type);
		} else {
			workQueue = null;
		}
	}
	
	protected final void setWorkProcedure(Procedure<Q> proc) {
		workProc = proc;
	}
	
	protected final Procedure<Q> getWorkProcedure() {
		return workProc;
	}
	
	protected void queueWork(Collection<Q> work) {
		if (workQueue != null) {
			List<Q> l = workQueue.asList();
			l.addAll(work);
			if (l.size() > FLUSH_SIZE) {
				flushWorkQueue();
			}
		}
	}
	
	private void flushWorkQueue() {
		if (workQueue != null && workProc != null) {
			List<Q> l = workQueue.asList();
			System.out.println("Flushing: "+l.size());
			workQueue.apply(workProc);
			l.clear();
		}
	}
	
	private <E> IParallelArray<E> createIParallelArray(Class<E> type) {
		final IParallelArray<E> array = runInParallel() ? 
				ParallelArray.create(0, type, pool) : new NonParallelArray<E>();	
		return array;
	}
	
	protected <E> void runInParallel(Class<E> type, Collection<E> c, Procedure<E> proc) {
		if (c.isEmpty()) {
			return;
		}
		final IParallelArray<E> array = createIParallelArray(type);
		array.asList().addAll(c);
		/*
		for(Procedure<E> p : procs) {
			array.apply(p);
		}
		*/
		array.apply(proc);
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
	
	protected boolean runInParallel() {
		return false;
	}
	
	public String name() {
		return getClass().getSimpleName();
	}
	
	public final void handleBuilder(AbstractDropBuilder b) {
		builders.add(b);
	}
	
	protected final void finishBuild() {
		flushWorkQueue();
		
		for(AbstractDropBuilder b : builders) {
			b.build();
		}
		builders.clear();
	}
	
	public final void analyzeBegin(final IIRProject p) {
		final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv(p);
		final IBinder binder        = tEnv.getBinder(); 
		//final IIRProject old        = project;
		project = p;		
		this.binder = binder;
		builders.clear();
		
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
	
	protected abstract T constructIRAnalysis(IBinder binder);
	
	/*
	protected final void finishAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	*/
	
	public final boolean doAnalysisOnAFile(final CUDrop cud, 
			final IAnalysisMonitor monitor) {
		/*
		T analysis = getAnalysis();
		if (analysis == null) {
			setupAnalysis();
		}
		*/
		Object rv = runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				result = doAnalysisOnAFile(cud, cud.cu, monitor);
			}
		});
		return rv == Boolean.TRUE;
	}
	protected abstract boolean doAnalysisOnAFile(CUDrop cud, IRNode cu, IAnalysisMonitor monitor);
	
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
				T a = constructIRAnalysis(binder);
				AtomicReference<T> ref = new AtomicReference<T>(a);				
				analysisRefs.add(ref);
				return ref;
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
}
