package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.SLUtility;
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

  /*
   * The way the fields below work is that if there are no children then this is
   * a leaf node and the <tt>durationNs</tt> field needs to be set by the
   * implementation.
   */

  @NonNull
  private ArrayList<ScanTimeElement> f_children = null;
  private long f_durationNs;
  private String f_durationNsCachedString; // avoid re-computing this a lot

  final void setDurationNs(long value) {
    if (value < 0)
      throw new IllegalArgumentException(I18N.err(315, value));
    if (f_children != null)
      throw new IllegalArgumentException(I18N.err(314, this.toString()));
    f_durationNs = value;
    f_durationNsCachedString = SLUtility.toStringDurationNS(value, TimeUnit.NANOSECONDS);
  }

  final void addChild(ScanTimeElement child) {
    if (child == null)
      return;
    if (f_children == null)
      f_children = new ArrayList<ScanTimeElement>();
    f_children.add(child);
  }

  @NonNull
  public final ScanTimeElement[] getChildren() {
    if (f_children == null)
      return EMPTY;
    else
      return f_children.toArray(new ScanTimeElement[f_children.size()]);
  }

  /**
   * Used for implementation efficiency in the tree. Do not mutate anything
   * passed back. Never returns null, if there are no children an empty list is
   * returned.
   * 
   * @return a reference to the child list, or an empty list if no children.
   */
  @NonNull
  final List<ScanTimeElement> getChildrenAsListReference() {
    if (f_children == null)
      return Collections.emptyList();
    else
      return f_children;
  }

  public final boolean hasChildren() {
    if (f_children == null)
      return false;
    else
      return !f_children.isEmpty();
  }

  public final boolean isLeaf() {
    return !hasChildren();
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
  public final boolean highlightDueToSlocThreshold(ScanTimeOptions options) {
    if (f_children == null) {
      final long threshold = options.getThreshold();
      final boolean showAbove = options.getThresholdShowAbove();
      final long metricValue = f_durationNs;
      return showAbove ? metricValue >= threshold : metricValue <= threshold;
    } else {
      boolean result = false;
      for (ScanTimeElement element : f_children)
        result |= element.highlightDueToSlocThreshold(options);
      return result;
    }
  }

  /**
   * Gets the duration for this element in nanoseconds. This value may change
   * based upon filtering and so on for non-leaf nodes.
   * 
   * @param options
   *          the options configured about this metric.
   * @return the duration for this element.
   */
  public final long getDurationNs(ScanTimeOptions options) {
    if (f_children == null)
      return f_durationNs;
    else {
      final boolean filterResultsByThreshold = options.getFilterResultsByThreshold();
      long result = 0;
      for (ScanTimeElement element : f_children) {
        // Take filtering into account if filtering is on
        boolean includeChild = !filterResultsByThreshold
            || (filterResultsByThreshold && element.highlightDueToSlocThreshold(options));
        if (includeChild)
          result += element.getDurationNs(options);
      }
      return result;
    }
  }

  /**
   * Gets the duration for this element in a human readable form. This value may
   * change based upon filtering and so on for non-leaf nodes.
   * 
   * @param options
   *          the options configured about this metric.
   * @return the duration for this element.
   */
  public final String getDurationAsHumanReadableString(ScanTimeOptions options) {
    if (f_children == null)
      return f_durationNsCachedString;
    else {
      return SLUtility.toStringDurationNS(getDurationNs(options), TimeUnit.NANOSECONDS);
    }
  }
}
