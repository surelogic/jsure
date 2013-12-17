package com.surelogic.dropsea;

import java.util.*;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.javac.persistence.JSureScanInfo;

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
    private final HashMap<IDrop, IDrop> f_newSameAsOld = new HashMap<IDrop, IDrop>();
    private final HashMap<IDrop, IDrop> f_newChangedFromOld = new HashMap<IDrop, IDrop>();
    private final Set<IDrop> f_new = new HashSet<IDrop>();
    
    public void addAllNewSameAsOld(Map<IDrop, IDrop> matching) {
      f_newSameAsOld.putAll(matching);
    }

    public void addNewChangedFromOld(IDrop n, IDrop o) {
      f_newChangedFromOld.put(n, o);
    }

	public void addNew(IDrop drop) {
		if (drop != null) {
			f_new.add(drop);
		}
	}
    
    public ScanDifferences build() {
      return new ScanDifferences(f_newSameAsOld, f_newChangedFromOld, f_new);
    }


  }

  private final HashMap<IDrop, IDrop> f_newSameAsOld;
  private final HashMap<IDrop, IDrop> f_newChangedFromOld;
  private final Set<IDrop> f_new;
  
  ScanDifferences(Map<IDrop, IDrop> newSameAsOld, Map<IDrop, IDrop> newChangedFromOld, Set<IDrop> newSet) {
    f_newSameAsOld = new HashMap<IDrop, IDrop>(newSameAsOld);
    f_newChangedFromOld = new HashMap<IDrop, IDrop>(newChangedFromOld);
    f_new = new HashSet<IDrop>(newSet);
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
   * @param newScan
   *          the new scan.
   * @return a set of drops from the new scan that are only in the new scan. The
   *         returned set is a copy and may be freely mutated.
   */
  @NonNull
  public HashSet<IDrop> getDropsOnlyInNewScan(@NonNull final JSureScanInfo newScan) {
    if (newScan == null)
      throw new IllegalArgumentException(I18N.err(44, "newScan"));
    final HashSet<IDrop> result = new HashSet<IDrop>(newScan.getDropInfo());
    for (Iterator<IDrop> iterator = result.iterator(); iterator.hasNext();) {
      final IDrop inNewScan = iterator.next();
      if (isNotInOldScan(inNewScan))
        iterator.remove();
    }
    return result;
  }

  /**
   * Gets the set of drops in the old scan that were not matched in the new
   * scan.
   * 
   * @param oldScan
   *          the old scan.
   * @return a set of drops from the old scan that are only in the old scan. The
   *         returned set is a copy and may be freely mutated.
   */
  @NonNull
  public HashSet<IDrop> getDropsOnlyInOldScan(@NonNull final JSureScanInfo oldScan) {
    if (oldScan == null)
      throw new IllegalArgumentException(I18N.err(44, "oldScan"));
    final HashSet<IDrop> result = new HashSet<IDrop>(oldScan.getDropInfo());
    for (Iterator<IDrop> iterator = result.iterator(); iterator.hasNext();) {
      final IDrop inOldScan = iterator.next();
      if (isNotInNewScan(inOldScan))
        iterator.remove();
    }
    return result;
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
    if (inOldScan == null)
      throw new IllegalArgumentException(I18N.err(44, "inOldScan"));
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
  public <T extends IDrop> T getSameInOldScan(@NonNull T inNewScan) {
    if (inNewScan == null)
      throw new IllegalArgumentException(I18N.err(44, "inNewScan"));
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
  public boolean isChangedButInBothScans(@NonNull IDrop drop) {
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
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
  public boolean isNotInNewScan(@NonNull IDrop inOldScan) {
    if (inOldScan == null)
      throw new IllegalArgumentException(I18N.err(44, "inOldScan"));
    final boolean inNewToo = f_newChangedFromOld.containsValue(inOldScan) || f_newSameAsOld.containsValue(inOldScan);
    return !inNewToo;

  }

  /**
   * Checks if the passed drop from the new scan was not matched in the old
   * scan.
   * 
   * @param inNewScan
   *          a drop from the new scan.
   * @return {@code true} if <tt>inNewScan</tt> was not in the old scan.
   */
  public boolean isNotInOldScan(@NonNull IDrop inNewScan) {
    if (inNewScan == null)
      throw new IllegalArgumentException(I18N.err(44, "inNewScan"));
    //final boolean inOldToo = f_newChangedFromOld.containsKey(inNewScan) || f_newSameAsOld.containsKey(inNewScan);
    //return !inOldToo;
    return f_new.contains(inNewScan);
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
  public boolean isSameInBothScans(@NonNull IDrop drop) {
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    final boolean newDropSame = f_newSameAsOld.containsKey(drop);
    if (newDropSame)
      return true;
    for (Map.Entry<IDrop, IDrop> entry : f_newSameAsOld.entrySet()) {
      if (entry.getValue().equals(drop))
        return true;
    }
    return false;
  }

  @Nullable
  public static String getMessageAboutWhatChanged(IDrop oldDrop, IDrop newDrop) {
    if (newDrop == null || oldDrop == null || newDrop == oldDrop)
      return null;
    final StringBuilder b = new StringBuilder();
    if (oldDrop instanceof IProofDrop && newDrop instanceof IProofDrop) {
      final IProofDrop oldProofDrop = (IProofDrop) oldDrop;
      final IProofDrop newProofDrop = (IProofDrop) newDrop;
      if (newProofDrop.provedConsistent() && !oldProofDrop.provedConsistent())
        b.append("consistent with code, ");
      else if (!newProofDrop.provedConsistent() && oldProofDrop.provedConsistent())
        b.append("not consistent with code, ");
      if (newProofDrop.proofUsesRedDot() && !oldProofDrop.proofUsesRedDot())
        b.append("contingent (red-dot), ");
      else if (!newProofDrop.proofUsesRedDot() && oldProofDrop.proofUsesRedDot())
        b.append("not contingent (no red-dot), ");

      if (oldProofDrop instanceof IResultDrop && newProofDrop instanceof IResultDrop) {
        final IResultDrop oldResultDrop = (IResultDrop) oldProofDrop;
        final IResultDrop newResultDrop = (IResultDrop) newProofDrop;
        if (newResultDrop.isConsistent() && !oldResultDrop.isConsistent())
          b.append("consistent analysis result, ");
        else if (!newResultDrop.isConsistent() && oldResultDrop.isConsistent())
          b.append("inconsistent analysis result, ");
        if (newResultDrop.isTimeout() && !oldResultDrop.isTimeout())
          b.append("analysis execution timed out, ");
        else if (!newResultDrop.isTimeout() && oldResultDrop.isTimeout())
          b.append("analysis execution did not time out, ");
        if (newResultDrop.isVouched() && !oldResultDrop.isVouched())
          b.append("programmer vouch, ");
        else if (!newResultDrop.isVouched() && oldResultDrop.isVouched())
          b.append("no programmer vouch, ");
      }
      if (b.length() > 0)
        // remove last ", "
        return b.delete(b.length() - 2, b.length()).toString();
    }
    return null;
  }
}
