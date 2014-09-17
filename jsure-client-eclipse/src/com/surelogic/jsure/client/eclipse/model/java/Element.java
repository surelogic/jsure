package com.surelogic.jsure.client.eclipse.model.java;

import java.util.Comparator;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.ScanDifferences;

public abstract class Element {

  /**
   * Compares elements by their label.
   */
  public static final Comparator<Element> ALPHA = new Comparator<Element>() {
    @Override
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
   * An empty array of {@link Element} objects. Should be returned by
   * {@link #constructChildren()} if an element has none.
   */
  public static final Element[] EMPTY = new Element[0];

  @Nullable
  private final Element f_parent;

  /**
   * Gets the parent content of this content or {@code null} if this content is
   * at the root of the tree.
   * 
   * @return the parent content of this content or {@code null}.
   */
  @Nullable
  public final Element getParent() {
    return f_parent;
  }

  /**
   * Provides scan differences to all elements, {@code null} if none no scan
   * difference information is available.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by the
   * content provider when it constructs a model of elements for a scan. It
   * cannot be changed without rebuilding the model.
   */
  public static ScanDifferences f_diff;

  /**
   * {@code true} if scan differences should be highlighted in the tree,
   * {@code false} if not.
   * <p>
   * This may be toggled on an existing model to change the display.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by the
   * content provider's <code>setHighlightDifferences(boolean)</code> method and
   * then followed by a view refresh.
   */
  public static volatile boolean f_highlightDifferences;

  protected Element(@Nullable Element parent) {
    f_parent = parent;
  }

  abstract void addChild(Element child);

  @NonNull
  public abstract Element[] getChildren();

  public final boolean hasChildren() {
    return getChildren().length > 0;
  }

  /**
   * Gets the text label which should appear in the tree portion of the viewer.
   * 
   * @return a text label.
   */
  public abstract String getLabel();

  public int getLineNumber() {
    return -1;
  }

  public final String getLineNumberAsStringOrNull() {
    final int line = getLineNumber();
    if (line < 1)
      return null;
    else
      return Integer.toString(line);
  }

  public IJavaRef.Position getPositionRelativeToDeclarationOrNull() {
    return null;
  }

  public final String getPositionRelativeToDeclarationAsStringOrNull() {
    final IJavaRef.Position pos = getPositionRelativeToDeclarationOrNull();
    if (pos != null)
      switch (pos) {
      case ON_DECL:
        return "On the declaration";
      case ON_RECEIVER:
        return "On the receiver (this)";
      case ON_RETURN_VALUE:
        return "On the return value";
      case IS_DECL:
        return "About the declaration";
      case WITHIN_DECL:
        return "About code within the declaration";
      }
    return null;
  }

  /**
   * The decorated image for this element.
   * 
   * @return the decorated image for this element.
   */
  @Nullable
  public abstract Image getElementImage();

  /**
   * Gets the decorated the image associated with this element.
   * 
   * @return an image, or {@code null} for no image.
   */
  @Nullable
  public abstract Image getImage();

  @Override
  public String toString() {
    return getLabel();
  }
}
