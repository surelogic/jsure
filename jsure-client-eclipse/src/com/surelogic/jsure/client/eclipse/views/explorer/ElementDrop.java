package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

public class ElementDrop extends Element {

  /**
   * Adds the passed drop to the tree.
   * 
   * @param tree
   *          a folderizer for Java element declarations.
   * @param drop
   *          any drop.
   * @return the newly created {@link ElementDrop} if successful, {@code null}
   *         if unsuccessful.
   */
  @Nullable
  static ElementDrop addToTree(@NonNull final ElementJavaDecl.Folderizer tree, @NonNull IDrop drop) {
    if (tree == null)
      throw new IllegalArgumentException(I18N.err(44, "tree"));
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    final ElementJavaDecl parent = tree.getParentOf(drop, false);
    if (parent != null) {
      final ElementDrop result = new ElementDrop(parent, drop);
      parent.addChild(result);
      return result;
    }
    return null;
  }

  private ElementDrop(Element parent, @NonNull IDrop drop) {
    super(parent);
    if (drop == null)
      throw new IllegalArgumentException(I18N.err(44, "drop"));
    f_drop = drop;
  }

  @NonNull
  private final IDrop f_drop;

  @Override
  void addChild(Element child) {
    throw new UnsupportedOperationException();
  }

  @Override
  @NonNull
  Element[] getChildren() {
    return EMPTY;
  }

  @Override
  String getLabel() {
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
  Image getElementImage() {
    return JSureDecoratedImageUtility.getImageForDrop(f_drop, true);
  }
}
