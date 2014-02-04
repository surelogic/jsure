package com.surelogic.jsure.client.eclipse.views.metrics.dropcounter;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.ColumnResizeListener;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.metrics.AbstractScanMetricMediator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class DropCounterMetricMediator extends AbstractScanMetricMediator {

  @Override
  protected String getMetricLabel() {
    return "Proof \"Drop\" Counts";
  }

  public DropCounterMetricMediator(TabFolder folder, ViewPart view) {
    super(folder, view);
  }

  final ViewerSorter f_alphaSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof DropCounterElement && e2 instanceof DropCounterElement) {
        return DropCounterElement.ALPHA.compare((DropCounterElement) e1, (DropCounterElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  final ViewerSorter f_metricSorter = new ViewerSorter() {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof DropCounterElement && e2 instanceof DropCounterElement) {
        return DropCounterElement.METRIC.compare((DropCounterElement) e1, (DropCounterElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  TableViewer f_tableViewer;

  @Override
  protected Control initMetricDisplay(PageBook parent, ViewPart view) {
    f_tableViewer = new TableViewer(parent, SWT.FULL_SELECTION);
    f_tableViewer.setContentProvider(new DropCounterViewContentProvided());
    f_tableViewer.getTable().setHeaderVisible(true);
    f_tableViewer.getTable().setLinesVisible(true);

    TableViewerColumn dropType = new TableViewerColumn(f_tableViewer, SWT.LEFT);
    dropType.setLabelProvider(DROP);
    dropType.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_DROP_COUNTER_COL_DROP_WIDTH));
    dropType.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_DROP_COUNTER_COL_DROP_WIDTH));
    dropType.getColumn().setText("Proof \"Drop\" Type");

    TableViewerColumn count = new TableViewerColumn(f_tableViewer, SWT.RIGHT);
    count.setLabelProvider(COUNT);
    count.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_DROP_COUNTER_COL_COUNT_WIDTH));
    count.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_DROP_COUNTER_COL_COUNT_WIDTH));
    count.getColumn().setText("Instances");

    return f_tableViewer.getControl();
  }

  @Override
  protected void refreshMetricContentsFor(JSureScanInfo scan, ArrayList<IMetricDrop> drops) {
    f_tableViewer.setInput(scan);
  }

  void fixSortingIndicatorOnTreeTable() {
    if (f_tableViewer.getSorter() == f_alphaSorter) {
      f_tableViewer.getTable().setSortColumn(f_tableViewer.getTable().getColumn(0));
      f_tableViewer.getTable().setSortDirection(SWT.DOWN);
    } else {
      f_tableViewer.getTable().setSortColumn(f_tableViewer.getTable().getColumn(1));
      f_tableViewer.getTable().setSortDirection(SWT.UP);
    }
  }

  final CellLabelProvider DROP = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof DropCounterElement) {
        final DropCounterElement element = (DropCounterElement) cell.getElement();
        cell.setText(element.dropTypeName);
        cell.setImage(SLImages.getImage(CommonImages.IMG_METHOD_PUBLIC));
      }
    }
  };

  final CellLabelProvider COUNT = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof DropCounterElement) {
        final DropCounterElement element = (DropCounterElement) cell.getElement();
        cell.setText(SLUtility.toStringHumanWithCommas(element.dropCount));
      }
    }
  };

  @Override
  public void takeActionCollapseAll() {
    // ignore
  }

  @Override
  public void takeActionUseAlphaSort(boolean value) {
    if (value)
      f_tableViewer.setSorter(f_alphaSorter);
    else
      f_tableViewer.setSorter(f_metricSorter);
    fixSortingIndicatorOnTreeTable();
  }

  @Override
  public void takeActionUseFilter(boolean value) {
    // ignore
  }

  @Override
  public void dispose() {
    // nothing to do
  }
}
