package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import java.util.ArrayList;
import java.util.logging.Level;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
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

    Input(@NonNull JSureScanInfo scan, @NonNull ArrayList<IMetricDrop> drops) {
      f_scan = scan;
      f_drops = drops;
    }
  }

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof Input) {
      final Input in = (Input) newInput;
      final Folderizer tree = new Folderizer(in.f_scan.findProjectsLabel());
      for (IMetricDrop drop : in.f_drops) {
        tree.addToTree(drop);
      }
      f_root = tree.getRootElements();
    } else if (newInput == null) {
      f_root = SlocElement.EMPTY;
    } else {
      SLLogger.getLogger().log(Level.SEVERE, I18N.err(301, this.getClass().getSimpleName(), newInput));
    }

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
