package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public final class StateWrtElementScan extends StateWrtElement {

  protected StateWrtElementScan(String scanLabel) {
    super(null, scanLabel);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JSURE_LOGO);
  }
}
