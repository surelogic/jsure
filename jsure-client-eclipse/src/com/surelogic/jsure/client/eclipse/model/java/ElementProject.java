package com.surelogic.jsure.client.eclipse.model.java;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;

public final class ElementProject extends ElementWithChildren {

  protected ElementProject(@NonNull final String projectLabel, boolean grayscale) {
    super(null);
    if (projectLabel == null)
      throw new IllegalArgumentException(I18N.err(44, "projectLabel"));
    f_projectLabel = projectLabel;
    f_grayscale = grayscale;
  }

  @NonNull
  private final String f_projectLabel;
  private final boolean f_grayscale;

  @Override
  public String getLabel() {
    return f_projectLabel;
  }

  @Override
  @Nullable
  public Image getElementImage() {
    final Image baseImage = SLImages.getImageForProject(f_projectLabel);
    if (f_grayscale)
      return SLImages.getGrayscaleImage(baseImage);
    else
      return baseImage;
  }
}
