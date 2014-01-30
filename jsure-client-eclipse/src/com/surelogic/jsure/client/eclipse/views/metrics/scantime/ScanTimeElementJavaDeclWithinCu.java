package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ui.SLImages;

public class ScanTimeElementJavaDeclWithinCu extends ScanTimeElement {

  @NonNull
  final IDecl f_javaDecl;

  protected ScanTimeElementJavaDeclWithinCu(ScanTimeElement parent, IDecl javaDecl) {
    super(parent, DeclUtil.getEclipseJavaOutlineLikeLabel(javaDecl));
    if (javaDecl == null)
      throw new IllegalArgumentException(I18N.err(44, "javaDecl"));
    else
      f_javaDecl = javaDecl;
  }

  @NonNull
  public IDecl getJavaDecl() {
    return f_javaDecl;
  }

  @Override
  public Image getImage() {
    return SLImages.getImageFor(f_javaDecl);
  }
}
