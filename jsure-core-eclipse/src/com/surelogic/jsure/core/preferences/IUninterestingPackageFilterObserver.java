package com.surelogic.jsure.core.preferences;

public interface IUninterestingPackageFilterObserver {
  /**
   * Invoked if the filter managed by {@link UninterestingPackageFilterUtility}
   * changed to notify listeners.
   * <p>
   * <b>Warning:</b> the thread context of this call is undefined, do not assume
   * it is a UI thread or any other particular thread.
   */
  void uninterestingPackageFilterChanged();
}
