package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.dropsea.IProofDrop;

/**
 * Abstract base class for all JSure results filters. Intended to be subclassed.
 * <p>
 * This class is thread-safe.
 */
public abstract class Filter {

  /**
   * Gets the factory for this filter.
   * 
   * @return a filter factory object.
   */
  public abstract ISelectionFilterFactory getFactory();

  private final Selection f_selection;

  /**
   * Gets the selection this filter exists within.
   * 
   * @return a selection.
   */
  public Selection getSelection() {
    /*
     * Not mutable so we don't need to hold a lock on this.
     */
    return f_selection;
  }

  /**
   * Indicates if this is the first filter of its selection.
   * 
   * @return <code>true</code> if this filter is the first filter of its
   *         selection, <code>false</code> otherwise.
   */
  public final boolean isFirstFilter() {
    return f_selection.isFirstFilter(this);
  }

  /**
   * Indicates if this is the last filter of its selection.
   * 
   * @return <code>true</code> if this filter is the last filter of its
   *         selection, <code>false</code> otherwise.
   */
  public final boolean isLastFilter() {
    return f_selection.isLastFilter(this);
  }

  /**
   * Link to the previous filter, may be <code>null</code>.
   */
  protected final Filter f_previous;

  /**
   * The user interface label or name of this filter.
   */
  private final String f_filterLabel;

  /**
   * Gets the label for this filter.
   * 
   * @return a user interface label.
   */
  public final String getFilterLabel() {
    return f_filterLabel;
  }

  Filter(final Selection selection, final Filter previous, String filterLabel) {
    assert selection != null;
    f_selection = selection;
    f_previous = previous;
    f_filterLabel = filterLabel;
  }

  void dispose() {
    notifyDispose();
    synchronized (this) {
      f_observers.clear();
    }
  }

  /**
   * Clones this filter without its data (except the filter expression). Only
   * the set of porous values is remembered. {@link #refresh()} must be invoked
   * on the clone before it can be used.
   * 
   * @param selection
   *          the selection the new filter should be within.
   * @param previous
   *          the (new) filter before the new filter.
   * @return a clone of this filter without its data.
   */
  Filter copyNoQuery(Selection selection, Filter previous) {
    // construct a filter of the right type
    final Filter result = getFactory().construct(selection, previous);
    if (!isFilterExpressionClear()) {
      result.setFilterExpression(getFilterExpression());
    }
    result.f_porousValues.addAll(f_porousValues);
    return result;
  }

  /**
   * Counts for just this filter. The key of this map is the definitive list of
   * values for this filter. Only mutated by {@link #refreshCountsFor(List)}.
   */
  protected final Map<String, Integer> f_counts = new HashMap<String, Integer>();

  /**
   * Gets a string value used in the filter for the passed drop. Subclasses
   * should override. If they do not want the drop, they should return null.
   * 
   * @param drop
   *          a proof drop.
   * @return a string value, or null.
   */
  @Nullable
  public abstract String getFilterValueFromDropOrNull(IProofDrop drop);

  /**
   * Gets the count for the passed value for this filter
   * 
   * @param value
   *          a value for this filter.
   * @return the count of results that exist.
   */
  public int getSummaryCountFor(String value) {
    final Integer result = f_counts.get(value);
    return result == null ? 0 : result.intValue();
  }

  /**
   * Set by {@link #queryCounts()}. Only mutated by {@link #refresh()}.
   */
  protected int f_countTotal = 0;

  /**
   * The set of values in alphabetical order.
   */
  protected final LinkedList<String> f_allValues = new LinkedList<String>();

  /**
   * Gets the current list of all possible values for this filter. This set of
   * values defines a set of bins that partition the set of results entering
   * this filter.
   * 
   * @return all possible values for this filter.
   */
  public List<String> getAllValues() {
    synchronized (this) {
      return new LinkedList<String>(f_allValues);
    }
  }

  private static final String NO_FILTER = "none";

  /**
   * A filter expression used to filter the values that are listed by this
   * filter.
   * 
   * @see #filterAllValues()
   */
  private String f_filterExpression = NO_FILTER;

  /**
   * Gets this filter's filter expression.
   * 
   * @return this filter's filter expression.
   */
  public String getFilterExpression() {
    return f_filterExpression;
  }

