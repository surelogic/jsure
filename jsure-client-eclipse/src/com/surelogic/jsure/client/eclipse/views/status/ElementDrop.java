package com.surelogic.jsure.client.eclipse.views.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

abstract class ElementDrop extends Element {

  static Collection<Element> factory(Element parent, IDrop drop) {
    final List<Element> result = new ArrayList<Element>();
    if (drop instanceof IPromiseDrop)
      result.add(new ElementPromiseDrop(parent, (IPromiseDrop) drop));
    else if (drop instanceof IResultDrop)
      result.add(new ElementResultDrop(parent, (IResultDrop) drop));
    else if (drop instanceof IResultFolderDrop)
      result.addAll(ElementResultFolderDrop.getInstanceOrElideFolder(parent, (IResultFolderDrop) drop));
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

  abstract boolean isSame();

  abstract boolean isNew();

  @Nullable
  abstract IDrop getChangedFromDropOrNull();

  abstract EnumSet<Flag> getImageFlagsForChangedFromDrop();

  @Nullable
  abstract String getImageNameForChangedFromDrop();

  @Nullable
  abstract String getMessageAboutWhatChangedOrNull();

  @Override
  String getLabel() {
    return getDrop().getMessage();
  }

  @Override
  String getLabelToPersistViewerState() {
    return getDrop().getMessage();
  }

  @Override
  String getProjectNameOrNull() {
    final IJavaRef jr = getDrop().getJavaRef();
    if (jr != null)
      return jr.getEclipseProjectName();
    else
      return null;
  }

  @Override
  Image getProjectImageOrNull() {
    final IJavaRef jr = getDrop().getJavaRef();
    if (jr != null)
      return SLImages.getImageForProject(jr.getRealEclipseProjectNameOrNull());
    else
      return null;
  }

  @Override
  String getPackageNameOrNull() {
    final IJavaRef jr = getDrop().getJavaRef();
    if (jr != null)
      return jr.getPackageName();
    else
      return null;
  }

  @Override
  String getSimpleTypeNameOrNull() {
    final IJavaRef jr = getDrop().getJavaRef();
    if (jr != null)
      return jr.getTypeNameOrNull();
    else
      return null;
  }

  @Override
  int getLineNumber() {
    final IJavaRef jr = getDrop().getJavaRef();
    if (jr != null)
      return jr.getLineNumber();
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
