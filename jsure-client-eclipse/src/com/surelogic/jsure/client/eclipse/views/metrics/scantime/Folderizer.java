package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IMetricDrop;

public final class Folderizer {

  /**
   * Adds the passed drop to the tree.
   * 
   * @param scanTimeMetricDrop
   *          the drop to add to the tree being built by this folderizer.
   * @return the new element, or {@code null} if something went wrong.
   */
  @Nullable
  public ScanTimeElementLeaf addToTree(@NonNull IMetricDrop scanTimeMetricDrop) {
    if (scanTimeMetricDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "slocMetricDrop"));
    final ScanTimeElementCu cu = getParentOf(scanTimeMetricDrop);
    if (cu == null) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(313, scanTimeMetricDrop.toString()));
      return null;
    }
    int durationNs = scanTimeMetricDrop.getMetricInfoAsInt(IMetricDrop.SCAN_TIME_DURATION_NS, 0);
    if (durationNs < 0) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(311, durationNs));
      durationNs = 0;
    }
    String analysisName = scanTimeMetricDrop.getMetricInfoOrNull(IMetricDrop.SCAN_TIME_ANALYSIS_NAME);
    if (analysisName == null) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(312));
      analysisName = "(unknown analysis)";
    }
    final ScanTimeElementLeaf leaf = new ScanTimeElementLeaf(cu, analysisName, durationNs);
    cu.addChild(leaf);
    return leaf;
  }

  private final ScanTimeElementScan f_scan;

  public Folderizer(String scanLabel) {
    f_scan = new ScanTimeElementScan(scanLabel);
  }

  /**
   * Gets the set of root elements constructed by this, so far.
   * 
   * @return a set of root elements.
   */
  @NonNull
  public ScanTimeElement[] getRootElements() {
    return new ScanTimeElement[] { f_scan };
  }

  /**
   * Gets a parent element for a drop.
   * 
   * @param drop
   *          the metric drop to get a parent element representing its enclosing
   *          Java declaration.
   * @return a parent element representing the enclosing Java declaration for
   *         the passed drop.
   */
  @Nullable
  public ScanTimeElementCu getParentOf(IMetricDrop drop) {
    if (drop == null)
      return null;
    final IJavaRef javaRef = drop.getJavaRef();
    if (javaRef == null)
      return null;
    /*
     * Project
     */
    final String projectName = javaRef.getEclipseProjectName();
    ScanTimeElementProject project = null;
    for (ScanTimeElement e : f_scan.getChildrenAsListReference()) {
      if (e instanceof ScanTimeElementProject) {
        final ScanTimeElementProject ep = (ScanTimeElementProject) e;
        if (projectName.equals(ep.getLabel())) {
          project = ep;
          break; // found
        }
      }
    }
    if (project == null) { // need to create this project
      project = new ScanTimeElementProject(f_scan, projectName);
      f_scan.addChild(project);
    }
    /*
     * Package
     */
    ScanTimeElementPackage pkg = null;
    final String pkgName = drop.getJavaRef().getPackageName();
    for (ScanTimeElement e : project.getChildrenAsListReference()) {
      if (e instanceof ScanTimeElementPackage) {
        final ScanTimeElementPackage ep = (ScanTimeElementPackage) e;
        if (pkgName.equals(ep.getLabel())) {
          pkg = ep;
          break; // found
        }
      }
    }
    if (pkg == null) { // need to create this package in the project
      pkg = new ScanTimeElementPackage(project, pkgName);
      project.addChild(pkg);
    }
    /*
     * Compilation Unit
     */
    String cuName = drop.getJavaRef().getSimpleFileName();
    for (ScanTimeElement e : pkg.getChildrenAsListReference()) {
      if (e instanceof ScanTimeElementCu) {
        final ScanTimeElementCu cu = (ScanTimeElementCu) e;
        if (cu.getLabel().equals(cuName)) {
          return cu; // found
        }
      }
    }
    // need to create this compilation unit in the package
    final ScanTimeElementCu cu = new ScanTimeElementCu(pkg, cuName);
    pkg.addChild(cu);
    return cu;
  }
}
