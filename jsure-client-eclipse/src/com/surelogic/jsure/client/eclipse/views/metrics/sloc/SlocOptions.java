package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

/**
 * Cached for UI use.
 * <p>
 * Should only be used within the SWT thread.
 */
public final class SlocOptions {

  int f_threshold;

  public void setThreshold(int threshold) {
    f_threshold = threshold;
  }

  public int getThreshold() {
    return f_threshold;
  }

}
