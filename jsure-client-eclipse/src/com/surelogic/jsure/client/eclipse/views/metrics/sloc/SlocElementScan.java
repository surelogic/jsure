package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public final class SlocElementScan extends SlocElementWithChildren {

  protected SlocElementScan(String label) {
    super(null, label);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JSURE_LOGO);
  }
}
