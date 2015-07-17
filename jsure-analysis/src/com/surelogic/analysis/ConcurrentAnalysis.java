package com.surelogic.analysis;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.common.concurrent.ParallelArray;
import com.surelogic.common.concurrent.Procedure;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class ConcurrentAnalysis<Q extends IAnalysisGranule> {

  public static int getThreadCountToUse() {
    int result = IDE.getInstance().getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT);
    if (result < 1)
      result = Runtime.getRuntime().availableProcessors();
    return result;
  }

  final int f_flushSize = getThreadCountToUse() * 20;

  /**
   * Used to queue up work across comp units before running in parallel
   */
  @NonNull
  private final ParallelArray<Q> f_workQueue;

  final boolean f_inParallel;

  protected ConcurrentAnalysis(boolean inParallel) {
    f_inParallel = inParallel;
    f_workQueue = new ParallelArray<>();
  }

  @Nullable
  private Procedure<Q> f_workProc;

  protected final void setWorkProcedure(Procedure<Q> value) {
    f_workProc = value;
  }

  @Nullable
  protected final Procedure<Q> getWorkProcedure() {
    return f_workProc;
  }

  /**
   * Queues work to do.
   * 
   * @param work
   *          to do.
   * @return {@code true} if the work was run and flushed, {@code false}
   *         otherwise.
   */
  protected boolean queueWork(Q work) {
    if (work == null) {
      SLLogger.getLogger().log(Level.WARNING, "queueWork(null) called", new IllegalArgumentException());
    } else {
      f_workQueue.asList().add(work);
      if (f_workQueue.asList().size() > f_flushSize) {
        flushWorkQueue();
        return true;
      }
    }
    return false;
  }

  /**
   * Queues work to do.
   * 
   * @param work
   *          collection of to do.
   * @return {@code true} if the work was run and flushed, {@code false}
   *         otherwise.
   */
  protected boolean queueWork(Collection<? extends Q> work) {
    if (work == null) {
      SLLogger.getLogger().log(Level.WARNING, "queueWork(null) called", new IllegalArgumentException());
    } else {
      f_workQueue.asList().addAll(work);
      if (f_workQueue.asList().size() > f_flushSize) {
        flushWorkQueue();
        return true;
      }
    }
    return false;
  }

  protected void flushWorkQueue() {
    if (f_workProc == null) {
      // only warn if something exists to run
      if (!f_workQueue.asList().isEmpty())
        SLLogger.getLogger().log(Level.WARNING, "flushWorkQueue() called with no work procedure set",
            new IllegalArgumentException());
    } else {
      f_workQueue.apply(f_workProc, f_inParallel ? getThreadCountToUse() : 1);
      f_workQueue.asList().clear();
    }
  }

  /**
   * Used by various analyses to handle concurrency themselves
   */
  public <E extends IAnalysisGranule> void runInParallel(Class<E> type, Collection<? extends E> c, final Procedure<E> proc) {
    if (c == null || c.isEmpty()) {
      SLLogger.getLogger().log(Level.WARNING, "runInParallel() called with null or empty collection",
          new IllegalArgumentException());
      return;
    }
    if (proc == null) {
      SLLogger.getLogger().log(Level.WARNING, "runInParallel() called with null proc", new IllegalArgumentException());
      return;
    }
    final ParallelArray<E> array = new ParallelArray<>();
    array.asList().addAll(c);
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
    return f_inParallel ? ConcurrencyType.INTERNALLY : ConcurrencyType.EXTERNALLY;
  }
}
