package com.surelogic.jsure.client.eclipse.views.metrics.dropcounter;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.java.persistence.JSureScanInfo;

public class DropCounterViewContentProvider implements IStructuredContentProvider {

  interface IDropTotalCountUpdater {
    void updateTotalLabel(long value);
  }

  final IDropTotalCountUpdater f_provider;

  public DropCounterViewContentProvider(IDropTotalCountUpdater provider) {
    f_provider = provider;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof JSureScanInfo) {
      final JSureScanInfo scanInfo = (JSureScanInfo) newInput;
      final HashMap<String, DropCounterElement> counts = new HashMap<String, DropCounterElement>();
      final List<IDrop> drops = scanInfo.getDropInfo();
      f_provider.updateTotalLabel(drops.size());
      for (IDrop drop : drops) {
        final String dropTypeName;
        if (drop instanceof IMetricDrop) {
          final String metricType = ((IMetricDrop) drop).getMetric().toString();
          dropTypeName = drop.getSimpleClassName() + " : " + metricType;
        } else {
          dropTypeName = drop.getSimpleClassName();
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
