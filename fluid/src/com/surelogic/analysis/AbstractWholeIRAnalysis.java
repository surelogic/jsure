/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractWholeIRAnalysis.java,v 1.5 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.util.CachedSet;

public abstract class AbstractWholeIRAnalysis<T extends IBinderClient, Q extends ICompUnitContext> extends AbstractIRAnalysis<T, Q> {

  protected final Logger LOG;

  protected AbstractWholeIRAnalysis(String logName) {
    this(false, null, logName);
  }

  protected AbstractWholeIRAnalysis(boolean inParallel, Class<Q> type, String logName) {
    super(inParallel, type);
    LOG = SLLogger.getLogger(logName);
  }

  public final boolean analyzeAll() {
    return true;
  }

  @Override
  public void postAnalysis(IIRProject p) {
    clearCaches();
    CachedSet.clearCache();
    LabelList.clearCache();
  }

  protected abstract void clearCaches();
}
