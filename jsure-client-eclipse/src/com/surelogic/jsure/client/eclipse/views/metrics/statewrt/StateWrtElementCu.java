package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public final class StateWrtElementCu extends StateWrtElement {

  protected StateWrtElementCu(StateWrtElementPackage parent, @Nullable String javaFileName) {
    super(parent, javaFileName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
  }
}
