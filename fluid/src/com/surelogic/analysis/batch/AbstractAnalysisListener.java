/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/batch/AbstractAnalysisListener.java,v 1.1 2007/11/21 16:05:03 chance Exp $*/
package com.surelogic.analysis.batch;


public abstract class AbstractAnalysisListener implements IAnalysisListener {
  public void analysisStarting() {
	  // Nothing to do
  }

  public void analysisCompleted() {
	  // Nothing to do
  }

  public void analysisPostponed() {
	  // Nothing to do
  }

  public void analysisCancelled() {
	  // Nothing to do
  }
}
