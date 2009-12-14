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
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;

public abstract class AbstractIRAnalysis<T> implements IIRAnalysis {
	@SuppressWarnings("unchecked")
	protected final T nullAnalysis = (T) new Object();
	private IIRProject project;
	private IBinder binder;
	private final ThreadLocal<T> analysis = new ThreadLocal<T>();
	
    public static final boolean singleThreaded  = false || SystemUtils.IS_JAVA_1_5;
    private static final int threadCount = 
    	IDE.getInstance().getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT);
	private static final ForkJoinExecutor pool   = singleThreaded ? null : new ForkJoinPool(threadCount);  
	protected static final List<Void> nulls = new ArrayList<Void>();
	static {
		for(int i=0;i<threadCount; i++) {
			nulls.add(null);
		}
	}
	
	// TODO use ThreadLocal trick to collect all the builders
	private List<ResultDropBuilder> builders = new Vector<ResultDropBuilder>();
	
	protected <E> void runInParallel(Class<E> type, Collection<E> c, Procedure<E> proc) {
		final IParallelArray<E> array = singleThreaded ? 
				new NonParallelArray<E>() : ParallelArray.create(0, type, pool);	
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
		return analysis.get();
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
	
	public final void handleBuilder(ResultDropBuilder b) {
		builders.add(b);
	}
	
	protected final void finishBuild() {
		for(ResultDropBuilder b : builders) {
			b.build();
		}
		builders.clear();
	}
	
	public final void analyzeBegin(final IIRProject p) {
		final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv(p);
		final IBinder binder        = tEnv.getBinder(); 
		final IIRProject old        = project;
		project = p;		
		this.binder = binder;
		builders.clear();
		
		startAnalyzeBegin(p, binder);
		if (!runInParallel() || singleThreaded) {
			setupAnalysis(old != p);
		} else {
			runInParallel(Void.class, nulls, new Procedure<Void>() {
				public void op(Void v) {
					setupAnalysis(old != p);
				}				
			});
		}
		finishAnalyzeBegin(p, binder);
	}
	
	private final void setupAnalysis(boolean diffProject) {
		if (flushAnalysis() || diffProject || analysis.get() == null) {			
			runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
				public void run() {
					T a = constructIRAnalysis(binder);
					analysis.set(a);
				}
			});
		}
	}
	
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	
	@SuppressWarnings("unchecked")
	protected T constructIRAnalysis(IBinder binder) {
		return nullAnalysis;
	}
	
	protected void finishAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	
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
}
