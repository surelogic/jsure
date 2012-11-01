package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.Comparator;
import java.util.EnumSet;

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
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

abstract class Element {

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
   * {@link VerificationExplorerViewContentProvider} when it constructs a model
   * of elements for a scan.
   */
  static boolean f_showHints;

  /**
   * Provides scan differences to all elements, {@code null} if none no scan
   * difference information is available.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by
   * {@link VerificationExplorerViewContentProvider} when it constructs a model
   * of elements for a scan.
   */
  static ScanDifferences f_diff;

  /**
   * {@code true} if scan differences should be highlighted in the tree,
   * {@code false} if not.
   * <p>
   * This may be toggled on an existing model to change the display.
   * <p>
   * <i>Implementation Note:</i> This field should <b>only</b> be set by
   * {@link VerificationExplorerViewContentProvider} when it constructs a model
   * of elements for a scan.
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

  private Boolean f_descendantHasWarningHintCache = null;

  /**
   * Checks if this element has a descendant with a warning hint about it. This
   * is used by the UI to show a label decorator, if the user desires it, path
   * along the tree to the warning.
   * 
   * @return {@code true} if this element has a descendant with a warning hint
   *         about it, {@code false} otherwise.
   */
  final boolean descendantHasWarningHint() {
    if (f_descendantHasWarningHintCache == null)
      f_descendantHasWarningHintCache = searchForWarningHelper(this);
    return f_descendantHasWarningHintCache;
  }

  /**
   * Helper method to determine the answer for
   * {@link #descendantHasWarningHint()} for any element.
   * 
   * @param e
   *          any element.
   * @return {@code true} if this element has a descendant with a warning hint
   *         about it, {@code false} otherwise.
   */
  private final boolean searchForWarningHelper(Element e) {
    return false; // TODO
//    if (e instanceof ElementHintDrop) {
//      if (((ElementHintDrop) e).getDrop().getHintType() == IHintDrop.HintType.WARNING)
//        return true;
//    } else if (e instanceof ElementProofDrop) {
//      /*
//       * Stop looking here because the proof drops provide a "deep" answer. We
//       * do not want to examine children in this case because this could cause
//       * more of the viewer model to be built out than we need.
//       */
//      if (((ElementProofDrop) e).getDrop().derivedFromWarningHint())
//        return true;
//      else
//        return false;
//    }
//    boolean result = false;
//    for (Element c : e.getChildren()) {
//      result |= searchForWarningHelper(c);
//    }
//    return result;
  }

  private Boolean f_descendantHasDifferenceCache = null;

  /**
   * Checks if this element has a descendant with a difference from the old
   * scan. This is used by the UI to highlight a path to the difference, if the
   * user desires it.
   * 
   * @return {@code true} if this element has a descendant with a difference
   *         from the old scan, {@code false} otherwise.
   */
  final boolean descendantHasDifference() {
    if (f_descendantHasDifferenceCache == null)
      f_descendantHasDifferenceCache = searchForDifferenceHelper(this);
    return f_descendantHasDifferenceCache;
  }

  /**
   * Helper method to determine the answer for
   * {@link #descendantHasDifference()} for any element.
   * 
   * @param e
   *          any element.
   * @return {@code true} if this element has a descendant with a difference
   *         from the old scan, {@code false} otherwise.
   */
  private final boolean searchForDifferenceHelper(Element e) {
    return false; // TODO
//    if (e instanceof ElementDrop) {
//      final boolean isSame = ((ElementDrop) e).isSame();
//      if (!isSame)
//        return true;
//    }
//    boolean result = false;
//    for (Element c : e.getChildren()) {
//      result |= searchForDifferenceHelper(c);
//    }
//    return result;
  }

  /**
   * The decorated image for this element.
   * 
   * @return the decorated image for this element.
   */
  @Nullable
  abstract Image getElementImage();

  /**
   * Helps get a complete decorated image associated with this element. This
   * includes warning and delta decorations.
   * 
   * @param withWarningDecoratorIfApplicable
   *          if {@code true} then a warning decorator is added to the returned
   *          image if {@link #descendantHasWarningHint()}.
   * @param withDeltaDecoratorIfApplicable
   *          if {@code true} then a delta decorator is added to the returned
   *          image based upon the user's preference. if {@code false} a delta
   *          decorator is never added.
   * @return an image, or {@code null} for none.
   */
  @Nullable
  final Image getImageHelper(boolean gray, boolean withWarningDecoratorIfApplicable, boolean withDeltaDecoratorIfApplicable) {
    final Image baseImage = getElementImage();
    if (baseImage == null)
      return null;
    ImageDescriptor decorator = null;
    if (withWarningDecoratorIfApplicable) {
      if (descendantHasWarningHint())
        decorator = SLImages.getImageDescriptor(CommonImages.DECR_WARNING);
    }
    if (withDeltaDecoratorIfApplicable) {
      if (Element.f_highlightDifferences) {
        if (descendantHasDifference())
          decorator = SLImages.getImageDescriptor(CommonImages.DECR_DELTA);
      }
    }
    return SLImages.getDecoratedImage(baseImage, new ImageDescriptor[] { null, null, null, decorator, null },
        JSureDecoratedImageUtility.SIZE, gray);
  }

  /**
   * Gets the decorated the image associated with this element.
   * 
   * @return an image, or {@code null} for no image.
   */
  @Nullable
  final Image getImage() {
    return getImageHelper(false, f_showHints, true);
  }

  @Override
  public String toString() {
    return getLabel();
  }
}