  /**
   * Sets the filter expression for this filter. This expression is used to
   * filter the values that are listed by this filter.
   * <p>
   * A value of {code ""} (the empty string) cause the filter expression to be
   * cleared.
   * 
   * @param filter
   *          non-null filter string.
   * @return {@code true} if the filter has been changed, {@code false}
   *         otherwise.
   * 
   * @throws IllegalArgumentException
   *           if filter is {@code null}.
   */
  public boolean setFilterExpression(String filter) {
    if (filter == null)
      throw new IllegalArgumentException("filter must be non-null");
    if ("".equals(filter)) {
      if (!isFilterExpressionClear()) {
        clearFilterExpression();
        return true;
      } else
        return false;
    }
    if (!f_filterExpression.equals(filter)) {
      f_filterExpression = filter;
      return true;
    }
    return false;
  }

  /**
   * Clears the filter expression for this filter.
   */
  public void clearFilterExpression() {
    f_filterExpression = NO_FILTER;
  }

  public boolean isFilterExpressionClear() {
    return f_filterExpression == NO_FILTER;
  }

  /**
   * Gets the complete set of all values for this filter. This set is is
   * filtered by previous filters.
   * 
   * @return a list of all values for this filter.
   */
  public List<String> getValues() {
    synchronized (this) {
      return new LinkedList<String>(f_allValues);
    }
  }

  /**
   * Indicates if this filter has any values. Its return value is equal to, but
   * more efficient than, using the following expression:
   * 
   * <pre>
   * !(getValues().isEmpty())
   * </pre>
   * 
   * @return <code>true</code> if this filter has any values, <code>false</code>
   *         otherwise.
   */
  public boolean hasValues() {
    synchronized (this) {
      return !f_allValues.isEmpty();
    }
  }

  /**
   * Returns the list of all values for this filter ordered, from highest to
   * lowest, by the summary count for that value.
   * 
   * @return a list of all values ordered by summary count.
   */
  public List<String> getValuesOrderedBySummaryCount() {
    final List<String> values = getValues();
    final LinkedList<String> result = new LinkedList<String>();
    int count = 0;
    while (!values.isEmpty()) {
      for (Iterator<String> i = values.iterator(); i.hasNext();) {
        String value = i.next();
        if (getSummaryCountFor(value) < count) {
          result.add(value);
          i.remove();
        }
      }
      count++;
    }
    Collections.reverse(result);
    return result;
  }

  /**
   * Records the set of values allowed through this filter. They would be
   * "checked" in the user interface. It should be an invariant that for all
   * elements <code>e</code> of this set
   * <code>f_summaryCounts.containsKey(e)</code> is true.
   * <p>
   * If this set is mutated other than via a call to {@link #setPorous(String)}
   * then it is important to remember to invoke {@link #notifyPorous()} to let
   * observers know about this mutation.
   */
  protected final Set<String> f_porousValues = new HashSet<String>();

  protected final Set<IFilterObserver> f_observers = new CopyOnWriteArraySet<IFilterObserver>();

  public final void addObserver(IFilterObserver o) {
    if (o == null)
      return;
    /*
     * No lock needed because we are using a util.concurrent collection.
     */
    f_observers.add(o);
  }

  public final void removeObserver(IFilterObserver o) {
    /*
     * No lock needed because we are using a util.concurrent collection.
     */
    f_observers.remove(o);
  }

  /**
   * Do not call this method holding a lock on <code>this</code>. Deadlock could
   * occur as we are invoking an alien method.
   */
  protected void notifyFilterChanged() {
    for (IFilterObserver o : f_observers) {
      o.filterChanged(this);
    }
  }

  /**
   * Do not call this method holding a lock on <code>this</code>. Deadlock could
   * occur as we are invoking an alien method.
   */
  protected void notifyDispose() {
    for (IFilterObserver o : f_observers) {
      o.filterDisposed(this);
    }
  }

  /**
   * Checks if the passed value is porous. It would be "checked" in the user
   * interface.
   * 
   * @param value
   *          a value managed by this filter.
   * @return <code>true</code> if the value is porous, <code>false</code>
   *         otherwise.
   * @throws IllegalArgumentException
   *           if <code>getValues().contains(value)</code> is not true.
   */
  public boolean isPorous(String value) {
    synchronized (this) {
      if (!f_allValues.contains(value))
        throw new IllegalArgumentException("value not filtered by " + this);
      return f_porousValues.contains(value);
    }
  }

