package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.Comparator;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.ScanDifferences;
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

      return o1.getLabelToPersistViewerState().compareTo(o2.getLabelToPersistViewerState());
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

  String getPackageNameOrNull() {
    return null;
  }

  String getSimpleTypeNameOrNull() {
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
    if (e instanceof ElementHintDrop) {
      if (((ElementHintDrop) e).getDrop().getHintType() == IHintDrop.HintType.WARNING)
        return true;
    } else if (e instanceof ElementProofDrop) {
      /*
       * Stop looking here because the proof drops provide a "deep" answer. We
       * do not want to examine children in this case because this could cause
       * more of the viewer model to be built out than we need.
       */
      if (((ElementProofDrop) e).getDrop().derivedFromWarningHint())
        return true;
      else
        return false;
    }
    boolean result = false;
    for (Element c : e.getChildren()) {
      result |= searchForWarningHelper(c);
    }
    return result;
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
    if (e instanceof ElementDrop) {
      final boolean isSame = ((ElementDrop) e).isSame();
      if (!isSame)
        return true;
    }
    boolean result = false;
    for (Element c : e.getChildren()) {
      result |= searchForDifferenceHelper(c);
    }
    return result;
  }

  /**
   * The desired image name from {@link CommonImages}.
   * 
   * @return the desired image name or {@code null} for no image.
   */
  @Nullable
  abstract String getImageName();

  /**
   * Gets the the decorated image associated with this element.
   * 
   * @param name
   *          the image name from {@link CommonImages} or {@code null} for no
   *          image.
   * @param flags
   *          image decorator flags per a {@link ResultsImageDescriptor}.
   * @param withWarningDecoratorIfApplicable
   *          if {@code true} then a warning decorator is added to the returned
   *          image if {@link #descendantHasWarningHint()}.
   * @param withDeltaDecoratorIfApplicable
   *          if {@code true} then a delta decorator is added to the returned
   *          image based upon the user's preference. if {@code false} a delta
   *          decorator is never added.
   * @return an image, or {@code null} for no image.
   */
  @Nullable
  final Image getImageHelper(String name, int flags, boolean withWarningDecoratorIfApplicable,
      boolean withDeltaDecoratorIfApplicable) {
    if (name == null)
      return null;
    if (withDeltaDecoratorIfApplicable) {
      if (Element.f_highlightDifferences) {
        if (descendantHasDifference())
          flags |= CoE_Constants.DELTA;
      }
    }
    if (withWarningDecoratorIfApplicable) {
      if (descendantHasWarningHint())
        flags |= CoE_Constants.HINT_WARNING;
    }
    return (new ResultsImageDescriptor(name, flags, VerificationStatusView.ICONSIZE)).getCachedImage();
  }

  /**
   * Gets the decorated the image associated with this element.
   * 
   * @return an image, or {@code null} for no image.
   */
  @Nullable
  final Image getImage() {
    return getImageHelper(getImageName(), getImageFlags(), f_showHints, true);
  }

  @Override
  public String toString() {
    return getLabelToPersistViewerState();
  }
}
