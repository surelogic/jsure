/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractWholeIRAnalysis.java,v 1.5 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.util.CachedSet;

public abstract class AbstractWholeIRAnalysis<T extends IBinderClient, Q extends IAnalysisGranule>
    extends AbstractIRAnalysis<T, Q> {

  protected final Logger LOG;

  protected AbstractWholeIRAnalysis(String logName) {
    this(false, logName);
  }

  protected AbstractWholeIRAnalysis(boolean inParallel, String logName) {
    super(inParallel);
    LOG = SLLogger.getLogger(logName);
  }

  @Override
  public final boolean analyzeAll() {
    return true;
  }

  @Override
  public void postAnalysis(IIRProject p) {
    try {
      clearCaches();
      CachedSet.clearCache();
      LabelList.clearCache();
    } catch (NullPointerException e) {
      LOG.log(Level.INFO, "Ignoring NPE", e);
    }
  }

  protected abstract void clearCaches();
}
