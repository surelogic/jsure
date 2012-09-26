package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IProposedPromiseDrop;

final class ElementProposedPromiseDrop extends ElementDrop {

  protected ElementProposedPromiseDrop(Element parent, IProposedPromiseDrop proposedPromiseDrop) {
    super(parent);
    if (proposedPromiseDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "proposedPromiseDrop"));
    f_proposedPromiseDrop = proposedPromiseDrop;
  }

  private final IProposedPromiseDrop f_proposedPromiseDrop;

  @Override
  @NonNull
  IProposedPromiseDrop getDrop() {
    return f_proposedPromiseDrop;
  }

  @Override
  int getImageFlags() {
    return 0;
  }

  @Override
  @Nullable
  String getImageName() {
    return CommonImages.IMG_ANNOTATION_ABDUCTIVE;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    if (f_showHints)
      c.addAll(getDrop().getHints());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
