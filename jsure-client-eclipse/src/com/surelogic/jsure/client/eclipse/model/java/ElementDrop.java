package com.surelogic.jsure.client.eclipse.model.java;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.IJavaRef.Position;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

public final class ElementDrop extends Element {

  /**
   * Adds the passed drop to the tree.
   * 
   * @param tree
   *          a folderizer for Java element declarations.
   * @param drop
   *          any drop.
   * @param fromOldScan
   *          {@code true} if this element represents a drop only in the old
   *          scan, {@code false} if the drop is from the current scan.
   * @return the newly created {@link ElementDrop} if successful, {@code null}
   *         if unsuccessful.
   */
  @Nullable
  public static ElementDrop addToTree(@NonNull final ElementJavaDecl.Folderizer tree, @NonNull IDrop drop, boolean fromOldScan) {
    if (tree == null)
      throw new IllegalArgumentException(I18N.err(44, "tree"));
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    final ElementJavaDecl parent = tree.getParentOf(drop, fromOldScan);
    if (parent != null) {
      final ElementDrop result = new ElementDrop(parent, drop, tree.getDiff(), fromOldScan);
      parent.addChild(result);
      return result;
    }
    return null;
  }

  /**
   * Highlights rows in a {@link ViewerCell} based upon differences in this
   * model.
   * <p>
   * This is a utility method used by several JSure views.
   * 
   * @param cell
   *          the cell to examine and set the background color of if differences
   *          exist.
   */
  public static void highlightRowHelper(ViewerCell cell) {
    highlightRowHelper(cell, null, null);
  }

  /**
   * Highlights rows in a {@link ViewerCell} based upon differences in this
   * model.
   * <p>
   * This is a utility method used by several JSure views.
   * <p>
   * The colors passed to this method must be managed by the caller, this method
   * does not dispose of them properly.
   * 
   * @param cell
   *          the cell to examine and set the background color of if differences
   *          exist.
   * @param colorNewChanged
   *          cell background color for new or changed rows.
   * @param colorObsolete
   *          cell background color for obsolete rows.
   */
  public static void highlightRowHelper(ViewerCell cell, Color colorNewChanged, Color colorObsolete) {
    if (cell.getElement() instanceof ElementDrop) {
      final ElementDrop element = (ElementDrop) cell.getElement();
      if (element.getHighlightDifferences()) {
        if (element.isNew() || element.isChanged()) {
          if (colorNewChanged == null)
            colorNewChanged = EclipseColorUtility.getDiffHighlightColorNewChanged();
          cell.setBackground(colorNewChanged);
          return;
        }
        if (element.isOld()) {
          if (colorObsolete == null)
            colorObsolete = EclipseColorUtility.getDiffHighlightColorObsolete();
          cell.setBackground(colorObsolete);
          return;
        }
      }
    }
    cell.setBackground(null);
  }

  private ElementDrop(@NonNull Element parent, @NonNull IDrop drop, @Nullable ScanDifferences diff, boolean fromOldScan) {
    super(parent, parent.getSource());
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    f_drop = drop;
    f_fromOldScan = fromOldScan;
    if (fromOldScan) {
      f_diffDrop = f_drop;
    } else {
      if (diff == null) {
        f_diffDrop = null;
      } else {
        if (diff.isNotInOldScan(drop)) {
          f_diffDrop = drop;
        } else {
          f_diffDrop = diff.getChangedInOldScan(drop);
        }
      }
    }
  }

  private final boolean f_fromOldScan;
  @NonNull
  private final IDrop f_drop;

  @NonNull
  public IDrop getDrop() {
    return f_drop;
  }

  @Override
  void addChild(Element child) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull
  public Element[] getChildren() {
    return EMPTY;
  }

  @Override
  public String getLabel() {
    String result = f_drop.getMessage();
    if (f_drop instanceof IPromiseDrop) {
      // strip off "on" clause, if any
      final int onIndex = result.indexOf(" on ");
      if (onIndex != -1)
        result = result.substring(0, onIndex);
    }
    return result;
  }

  @Override
  @Nullable
  public final Image getElementImage() {
    return JSureDecoratedImageUtility.getImageForDrop(f_drop, true, null, isOld());
  }

  @Override
  @Nullable
  public final Image getImage() {
    final Image baseImage = getElementImage();
    return baseImage;
  }

  @Override
  public int getLineNumber() {
    final IJavaRef jr = getDrop().getJavaRef();
    return jr.getLineNumber();
  }

  @Override
  public Position getPositionRelativeToDeclarationOrNull() {
    final IJavaRef jr = getDrop().getJavaRef();
    return jr.getPositionRelativeToDeclaration();
  }

  /**
   * There are three cases:
   * <ul>
   * <li>if {@link #f_diffDrop} <tt>== null</tt> the drop is unchanged.</li>
   * <li>if {@link #f_diffDrop} <tt>==</tt> {@link #f_drop} the drop is new in
   * this scan, unless {@link #f_fromOldScan} is <tt>true</tt> in which case the
   * drop did not appear in this scan.</li>
   * <li>if {@link #f_diffDrop} <tt>!= null &&</tt> {@link #f_diffDrop}
   * <tt>!=</tt> {@link #f_drop} the drop changed&mdash;and {@link #f_diffDrop}
   * references the drop in the old scan.</li>
   * </ul>
   */
  @Nullable
  private IDrop f_diffDrop;

  public final boolean isSame() {
    return f_diffDrop == null;
  }

  public final boolean isNew() {
    return !f_fromOldScan && f_diffDrop == getDrop();
  }

  public final boolean isChanged() {
    if (f_fromOldScan)
      return false;
    return f_diffDrop != null && f_diffDrop != f_drop;
  }

  public final boolean isOld() {
    return f_fromOldScan;
  }

  @Nullable
  public final IDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Nullable
  public final Image getElementImageChangedFromDropOrNull() {
    final IDrop changedDrop = getChangedFromDropOrNull();
    if (changedDrop == null)
      return null;
    else
      return JSureDecoratedImageUtility.getImageForDrop(changedDrop, true);
  }
}
