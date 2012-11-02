package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;

public final class ElementProject extends ElementWithChildren {

  protected ElementProject(@NonNull final String projectLabel) {
    super(null);
    if (projectLabel == null)
      throw new IllegalArgumentException(I18N.err(44, "projectLabel"));
    f_projectLabel = projectLabel;
  }

  @NonNull
  private final String f_projectLabel;

  @Override
  String getLabel() {
    return f_projectLabel;
  }

  @Override
  @Nullable
  Image getElementImage() {
    return SLImages.getImageForProject(f_projectLabel);
  }
}
