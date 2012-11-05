package com.surelogic.dropsea;

import com.surelogic.NonNull;

public interface IMetricDrop extends IDrop, ISnapshotDrop {

  /**
   * The metrics supported by JSure.
   * <p>
   * This list can be added too, however these names are used for persistence so
   * do not change the names without updating persistence for backwards
   * compatibility.
   */
  public enum Metric {
    STATE_WRT_CONCURRENCY
  }

  /**
   * Gets the metric this drop of information contributes too.
   * <p>
   * This must be non-{@code null} because, by definition, a metric drop must
   * contribute information about some kind metric.
   * 
   * @return the metric this drop of information contributes too.
   */
  @NonNull
  Metric getMetric();

  /**
   * Checks if this drop has a mapping to a metric-info value for the passed
   * key.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @return {@code true} if a value exists for <tt>key</tt>, {@code false}
   *         otherwise.
   */
  boolean containsMetricInfoKey(String key);

  /**
   * Returns the metric-info value to which the specified key is mapped, or
   * {@code null} if this drop contains no mapping for the key. This method will
   * return a string version of the metric-info value for any mapping&mdash;even
   * if the value is an enum, an int, or a long.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @return the metric-info value to which the specified key is mapped, or
   *         {@code null} if this drop contains no mapping for the key.
   */
  String getMetricInfoOrNull(String key);

  /**
   * Returns the metric-info value to which the specified key is mapped as a
   * long, or <tt>valueIfNotRepresentable</tt> if this drop contains no mapping
   * for the key or the metric-info value cannot be represented as a long.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @param valueIfNotRepresentable
   *          a value.
   * @return the metric-info value to which the specified key is mapped as a
   *         long, or <tt>valueIfNotFound</tt> if this drop contains no mapping
   *         for the key or the metric-info value cannot be represented as a
   *         long.
   */
  long getMetricInfoAsLong(String key, long valueIfNotRepresentable);

  /**
   * Returns the metric-info value to which the specified key is mapped as an
   * int, or <tt>valueIfNotRepresentable</tt> if this drop contains no mapping
   * for the key or the metric-info value cannot be represented as an int. For
   * example, if the metric-info value was set as a long then this method will
   * return its value if it fits into an int, or return <tt>valueIfNotFound</tt>
   * if it doesn't.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @param valueIfNotRepresentable
   *          a value.
   * @return the metric-info value to which the specified key is mapped as an
   *         int, or <tt>valueIfNotFound</tt> if this drop contains no mapping
   *         for the key or the metric-info value cannot be represented as an
   *         int.
   */
  int getMetricInfoAsInt(String key, int valueIfNotRepresentable);

  /**
   * Returns the metric-info value to which the specified key is mapped as an
   * enum, or <tt>valueIfNotRepresentable</tt> if this drop contains no mapping
   * for the key or the metric-info value cannot be represented as an enum.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @param valueIfNotRepresentable
   *          a value.
   * @param elementType
   *          the class object of the element type for the resulting enum.
   * @return the metric-info value to which the specified key is mapped as an
   *         enum, or <tt>valueIfNotFound</tt> if this drop contains no mapping
   *         for the key or the metric-info value cannot be represented as an
   *         enum.
   */
  <T extends Enum<T>> T getMetricInfoAsEnum(String key, T valueIfNotRepresentable, Class<T> elementType);

}
