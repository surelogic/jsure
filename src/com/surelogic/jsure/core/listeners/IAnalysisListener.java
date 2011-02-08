package com.surelogic.jsure.core.listeners;

/**
 * Interface to notify registered listeners when Fluid analysis events occur.
 * 
 * @see com.surelogic.jsure.core.listeners.NotificationHub
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
}
