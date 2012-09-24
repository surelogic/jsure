package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IDrop;

public abstract class ElementDrop extends Element {

  protected ElementDrop(Element parent) {
    super(parent);
  }

  @NonNull
  abstract IDrop getDrop();

  ElementDrop getAncestorWithSameDropOrNull() {
    Element e = this;
    IDrop thisDrop = getDrop();

    while (true) {
      e = e.getParent();
      if (e == null)
        break;
      if (e instanceof ElementDrop) {
        final ElementDrop ed = (ElementDrop) e;
        final IDrop ancestorDrop = ed.getDrop();
        if (thisDrop.equals(ancestorDrop))
          return ed;
      }
    }
    return null;
  }
}
