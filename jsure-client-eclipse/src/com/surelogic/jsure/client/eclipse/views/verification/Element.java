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

  /**
   * Elements compare in the following order from greatest to least:
   * <ul>
   * <li>{@link ComparableFolder} ({@link ElementCategory} and
   * {@link ElementResultFolderDrop}) &mdash; by label.</li>
   * <li>{@link ComparableJava} ({@link ElementHintDrop},
   * {@link ElementResultDrop} and {@link ElementPromiseDrop}) &mdash; by
   * project, package, type, line number, label.</li>
   * <li>{@link ComparableProposal} ({@link ElementProposedPromiseDrop}) &mdash;
   * by label.</li>
   * </ul>
   */
  @Override
  final public int compareTo(Element o) {
    if (this instanceof ComparableFolder) {
      if (o instanceof ComparableFolder) {
        return this.getLabel().compareTo(o.getLabel());
      } else
        return 1; // this > o
    } else if (this instanceof ComparableJava) {
      if (o instanceof ComparableFolder)
        return -1; // this < o
      else if (o instanceof ComparableJava) {
        int c = nullToEmpty(this.getProjectOrNull()).compareTo(nullToEmpty(o.getProjectOrNull()));
        if (c != 0)
          return c;
        c = nullToEmpty(this.getPackageOrNull()).compareTo(nullToEmpty(o.getPackageOrNull()));
        if (c != 0)
          return c;
        c = nullToEmpty(this.getTypeOrNull()).compareTo(nullToEmpty(o.getTypeOrNull()));
        if (c != 0)
          return c;
        if (this.getLineNumber() != o.getLineNumber())
          return this.getLineNumber() - o.getLineNumber();
        if (c != 0)
          return c;
        return this.getLabel().compareTo(o.getLabel());
      } else if (o instanceof ComparableProposal)
        return 1; // this > o
    } else if (this instanceof ComparableProposal) {
      if (o instanceof ComparableProposal) {
        return this.getLabel().compareTo(o.getLabel());
      } else
        return -1; // this < o
    }
    return 1;
  }

  private String nullToEmpty(String value) {
    if (value == null)
      return "";
    else
      return value;
  }
}
