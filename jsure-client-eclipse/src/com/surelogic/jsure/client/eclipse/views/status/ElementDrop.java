package com.surelogic.jsure.client.eclipse.views.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

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

  protected ElementDrop(Element parent, @NonNull final IDrop drop) {
    super(parent);
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    f_drop = drop;
    final ScanDifferences diff = f_diff;
    if (diff == null) {
      f_diffDrop = null;
    } else {
      if (diff.isNotInOldScan(drop)) {
        f_diffDrop = drop;
      } else {
        f_diffDrop = diff.getChangedInOldScan(drop);
      }
    }
  }

  @NonNull
  private final IDrop f_drop;

  @NonNull
  IDrop getDrop() {
    return f_drop;
  }

  /**
   * There are three cases:
   * <ul>
   * <li>if {@link #f_diffDrop} <tt>== null</tt> the drop is unchanged.</li>
   * <li>if {@link #f_diffDrop} <tt>==</tt> {@link #f_drop} the drop is new in
   * this scan.</li>
   * <li>if {@link #f_diffDrop} <tt>!= null &&</tt> {@link #f_diffDrop}
   * <tt>!=</tt> {@link #f_drop} the drop changed&mdash;and {@link #f_diffDrop}
   * references the drop in the old scan.</li>
   * </ul>
   */
  @Nullable
  private IDrop f_diffDrop;

  final boolean isSame() {
    return f_diffDrop == null;
  }

  final boolean isNew() {
    return f_diffDrop == getDrop();
  }

  final boolean isChanged() {
    return f_diffDrop != null && f_diffDrop != f_drop;
  }

  @Nullable
  final IDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Nullable
  final Image getElementImageChangedFromDropOrNull() {
    final IDrop changedDrop = getChangedFromDropOrNull();
    if (changedDrop == null)
      return null;
    else
      return JSureDecoratedImageUtility.getImageForDrop(changedDrop, false);
  }

  @Override
  String getLabel() {
    return getDrop().getMessage();
  }

  @Override
  String getLabelToPersistViewerState() {
    return getDrop().getMessage();
  }

  @Override
  @Nullable
  final Image getElementImage() {
    return JSureDecoratedImageUtility.getImageForDrop(getDrop(), false);
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
    return SLImages.resizeImage(SLImages.getImageForProject(getDrop().getJavaRef()), JSureDecoratedImageUtility.SIZE);
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
  Image getSimpleTypeImageOrNull() {
    final IJavaRef jr = getDrop().getJavaRef();
    if (jr != null) {
      IDecl typeDecl = DeclUtil.getTypeNotInControlFlow(jr.getDeclaration());
      if (typeDecl == null)
        return null;
      return SLImages.resizeImage(SLImages.getImageFor(typeDecl), JSureDecoratedImageUtility.SIZE);
    } else
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
