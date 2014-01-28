package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.NonNull;

public abstract class ScanTimeElementWithChildren extends ScanTimeElement {

  protected ScanTimeElementWithChildren(ScanTimeElement parent, String label) {
    super(parent, label);
  }

  @NonNull
  private final ArrayList<ScanTimeElement> f_children = new ArrayList<ScanTimeElement>();

  final void addChild(ScanTimeElement child) {
    if (child == null)
      return;
    f_children.add(child);
  }

  @Override
  @NonNull
  public final ScanTimeElement[] getChildren() {
    return f_children.toArray(new ScanTimeElement[f_children.size()]);
  }

  @Override
  public final boolean hasChildren() {
    return !f_children.isEmpty();
  }

  @Override
  public final boolean highlightDueToSlocThreshold(ScanTimeOptions options) {
    boolean result = false;
    for (ScanTimeElement element : getChildrenAsListReference())
      result |= element.highlightDueToSlocThreshold(options);
    return result;
  }

  @NonNull
  final List<ScanTimeElement> getChildrenAsListReference() {
    return f_children;
  }
}
