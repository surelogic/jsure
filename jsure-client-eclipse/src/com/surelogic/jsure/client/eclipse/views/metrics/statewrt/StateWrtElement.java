package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;

public abstract class StateWrtElement {

  /**
   * Compares elements by their label.
   */
  public static final Comparator<StateWrtElement> ALPHA = new Comparator<StateWrtElement>() {
    @Override
    public int compare(StateWrtElement o1, StateWrtElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      return o1.getLabel().compareTo(o2.getLabel());
    }
  };

  protected StateWrtElement(@Nullable StateWrtElement parent, @NonNull String label) {
    if (label == null)
      throw new IllegalStateException(I18N.err(44, "label"));
    f_label = label;
    f_parent = parent;
    if (parent != null)
      parent.addChild(this);
  }

  /**
   * An empty array of {@link StateWrtElement} objects. Should be returned by
   * {@link #constructChildren()} if an element has none.
   */
  public static final StateWrtElement[] EMPTY = new StateWrtElement[0];

  @NonNull
  private ArrayList<StateWrtElement> f_children = null;

  final void addChild(StateWrtElement child) {
    if (child == null)
      return;
    if (f_children == null)
      f_children = new ArrayList<StateWrtElement>();
    f_children.add(child);
  }

  @NonNull
  public final StateWrtElement[] getChildren() {
    if (f_children == null)
      return EMPTY;
    else
      return f_children.toArray(new StateWrtElement[f_children.size()]);
  }

  /**
   * Used for implementation efficiency in the tree. Do not mutate anything
   * passed back. Never returns null, if there are no children an empty list is
   * returned.
   * 
   * @return a reference to the child list, or an empty list if no children.
   */
  @NonNull
  final List<StateWrtElement> getChildrenAsListReference() {
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
  private final StateWrtElement f_parent;

  /**
   * Gets the parent content of this content or {@code null} if this content is
   * at the root of the tree.
   * 
   * @return parent content or {@code null}
   */
  @Nullable
  public final StateWrtElement getParent() {
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
  public final boolean highlightDueToThreshold(StateWrtOptions options) {
    final long threshold = options.getThreshold();
    final boolean showAbove = options.getThresholdShowAbove();
    final long metricValue;
    switch (options.getSelectedColumnTitleIndex()) {
    case 1: // No declared policy
      metricValue = getOtherFieldCount();
      break;
    case 2: // @Immutable
      metricValue = getImmutableFieldCount();
      break;
    case 3: // @ThreadSafe
      metricValue = getThreadSafeFieldCount();
      break;
    case 4: // @RegionLock/@GuardedBy
      metricValue = getLockProtectedFieldCount();
      break;
    case 5: // @ThreadConfined
      metricValue = getThreadConfinedFieldCount();
      break;
    case 6: // @NotThreadSafe
      metricValue = getNotThreadSafeFieldCount();
      break;
    default: // Total (0 and default)
      metricValue = getFieldCountTotal();
      break;
    }
    return showAbove ? metricValue >= threshold : metricValue <= threshold;
  }

  /**
   * {@code true} indicates that this result has values from a metric drop,
   * {@code indicates} it shows aggregate data.
   */
  boolean f_hasDirectMetricData = false;

  /*
   * Counts should be set correctly by each implementation.
   */

  int f_immutableFieldCount = 0;
  int f_threadSafeFieldCount = 0;
  int f_notThreadSafeFieldCount = 0;
  int f_lockProtectedFieldCount = 0;
  int f_threadConfinedFieldCount = 0;
  int f_otherFieldCount = 0;

  public final int getFieldCountTotal() {
    return f_immutableFieldCount + f_threadSafeFieldCount + f_notThreadSafeFieldCount + f_lockProtectedFieldCount
        + f_threadConfinedFieldCount + f_otherFieldCount;
  }

  public final int getImmutableFieldCount() {
    return f_immutableFieldCount;
  }

  public final int getThreadSafeFieldCount() {
    return f_threadSafeFieldCount;
  }

  public final int getNotThreadSafeFieldCount() {
    return f_notThreadSafeFieldCount;
  }

  public final int getLockProtectedFieldCount() {
    return f_lockProtectedFieldCount;
  }

  public final int getThreadConfinedFieldCount() {
    return f_threadConfinedFieldCount;
  }

  public final int getOtherFieldCount() {
    return f_otherFieldCount;
  }

  final void setupTotalsForChildren() {
    if (f_hasDirectMetricData) {
      for (StateWrtElement child : getChildrenAsListReference()) {
        child.setupTotalsForChildren();
      }
    } else {
      for (StateWrtElement child : getChildrenAsListReference()) {
        setupTotalsHelper(this, child);
        child.setupTotalsForChildren();
      }
    }
  }

  static void setupTotalsHelper(StateWrtElement on, StateWrtElement toAdd) {
    if (toAdd.f_hasDirectMetricData) {
      on.f_immutableFieldCount += toAdd.f_immutableFieldCount;
      on.f_threadSafeFieldCount += toAdd.f_threadSafeFieldCount;
      on.f_notThreadSafeFieldCount += toAdd.f_notThreadSafeFieldCount;
      on.f_lockProtectedFieldCount += toAdd.f_lockProtectedFieldCount;
      on.f_threadConfinedFieldCount += toAdd.f_threadConfinedFieldCount;
      on.f_otherFieldCount += toAdd.f_otherFieldCount;
    }
    for (StateWrtElement child : toAdd.getChildrenAsListReference()) {
      setupTotalsHelper(on, child);
    }
  }
}
