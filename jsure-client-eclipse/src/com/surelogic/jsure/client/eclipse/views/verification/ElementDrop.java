package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IDrop;

abstract class ElementDrop extends Element {

  protected ElementDrop(Element parent) {
    super(parent);
  }

  @NonNull
  abstract IDrop getDrop();

  @Override
  final String getLabel() {
    return getDrop().getMessage();
  }

  final ElementDrop getAncestorWithSameDropOrNull() {
    final IDrop thisDrop = getDrop();
    Element e = this;
    while (true) {
      e = e.getParent();
      if (e == null)
        return null; // at root
      if (e instanceof ElementDrop) {
        final ElementDrop ed = (ElementDrop) e;
        final IDrop ancestorDrop = ed.getDrop();
        if (thisDrop.equals(ancestorDrop))
          return ed;
      }
    }
  }
}
