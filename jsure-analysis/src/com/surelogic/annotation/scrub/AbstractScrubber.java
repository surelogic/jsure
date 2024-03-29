package com.surelogic.annotation.scrub;

import java.util.Comparator;

import com.surelogic.NonNull;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractScrubber implements IAnnotationScrubber {
  private final String name;
  private final String[] dependencies;
  private final String[] runsBefore;
  private final ScrubberOrder order;

  public AbstractScrubber(String[] before, String name, ScrubberOrder order, String... deps) {
    this.name = name;
    this.order = order;
    this.dependencies = deps;
    this.runsBefore = before;
  }

  /**
   * Returns the scrubber's name
   */
  @Override
  public final String name() {
    return name;
  }

  @Override
  public ScrubberOrder order() {
    return order;
  }

  /**
   * Returns a list of strings, each of which is the name of another scrubber
   * that this scrubber depends on having run before it.
   */
  @Override
  public final String[] dependsOn() {
    return dependencies;
  }

  /**
   * Returns a list of strings, each of which is the name of another scrubber
   * that this scrubber needs to run before.
   */
  @Override
  public final String[] shouldRunBefore() {
    return runsBefore;
  }

  /**
   * Returns the context
   */
  @NonNull
  public final AnnotationScrubberContext getContext() {
    return AnnotationScrubberContext.getInstance();
  }

  /*
   * protected static final void markAsUnassociated(IAASTRootNode a) {
   * TestResult expected = AASTStore.getTestResult(a);
   * TestResult.checkIfMatchesResult(expected, TestResultType.UNASSOCIATED); }
   */

  protected static final Comparator<IAASTRootNode> aastComparator = new Comparator<IAASTRootNode>() {
    @Override
    public int compare(IAASTRootNode o1, IAASTRootNode o2) {
      final IRNode p1 = o1.getPromisedFor();
      final IRNode p2 = o2.getPromisedFor();
      int rv;
      if (p1.equals(p2)) {
        // Actually not sufficient, even though we're only comparing things in
        // the same type
        rv = o1.getOffset() - o2.getOffset();
      } else {
        rv = p1.hashCode() - p2.hashCode();
      }
      if (rv != 0) {
        return rv;
      }
      // Most likely used for library annotations
      return o1.unparse(false).compareTo(o2.unparse(false));
    }
  };

  protected static final Comparator<PromiseDrop<? extends IAASTRootNode>> dropComparator = new Comparator<PromiseDrop<? extends IAASTRootNode>>() {
    @Override
    public int compare(PromiseDrop<? extends IAASTRootNode> o1, PromiseDrop<? extends IAASTRootNode> o2) {
      return aastComparator.compare(o1.getAAST(), o2.getAAST());
    }

  };
}
