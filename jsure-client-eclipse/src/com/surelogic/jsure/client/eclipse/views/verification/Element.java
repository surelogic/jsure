package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.Comparator;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

abstract class Element {

  /**
   * Compares elements by their label.
   */
  static final Comparator<Element> ALPHA = new Comparator<Element>() {
    public int compare(Element o1, Element o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      return o1.getLabel().compareTo(o2.getLabel());
    }
  };

  /**
   * Compares elements by Java project, package, type, line number, label.
   */
  static final Comparator<Element> JAVA = new Comparator<Element>() {
    public int compare(Element o1, Element o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      int c = nullToEmpty(o1.getProjectOrNull()).compareTo(nullToEmpty(o2.getProjectOrNull()));
      if (c != 0)
        return c;
      c = nullToEmpty(o1.getPackageOrNull()).compareTo(nullToEmpty(o2.getPackageOrNull()));
      if (c != 0)
        return c;
      c = nullToEmpty(o1.getTypeOrNull()).compareTo(nullToEmpty(o2.getTypeOrNull()));
      if (c != 0)
        return c;
      if (o1.getLineNumber() != o2.getLineNumber())
        return o1.getLineNumber() - o2.getLineNumber();
      if (c != 0)
        return c;
      return o1.getLabel().compareTo(o2.getLabel());
    }

    private String nullToEmpty(String value) {
      if (value == null)
        return "";
      else
        return value;
    }
  };

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
   * Gets the text label which should appear in the tree portion of the viewer.
   * 
   * @return a text label.
   */
  abstract String getLabel();

  /**
   * Gets a text label which, while similar to what is returned from
   * {@link #getLabel()}, should avoid specific numbers in categories and other
   * details. This label is used to persist the viewer state.
   * 
   * @return a text label
   * @see #toString()
   */
  String getLabelToPersistViewerState() {
    // TODO this should be fixed when categories actually work
    return getLabel();
  }

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

  @Override
  public String toString() {
    return getLabelToPersistViewerState();
  }
}
