package com.surelogic.jsure.client.eclipse.views.metrics.dropcounter;

import java.util.HashMap;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;

public class DropCounterViewContentProvided implements IStructuredContentProvider {

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof JSureScanInfo) {
      final JSureScanInfo scanInfo = (JSureScanInfo) newInput;
      final HashMap<String, DropCounterElement> counts = new HashMap<String, DropCounterElement>();
      for (IDrop drop : scanInfo.getDropInfo()) {
        final String dropTypeName;
        if (drop instanceof IMetricDrop) {
          final String metricType = ((IMetricDrop) drop).getMetric().toString();
          dropTypeName = drop.getIRDropSeaClass().getSimpleName() + " : " + metricType;
        } else {
          dropTypeName = drop.getIRDropSeaClass().getSimpleName();
        }
        DropCounterElement element = counts.get(dropTypeName);
        if (element == null) {
          element = new DropCounterElement();
          element.dropTypeName = dropTypeName;
          counts.put(dropTypeName, element);
        }
        element.dropCount++;
      }
      f_root = counts.values().toArray(new DropCounterElement[counts.values().size()]);
    }
  }

  private DropCounterElement[] f_root = null;

  @Override
  public Object[] getElements(Object inputElement) {
    final DropCounterElement[] root = f_root;
    return root != null ? root : DropCounterElement.EMPTY;
  }

  @Override
  public void dispose() {
    // nothing to do
  }
}
