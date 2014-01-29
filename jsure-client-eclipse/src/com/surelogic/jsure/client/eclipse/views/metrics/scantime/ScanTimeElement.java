package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.Comparator;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;

public abstract class ScanTimeElement {

  /**
   * Compares elements by their label.
   */
  public static final Comparator<ScanTimeElement> ALPHA = new Comparator<ScanTimeElement>() {
    @Override
    public int compare(ScanTimeElement o1, ScanTimeElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      return o1.getLabel().compareTo(o2.getLabel());
    }
  };

  protected ScanTimeElement(@Nullable ScanTimeElement parent, @NonNull String label) {
    if (label == null)
      throw new IllegalStateException(I18N.err(44, "label"));
    f_label = label;
    f_parent = parent;
  }

  /**
   * An empty array of {@link ScanTimeElement} objects. Should be returned by
   * {@link #constructChildren()} if an element has none.
   */
  public static final ScanTimeElement[] EMPTY = new ScanTimeElement[0];

  public abstract ScanTimeElement[] getChildren();

  public boolean hasChildren() {
    return getChildren().length > 0;
  }

  @Nullable
  private final ScanTimeElement f_parent;

  /**
   * Gets the parent content of this content or {@code null} if this content is
   * at the root of the tree.
   * 
   * @return parent content or {@code null}
   */
  @Nullable
  public final ScanTimeElement getParent() {
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
   * Gets if this element should be highlighted due to the passed SLOC threshold
   * options.
   * <p>
   * We do not consider rolled up values, so for "folders" the implementation
   * should consider if any children are above the threshold.
   * 
   * @param options
   *          the options configured about this metric.
   * @return {@code true} if this element should highlighted, {@code false}
   *         otherwise.
   */
  public abstract boolean highlightDueToSlocThreshold(ScanTimeOptions options);

  /**
   * Gets the duration for this element in nanoseconds. This value may change
   * based upon filtering and so on for non-leaf nodes.
   * 
   * @param options
   *          the options configured about this metric.
   * @return the duration for this element.
   */
  public abstract long getDurationNs(ScanTimeOptions options);

  /**
   * Gets the duration for this element in a human readable form. This value may
   * change based upon filtering and so on for non-leaf nodes.
   * 
   * @param options
   *          the options configured about this metric.
   * @return the duration for this element.
   */
  public abstract String getDurationAsHumanReadableString(ScanTimeOptions options);
}
