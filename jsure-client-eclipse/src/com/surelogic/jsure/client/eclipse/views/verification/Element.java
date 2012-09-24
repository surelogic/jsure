package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

public abstract class Element {

  @Nullable
  private final Element f_parent;

  /**
   * Gets the parent content of this content or {@code null} if this content is
   * at the root of the tree.
   * 
   * @return
   */
  @Nullable
  final Element getParent() {
    return f_parent;
  }

  protected Element(Element parent) {
    f_parent = parent;
  }

  abstract Element[] getChildren();

  abstract boolean hasChildren();

  abstract String getLabel();

  abstract int getImageFlags();

  abstract String getImageName();

  final Image getImage() {
    final int flags = getImageFlags();
    String name = getImageName();
    if (name == null)
      name = CommonImages.IMG_UNKNOWN;
    return (new ResultsImageDescriptor(name, flags, VerificationStatusView.ICONSIZE)).getCachedImage();
  }
}
