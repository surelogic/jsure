package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.METRIC;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.METRIC_DROP;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IMetricDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public final class MetricDrop extends Drop implements IMetricDrop {

  /**
   * Constructs a new metric drop.
   * 
   * @param node
   *          what this metric is about in the fAST.
   * @param metric
   *          the metric this drop of information is about. Should be a constant
   *          in {@link IMetricDrop}, such as
   *          {@link IMetricDrop#SAFE_STATE_WRT_CONCURRENCY}.
   */
  public MetricDrop(IRNode node, String metric) {
    super(node);
    if (metric == null)
      throw new IllegalArgumentException(I18N.err(44, "metric"));
    f_metric = metric;
  }

  /**
   * Flags if this result indicates consistency with code.
   */
  private final String f_metric;

  @NonNull
  public String getMetric() {
    return f_metric;
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return METRIC_DROP;
  }

  @Override
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(METRIC, getMetric());
  }
}
