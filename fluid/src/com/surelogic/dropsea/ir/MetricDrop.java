package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.METRIC;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.METRIC_DROP;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.METRIC_INFO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.UniqueInRegion;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.Entities;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.irfree.XmlCreator;

import edu.cmu.cs.fluid.ir.IRNode;

public final class MetricDrop extends Drop implements IMetricDrop {

  /**
   * Constructs a new metric drop.
   * 
   * @param node
   *          what this metric is about in the fAST.
   * @param metric
   *          the metric this drop of information is about.
   */
  public MetricDrop(IRNode node, Metric metric) {
    super(node);
    if (metric == null)
      throw new IllegalArgumentException(I18N.err(44, "metric"));
    f_metric = metric;
  }

  /**
   * Flags if this result indicates consistency with code.
   */
  private final Metric f_metric;

  @Override
  @NonNull
  public Metric getMetric() {
    return f_metric;
  }

  /**
   * Holds the set of metric-info values for this drop.
   */
  @UniqueInRegion("DropState")
  private final List<IKeyValue> f_metricInfos = new ArrayList<IKeyValue>();

  @Override
  public boolean containsMetricInfoKey(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return true;
    }
    return false;
  }

  @Override
  public String getMetricInfoOrNull(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsString();
    }
    return null;
  }

  @Override
  public long getMetricInfoAsLong(String key, long valueIfNotRepresentable) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsLong(valueIfNotRepresentable);
    }
    return valueIfNotRepresentable;
  }

  @Override
  public int getMetricInfoAsInt(String key, int valueIfNotRepresentable) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsInt(valueIfNotRepresentable);
    }
    return valueIfNotRepresentable;
  }

  @Override
  public <T extends Enum<T>> T getMetricInfoAsEnum(String key, T valueIfNotRepresentable, Class<T> elementType) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsEnum(valueIfNotRepresentable, elementType);
    }
    return valueIfNotRepresentable;
  }

  @Override
  public IJavaRef getMetricAsJavaRefOrThrow(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsJavaRefOrThrow();
    }
    throw new IllegalArgumentException("no value for " + key);
  }

  @Override
  public IJavaRef getMetricAsJavaRefOrNull(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsJavaRefOrNull();
    }
    return null;
  }

  @Override
  public IDecl getMetricAsDeclOrThrow(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsDeclOrThrow();
    }
    throw new IllegalArgumentException("no value for " + key);
  }

  @Override
  public IDecl getMetricAsDeclOrNull(String key) {
    synchronized (f_seaLock) {
      for (IKeyValue di : f_metricInfos)
        if (di.getKey().equals(key))
          return di.getValueAsDeclOrNull();
    }
    return null;
  }

  /**
   * Adds a new diff-info value, or replaces an existing one with the same
   * {@link IKeyValue#getKey()} value.
   * <p>
   * To construct the {@link IKeyValue} instance to pass to this method, please
   * use one of:
   * <ul>
   * <li>{@link KeyValueUtility#getStringInstance(String, String)}</li>
   * <li>{@link KeyValueUtility#getIntInstance(String, int)}</li>
   * <li>{@link KeyValueUtility#getLongInstance(String, long)}</li>
   * <li>{@link KeyValueUtility#getEnumInstance(String, Enum)}</li>
   * </ul>
   * 
   * @param value
   *          a metric-info value.
   * 
   * @throws IllegalArgumentException
   *           if value is null.
   */
  public void addOrReplaceMetricInfo(@NonNull final IKeyValue value) {
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));
    synchronized (f_seaLock) {
      for (Iterator<IKeyValue> iterator = f_metricInfos.iterator(); iterator.hasNext();) {
        final IKeyValue existing = iterator.next();
        if (existing.getKey().equals(value.getKey()))
          iterator.remove();
      }
      f_metricInfos.add(value);
    }
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return METRIC_DROP;
  }

  @Override
  @RequiresLock("SeaLock")
  public void snapshotAttrs(XmlCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(METRIC, getMetric().name());

    /*
     * Output metric information
     * 
     * We want this to encode whitespace in the strings so we use a special
     * Entities instance for that purpose. Should be all on one line.
     */
    if (!f_metricInfos.isEmpty())
      s.addAttribute(METRIC_INFO, KeyValueUtility.encodeListForPersistence(f_metricInfos), Entities.Holder.DEFAULT_PLUS_WHITESPACE);
  }
}
