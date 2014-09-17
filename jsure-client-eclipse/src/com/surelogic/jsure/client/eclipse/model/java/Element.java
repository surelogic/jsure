package com.surelogic.jsure.client.eclipse.model.java;

import java.util.Comparator;
import java.util.EnumSet;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

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

  /**
   * {@code true} if this element has a descendant with a difference from the
   * old scan, {@code false} otherwise.
   * <p>
   * Set via {@link #updateFlagsDeep(Element)}
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
   * @param on
   *          the element to examine along with its children.
   */
  static final EnumSet<Flag> updateFlagsDeep(Element on) {
    boolean descendantHasDifference = false;
    if (on instanceof ElementDrop) {
      final boolean isSame = ((ElementDrop) on).isSame();
      if (!isSame)
        descendantHasDifference = true;
    }

    final EnumSet<Flag> result = EnumSet.noneOf(Flag.class);
    if (on instanceof ElementDrop) {
      final ElementDrop ed = (ElementDrop) on;
      if (!ed.isSame())
        result.add(Flag.DELTA);

      final IDrop drop = ed.getDrop();
      if (drop instanceof IProofDrop) {
        final IProofDrop pd = (IProofDrop) drop;
        if (pd.provedConsistent())
          result.add(Flag.CONSISTENT);
        else
          result.add(Flag.INCONSISTENT);
        if (pd.proofUsesRedDot())
          result.add(Flag.REDDOT);
        if (pd instanceof IAnalysisResultDrop) {
          final IAnalysisResultDrop ard = (IAnalysisResultDrop) pd;
          if (!ard.usedByProof()) {
            result.add(ard.provedConsistent() ? Flag.UNUSED_CONSISTENT : Flag.UNUSED_INCONSISTENT);
            result.remove(Flag.CONSISTENT);
            result.remove(Flag.INCONSISTENT);
          }
        }
      } else if (drop instanceof IHintDrop) {
        final IHintDrop hd = (IHintDrop) drop;
        if (hd.getHintType() == IHintDrop.HintType.WARNING)
          result.add(Flag.HINT_WARNING);
      }
    }

    for (Element c : on.getChildren()) {
      result.addAll(updateFlagsDeep(c));

      if (!descendantHasDifference && c.f_descendantHasDifference)
        descendantHasDifference = true;
    }

    /*
     * Fix up verification proof results in the flags (+ and X are in an X proof
     * most of the time, and so on)
     */
    if (result.contains(Flag.INCONSISTENT)) {
      result.remove(Flag.CONSISTENT);
      result.remove(Flag.UNUSED_CONSISTENT);
      result.remove(Flag.UNUSED_INCONSISTENT);
    }
    if (result.contains(Flag.CONSISTENT)) {
      result.remove(Flag.UNUSED_CONSISTENT);
      result.remove(Flag.UNUSED_INCONSISTENT);
    }

    // mutate flags
    on.f_descendantHasDifference = descendantHasDifference;
    if (on instanceof ElementWithChildren) {
      final ElementWithChildren ewc = (ElementWithChildren) on;
      ewc.f_descendantDecoratorFlags.clear();
      ewc.f_descendantDecoratorFlags.addAll(result);
    }

    return result;
  }

  static final EnumSet<Flag> getDecoratorFlagsFor(Element e) {
    EnumSet<Flag> result = EnumSet.noneOf(Flag.class);
    if (e instanceof ElementDrop) {
      final ElementDrop ed = (ElementDrop) e;
      if (!ed.isSame())
        result.add(Flag.DELTA);

      final IDrop drop = ed.getDrop();
      if (drop instanceof IProofDrop) {
        final IProofDrop pd = (IProofDrop) drop;
        if (pd.provedConsistent())
          result.add(Flag.CONSISTENT);
        else
          result.add(Flag.INCONSISTENT);
        if (pd.proofUsesRedDot())
          result.add(Flag.REDDOT);
        if (pd instanceof IAnalysisResultDrop) {
          final IAnalysisResultDrop ard = (IAnalysisResultDrop) pd;
          if (!ard.usedByProof()) {
            result.add(ard.provedConsistent() ? Flag.UNUSED_CONSISTENT : Flag.UNUSED_INCONSISTENT);
            result.remove(Flag.CONSISTENT);
            result.remove(Flag.INCONSISTENT);
          }
        }
      } else if (drop instanceof IHintDrop) {
        final IHintDrop hd = (IHintDrop) drop;
        if (hd.getHintType() == IHintDrop.HintType.WARNING)
          result.add(Flag.HINT_WARNING);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return getLabel();
  }
}
