package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.Activator;

public class StateWrtElementJavaDecl extends StateWrtElement {

  @NonNull
  final IDecl f_javaDecl;

  protected StateWrtElementJavaDecl(StateWrtElement parent, IDecl javaDecl) {
    super(parent, DeclUtil.getEclipseJavaOutlineLikeLabel(javaDecl));
    if (javaDecl == null)
      throw new IllegalArgumentException(I18N.err(44, "javaDecl"));
    else
      f_javaDecl = javaDecl;
  }

  @NonNull
  public IDecl getDeclaration() {
    return f_javaDecl;
  }

  @Override
  public Image getImage() {
    return SLImages.getImageFor(f_javaDecl);
  }

  @Override
  public void tryToOpenInJavaEditor() {
    Activator.highlightLineInJavaEditor(this.getDeclaration());
  }
}
