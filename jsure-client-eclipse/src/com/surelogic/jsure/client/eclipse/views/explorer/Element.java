package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.Comparator;
import java.util.EnumSet;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

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

  abstract void addChild(Element child);

  @NonNull
  abstract Element[] getChildren();

  final boolean hasChildren() {
    return getChildren().length > 0;
  }

  /**
   * Gets the text label which should appear in the tree portion of the viewer.
   * 
   * @return a text label.
   */
  abstract String getLabel();

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

  IJavaRef.Position getPositionRelativeToDeclarationOrNull() {
    return null;
  }

  final String getPositionRelativeToDeclarationAsStringOrNull() {
    final IJavaRef.Position pos = getPositionRelativeToDeclarationOrNull();
    if (pos != null)
      switch (pos) {
      case ON:
        return "on the declaration";
      case ON_RECEIVER:
        return "on the receiver (this)";
      case ON_RETURN_VALUE:
        return "on the return value";
      case WITHIN:
        return "within the declaration";
      }
    return null;
  }

  private EnumSet<Flag> f_descendantDecoratorFlagsCache = null;

  final EnumSet<Flag> getDescendantDecoratorFlags() {
    if (f_descendantDecoratorFlagsCache == null) {
      f_descendantDecoratorFlagsCache = descendantDecoratorFlagsHelper(this);
      /*
       * Fix up verification proof result (+ and X are in an X proof most of the
       * time)
       */
      if (f_descendantDecoratorFlagsCache.contains(Flag.INCONSISTENT))
        f_descendantDecoratorFlagsCache.remove(Flag.CONSISTENT);
    }
    return f_descendantDecoratorFlagsCache;
  }

  private EnumSet<Flag> descendantDecoratorFlagsHelper(Element e) {
    EnumSet<Flag> result = EnumSet.noneOf(Flag.class);
    if (e instanceof ElementDrop) {
      IDrop drop = ((ElementDrop) e).getDrop();
      if (drop instanceof IProofDrop) {
        IProofDrop pd = (IProofDrop) drop;
        if (pd.provedConsistent())
          result.add(Flag.CONSISTENT);
        else
          result.add(Flag.INCONSISTENT);
        if (pd.proofUsesRedDot())
          result.add(Flag.REDDOT);
      }

    } else {
      for (Element c : e.getChildren()) {
        result.addAll(descendantDecoratorFlagsHelper(c));
      }
    }
    return result;
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
    final EnumSet<Flag> flags = getDescendantDecoratorFlags();
    if (!withWarningDecoratorIfApplicable)
      flags.remove(Flag.HINT_WARNING);
    if (!withDeltaDecoratorIfApplicable)
      flags.remove(Flag.DELTA);
    if (gray)
      return JSureDecoratedImageUtility.getGrayscaleImage(baseImage, flags);
    else
      return JSureDecoratedImageUtility.getImage(baseImage, flags);
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
