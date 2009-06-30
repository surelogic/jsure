/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/SimpleScrubber.java,v 1.5 2007/10/17 13:49:47 chance Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * Only includes the basics needed to implement a scrubber,
 * mostly to support creation of scrubbers that don't specifically
 * iterate over AASTs
 * 
 * @author Edwin.Chan
 */
public abstract class SimpleScrubber implements IAnnotationScrubber<IAASTRootNode> {
  private IAnnotationScrubberContext context;
  private final String name;
  private final String[] dependencies;
  private final String[] runsBefore;
  private final ScrubberOrder order;
  
  public SimpleScrubber(String name, String... deps) {
    this(NONE, name, ScrubberOrder.NORMAL, deps);
  }
  
  public SimpleScrubber(String name, ScrubberOrder order, String... deps) {
    this(NONE, name, order, deps);
  }
  
  public SimpleScrubber(String[] before, String name, ScrubberOrder order, String... deps) {
    this.name         = name;
    this.order        = order;
    this.dependencies = deps;    
    this.runsBefore   = before;
  }
  
  public final String name() {
    return name;
  }

  public ScrubberOrder order() {
    return order;
  }
  
  public final String[] dependsOn() {
    return dependencies;
  }

  public final String[] shouldRunBefore() {
    return runsBefore;
  }
  
  public final void setContext(IAnnotationScrubberContext c) {
    context = c;
  }
  
  protected final IAnnotationScrubberContext getContext() {
    return context;
  }

  public final void run() {
    IDE.runAtMarker(new AbstractRunner() {
      public void run() {
        scrub();
      }
    });
  }
  
  protected abstract void scrub();
  
  protected final void markAsUnassociated(IAASTRootNode a) {
    TestResult expected = AASTStore.getTestResult(a);   
    TestResult.checkIfMatchesResult(expected, TestResultType.UNASSOCIATED);
  }
}
