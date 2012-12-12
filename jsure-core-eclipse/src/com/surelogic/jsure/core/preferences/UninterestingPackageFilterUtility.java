package com.surelogic.jsure.core.preferences;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.Utility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.irfree.IDropFilter;

import edu.cmu.cs.fluid.ide.IDEPreferences;

@Utility
public final class UninterestingPackageFilterUtility {

  /**
   * The default filters: a series of regular expressions separated by newlines
   * ('\n').
   */
  public static final List<String> DEFAULT = Arrays.asList("com\\.apple.*", "com\\.oracle.*", "com\\.sun.*", ".*\\.internal.*",
      "apple.*", "oracle.*", "org\\.junit.*", "junit\\.framework.*", "quicktime.*", "sun.*");

  public static final AtomicReference<List<String>> CACHE = new AtomicReference<List<String>>();

  public static void setPreference(final List<String> value, boolean notifyObservers) {
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));
    EclipseUtility.setStringListPreference(IDEPreferences.UNINTERESTING_PACKAGE_FILTERS, value);
    updateCache();
    if (notifyObservers)
      notifyObservers();
  }

  public static List<String> getPreference() {
    return EclipseUtility.getStringListPreference(IDEPreferences.UNINTERESTING_PACKAGE_FILTERS);
  }

  private static void updateCache() {
    CACHE.set(getPreference());
  }

  private static List<String> getCache() {
    List<String> result = CACHE.get();
    if (result == null)
      updateCache();
    result = CACHE.get(); // try again
    return result;
  }

  /**
   * Judges if the passed drop should be kept because it is <i>not</i> in an
   * interesting package.
   * 
   * @param drop
   *          a drop.
   * @return {@link true} if the drop should be kept, {@code false} otherwise.
   */
  public static boolean keep(final IDrop drop) {
    if (drop == null)
      return false;
    return keep(drop.getJavaRef());
  }

  /**
   * Judges if the passed location it is <i>not</i> in an interesting package.
   * 
   * @param javaRef
   *          a code location.
   * @return {@link true} if the location should be kept, {@code false}
   *         otherwise.
   */
  public static boolean keep(final IJavaRef javaRef) {
    if (javaRef == null)
      return false;
    final String name = javaRef.getTypeNameFullyQualified();
    List<String> filters = getCache();
    for (String regex : filters) {
      if (name.matches(regex))
        return false; // filter this resource out
    }
    return true; // show this resource
  }

  /**
   * Filters drops based upon the saved set of regular expressions that identify
   * uninteresting packages defined in {@link UninterestingPackageFilterUtility}
   */
  public static IDropFilter UNINTERESTING_PACKAGE_FILTER = new IDropFilter() {
    @Override
    public boolean keep(IDrop d) {
      return UninterestingPackageFilterUtility.keep(d);
    }
  };

  /**
   * Observers of changes to the filter.
   */
  private static final CopyOnWriteArrayList<IUninterestingPackageFilterObserver> f_observers = new CopyOnWriteArrayList<IUninterestingPackageFilterObserver>();

  /**
   * Appends the specified element to the end of the list of observers.
   * 
   * @param observer
   *          element to be appended to the list of observers.
   */
  public static void registerObserver(IUninterestingPackageFilterObserver observer) {
    f_observers.add(observer);
  }

  /**
   * Removes the first occurrence of the specified observer from the list of
   * observers, if it is present. If this list does not contain the element, it
   * is unchanged.
   * 
   * @param observer
   *          element to be removed from the list of observers, if present.
   * 
   * @return {@code true} if this list contained the specified element.
   */
  public static boolean unregisterObserver(IUninterestingPackageFilterObserver observer) {
    return f_observers.remove(observer);
  }

  /**
   * Notifies all registered observers of a change to the filters managed by
   * this utility.
   */
  public static void notifyObservers() {
    for (IUninterestingPackageFilterObserver o : f_observers)
      o.uninterestingPackageFilterChanged();
  }
}
