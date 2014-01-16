package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.dropsea.IMetricDrop;

public final class Folderizer {

  private final SlocElementScan f_scan;

  public Folderizer(String scanLabel) {
    f_scan = new SlocElementScan(scanLabel);
  }

  /**
   * Gets the set of root elements constructed by this, so far.
   * 
   * @return a set of root elements.
   */
  @NonNull
  public SlocElement[] getRootElements() {
    return new SlocElement[] { f_scan };
  }

  @Nullable
  public SlocElementWithChildren getParentOf(IMetricDrop drop) {
    String eclipseProjectName = drop.getJavaRef().getEclipseProjectNameOrEmpty();
    String packageName = drop.getJavaRef().getPackageName();
    return null;
  }
}
