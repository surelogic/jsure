package com.surelogic.jsure.core.listeners;

import java.util.HashSet;
import java.util.Set;

/**
 * Notifies listeners about Fluid analysis events.
 */
public final class NotificationHub {

  /**
   * The subscriber list.
   */
  static private final Set<IAnalysisListener> m_subscribers = new HashSet<IAnalysisListener>();

  /**
   * Flags if analysis has ever run on any project.
   */
  static private boolean m_hasAnalysisEverRun = false;

  /**
   * Subscribes an object for analysis notifications.
   * 
   * @param listener
   *          the object to notify
   */
  public static synchronized void addAnalysisListener(IAnalysisListener listener) {
    if (listener != null) {
      m_subscribers.add(listener);
      /*
       * let the new subscriber know that we have analysis results ready to see
       */
      if (m_hasAnalysisEverRun) listener.analysisCompleted();
    }
  }

  /**
   * Unsubscribes an object for analysis notifications. Does nothing if the
   * given object is not subscribed.
   * 
   * @param callback
   *          the object to stop notifying
   */
  public static synchronized void unsubscribe(IAnalysisListener callback) {
    if (callback != null) m_subscribers.remove(callback);
  }

  /**
   * Called by Majordomo to notify subscribers that analysis has been completed.
   * 
   * @see Majordomo#build(int, Map, IProgressMonitor)
   */
  public static synchronized void notifyAnalysisCompleted() {
    m_hasAnalysisEverRun = true;
    for (IAnalysisListener subscriber : m_subscribers) {
      subscriber.analysisCompleted();
    }
  }

  /**
   * Called by Majordomo to notify subscribers that analysis has been started.
   * 
   * @see Majordomo#build(int, Map, IProgressMonitor)
   */
  public static synchronized void notifyAnalysisStarting() {
    for (IAnalysisListener subscriber : m_subscribers) {
      subscriber.analysisStarting();
    }
  }

  /**
   * Called by Majordomo to notify subscribers that analysis has been postponed
   * until all compilation errors are fixed.
   * 
   * @see Majordomo#build(int, Map, IProgressMonitor)
   */
  public static synchronized void notifyAnalysisPostponed() {
    for (IAnalysisListener subscriber : m_subscribers) {
      subscriber.analysisPostponed();
    }
  }
}