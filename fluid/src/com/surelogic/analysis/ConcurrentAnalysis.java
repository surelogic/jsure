package com.surelogic.analysis;

import java.util.Collection;
import java.util.List;

import jsr166y.forkjoin.*;
import jsr166y.forkjoin.Ops.Procedure;

import org.apache.commons.lang3.SystemUtils;

import edu.cmu.cs.fluid.ide.*;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class ConcurrentAnalysis<Q extends ICompUnitContext> {
	public static final int threadCount = IDE.getInstance().getIntPreference(
			IDEPreferences.ANALYSIS_THREAD_COUNT);
	public static final boolean singleThreaded = false || SystemUtils.IS_JAVA_1_5 || threadCount < 2;
	public static final ForkJoinExecutor pool = singleThreaded ? null
			: new ForkJoinPool(threadCount);
	
	private static final IParallelArray<Integer> dummyArray = singleThreaded ? null :
		ParallelArray.create(threadCount*10, Integer.class, pool);
	
	public static void clearThreadLocal(final ThreadLocal<?> l) {
		if (pool == null) {
			return;
		}
		Procedure<Integer> proc = new Procedure<Integer>() {
			public void op(Integer ignored) {
				l.remove();
			}
		};
		dummyArray.apply(proc);
	}
	
	private final boolean runInParallel;

	/**
	 * Used to queue up work across comp units before running in parallel
	 */
	private final IParallelArray<Q> workQueue;
	private Procedure<Q> workProc;
	private static final int FLUSH_SIZE = 20 * threadCount;

	protected ConcurrentAnalysis(boolean inParallel, Class<Q> type) {
		runInParallel = inParallel;
		if (runInParallel && type != null) {
			// System.out.println("Threads: "+threadCount);
			// System.out.println("Singlethreaded? "+singleThreaded);
			workQueue = createIParallelArray(type);
		} else {
			workQueue = null;
		}
	}

	protected <E> IParallelArray<E> createIParallelArray(Class<E> type) {
		final IParallelArray<E> array = runInParallel ? ParallelArray.create(0,
				type, pool) : new NonParallelArray<E>();
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
				return true;
			}
		}
		return false;
	}

	protected boolean queueWork(Iterable<Q> work) {
		if (workQueue != null) {
			List<Q> l = workQueue.asList();
			for (Q w : work) {
				l.add(w);
			}
			if (l.size() > FLUSH_SIZE) {
				flushWorkQueue();
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
			// System.out.println("Flushing: "+l.size());
			workQueue.apply(workProc);
			l.clear();
		}
	}

	/**
	 * Used by various analyses to handle concurrency themselves
	 */
	protected <E extends ICompUnitContext> void runInParallel(Class<E> type, Collection<E> c,
			final Procedure<E> proc) {
		if (c.isEmpty()) {
			return;
		}
		final IParallelArray<E> array = createIParallelArray(type);
		array.asList().addAll(c);
		/*
		 * for(Procedure<E> p : procs) { array.apply(p); }
		 */
		final PromiseFramework frame = PromiseFramework.getInstance();
		array.apply(new Procedure<E>() {
			public void op(E arg) {
				try {
					frame.pushTypeContext(arg.getCompUnit());
					proc.op(arg);
				} finally {
		            frame.popTypeContext();
				}
			}
		});
	}

	public ConcurrencyType runInParallel() {
		return runInParallel ? ConcurrencyType.INTERNALLY : ConcurrencyType.EXTERNALLY;
	}
}
