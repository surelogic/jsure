package com.surelogic.dropsea;

import java.util.*;

import com.surelogic.NonNull;
import com.surelogic.Nullable;

/**
 * Records differences between two drop-sea instances that can be queried. This
 * class does not compute the actual differences, it records the results of that
 * computation for use by the user interface, tests, etc.
 * <p>
 * The results represent the differences between two JSure scans: a <b>new
 * scan</b> and an <b>old scan</b>. The two scans should be of the same code,
 * however, that is not mandated by this class.
 */
public final class ScanDifferences {
  public static class Builder {
    private final HashSet<IDrop> f_inNewOnly = new HashSet<IDrop>();
    private final HashMap<IDrop, IDrop> f_newSameAsOld = new HashMap<IDrop, IDrop>();
    private final HashMap<IDrop, IDrop> f_newChangedFromOld = new HashMap<IDrop, IDrop>();
    private final HashSet<IDrop> f_inOldOnly = new HashSet<IDrop>();

    public void addAsNew(IDrop d) {
      f_inNewOnly.add(d);
    }

    public void addAsOld(IDrop d) {
      f_inOldOnly.add(d);
    }

    public void addAllNewSameAsOld(Map<IDrop, IDrop> matching) {
      f_newSameAsOld.putAll(matching);
    }

    public void addNewChangedFromOld(IDrop n, IDrop o) {
      f_newChangedFromOld.put(n, o);
    }

    public ScanDifferences build() {
      return new ScanDifferences(f_inNewOnly, f_newSameAsOld, f_newChangedFromOld, f_inOldOnly);
    }
  }

  private final HashSet<IDrop> f_inNewOnly;
  private final HashMap<IDrop, IDrop> f_newSameAsOld;
  private final HashMap<IDrop, IDrop> f_newChangedFromOld;
  private final HashSet<IDrop> f_inOldOnly;

  private ScanDifferences(Set<IDrop> inNewOnly, Map<IDrop, IDrop> newSameAsOld, Map<IDrop, IDrop> newChangedFromOld,
      Set<IDrop> inOldOnly) {
    f_inNewOnly = new HashSet<IDrop>(inNewOnly);
    f_newSameAsOld = new HashMap<IDrop, IDrop>(newSameAsOld);
    f_newChangedFromOld = new HashMap<IDrop, IDrop>(newChangedFromOld);
    f_inOldOnly = new HashSet<IDrop>(inOldOnly);
  }

  /**
   * Checks if the passed drop from the old scan was matched in the new scan,
   * but has changed in some manner, and returns the matching drop from the new
   * scan, if possible.
   * <p>
   * <i>Implementation Note:</i> This method is less efficient than
   * {@link #getChangedInOldScan(IDrop)}&mdash;try to use that method if
   * possible.
   * 
   * @param inOldScan
   *          a drop from the old scan.
   * @return a drop from the new scan that matched <tt>inOldScan</tt>, or
   *         {@code null} if <tt>inOldScan</tt> was not matched.
   */
  public <T extends IDrop> T getChangedInNewScan(T inOldScan) {
    for (Map.Entry<IDrop, IDrop> entry : f_newChangedFromOld.entrySet()) {
      if (entry.getValue().equals(inOldScan)) {
        @SuppressWarnings("unchecked")
        final T result = (T) entry.getKey();
        return result;
      }
    }
    return null;
  }

  /**
   * Checks if the passed drop from the new scan was matched in the old scan,
   * but has changed in some manner, and returns the matching drop from the old
   * scan, if possible.
   * 
   * @param inNewScan
   *          a drop from the new scan.
   * @return a drop from the old scan that matched <tt>inNewScan</tt>, or
   *         {@code null} if <tt>inNewScan</tt> was not matched.
   */
  @Nullable
  public <T extends IDrop> T getChangedInOldScan(T inNewScan) {
    @SuppressWarnings("unchecked")
    final T result = (T) f_newChangedFromOld.get(inNewScan);
    return result;
  }

  /**
   * Gets the set of drops in the new scan that were not matched in the new
   * scan.
   * 
   * @return a set of drops from the new scan that are only in the new scan. The
   *         returned set is a copy and may be freely mutated.
   */
  @NonNull
  public HashSet<IDrop> getDropsOnlyInNewScan() {
    return new HashSet<IDrop>(f_inNewOnly);
  }

