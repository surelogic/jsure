package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

/**
 * Defines a selection of JSure analysis and verification results using a series
 * of filters.
 */
public final class Selection implements JSureDataDirHub.CurrentScanChangeListener {

  /**
   * Immutable set of all possible filters.
   */
  private static final Set<ISelectionFilterFactory> f_allFilters;
  static {
    Set<ISelectionFilterFactory> allFilters = new HashSet<ISelectionFilterFactory>();
    /*
     * Add in all the filter factories.
     */
    allFilters.add(FilterAnalysisResult.FACTORY);
    allFilters.add(FilterAnnotation.FACTORY);
    allFilters.add(FilterJavaType.FACTORY);
    allFilters.add(FilterJavaPackage.FACTORY);
    allFilters.add(FilterProject.FACTORY);
    allFilters.add(FilterVerificationJudgment.FACTORY);

    f_allFilters = Collections.unmodifiableSet(allFilters);
  }

  /**
   * Gets the immutable set of all possible filters.
   * 
   * @return the immutable set of all possible filters.
   */
  public static Set<ISelectionFilterFactory> getAllFilters() {
    return f_allFilters;
  }

  Selection(SelectionManager manager) {
    assert manager != null;
    f_manager = manager;
  }

  public Selection(Selection source) {
    synchronized (source) {
      f_manager = source.f_manager;
      f_showingResults = source.f_showingResults;

      Filter prev = null;
      for (Filter f : source.f_filters) {
        Filter clone = f.copyNoQuery(this, prev);
        prev = clone;
        f_filters.add(clone);
      }
    }
  }

  /**
   * This just connects this filter to the {@link JSureDataDirHub}. Making it
   * reflect changes to the current scan. Ensure that {@link #dispose()} is
   * called to disconnect this selection from the {@link JSureDataDirHub} when
   * the selection is no longer used.
   */
  public void initAndSyncToSea() {
    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
    refresh();
  }

  /**
   * Refreshes all the filters that comprise this selection.
   */
  public void refresh() {
    /*
     * The user has changed to a new JSure scan. Refresh this selection if it
     * has any filters.
     */
    final SLJob job = new AbstractSLJob("Selection changing to a different JSure scan") {

      @Override
      public SLStatus run(SLProgressMonitor monitor) {
        monitor.begin();
        try {
          /**
           * Refresh all the filters to the new scan's data.
           */
          synchronized (Selection.this) {
            for (Filter filter : f_filters) {
              filter.refresh();
            }
          }
          notifySelectionChanged();
        } catch (Exception e) {
          final int errNo = 234;
          final String msg = I18N.err(errNo);
          return SLStatus.createErrorStatus(errNo, msg, e);
        } finally {
          monitor.done();
        }
        return SLStatus.OK_STATUS;
      }
    };
    EclipseJob.getInstance().schedule(job, false, true);
  }

  public void dispose() {
    JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);

