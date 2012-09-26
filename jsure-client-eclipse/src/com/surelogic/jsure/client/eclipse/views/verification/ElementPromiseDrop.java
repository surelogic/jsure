package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IPromiseDrop;

final class ElementPromiseDrop extends ElementProofDrop {

  ElementPromiseDrop(Element parent, IPromiseDrop promiseDrop) {
    super(parent);
    if (promiseDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "promiseDrop"));
    f_promiseDrop = promiseDrop;
  }

  private final IPromiseDrop f_promiseDrop;

  @Override
  @NonNull
  IPromiseDrop getDrop() {
    return f_promiseDrop;
  }

  @Override
  int getImageFlags() {
    int flags = super.getImageFlags();
    if (f_promiseDrop.isVirtual())
      flags |= CoE_Constants.VIRTUAL;
    if (f_promiseDrop.isAssumed())
      flags |= CoE_Constants.ASSUME;
    if (!f_promiseDrop.isCheckedByAnalysis())
      flags |= CoE_Constants.TRUSTED;
    return flags;
  }

  @Override
  String getImageName() {
    return CommonImages.IMG_ANNOTATION;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    /*
     * Only show children if we are not encapsulating the same drop as one of
     * our ancestors.
     */
    if (getAncestorWithSameDropOrNull() == null) {
      final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
      c.addAll(getDrop().getProposals());
      c.addAll(getDrop().getHints());
      c.addAll(getDrop().getCheckedBy());
      c.addAll(getDrop().getDependentPromises());
      if (c.isEmpty())
        return EMPTY;
      else
        return c.getAllElementsAsArray();
    } else
      return EMPTY;
  }
}