  /**
   * Sets the passed value to be porous or non-porous within this filter. A
   * porous value would be "checked" in the user interface.
   * 
   * @param value
   *          a value managed by this filter.
   * @param porous
   *          <code>true</code> sets the value to be porous, <code>false</code>
   *          makes it non-porous.
   * @throws IllegalArgumentException
   *           if <code>getValues().contains(value)</code> is not true.
   */
  public void setPorous(String value, boolean porous) {
    synchronized (this) {
      if (!f_allValues.contains(value))
        throw new IllegalArgumentException("value not filtered by " + this);
      if (porous == isPorous(value))
        return; // not a change
      if (porous)
        f_porousValues.add(value);
      else
        f_porousValues.remove(value);
      refreshPorousDrops(getPreviousPorusDrops());
    }
    notifyFilterChanged();
    /*
     * Tell my enclosing selection to update filters after me because I changed
     * the set of results that I let through.
     */
    f_selection.filterChanged(this);
  }

  /**
   * A helper method when persistent selections are loaded to set what values
   * are porous. This method skips all notifications to obvservers.
   * 
   * @param value
   *          a value managed by this filter.
   * @param porous
   *          <code>true</code> sets the value to be porous, <code>false</code>
   *          makes it non-porous.
   */
  void setPorousOnLoad(String value, boolean porous) {
    synchronized (this) {
      if (porous)
        f_porousValues.add(value);
      else
        f_porousValues.remove(value);
    }
  }

  /**
   * Makes all values in this filter porous.
   * <p>
   * Note that this method removes any values that might exist as porous but are
   * not currently part of this filter. This situation can happen if a previous
   * filter became less porous and a value selected as porous in this filter no
   * longer exists.
   */
  public void setPorousAll() {
    synchronized (this) {
      if (f_porousValues.containsAll(f_allValues)) {
        return;
      }

      f_porousValues.clear();
      f_porousValues.addAll(f_allValues);
      refreshPorousDrops(getPreviousPorusDrops());
    }
    notifyFilterChanged();
    /*
     * Tell my enclosing selection to update filters after me because I changed
     * the set of results that I let through.
     */
    f_selection.filterChanged(this);
  }

  /**
   * Makes no values in this filter porous.
   * <p>
   * Note that this method removes any values that might exist as porous but are
   * not currently part of this filter. This situation can happen if a previous
   * filter became less porous and a value selected as porous in this filter no
   * longer exists.
   */
  public void setPorousNone() {
    synchronized (this) {
      if (f_porousValues.isEmpty())
        return;
      f_porousValues.clear();
      refreshPorousDrops(getPreviousPorusDrops());
    }
    notifyFilterChanged();
    /*
     * Tell my enclosing selection to update filters after me because I changed
     * the set of results that I let through.
     */
    f_selection.filterChanged(this);
  }

  /**
   * Returns a copy of the set of porous values for this filter.
   * 
   * @return a copy of the set of porous values for this filter.
   */
  public Set<String> getPorousValues() {
    synchronized (this) {
      return new HashSet<String>(f_porousValues);
    }
  }

  /**
   * The total count of results that this filter may filter. This is the count
   * of results that the previous filter let through.
   * 
   * @return a count of results.
   */
  public int getResultCountTotal() {
    synchronized (this) {
      return f_countTotal;
    }
  }

  /**
   * The count of results that this filter, based upon what is set to be porous,
   * will allow through.
   * <p>
   * it is an invariant that <code>getFindingCountPorous() <=
   * getFindingCountTotal()</code>.
   * 
   * @return count of results that this filter, based upon what is set to be
   *         porous, will allow through.
   */
  public int getResultCountPorous() {
    synchronized (this) {
      return f_porousDrops.size();
    }
  }

  /**
   * Indicates if this filter allows any possible results through it.
   * 
   * @return <code>true</code> if the filter allows results through it,
   *         <code>false</code> otherwise.
   */
  public final boolean isPorous() {
    return getResultCountPorous() > 0;
  }

  protected final List<IProofDrop> f_porousDrops = new ArrayList<IProofDrop>();

  /**
   * Gets a copy of the list of results that this filter allows through it.
   * 
   * @return the list of results that this filter allows through it.
   */
  public final List<IProofDrop> getPorousDrops() {
    synchronized (this) {
      return new ArrayList<IProofDrop>(f_porousDrops);
    }
  }

