package com.surelogic.dropsea;

import com.surelogic.NonNull;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;

public interface IMetricDrop extends IDrop, ISnapshotDrop {
  boolean INCLUDE_IN_DIFF = false;
	  
  /**
   * The metrics supported by JSure.
   * <p>
   * This list can be added too, however these names are used for persistence so
   * do not change the names without updating persistence for backwards
   * compatibility.
   */
  public enum Metric {
    SCAN_TIME, SLOC, STATE_WRT_CONCURRENCY
  }

  /*
   * SCAN_TIME
   */

  /**
   * The name of the analysis this scan time report is about. For example
   * <i>Lock policy</i> or <i>Static structure</i>.
   * <p>
   * Values are always <tt>String</tt>.
   */
  final String SCAN_TIME_ANALYSIS_NAME = "scan-time-analysis-name";

  /**
   * The duration in nanoseconds of the portion of the scan.
   * <p>
   * Values are always <tt>long</tt>.
   */
  final String SCAN_TIME_DURATION_NS = "scan-time-duration-ns";

  /*
   * SLOC
   */

  /**
   * A count of blank lines in the compilation unit.
   */
  final String SLOC_BLANK_LINE_COUNT = "sloc-blank-line-count";
  /**
   * A count of the lines in the compilation unit that are entirely or partially
   * a comment.
   */
  final String SLOC_CONTAINS_COMMENT_LINE_COUNT = "sloc-contains-comment-line-count";
  /**
   * A count of the number of Java declarations, at any scope level, in the
   * compilation unit.
   */
  final String SLOC_JAVA_DECLARATION_COUNT = "sloc-java-declaration-count";
  /**
   * A count of the number of Java statements, at any scope level, in the
   * compilation unit.
   */
  final String SLOC_JAVA_STATEMENT_COUNT = "sloc-java-statement-count";
  /**
   * A count of the number of lines in the compilation unit.
   */
  final String SLOC_LINE_COUNT = "sloc-line-count";
  /**
   * A count of the number of ";" which appear within the compilation unit.
   */
  final String SLOC_SEMICOLON_COUNT = "sloc-semicolon-count";

  /*
   * STATE_WRT_CONCURRENCY
   */

  /**
   * A count of @Immutable-typed fields in the type
   */
  final String CONCURR_IMMUTABLE_COUNT = "concurr-immutable-count";
  /**
   * A count of @ThreadSafe-typed fields in the type
   */
  final String CONCURR_THREADSAFE_COUNT = "concurr-threadsafe-count";
  /**
   * A count of @NotThreadSafe-typed fields in the type
   */
  final String CONCURR_NOTTHREADSAFE_COUNT = "concurr-notthreadsafe-count";
  /**
   * A count of lock-protected fields in the type
   */
  final String CONCURR_LOCK_PROTECTED_COUNT = "concurr-lock-protected-count";
  /**
   * A count of thread-confined fields in the type
   */
  final String CONCURR_THREAD_CONFINED_COUNT = "concurr-thread-confined-count";
  /**
   * A count of fields not covered above in the type
   */
  final String CONCURR_OTHER_COUNT = "concurr-other-count";

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

  /**
   * Returns the metric-info value to which the specified key is mapped as a
   * {@link IJavaRef}, or throws IllegalArgumentException if this drop contains
   * no mapping for the key or the metric-info value cannot be represented as a
   * {@link IJavaRef}.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @return the metric-info value to which the specified key is mapped as a
   *         {@link IJavaRef}.
   * @throws IllegalArgumentException
   *           if this drop contains no mapping for the key or the metric-info
   *           value cannot be represented as a {@link IJavaRef}.
   */
  IJavaRef getMetricAsJavaRefOrThrow(String key);

  /**
   * Returns the metric-info value to which the specified key is mapped as a
   * {@link IJavaRef}, or {code null} if this drop contains no mapping for the
   * key or the metric-info value cannot be represented as a {@link IJavaRef}.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @return the metric-info value to which the specified key is mapped as a
   *         {@link IJavaRef}, or {code null} if this drop contains no mapping
   *         for the key or the metric-info value cannot be represented as a
   *         {@link IJavaRef}.
   */
  IJavaRef getMetricAsJavaRefOrNull(String key);

  /**
   * Returns the metric-info value to which the specified key is mapped as a
   * {@link IDecl}, or throws IllegalArgumentException if this drop contains no
   * mapping for the key or the metric-info value cannot be represented as a
   * {@link IDecl}.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @return the metric-info value to which the specified key is mapped as a
   *         {@link IDecl}.
   * @throws IllegalArgumentException
   *           if this drop contains no mapping for the key or the metric-info
   *           value cannot be represented as a {@link IDecl}.
   */
  IDecl getMetricAsDeclOrThrow(String key);

  /**
   * Returns the metric-info value to which the specified key is mapped as a
   * {@link IDecl}, or {code null} if this drop contains no mapping for the key
   * or the metric-info value cannot be represented as a {@link IDecl}.
   * <p>
   * Metric-info values are used to store values, with the overall purpose of
   * creating a metric about a scan.
   * 
   * @param key
   *          a key.
   * @return the metric-info value to which the specified key is mapped as a
   *         {@link IDecl}, or {code null} if this drop contains no mapping for
   *         the key or the metric-info value cannot be represented as a
   *         {@link IDecl}.
   */
  IDecl getMetricAsDeclOrNull(String key);
}
