package com.surelogic.analysis;

import java.util.Collection;
import java.util.List;

import jsr166y.*;

import org.apache.commons.lang3.SystemUtils;

import com.surelogic.analysis.granules.IAnalysisGranule;

import edu.cmu.cs.fluid.ide.*;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import extra166y.*;
import extra166y.Ops.Procedure;

public class ConcurrentAnalysis<Q extends IAnalysisGranule> {
	public static final int threadCount = IDE.getInstance().getIntPreference(
			IDEPreferences.ANALYSIS_THREAD_COUNT);
	public static final boolean singleThreaded = false || SystemUtils.IS_JAVA_1_5 || threadCount < 2;
	public static final ForkJoinPool pool = new ForkJoinPool(singleThreaded ? 1 : threadCount);
	
	private static final ParallelArray<Integer> dummyArray = singleThreaded ? null :
		ParallelArray.create(threadCount*2, Integer.class, pool);
	
	public static void executeOnAllThreads(Procedure<Integer> proc) {
		if (dummyArray == null) {
			return;
		}
		dummyArray.apply(proc);
	}
	
	public static void clearThreadLocal(final ThreadLocal<?> l) {
		if (pool == null) {
			return;
		}
		Procedure<Integer> proc = new Procedure<Integer>() {
			@Override
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
	private final ParallelArray<Q> workQueue;
	private Procedure<Q> workProc;
	private static final int FLUSH_SIZE = 20 * threadCount;

	protected ConcurrentAnalysis(boolean inParallel, Class<Q> type) {
		runInParallel = inParallel;
		if (runInParallel && type != null) {
			// System.out.println("Threads: "+threadCount);
			// System.out.println("Singlethreaded? "+singleThreaded);
			workQueue = createParallelArray(type);
		} else {
			workQueue = null;
		}
	}

	protected <E> ParallelArray<E> createParallelArray(Class<E> type) {
		final ParallelArray<E> array = ParallelArray.create(0, type, pool);
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

	protected boolean queueWork(Iterable<? extends Q> work) {
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

	protected boolean queueWork(Collection<? extends Q> work) {
		if (workQueue != null) {
			List<Q> l = workQueue.asList();
			l.addAll(work);
			if (l.size() > FLUSH_SIZE) {
				flushWorkQueue();
				return true;
			}
		} else {
			throw new IllegalStateException();
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
	public <E extends IAnalysisGranule> void runInParallel(Class<E> type, Collection<? extends E> c,
			final Procedure<E> proc) {
		if (c.isEmpty()) {
			return;
		}
		final ParallelArray<E> array = createParallelArray(type);
		array.asList().addAll(c);
		/*
		 * for(Procedure<E> p : procs) { array.apply(p); }
		 */
		final PromiseFramework frame = PromiseFramework.getInstance();
		array.apply(new Procedure<E>() {
			@Override
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
	
	// Probably shouldn't be run on one granule 
	@SuppressWarnings("serial")
	public <E extends IAnalysisGranule> void runAsTasks(final List<? extends E> c,
			final Procedure<E> proc) {
		if (c.isEmpty()) {
			return;
		}
		final int size = c.size();
		if (size == 1) {
			proc.op(c.get(0));
			return;
		}
		// TODO what about other cases?
		
		if (false) {
			// Note: this thread blocks if called w/ invoke()
			pool.invoke(new RecursiveAction() {
				@Override
				protected void compute() {
					RecursiveAction[] tasks = new RecursiveAction[c.size()];
					int i = 0;
					for(final E g : c) {
						tasks[i] = new RecursiveAction() {
							@Override
							protected void compute() {
								proc.op(g);
							}			
						};
						i++;
					}
					invokeAll(tasks);
				}			
			});
		} else {
			final E first = c.get(0);
			final ForkJoinTask<Void> f;
			if (size == 2) {
				final E second = c.get(1);			
				f = pool.submit(new RecursiveAction() {
					@Override
					protected void compute() {
						proc.op(second);
					}
				});
			} else { // should be n > 2
				f = pool.submit(new RecursiveAction() {
					@Override
					protected void compute() {
						RecursiveAction[] tasks = new RecursiveAction[c.size()-1];					
						for(int i=1; i<size; i++) {
							final E g = c.get(i);
							tasks[i] = new RecursiveAction() {
								@Override
								protected void compute() {
									proc.op(g);
								}			
							};
							i++;
						}
						invokeAll(tasks);
					}			
				});
			}
			proc.op(first);
			f.join();
		}
	}
}
