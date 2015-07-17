package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.NonNull;

public abstract class SlocElementWithChildren extends SlocElement {

  protected SlocElementWithChildren(SlocElement parent, String label) {
    super(parent, label);
  }

  @NonNull
  private final ArrayList<SlocElement> f_children = new ArrayList<>();

  final void addChild(SlocElement child) {
    if (child == null)
      return;
    f_children.add(child);
  }

  @Override
  @NonNull
  public final SlocElement[] getChildren() {
    return f_children.toArray(new SlocElement[f_children.size()]);
  }

  @Override
  public final boolean hasChildren() {
    return !f_children.isEmpty();
  }

  @Override
  public final boolean highlightDueToSlocThreshold(SlocOptions options) {
    boolean result = false;
    for (SlocElement element : getChildrenAsListReference())
      result |= element.highlightDueToSlocThreshold(options);
    return result;
  }

  @NonNull
  final List<SlocElement> getChildrenAsListReference() {
    return f_children;
  }
}
