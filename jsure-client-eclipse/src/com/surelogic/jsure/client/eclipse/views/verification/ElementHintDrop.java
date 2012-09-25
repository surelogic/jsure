package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IHintDrop;

final class ElementHintDrop extends ElementDrop {

  protected ElementHintDrop(Element parent, IHintDrop hintDrop) {
    super(parent);
    if (hintDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "hintDrop"));
    f_resultDrop = hintDrop;
  }

  private final IHintDrop f_resultDrop;

  @Override
  @NonNull
  IHintDrop getDrop() {
    return f_resultDrop;
  }

  @Override
  int getImageFlags() {
    return 0;
  }

  @Override
  @Nullable
  String getImageName() {
    if (getDrop().getHintType() == IHintDrop.HintType.WARNING)
      return CommonImages.IMG_WARNING;
    else
      return CommonImages.IMG_INFO;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    c.addAll(getDrop().getHints());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
