package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

/**
 * Cached for UI use.
 * <p>
 * Should only be used within the SWT thread.
 */
public final class ScanTimeOptions {

  int f_threshold;

  /**
   * Sets the metric threshold value.
   * 
   * @param value
   *          a metric threshold value.
   * @return {@code true} if the value changed, {@code false} if it was already
   *         set to this value.
   */
  public boolean setThreshold(int value) {
    if (f_threshold != value) {
      f_threshold = value;
      return true;
    } else
      return false;
  }

  /**
   * Gets the metric threshold value.
   * 
   * @return the metric threshold value.
   */
  public int getThreshold() {
    return f_threshold;
  }

  int f_selectedColumnTitleIndex = 0;

  /**
   * Sets the column to sort and threshold filter on. This is an index into the
   * {@link SlocMetricMediator#f_columnTitles} array.
   * 
   * @param value
   *          the column to sort and threshold filter on.
   * @return {@code true} if the value changed, {@code false} if it was already
   *         set to this value.
   */
  public boolean setSelectedColumnTitleIndex(int value) {
    if (f_selectedColumnTitleIndex != value) {
      f_selectedColumnTitleIndex = value;
      return true;
    } else
      return false;
  }

  /**
   * Gets the column to sort and threshold filter on. This is an index into the
   * {@link SlocMetricMediator#f_columnTitles} array.
   * 
   * @return the column to sort and threshold filter on.
   */
  public int getSelectedColumnTitleIndex() {
    return f_selectedColumnTitleIndex;
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
}
