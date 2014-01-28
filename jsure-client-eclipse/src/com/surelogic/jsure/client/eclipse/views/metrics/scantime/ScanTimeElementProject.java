package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.ui.SLImages;

public final class ScanTimeElementProject extends ScanTimeElementWithChildren {

  protected ScanTimeElementProject(ScanTimeElement parent, String projectName) {
    super(parent, projectName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImageForProject(f_label);
  }
}
