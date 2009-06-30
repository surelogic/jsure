/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/batch/AbstractAnalysisListener.java,v 1.1 2007/11/21 16:05:03 chance Exp $*/
package com.surelogic.analysis.batch;


public abstract class AbstractAnalysisListener implements IAnalysisListener {
  public void analysisStarting() {
  }

  public void analysisCompleted() {
  }

  public void analysisPostponed() {
  }

  public void analysisCancelled() {
  }
}
