package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ui.SLImages;

public final class SlocElementPackage extends SlocElementWithChildren {

  protected SlocElementPackage(SlocElement parent, @Nullable String packageName) {
    super(parent, packageName != null ? packageName : SLUtility.JAVA_DEFAULT_PACKAGE);
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_PACKAGE);
  }
}
