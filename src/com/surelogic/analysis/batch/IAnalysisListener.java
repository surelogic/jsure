package com.surelogic.analysis.batch;

/**
 * Interface to notify registered listeners when Fluid analysis events occur.
 * 
 */
public interface IAnalysisListener {
  /**
   * Notification that analysis is starting.
   */
  void analysisStarting();

  /**
   * Notification that analysis has been completed.
   */
  void analysisCompleted();

  /**
   * Notification that analysis has been postponed until compilation errors are
   * removed.
   */
  void analysisPostponed();
  
  /**
   * Notification that analysis has been cancelled
   */
  void analysisCancelled();
}
