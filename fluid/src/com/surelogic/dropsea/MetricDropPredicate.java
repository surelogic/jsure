package com.surelogic.dropsea;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.ir.DropPredicate;

/**
 * Used to obtain only the metric drops of a particular metric type.
 */
public final class MetricDropPredicate implements DropPredicate {

  @NonNull
  final IMetricDrop.Metric f_metric;

  public MetricDropPredicate(IMetricDrop.Metric metric) {
    if (metric == null)
      throw new IllegalArgumentException(I18N.err(44, "metric"));
    f_metric = metric;
  }

  @Override
  public boolean match(IDrop d) {
    if (d instanceof IMetricDrop) {
      final IMetricDrop md = (IMetricDrop) d;
      if (md.getMetric() == f_metric)
        return true;
    }
    return false;
  }
}
