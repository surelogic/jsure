package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;

public abstract class SlocElement {

  protected SlocElement(@Nullable SlocElement parent, @NonNull String label) {
    if (label == null)
      throw new IllegalStateException(I18N.err(44, "label"));
    f_label = label;
    f_parent = parent;
  }

  /**
   * An empty array of {@link SlocElement} objects. Should be returned by
   * {@link #constructChildren()} if an element has none.
   */
  public static final SlocElement[] EMPTY = new SlocElement[0];

  public abstract SlocElement[] getChildren();

  public boolean hasChildren() {
    return getChildren().length > 0;
  }

  @Nullable
  private final SlocElement f_parent;

  /**
   * Gets the parent content of this content or {@code null} if this content is
   * at the root of the tree.
   * 
   * @return parent content or {@code null}
   */
  @Nullable
  public final SlocElement getParent() {
    return f_parent;
  }

  @NonNull
  final String f_label;

  /**
   * Gets the text label which should appear in the tree portion of the viewer.
   * 
   * @return a text label.
   */
  public String getLabel() {
    return f_label;
  }

  /**
   * Gets the decorated the image associated with this element.
   * 
   * @return an image, or {@code null} for no image.
   */
  @Nullable
  public abstract Image getImage();

  /**
   * Gets if this element should be highlighted as being above the passed SLOC
   * threshold.
   * <p>
   * We do not consider rolled up values, so for "folders" the implementation
   * should consider if any children are above the threshold.
   * 
   * @param slocThreshold
   *          a SLOC threshold.
   * @return {@code true} if this element should highlighted, {@code false}
   *         otherwise.
   */
  public abstract boolean aboveSlocThreshold(int slocThreshold);

  /*
   * Counts should be set correctly by each implementation.
   */

  long f_blankLineCount;
  long f_containsCommentLineCount;
  long f_javaDeclarationCount;
  long f_javaStatementCount;
  long f_lineCount;
  long f_semicolonCount;

  public final long getBlankLineCount() {
    return f_blankLineCount;
  }

  public final long getContainsCommentLineCount() {
    return f_containsCommentLineCount;
  }

  public final long getJavaDeclarationCount() {
    return f_javaDeclarationCount;
  }

  public final long getJavaStatementCount() {
    return f_javaStatementCount;
  }

  public final long getLineCount() {
    return f_lineCount;
  }

  public final long getSemicolonCount() {
    return f_semicolonCount;
  }
}
