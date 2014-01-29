package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.concurrent.TimeUnit;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.common.ui.SLImages;

/**
 * Represents SLOC metric information for a Java compilation unit.
 */
public final class ScanTimeElementLeaf extends ScanTimeElement {

  final long f_durationNs;
  final String f_durationLabel;

  ScanTimeElementLeaf(ScanTimeElement parent, String analysisName, long durationNs) {
    super(parent, analysisName);
    f_durationNs = durationNs;
    f_durationLabel = SLUtility.toStringDurationNS(durationNs, TimeUnit.NANOSECONDS);
  }

  @Override
  public ScanTimeElement[] getChildren() {
    return ScanTimeElement.EMPTY;
  }

  @Override
  public boolean highlightDueToSlocThreshold(ScanTimeOptions options) {
    final long threshold = options.getThreshold();
    final boolean showAbove = options.getThresholdShowAbove();
    final long metricValue = f_durationNs;
    return showAbove ? metricValue >= threshold : metricValue <= threshold;
  }

  @Override
  public Image getImage() {
    return SLImages.getImage(CommonImages.IMG_JSURE_VERIFY);
  }

  public void tryToOpenInJavaEditor() {
    if (getParent() == null || getParent().getParent() == null || getParent().getParent() == null)
      return; // can't figure out what to open
    /*
     * This method makes a lot of assumptions about the tree. First the leaf is
     * of the form "Foo.java" which is changed to "Foo" and assumed to be a type
     * name. Second, the parent node is a package name. Third the parent node of
     * the parent node is a project name.
     */
    String cu = getParent().getLabel();
    cu = cu.substring(0, cu.length() - 5); // take off ".java"
    String pkg = getParent().getParent().getLabel(); // package
    String proj = getParent().getParent().getParent().getLabel(); // project
    JDTUIUtility.tryToOpenInEditor(proj, pkg, cu);
  }

  @Override
  public long getDurationNs(ScanTimeOptions options) {
    return f_durationNs;
  }

  @Override
  public String getDurationAsHumanReadableString(ScanTimeOptions options) {
    return f_durationLabel;
  }
}
