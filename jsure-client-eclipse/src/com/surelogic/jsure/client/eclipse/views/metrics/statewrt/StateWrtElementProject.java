package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.ui.SLImages;

public final class StateWrtElementProject extends StateWrtElement {

  protected StateWrtElementProject(StateWrtElementScan parent, String projectName) {
    super(parent, projectName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImageForProject(f_label);
  }
}
