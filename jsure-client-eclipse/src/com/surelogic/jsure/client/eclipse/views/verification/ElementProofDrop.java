package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;

public final class ElementProofDrop extends ElementDrop {

  ElementProofDrop(IProofDrop proofDrop, Element parent) {
    super(parent);
    if (proofDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "proofDrop"));
    f_proofDrop = proofDrop;
  }

  private final IProofDrop f_proofDrop;

  @Override
  @NonNull
  IProofDrop getDrop() {
    return f_proofDrop;
  }

  @Override
  String getLabel() {
    return f_proofDrop.getMessage();
  }

  @Override
  int getImageFlags() {
    int flags = 0;
    flags |= f_proofDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT;
    if (f_proofDrop.proofUsesRedDot())
      flags |= CoE_Constants.REDDOT;
    if (f_proofDrop instanceof IPromiseDrop) {
      final IPromiseDrop promiseDrop = (IPromiseDrop) f_proofDrop;
      if (promiseDrop.isVirtual())
        flags |= CoE_Constants.VIRTUAL;
      if (promiseDrop.isAssumed())
        flags |= CoE_Constants.ASSUME;
      if (!promiseDrop.isCheckedByAnalysis())
        flags |= CoE_Constants.TRUSTED;
    }
    return flags;
  }

  @Override
  String getImageName() {
    if (f_proofDrop instanceof IPromiseDrop)
      return CommonImages.IMG_ANNOTATION;
    if (f_proofDrop instanceof IResultFolderDrop) {
      if (((IResultFolderDrop) f_proofDrop).getLogicOperator() == IResultFolderDrop.LogicOperator.AND)
        return CommonImages.IMG_FOLDER;
      else
        return CommonImages.IMG_FOLDER_OR;
    }
    if (f_proofDrop instanceof IResultDrop) {
      if (((IResultDrop) f_proofDrop).isConsistent())
        return CommonImages.IMG_PLUS;
      else
        return CommonImages.IMG_RED_X;
    }
    return null;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    if (getAncestorWithSameDropOrNull() == null) {
      return null;
    } else
      return EMPTY;
  }
}