  public String getLabel(String value) {
    return value;
  }

  public Image getImageFor(String value) {
    return null;
  }

  @Override
  public String toString() {
    return "[Filter on " + f_filterLabel + "]";
  }

  /*
   * REFRESH METHODS
   */

  void refresh() {
    final List<IProofDrop> incomingResults = getPreviousPorusDrops();
    synchronized (this) {
      refreshCounts(incomingResults);
      deriveAllValues();
      filterAllValues();
      fixupPorousValues();
      refreshPorousDrops(incomingResults);
    }
    notifyFilterChanged();
  }

  /**
   * Gets the results coming through the previous filter. Handles the first
   * filter by asking the scan information. If this is the first filter and
   * there is no scan information then an empty list is returned.
   * 
   * @return a non-empty list of scan results coming through the previous
   *         filter.
   */
  private List<IProofDrop> getPreviousPorusDrops() {
    final List<IProofDrop> result;
    if (f_previous == null) {
      result = f_selection.getDropsFromSea();
    } else {
      result = f_previous.getPorousDrops();
    }
    return result;
  }

  /**
   * This method must use the passed incoming scan results to refresh the
   * counts.
   * <p>
   * The contents of {@link #f_counts} and {@link #f_countTotal} need to be
   * updated to refresh the counts.
   * <p>
   * Any caller must be holding a lock on <code>this</code>.
   * 
   * @param incomingResults
   *          the list of scan results coming through the previous filter.
   */
  protected void refreshCounts(List<IProofDrop> incomingResults) {
    f_counts.clear();
    int runningTotal = 0;
    for (IProofDrop d : incomingResults) {
      final String value = getFilterValueFromDropOrNull(d);
      if (value != null) {
        Integer count = f_counts.get(value);
        if (count == null) {
          f_counts.put(value, 1);
        } else {
          f_counts.put(value, count + 1);
        }
        runningTotal++;
      }
    }
    f_countTotal = runningTotal;
  }

  /**
   * May need to be overridden if the set of values includes values not able to
   * be determined from the filter context.
   * <p>
   * Any caller must be holding a lock on <code>this</code>.
   */
  protected void deriveAllValues() {
    f_allValues.clear();
    f_allValues.addAll(f_counts.keySet());
    sortAllValues();
  }

  /**
   * Any caller must be holding a lock on <code>this</code>.
   */
  protected void sortAllValues() {
    Collections.sort(f_allValues);
  }

  /**
   * Any caller must be holding a lock on <code>this</code>.
   */
  protected void filterAllValues() {
    if (f_filterExpression != null && f_filterExpression != NO_FILTER && f_filterExpression.length() > 0) {
      final Iterator<String> it = f_allValues.iterator();
      while (it.hasNext()) {
        String value = it.next();
        // Keep all checked values shown--regardless of the filter
        if (this.isPorous(value))
          continue;
        String label = getLabel(value);
        if (label != null && !label.toLowerCase().contains(f_filterExpression.toLowerCase())) {
          it.remove();
        }
      }
    }
  }

  /**
   * Any caller must be holding a lock on <code>this</code>.
   */
  private void fixupPorousValues() {
    /*
     * We don't want to delete values that are no longer in this filter because
     * they may be in the future.
     */
    /*
     * If only one choice exists, go ahead and select it. Bill Scherlis had this
     * idea for making the filter easier to use.
     */
    if (f_allValues.size() == 1 && isFilterExpressionClear())
      f_porousValues.addAll(f_allValues);
    /*
     * Don't call notifyPorous() here, the caller of this method will do it in a
     * manner where we are not suspectable to deadlock.
     */
  }

  /**
   * This method must use the passed incoming scan results to refresh the list
   * of results that this filter allows through it.
   * <p>
   * The contents of {@link #f_porousDrops} needs to be updated to refresh the
   * list of results that this filter allows through it.
   * <p>
   * Any caller must be holding a lock on <code>this</code>.
   * 
   * @param incomingResults
   *          the list of scan results coming through the previous filter.
   */
  final void refreshPorousDrops(List<IProofDrop> incomingResults) {
    f_porousDrops.clear();
    for (IProofDrop d : incomingResults) {
      final String value = getFilterValueFromDropOrNull(d);
      if (value != null) {
        if (f_porousValues.contains(value))
          f_porousDrops.add(d);
      }
    }
  }
}
