package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.concurrent.TimeUnit;

import com.surelogic.Nullable;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * Cached for UI use.
 * <p>
 * Should only be used within the SWT thread.
 */
public final class ScanTimeOptions {

  long f_threshold;

  /**
   * Sets the metric threshold value.
   * 
   * @param value
   *          a metric threshold value in milliseconds.
   * @return {@code true} if the value changed, {@code false} if it was already
   *         set to this value.
   */
  public boolean setThreshold(long valueInSeconds) {
    final long valueNS = TimeUnit.MILLISECONDS.toNanos(valueInSeconds);
    if (f_threshold != valueNS) {
      f_threshold = valueNS;
      return true;
    } else
      return false;
  }

  /**
   * Gets the metric threshold value.
   * 
   * @return the metric threshold value.
   */
  public long getThreshold() {
    return f_threshold;
  }

  /**
   * The analysis name to filter the displayed timing result to where
   * {@code null} indicates to show everything. For example,
   * <tt>"Lock policy"</tt>.
   */
  String f_analysisToShow = null;

  /**
   * Sets the analysis name to filter the displayed timing result to where
   * {@code null} indicates to show everything. For example,
   * <tt>"Lock policy"</tt>.
   * <p>
   * This method persists the choice in Eclipse if it actually is different.
   * 
   * @param value
   *          the analysis name to filter the displayed timing result to where
   *          {@code null} indicates to show everything.
   * @return {@code true} if the value changed, {@code false} if it was already
   *         set to this value.
   */
  public boolean setAnalysisToShow(@Nullable String value) {
    if (f_analysisToShow != value) {
      f_analysisToShow = value;
      EclipseUtility.setStringPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_ANALYSIS_TO_SHOW, value == null ? "" : value);
      return true;
    } else
      return false;
  }

  /**
   * Gets the analysis name to filter the displayed timing result to where
   * {@code null} indicates to show everything. For example,
   * <tt>"Lock policy"</tt>.
   * 
   * @return the analysis to filter the displayed timing result to where
   *         {@code null} indicates to show everything.
   */
  @Nullable
  public String getAnalysisToShow() {
    return f_analysisToShow;
  }

  boolean f_thresholdShowAbove = true;

  /**
   * Sets if metric values above the threshold or below the threshold are
   * highlighted.
   * 
   * @param value
   *          {@code true} if metric values at or above the threshold are
   *          highlighted, {@code false} if metric values at or below the
   *          threshold are highlighted.
   * @return {@code true} if the value changed, {@code false} if it was already
   *         set to this value.
   */
  public boolean setThresholdShowAbove(boolean value) {
    if (f_thresholdShowAbove != value) {
      f_thresholdShowAbove = value;
      return true;
    } else
      return false;
  }

  /**
   * Gets if metric values above the threshold or below the threshold are
   * highlighted.
   * 
   * @return {@code true} if metric values at or above the threshold are
   *         highlighted, {@code false} if metric values at or below the
   *         threshold are highlighted.
   */
  public boolean getThresholdShowAbove() {
    return f_thresholdShowAbove;
  }

  boolean f_filterResultsByThreshold = false;

  public boolean setFilterResultsByThreshold(boolean value) {
    if (f_filterResultsByThreshold != value) {
      f_filterResultsByThreshold = value;
      return true;
    } else
      return false;
  }

  public boolean getFilterResultsByThreshold() {
    return f_filterResultsByThreshold;
  }
}
