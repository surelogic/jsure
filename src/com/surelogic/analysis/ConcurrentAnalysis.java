/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import java.util.Collection;
import java.util.List;

import jsr166y.forkjoin.*;
import jsr166y.forkjoin.Ops.Procedure;

import org.apache.commons.lang.SystemUtils;

import edu.cmu.cs.fluid.ide.*;

public class ConcurrentAnalysis<Q> {
    public static final boolean singleThreaded  = false || SystemUtils.IS_JAVA_1_5;
    public static final int threadCount = 
    	IDE.getInstance().getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT);
	public static final ForkJoinExecutor pool   = singleThreaded ? null : new ForkJoinPool(threadCount);  

	private final boolean runInParallel;
	
	/**
	 * Used to queue up work across comp units before running in parallel
	 */
	private final IParallelArray<Q> workQueue;
	private Procedure<Q> workProc;
	private static final int FLUSH_SIZE = 20*threadCount;
	
	protected ConcurrentAnalysis(boolean inParallel, Class<Q> type) {
		runInParallel = inParallel;
		if (runInParallel && type != null) {
			//System.out.println("Threads: "+threadCount);
			//System.out.println("Singlethreaded? "+singleThreaded);
			workQueue = createIParallelArray(type);
		} else {
			workQueue = null;
		}
	}
	
	protected <E> IParallelArray<E> createIParallelArray(Class<E> type) {
		final IParallelArray<E> array = runInParallel ? 
				ParallelArray.create(0, type, pool) : new NonParallelArray<E>();	
		return array;
	}
	
	protected final void setWorkProcedure(Procedure<Q> proc) {
		workProc = proc;
	}
	
	protected final Procedure<Q> getWorkProcedure() {
		return workProc;
	}
	
	protected boolean queueWork(Q work) {
		if (workQueue != null) {
			List<Q> l = workQueue.asList();
			l.add(work);
			if (l.size() > FLUSH_SIZE) {
				flushWorkQueue();
				
				//System.out.println("#builders    : "+builders.size());
				return true;
			}
		}
		return false;
	}
	
	protected boolean queueWork(Collection<Q> work) {
		if (workQueue != null) {
			List<Q> l = workQueue.asList();
			l.addAll(work);
			if (l.size() > FLUSH_SIZE) {
				flushWorkQueue();
				return true;
			}
		}
		return false;
	}
	
	protected void flushWorkQueue() {
		if (workQueue != null && workProc != null) {
			List<Q> l = workQueue.asList();
			//System.out.println("Flushing: "+l.size());
			workQueue.apply(workProc);
			l.clear();
		}
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
	
	protected final boolean runInParallel() {
		return runInParallel;
	}
}
