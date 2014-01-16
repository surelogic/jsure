package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.model.java.Element;

public class SlocViewContentProvider implements ITreeContentProvider {

  /**
   * Represents input for this content provider.
   */
  static class Input {
    @NonNull
    final JSureScanInfo f_scan;
    @NonNull
    final ArrayList<IMetricDrop> f_drops;
    final int f_thresholdSloc;
    final boolean f_filter;
    final boolean f_highlight;

    Input(@NonNull JSureScanInfo scan, @NonNull ArrayList<IMetricDrop> drops, int thresholdSloc, boolean filter, boolean highlight) {
      f_scan = scan;
      f_drops = drops;
      f_thresholdSloc = thresholdSloc;
      f_filter = filter;
      f_highlight = highlight;
    }

  }

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // TODO Auto-generated method stub

  }

  private SlocElement[] f_root = null;

  @Override
  public Object[] getElements(Object inputElement) {
    final SlocElement[] root = f_root;
    return root != null ? root : SlocElement.EMPTY;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof SlocElement)
      return ((SlocElement) parentElement).getChildren();
    else
      return Element.EMPTY;
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof SlocElement)
      return ((SlocElement) element).getParent();
    else
      return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof SlocElement)
      return ((SlocElement) element).hasChildren();
    else
      return false;
  }
}