  /**
   * Gets the set of drops in the old scan that were not matched in the new
   * scan.
   * 
   * @return a set of drops from the old scan that are only in the old scan. The
   *         returned set is a copy and may be freely mutated.
   */
  @NonNull
  public HashSet<IDrop> getDropsOnlyInOldScan() {
    return new HashSet<IDrop>(f_inOldOnly);
  }

  /**
   * Checks if the passed drop from the old scan was the same as a drop in the
   * new scan and returns the matching drop from the new scan, if possible.
   * <p>
   * <i>Implementation Note:</i> This method is less efficient than
   * {@link #getSameInOldScan(IDrop)}&mdash;try to use that method if possible.
   * 
   * @param inOldScan
   *          a drop from the old scan.
   * @return a drop from the new scan that is the same as <tt>inOldScan</tt>, or
   *         {@code null} if <tt>inOldScan</tt> was not the same as any drop in
   *         the new scan.
   */
  public <T extends IDrop> T getSameInNewScan(T inOldScan) {
    for (Map.Entry<IDrop, IDrop> entry : f_newSameAsOld.entrySet()) {
      if (entry.getValue().equals(inOldScan)) {
        @SuppressWarnings("unchecked")
        final T result = (T) entry.getKey();
        return result;
      }
    }
    return null;
  }

  /**
   * Checks if the passed drop from the new scan was the same as a drop in the
   * old scan and returns the matching drop from the old scan, if possible.
   * 
   * @param inNewScan
   *          a drop from the new scan.
   * @return a drop from the old scan that is the same as <tt>inNewScan</tt>, or
   *         {@code null} if <tt>inNewScan</tt> was not the same as any drop in
   *         the old scan.
   */
  @Nullable
  public <T extends IDrop> T getSameInOldScan(T inNewScan) {
    @SuppressWarnings("unchecked")
    final T result = (T) f_newSameAsOld.get(inNewScan);
    return result;
  }

  /**
   * Checks if the passed drop from either scan was in both scans, but changed
   * in some manner.
   * 
   * @param drop
   *          a drop from the new scan or the old scan.
   * @return {@code true} if <tt>drop</tt> was in both scans, but changed in
   *         some manner.
   * 
   * @see #getChangedFrom(IDrop)
   * @see #getChangedTo(IDrop)
   */
  public boolean isChangedButInBothScans(IDrop drop) {
    final boolean newDropChanged = f_newChangedFromOld.containsKey(drop);
    if (newDropChanged)
      return true;
    for (Map.Entry<IDrop, IDrop> entry : f_newChangedFromOld.entrySet()) {
      if (entry.getValue().equals(drop))
        return true;
    }
    return false;
  }

  /**
   * Checks if the passed drop from the old scan was not matched in the new
   * scan.
   * 
   * @param inOldScan
   *          a drop from the old scan.
   * @return {@code true} if <tt>inOldScan</tt> is not in the new scan.
   */
  public boolean isNotInNewScan(IDrop inOldScan) {
    return f_inOldOnly.contains(inOldScan);

  }

  /**
   * Checks if the passed drop from the new scan was not matched in the old
   * scan.
   * 
   * @param inNewScan
   *          a drop from the new scan.
   * @return {@code true} if <tt>inNewScan</tt> was not in the old scan.
   */
  public boolean isNotInOldScan(IDrop inNewScan) {
    return f_inNewOnly.contains(inNewScan);
  }

  /**
   * Checks if the passed drop from either scan was the same in both scans.
   * 
   * @param drop
   *          a drop from the new scan or the old scan.
   * @return {@code true} if <tt>drop</tt> was the same in both scans.
   * 
   * @see #getChangedFrom(IDrop)
   * @see #getChangedTo(IDrop)
   */
  public boolean isSameInBothScans(IDrop drop) {
    final boolean newDropSame = f_newSameAsOld.containsKey(drop);
    if (newDropSame)
      return true;
    for (Map.Entry<IDrop, IDrop> entry : f_newSameAsOld.entrySet()) {
      if (entry.getValue().equals(drop))
        return true;
    }
    return false;
  }
}
