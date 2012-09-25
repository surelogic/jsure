package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

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

  @Override
  String getProjectOrNull() {
    final ISrcRef sr = getDrop().getSrcRef();
    if (sr != null)
      return sr.getProject();
    else
      return null;
  }

  @Override
  String getPackageOrNull() {
    final ISrcRef sr = getDrop().getSrcRef();
    if (sr != null)
      return sr.getPackage();
    else
      return null;
  }

  @Override
  String getTypeOrNull() {
    final ISrcRef sr = getDrop().getSrcRef();
    if (sr != null)
      return sr.getCUName();
    else
      return null;
  }

  @Override
  int getLineNumber() {
    final ISrcRef sr = getDrop().getSrcRef();
    if (sr != null)
      return sr.getLineNumber();
    else
      return super.getLineNumber();
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
