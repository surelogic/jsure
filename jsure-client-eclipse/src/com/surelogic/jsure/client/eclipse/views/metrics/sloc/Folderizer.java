package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import java.util.List;
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
   * @param slocMetricDrop
   *          the drop to add to the tree being built by this folderizer.
   * @return the new element, or {@code null} if something went wrong.
   */
  @Nullable
  public SlocElementLeaf addToTree(@NonNull IMetricDrop slocMetricDrop) {
    if (slocMetricDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "slocMetricDrop"));
    final SlocElementPackage pkg = getParentOf(slocMetricDrop);
    if (pkg == null) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(310, slocMetricDrop.toString()));
      return null;
    }
    int blankLineCount = slocMetricDrop.getMetricInfoAsInt(IMetricDrop.SLOC_BLANK_LINE_COUNT, -1);
    int containsCommentLineCount = slocMetricDrop.getMetricInfoAsInt(IMetricDrop.SLOC_CONTAINS_COMMENT_LINE_COUNT, -1);
    int javaDeclarationCount = slocMetricDrop.getMetricInfoAsInt(IMetricDrop.SLOC_JAVA_DECLARATION_COUNT, -1);
    int javaStatementCount = slocMetricDrop.getMetricInfoAsInt(IMetricDrop.SLOC_JAVA_STATEMENT_COUNT, -1);
    int lineCount = slocMetricDrop.getMetricInfoAsInt(IMetricDrop.SLOC_LINE_COUNT, -1);
    int semicolonCount = slocMetricDrop.getMetricInfoAsInt(IMetricDrop.SLOC_SEMICOLON_COUNT, -1);
    final SlocElementLeaf leaf = new SlocElementLeaf(pkg, slocMetricDrop.getJavaRef().getSimpleFileName(), blankLineCount,
        containsCommentLineCount, javaDeclarationCount, javaStatementCount, lineCount, semicolonCount);
    pkg.addChild(leaf);
    return leaf;
  }

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
  public SlocElementPackage getParentOf(IMetricDrop drop) {
    if (drop == null)
      return null;
    final IJavaRef javaRef = drop.getJavaRef();
    if (javaRef == null)
      return null;
    /*
     * Project
     */
    final String projectName = javaRef.getEclipseProjectName();
    SlocElementProject project = null;
    for (SlocElement e : f_scan.getChildren()) {
      if (e instanceof SlocElementProject) {
        final SlocElementProject ep = (SlocElementProject) e;
        if (projectName.equals(ep.getLabel())) {
          project = ep;
          break; // found
        }
      }
    }
    if (project == null) { // need to create this project
      project = new SlocElementProject(projectName);
      f_scan.addChild(project);
    }
    /*
     * Package
     */
    final String pkgName = drop.getJavaRef().getPackageName();
    final List<SlocElement> children = project.getChildrenAsListReference();
    for (SlocElement e : children) {
      if (e instanceof SlocElementPackage) {
        final SlocElementPackage ep = (SlocElementPackage) e;
        if (pkgName.equals(ep.getLabel())) {
          return ep; // found
        }
      }
    }
    // need to create this package in the project
    final SlocElementPackage pkg = new SlocElementPackage(pkgName);
    project.addChild(pkg);
    return pkg;
  }
}
