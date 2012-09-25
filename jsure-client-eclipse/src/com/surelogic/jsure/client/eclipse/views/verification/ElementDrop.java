package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

abstract class ElementDrop extends Element {

  static Collection<Element> factory(Element parent, IDrop drop) {
    final List<Element> result = new ArrayList<Element>();
    if (drop instanceof IPromiseDrop)
      result.add(new ElementPromiseDrop(parent, (IPromiseDrop) drop));
    else if (drop instanceof IResultDrop)
      result.add(new ElementResultDrop(parent, (IResultDrop) drop));
    else if (drop instanceof IResultFolderDrop)
      result.addAll(ElementResultFolderDrop.getInstanceOrElide(parent, (IResultFolderDrop) drop));
    else if (drop instanceof IHintDrop)
      result.add(new ElementHintDrop(parent, (IHintDrop) drop));
    else if (drop instanceof IProposedPromiseDrop)
      result.add(new ElementProposedPromiseDrop(parent, (IProposedPromiseDrop) drop));
    return result;
  }

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
