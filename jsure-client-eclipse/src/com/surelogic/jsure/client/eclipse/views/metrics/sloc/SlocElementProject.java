package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.ui.SLImages;

public final class SlocElementProject extends SlocElementWithChildren {

  protected SlocElementProject(String projectName) {
    super(null, projectName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImageForProject(f_label);
  }
}
