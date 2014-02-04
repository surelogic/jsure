package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

public class ScanTimeElementAnalysis extends ScanTimeElement {

  protected ScanTimeElementAnalysis(ScanTimeElementCu parent, String analysisName) {
    super(parent, analysisName);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_METHOD_PUBLIC);
  }
}
