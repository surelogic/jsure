package com.surelogic.jsure.client.eclipse.views.status;

import java.util.Comparator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

abstract class Element {

  /**
   * Compares elements by their label.
   */
  static final Comparator<Element> ALPHA = new Comparator<Element>() {
    @Override
    public int compare(Element o1, Element o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      return o1.getLabelToPersistViewerState().compareTo(o2.getLabelToPersistViewerState());
    }
  };

  /**
   * Compares elements by Java project, package, type, line number, label.
   */
  static final Comparator<Element> JAVA = new Comparator<Element>() {
    @Override
    public int compare(Element o1, Element o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      // special logic to put hints folder at the bottom (only at viewer root)
      if (o1.getParent() == null) {
        if (o1 instanceof ElementCategory) {
          if (ElementCategory.SPECIAL_HINT_FOLDER_NAME.equals(o1.getLabelToPersistViewerState()))
            return 1;
        }
      }
      if (o2.getParent() == null) {
        if (o2 instanceof ElementCategory) {
          if (ElementCategory.SPECIAL_HINT_FOLDER_NAME.equals(o2.getLabelToPersistViewerState()))
            return -1;
        }
      }

      int c = SLUtility.nullToEmpty(o1.getProjectNameOrNull()).compareTo(SLUtility.nullToEmpty(o2.getProjectNameOrNull()));
      if (c != 0)
        return c;
      c = SLUtility.nullToEmpty(o1.getPackageNameOrNull()).compareTo(SLUtility.nullToEmpty(o2.getPackageNameOrNull()));
      if (c != 0)
        return c;
      c = SLUtility.nullToEmpty(o1.getSimpleTypeNameOrNull()).compareTo(SLUtility.nullToEmpty(o2.getSimpleTypeNameOrNull()));
      if (c != 0)
        return c;
      if (o1.getLineNumber() != o2.getLineNumber())
        return o1.getLineNumber() - o2.getLineNumber();
      if (c != 0)
        return c;
      return o1.getLabelToPersistViewerState().compareTo(o2.getLabelToPersistViewerState());
    }
  };

  /**
   * An empty array of {@link Element} objects. Should be returned by
   * {@link #constructChildren()} if an element has none.
   */
  static final Element[] EMPTY = new Element[0];

  /**
   * {@code true} if hints are to be shown by all elements, {@code false}
   * otherwise.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by
   * {@link VerificationStatusViewContentProvider} when it constructs a model of
   * elements for a scan.
   */
  static boolean f_showHints;

  /**
   * Provides scan differences to all elements, {@code null} if none no scan
   * difference information is available.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by
   * {@link VerificationStatusViewContentProvider} when it constructs a model of
   * elements for a scan.
   */
  static ScanDifferences f_diff;

  /**
   * {@code true} if scan differences should be highlighted in the tree,
   * {@code false} if not.
   * <p>
   * This may be toggled on an existing model to change the display.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by
   * {@link VerificationStatusViewContentProvider} when it constructs a model of
   * elements for a scan.
   */
  static volatile boolean f_highlightDifferences;

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
   * <p>
   * This method returns the same result as {@link #getLabel()} except for
   * folders and categories.
   * 
   * @return a text label
   * @see #toString()
   */
  String getLabelToPersistViewerState() {
    return getLabel();
  }

  String getProjectNameOrNull() {
    return null;
  }

  Image getProjectImageOrNull() {
    return null;
  }

  String getPackageNameOrNull() {
    return null;
  }

  String getSimpleTypeNameOrNull() {
    return null;
  }

  Image getSimpleTypeImageOrNull() {
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
   * {@code true} if this element has a descendant with a warning hint about it,
   * {@code false} otherwise. *
   * <p>
   * Set via {@link #updateFlagsDeepHelper(Element)}
   */
  private boolean f_descendantHasWarningHint;

  /**
   * Checks if this element has a descendant with a warning hint about it. This
   * is used by the UI to show a label decorator, if the user desires it, path
   * along the tree to the warning.
   * 
   * @return {@code true} if this element has a descendant with a warning hint
   *         about it, {@code false} otherwise.
   */
  final boolean descendantHasWarningHint() {
    return f_descendantHasWarningHint;
  }

  /**
   * {@code true} if this element has a descendant with a difference from the
   * old scan, {@code false} otherwise.
   * <p>
   * Set via {@link #updateFlagsDeepHelper(Element)}
   */
  private boolean f_descendantHasDifference;

  /**
   * Checks if this element has a descendant with a difference from the old
   * scan. This is used by the UI to highlight a path to the difference, if the
   * user desires it.
   * 
   * @return {@code true} if this element has a descendant with a difference
   *         from the old scan, {@code false} otherwise.
   */
  final boolean descendantHasDifference() {
    return f_descendantHasDifference;
  }

  /**
   * This method helps do a deep descent into the tree after construction to set
   * flags that are used by the viewer.
   * <p>
   * If any new flags are added they should get set here so the whole-tree
   * traversal is just once, not many times.
   * 
   * @param e
   *          the element to examine along with its children.
   */
  static final void updateFlagsDeepHelper(Element e) {
    boolean descendantHasDifference = false;
    if (e instanceof ElementDrop) {
      final boolean isSame = ((ElementDrop) e).isSame();
      if (!isSame)
        descendantHasDifference = true;
    }

    boolean descendantHasWarningHint = false;
    boolean warningHintDone = false;
    if (e instanceof ElementHintDrop) {
      if (((ElementHintDrop) e).getDrop().getHintType() == IHintDrop.HintType.WARNING) {
        warningHintDone = descendantHasWarningHint = true;
      }
    } else if (e instanceof ElementProofDrop) {
      /*
       * Stop looking here because the proof drops provide a "deep" answer. We
       * do not want to examine children in this case because this could cause
       * more of the viewer model to be built out than we need.
       */
      descendantHasWarningHint = ((ElementProofDrop) e).getDrop().derivedFromWarningHint();
      warningHintDone = true;
    }

    for (Element c : e.getChildren()) {
      updateFlagsDeepHelper(c);

      if (!descendantHasDifference && c.f_descendantHasDifference)
        descendantHasDifference = true;

      if (!warningHintDone && c.f_descendantHasWarningHint) {
        warningHintDone = descendantHasWarningHint = true;
      }
    }
    // mutate flags
    e.f_descendantHasDifference = descendantHasDifference;
    e.f_descendantHasWarningHint = descendantHasWarningHint;
  }

  /**
   * The decorated image for this element.
   * 
   * @return the decorated image for this element.
   */
  @Nullable
  abstract Image getElementImage();

  /**
   * Gets the decorated the image associated with this element.
   * 
   * @return an image, or {@code null} for no image.
   */
  @Nullable
  final Image getImage() {
    final Image baseImage = getElementImage();
    if (baseImage == null)
      return null;
    ImageDescriptor decorator = null;
    if (f_showHints) {
      if (descendantHasWarningHint())
        decorator = SLImages.getImageDescriptor(CommonImages.DECR_WARNING);
    }
    if (Element.f_highlightDifferences) {
      if (descendantHasDifference())
        decorator = SLImages.getImageDescriptor(CommonImages.DECR_DELTA);
    }
    return SLImages.getDecoratedImage(baseImage, new ImageDescriptor[] { null, null, null, decorator, null },
        JSureDecoratedImageUtility.SIZE);
  }

  @Override
  public String toString() {
    return getLabelToPersistViewerState();
  }
}
