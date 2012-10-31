package com.surelogic.jsure.views.debug.imageCache;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TableUtility;
import com.surelogic.common.ui.jobs.SLUIJob;

public final class ImageCacheView extends ViewPart {

  private Table f_table;
  private Label f_label;

  private final Action f_refresh = new Action() {
    @Override
    public void run() {
      updateContents();
    }
  };

  @Override
  public void createPartControl(Composite parent) {
    parent.setLayout(new GridLayout());
    f_label = new Label(parent, SWT.NONE);
    f_label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

    f_table = new Table(parent, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
    f_table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    f_table.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
    f_table.setHeaderVisible(true);
    f_table.setLinesVisible(true);
    TableColumn imageColumn = new TableColumn(f_table, SWT.NONE);
    imageColumn.setText("Image");
    TableColumn sizeColumn = new TableColumn(f_table, SWT.NONE);
    sizeColumn.setText("Size");
    TableColumn keyColumn = new TableColumn(f_table, SWT.NONE);
    keyColumn.setText("Key to Image");

    f_refresh.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_REFRESH));
    f_refresh.setText("Refresh");
    f_refresh.setToolTipText("Refresh display with the current contents of the image cache.");

    final IActionBars bars = getViewSite().getActionBars();

    final IMenuManager pulldown = bars.getMenuManager();
    pulldown.add(f_refresh);
    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_refresh);

    final UIJob job = new SLUIJob() {
      public IStatus runInUIThread(IProgressMonitor monitor) {
        updateContents();
        return Status.OK_STATUS;
      }
    };
    job.schedule();

    // parent.pack();
  }

  @Override
  public void setFocus() {
    if (f_table != null && !f_table.isDisposed())
      f_table.setFocus();
  }

  private final ArrayList<Image> f_showing = new ArrayList<Image>();

  private void updateContents() {
    SLImages.getImage(CommonImages.IMG_EMPTY);
    f_table.setRedraw(false);
    for (TableItem ti : f_table.getItems())
      ti.dispose();
    for (Image i : f_showing)
      i.dispose();
    f_showing.clear();
    Map<String, Image> cache = SLImages.getCopyOfImageCache();
    f_label.setText(cache.size() + " images in the SLImages cache");
    // calculate largest image size because table takes first size
    int width = 0;
    int height = 0;
    for (Image i : cache.values()) {
      final int iWidth = i.getBounds().width;
      final int iHeight = i.getBounds().height;
      if (iWidth > width)
        width = iWidth;
      if (iHeight > height)
        height = iHeight;
    }
    final Point size = new Point(width, height);
    for (Map.Entry<String, Image> e : cache.entrySet()) {
      TableItem ti = new TableItem(f_table, SWT.NONE);
      final Image i = e.getValue();
      if (i.getBounds().width == size.x && i.getBounds().height == size.y) {
        ti.setImage(0, i);
      } else {
        final Image forTable = SLImages.resizeImage(i, size);
        f_showing.add(forTable);
        ti.setImage(0, forTable);
      }
      ti.setText(1, i.getBounds().width + "x" + i.getBounds().height);
      ti.setText(2, e.getKey());
    }
    TableUtility.packColumns(f_table);
    f_table.setRedraw(true);
  }
}
