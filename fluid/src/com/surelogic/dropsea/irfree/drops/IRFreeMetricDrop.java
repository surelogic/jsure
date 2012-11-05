package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.METRIC;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IMetricDrop;

public final class IRFreeMetricDrop extends IRFreeDrop implements IMetricDrop {

  IRFreeMetricDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    f_metric = e.getAttribute(METRIC);
    if (f_metric == null)
      throw new IllegalArgumentException(I18N.err(291, getMessage(), getJavaRef()));
  }

  @NonNull
  private final String f_metric;

  @NonNull
  public String getMetric() {
    return f_metric;
  }
}
