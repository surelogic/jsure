package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ui.SLImages;

public final class ElementJavaDecl extends Element {

  protected ElementJavaDecl(Element parent, IDecl javaDecl) {
    super(parent);
    f_javaDecl = javaDecl;
  }

  private final IDecl f_javaDecl;

  @Override
  @NonNull
  Element[] constructChildren() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  String getLabel() {
    return f_javaDecl.getName();
  }

  @Override
  @Nullable
  Image getElementImage() {
    return SLImages.getImageFor(f_javaDecl);
  }
}
