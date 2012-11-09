package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.METRIC;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.METRIC_INFO;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.irfree.Entity;

public final class IRFreeMetricDrop extends IRFreeDrop implements IMetricDrop {

  @NonNull
  private final Metric f_metric;

  @NonNull
  private final List<IKeyValue> f_metricInfos;

  IRFreeMetricDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    String metricString = e.getAttribute(METRIC);
    try {
      f_metric = Metric.valueOf(metricString);
    } catch (Exception badEnum) {
      throw new IllegalArgumentException(I18N.err(291, metricString, getMessage(), getJavaRef()));
    }

    String diffInfoString = e.getAttribute(METRIC_INFO);
    if (diffInfoString != null) {
      f_metricInfos = KeyValueUtility.parseListEncodedForPersistence(diffInfoString);
    } else {
      f_metricInfos = new ArrayList<IKeyValue>();
    }
  }

  @NonNull
  public Metric getMetric() {
    return f_metric;
  }

  public boolean containsMetricInfoKey(String key) {
    for (IKeyValue di : f_metricInfos)
      if (di.getKey().equals(key))
        return true;
    return false;
  }

  public String getMetricInfoOrNull(String key) {
    for (IKeyValue di : f_metricInfos)
      if (di.getKey().equals(key))
        return di.getValueAsString();
    return null;
  }

  public long getMetricInfoAsLong(String key, long valueIfNotRepresentable) {
    for (IKeyValue di : f_metricInfos)
      if (di.getKey().equals(key))
        return di.getValueAsLong(valueIfNotRepresentable);
    return valueIfNotRepresentable;
  }

  public int getMetricInfoAsInt(String key, int valueIfNotRepresentable) {
    for (IKeyValue di : f_metricInfos)
      if (di.getKey().equals(key))
        return di.getValueAsInt(valueIfNotRepresentable);
    return valueIfNotRepresentable;
  }

  public <T extends Enum<T>> T getMetricInfoAsEnum(String key, T valueIfNotRepresentable, Class<T> elementType) {
    for (IKeyValue di : f_metricInfos)
      if (di.getKey().equals(key))
        return di.getValueAsEnum(valueIfNotRepresentable, elementType);
    return valueIfNotRepresentable;
  }

}
