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
    if (parent != null)
      parent.addChild(this);
  }

  /**
   * An empty array of {@link ScanTimeElement} objects. Should be returned by
   * {@link #constructChildren()} if an element has none.
   */
  public static final ScanTimeElement[] EMPTY = new ScanTimeElement[0];

  /*
   * The way the fields below work is that if there is a duration for this node
   * then <tt>f_durationNsCachedString</tt> will be non-null. In this case the
   * value in <tt>durationNs</tt> is meaningful.
   * 
   * if the duration is meaningful, this is most likely a leaf node. But this is
   * not true in all cases. In particular the layout of nested classes and other
   * complex nested declarations in the code base.
   */

  @NonNull
  private ArrayList<ScanTimeElement> f_children = null;
  private long f_durationNs;
  private String f_durationNsCachedString; // avoid re-computing this a lot

  final void setDurationNs(long value, String analysisName) {
    if (value < 0)
      throw new IllegalArgumentException(I18N.err(315, value));
    f_durationNs = value;
    f_durationNsCachedString = SLUtility.toStringDurationNS(value, TimeUnit.NANOSECONDS);
  }

  final void addChild(ScanTimeElement child) {
    if (child == null)
      return;
    if (f_children == null)
      f_children = new ArrayList<>();
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
   * Gets if this element should be highlighted due to the passed threshold
   * options.
   * 
   * @param options
   *          the options configured about this metric.
   * @return {@code true} if this element should highlighted, {@code false}
   *         otherwise.
   */
  public final boolean highlightDueToThreshold(ScanTimeOptions options) {
    if (f_children == null) {
      final long threshold = options.getThreshold();
      final boolean showAbove = options.getThresholdShowAbove();
      final long metricValue = f_durationNs;
      return showAbove ? metricValue >= threshold : metricValue <= threshold;
    } else {
      boolean result = false;
      /*
       * We include a guard on if the child should be displayed so we don't
       * asterisk any parents that are not showing the highlighted row.
       */
      for (ScanTimeElement element : f_children)
        result |= element.highlightDueToThreshold(options) && element.includeBasedOnAnalysisToShow(options);
      return result;
    }
  }

  /**
   * Gets if this element should be shown based upon the setting about what
   * analysis timings should be shown.
   * <p>
   * The default implementation {@code true} if any child should be shown.
   * {@link ScanTimeElementAnalysis} and {@link ScanTimeElementJavaDecl} provide
   * implementations to actually do the filter based on the passed options.
   * 
   * @param options
   *          the options configured about this metric.
   * @return {@code true} if this result should be included in the display,
   *         {@code false} otherwise.
   */
  public boolean includeBasedOnAnalysisToShow(ScanTimeOptions options) {
    for (ScanTimeElement element : f_children) {
      if (element.includeBasedOnAnalysisToShow(options))
        return true;
    }
    return false;
  }

  /**
   * Gets if this element has a meaningful duration set for it, and is not just
   * a summary node with totals.
   * 
   * @return {@code true} if this element has duration, {@code false} otherwise.
   */
  public final boolean hasDurationNs() {
    return f_durationNsCachedString != null;
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
    long result = 0;

    // handle reported duration
    if (hasDurationNs()) {
      result += f_durationNs;
    }

    // handle children, if necessary
    if (f_children != null) {
      final boolean filterResultsByThreshold = options.getFilterResultsByThreshold();
      for (ScanTimeElement element : f_children) {
        // Take filtering into account if threshold filtering is on
        boolean includeChildBasedOnThresholdFilter = !filterResultsByThreshold
            || (filterResultsByThreshold && element.highlightDueToThreshold(options));
        boolean includeChildBasedOnAnalysisToShow = element.includeBasedOnAnalysisToShow(options);
        boolean includeChild = includeChildBasedOnAnalysisToShow && includeChildBasedOnThresholdFilter;
        if (includeChild)
          result += element.getDurationNs(options);
      }
    }
    return result;
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + " : " + getLabel();
  }
}