    synchronized (this) {
      for (Filter f : f_filters)
        f.dispose();
      f_observers.clear();
    }
  }

  private final SelectionManager f_manager;

  public SelectionManager getManager() {
    /*
     * Not mutable so we don't need to hold a lock on this.
     */
    return f_manager;
  }

  /**
   * The ordered list of filters within this selection.
   */
  private final LinkedList<Filter> f_filters = new LinkedList<Filter>();

  /**
   * Gets the ordered list of filters managed by this Selection;
   * 
   * @return
   */
  public final List<Filter> getFilters() {
    synchronized (this) {
      return new LinkedList<Filter>(f_filters);
    }
  }

  /**
   * Gets the first filter of this selection.
   * 
   * @return a filter within this selection.
   */
  public Filter getFirstFilter() {
    synchronized (this) {
      return f_filters.getFirst();
    }
  }

  /**
   * Indicates if the passed filter is the first filter of this selection.
   * 
   * @param filter
   *          a filter within this selection.
   * @return <code>true</code> if the passed filter is the first filter of this
   *         selection, <code>false</code> otherwise.
   */
  public boolean isFirstFilter(Filter filter) {
    synchronized (this) {
      return f_filters.getFirst() == filter;
    }
  }

  /**
   * Gets the last filter of this selection.
   * 
   * @return a filter within this selection.
   */
  public Filter getLastFilter() {
    synchronized (this) {
      return f_filters.getLast();
    }
  }

  /**
   * Indicates if the passed filter is the last filter of this selection.
   * 
   * @param filter
   *          a filter within this selection.
   * @return <code>true</code> if the passed filter is the last filter of this
   *         selection, <code>false</code> otherwise.
   */
  public boolean isLastFilter(Filter filter) {
    synchronized (this) {
      return f_filters.getLast() == filter;
    }
  }

  /**
   * Removes all the passed filter and all subsequent filters from this
   * selection.
   * 
   * @param filter
   *          a filter within this selection, may be <code>null</code> in which
   *          case the selection is not modified.
   */
  public void emptyAfter(Filter filter) {
    if (filter == null)
      return;
    final List<Filter> disposeList = new ArrayList<Filter>();
    boolean found = false;
    synchronized (this) {
      for (Iterator<Filter> iterator = f_filters.iterator(); iterator.hasNext();) {
        final Filter next = iterator.next();
        if (filter == next)
          found = true;
        if (found) {
          disposeList.add(filter);
          iterator.remove();
        }
      }
    }
    if (found) {
      for (Filter f : disposeList) {
        f.dispose();
      }
      notifySelectionChanged();
    }
  }

  /**
   * Removes all existing filters from this selection with an index after the
   * specified index.
   * 
   * @param filterIndex
   *          the index of a filter used by this selection. A value of -1 will
   *          clear out all filters.
   */
  public void emptyAfter(int filterIndex) {
    final List<Filter> disposeList = new ArrayList<Filter>();
    boolean changed = false;
    int index = 0;
    synchronized (this) {
      for (Iterator<Filter> iterator = f_filters.iterator(); iterator.hasNext();) {
        Filter filter = iterator.next();
        if (index > filterIndex) {
          disposeList.add(filter);
          iterator.remove();
          changed = true;
        }
        index++;
      }
    }
    if (changed) {
      for (Filter f : disposeList) {
        f.dispose();
      }
      notifySelectionChanged();
    }
  }

  /**
   * Gets the number of filters used by this selection.
   * 
   * @return the number of filters used by this selection.
   */
  public int getFilterCount() {
    synchronized (this) {
      return f_filters.size();
    }
  }

  /**
   * Indicates if this selection should show the list of results selected.
   */
  private boolean f_showingResults = false;

  /**
   * Indicates if this selection shows the list of results in the UI.
   * 
   * @return <code>true</code> if this selection should show the list of
   *         results, <code>false<code> if it should not.
   */
  public boolean isShowingResults() {
    synchronized (this) {
      return f_showingResults;
    }
  }

  /**
   * Sets the status of this selection with regard to showing the list of
   * results.
   * 
   * @param value
   *          <code>true</code> if this selection should show the list of
   *          results, <code>false<code> if it should not.
   */
  public void setShowingResults(boolean value) {
    synchronized (this) {
      f_showingResults = value;
    }
  }

  /**
   * Constructs a filter at the end of this selections chain of filters. Adds an
   * optional observer to that filter. This method does <i>not</i> initiate the
   * query to populate the filter.
   * 
   * @param factory
   *          a filter factory used to select the filter to be constructed.
   * @param observer
   *          an observer for the new filter, may be <code>null</code> if no
   *          observer is desired.
   * @return the new filter.
   */
  public Filter construct(ISelectionFilterFactory factory, IFilterObserver observer) {
    if (factory == null)
      throw new IllegalArgumentException("factory must be non-null");
    final Filter filter;
    synchronized (this) {
      if (!getAvailableFilters().contains(factory))
        throw new IllegalArgumentException(factory.getFilterLabel() + " already used in selection");
      final Filter previous = f_filters.isEmpty() ? null : f_filters.getLast();
      filter = factory.construct(this, previous);
      f_filters.add(filter);
    }
    filter.addObserver(observer);
    return filter;
  }

  /**
   * Gets the list of filters that are not yet being used as part of this
   * selection. Any member of this result could be used to in a call to
   * {@link #construct(ISelectionFilterFactory)}.
   * 
   * @return factories for unused filters.
   */
  public List<ISelectionFilterFactory> getAvailableFilters() {
    List<ISelectionFilterFactory> result = new ArrayList<ISelectionFilterFactory>(f_allFilters);
    synchronized (this) {
      for (Filter filter : f_filters) {
        result.remove(filter.getFactory());
      }
    }
    Collections.sort(result);
    return result;
  }

  /**
   * The count of results that this selection, based upon what its filters have
   * set to be porous, will allow through. A selection with no filters allows
   * everything through.
   * 
   * @return count of results that this selection, based upon what its filters
   *         have set to be porous, will allow through.
   */
  public int getResultCountPorous() {
    return getPorousDrops().size();
  }

  /**
   * Gets all the drop information from the scan that we care about.
   * 
   * @return a collection of all the proof drop information from the scan.
   */
  List<IProofDrop> getDropsFromSea() {
    final List<IProofDrop> result;
    JSureScanInfo scanInfo = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (scanInfo == null) {
      result = Collections.emptyList();
    } else {
      result = scanInfo.getProofDrops();
    }

    /*
     * Filter out annotations that are not from source.
     */
    for (Iterator<IProofDrop> i = result.iterator(); i.hasNext();) {
      IProofDrop drop = i.next();
      if (!drop.derivedFromSrc())
        i.remove();
    }
    return result;
  }

  /**
   * Gets a copy of the list of results that this selection allows through it. A
   * selection with no filters allows everything through.
   * 
   * @return the list of results that this selection allows through it.
   */
  public List<IProofDrop> getPorousDrops() {
    synchronized (this) {
      if (!f_filters.isEmpty()) {
        return f_filters.getLast().getPorousDrops();
      } else {
        return getDropsFromSea();
      }
    }
  }

  /**
   * Indicates if this selection allows any possible results through it.
   * 
   * @return <code>true</code> if the selection allows results through it,
   *         <code>false</code> otherwise.
   */
  public boolean isPorous() {
    return !getPorousDrops().isEmpty();
  }

  private final Set<ISelectionObserver> f_observers = new CopyOnWriteArraySet<ISelectionObserver>();

  public void addObserver(ISelectionObserver o) {
    if (o == null)
      return;
    f_observers.add(o);
  }

  public void removeObserver(ISelectionObserver o) {
    f_observers.remove(o);
  }

  /**
   * Do not call this method holding a lock on <code>this</code>. Deadlock could
   * occur as we are invoking an alien method.
   */
  private void notifySelectionChanged() {
    for (ISelectionObserver o : f_observers)
      o.selectionChanged(this);
  }

  /**
   * Invoked by a filter when the amount of results allowed through the filter
   * changed. This would be a change that occurred in the user interface.
   * <p>
   * This method must never be called during a refresh or an infinite loop of
   * refreshes could occur.
   * 
   * @param changedFilter
   *          a filter that is part of this selection.
   */
  void filterChanged(final Filter changedFilter) {
    final SLJob job = new AbstractSLJob("Refresh filter") {

      @Override
      public SLStatus run(SLProgressMonitor monitor) {
        monitor.begin();
        try {
          /*
           * Refreshes the data in each filter after the changed one.
           */
          boolean needsRefresh = false;
          synchronized (Selection.this) {
            for (Filter filter : f_filters) {
              if (needsRefresh)
                filter.refresh();
              if (!needsRefresh) {
                if (filter == changedFilter)
                  needsRefresh = true;
              }
            }
          }
          notifySelectionChanged();
        } catch (Exception e) {
          final int errNo = 234;
          final String msg = I18N.err(errNo);
          return SLStatus.createErrorStatus(errNo, msg, e);
        } finally {
          monitor.done();
        }
        return SLStatus.OK_STATUS;
      }
    };
    EclipseJob.getInstance().schedule(job, false, true);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("[Selection filters={");
    boolean first = true;
    for (Filter f : f_filters) {
      if (first)
        first = false;
      else
        b.append(",");
      b.append(f.toString());
    }
    b.append("}]");
    return b.toString();
  }
}
