package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

public class ElementDrop extends Element {

  protected ElementDrop(Element parent, @NonNull IDrop drop) {
    super(parent);
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    f_drop = drop;
  }

  @NonNull
  private final IDrop f_drop;

  @Override
  void addChild(Element child) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull
  Element[] getChildren() {
    return EMPTY;
  }

  @Override
  String getLabel() {
    return f_drop.getMessage();
  }

  @Override
  @Nullable
  Image getElementImage() {
    return null;  // todo
  }

}
