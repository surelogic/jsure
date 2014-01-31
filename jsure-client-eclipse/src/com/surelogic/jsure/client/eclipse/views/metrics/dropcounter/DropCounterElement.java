package com.surelogic.jsure.client.eclipse.views.metrics.dropcounter;

import java.util.Comparator;

import com.surelogic.common.SLUtility;

public final class DropCounterElement {

  /**
   * An empty array of {@link DropCounterElement} objects.
   */
  public static final DropCounterElement[] EMPTY = new DropCounterElement[0];

  /**
   * Compares elements by their name.
   */
  public static final Comparator<DropCounterElement> ALPHA = new Comparator<DropCounterElement>() {
    @Override
    public int compare(DropCounterElement o1, DropCounterElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      return o1.dropTypeName.compareTo(o2.dropTypeName);
    }
  };

  /**
   * Compares elements by their counts.
   */
  public static final Comparator<DropCounterElement> METRIC = new Comparator<DropCounterElement>() {
    @Override
    public int compare(DropCounterElement o1, DropCounterElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      final long o1MetricValue = o1.dropCount;
      final long o2MetricValue = o2.dropCount;
      return SLUtility.safeLongToInt(o2MetricValue - o1MetricValue);
    }
  };

  String dropTypeName;

  long dropCount;
}
