package com.surelogic.annotation.scrub;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;

/**
 * Works like AbstractAASTScrubber, but on PromiseDrops. Requires you to define
 * getRelevantAnnotations() and override other methods if needed
 * 
 * Probably registered as shown below:
 * 
 * @Override public void register(PromiseFramework fw) { ...
 *           registerScrubber(fw, myNewScrubber); }
 * @author Edwin
 */
public abstract class AbstractPromiseScrubber<P extends PromiseDrop<? extends IAASTRootNode>> extends AbstractHierarchyScrubber<P> {
  protected AbstractPromiseScrubber(ScrubberType type, String[] before, String name, ScrubberOrder order, String[] deps) {
    super(type, before, name, order, deps);
  }

  protected abstract void processDrop(P a);

  /**
   * @return true if this scrubs an annotation that allows multiple annotation
   *         on the same declaration, e.g. @Promise
   */
  protected boolean allowsMultipleAnnosOnSameDecl() {
    return false;
  }

  @Override
  protected void processAASTsForType(IAnnotationTraversalCallback<P> ignored, IRNode decl, List<P> l) {
    try {
      if (!allowsMultipleAnnosOnSameDecl()) {
        // Sort to process in a consistent order
        Collections.sort(l, dropComparator);
      }
    } catch (IllegalArgumentException e) {
      SLLogger.getLogger().log(Level.WARNING, "While sorting AASTs for " + JavaNames.getFullName(decl), e);
    }

    for (P a : l) {
      /*
       * if ("MUTEX".equals(a.toString())) { System.out.println("Scrubbing: "
       * +a.toString()); }
       */
      processDrop(a);
    }
  }

  public final IAnnotationTraversalCallback<P> NULL_CALLBACK = new IAnnotationTraversalCallback<P>() {
    @Override
    public void addDerived(P derivedPromise, PromiseDrop<? extends P> originalPromise) {
      SLLogger.getLogger().log(Level.WARNING,
          "A promise derived the promise \"" + derivedPromise + "\" then ignored it...the original promise is \"" + originalPromise
              + "\" (with AAST: " + originalPromise.getAAST() + ")",
          new Exception());
    }
  };

  @Override
  protected IAnnotationTraversalCallback<P> getNullCallback() {
    return NULL_CALLBACK;
  }

  @Override
  protected void finishAddDerived(P clone, PromiseDrop<? extends P> nonsensical) {
    // Nothing to do?
  }

  @Override
  protected void finishRun() {
    // Nothing to do?
    // TODO Reset state?
  }
}
