package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

abstract class Element implements Comparable<Element> {

  static final Element[] EMPTY = new Element[0];

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

  private Element[] f_children = null;

  final Element[] getChildren() {
    if (f_children == null) {
      f_children = constructChildren();
    }
    return f_children;
  }

  final boolean hasChildren() {
    return getChildren().length > 0;
  }

  /**
   * Called once to construct the children of this element. If no children
   * return {@link #EMPTY}&mdash;do not return {@code null}.
   * 
   * @return the non-{@code null} children of this element.
   */
  @NonNull
  abstract Element[] constructChildren();

  /**
   * Gets the image
   * 
   * @return
   */
  abstract String getLabel();

  String getProjectOrNull() {
    return null;
  }

  String getPackageOrNull() {
    return null;
  }

  String getTypeOrNull() {
    return null;
  }

  int getLineNumber() {
    return -1;
  }

  final String getLineNumberAsStringOrNull() {
    final int line = getLineNumber();
    if (line < 1)
      return null;
    else
      return Integer.toString(line);
  }

  /**
   * The desired image decoration flags from {@link CoE_Constants} or 0 for
   * none.
   * 
   * @return desired image decoration flags from {@link CoE_Constants}.
   */
  abstract int getImageFlags();

  /**
   * The desired image name from {@link CommonImages}.
   * 
   * @return the desired image name or {@code null} for no image.
   */
  @Nullable
  abstract String getImageName();

  final Image getImage() {
    String name = getImageName();
    if (name == null)
      return null;
    final int flags = getImageFlags();
    return (new ResultsImageDescriptor(name, flags, VerificationStatusView.ICONSIZE)).getCachedImage();
  }
}
