package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.NonNull;

abstract class ElementWithChildren extends Element {

  protected ElementWithChildren(Element parent) {
    super(parent);
  }

  @NonNull
  private final ArrayList<Element> f_children = new ArrayList<Element>();

  @Override
  final void addChild(Element child) {
    if (child == null)
      return;
    f_children.add(child);
  }

  @Override
  @NonNull
  final Element[] getChildren() {
    return f_children.toArray(new Element[f_children.size()]);
  }

  @NonNull
  final List<Element> getChildrenAsListReference() {
    return f_children;
  }
}
