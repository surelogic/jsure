package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public final class ScanTimeElementCu extends ScanTimeElementWithChildren {

  protected ScanTimeElementCu(ScanTimeElement parent, @Nullable String javaFileName) {
    super(parent, javaFileName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
  }
}
