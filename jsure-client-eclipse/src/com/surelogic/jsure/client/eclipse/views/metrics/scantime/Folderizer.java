package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.DeclVisitor;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IDeclField;
import com.surelogic.common.ref.IDeclFunction;
import com.surelogic.common.ref.IDeclPackage;
import com.surelogic.common.ref.IDeclParameter;
import com.surelogic.common.ref.IDeclType;
import com.surelogic.common.ref.IDeclTypeParameter;
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
  public ScanTimeElement addToTree(@NonNull IMetricDrop scanTimeMetricDrop) {
    if (scanTimeMetricDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "scanTimeMetricDrop"));

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
    final ScanTimeElement node = createNodeFor(scanTimeMetricDrop, analysisName, durationNs);
    return node;
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
   * Creates an element for a drop.
   * 
   * @param drop
   *          the metric drop to create an element representing its enclosing
   *          Java declaration.
   * @param analysisName
   *          the name of the verifying analysis the timing information is
   *          about.
   * @param durationNs
   *          the duration of the analysis in nanoseconds.
   * @return a parent element representing the enclosing Java declaration for
   *         the passed drop.
   */
  @Nullable
  ScanTimeElement createNodeFor(@NonNull IMetricDrop drop, @NonNull String analysisName, long durationNs) {
    final IJavaRef javaRef = drop.getJavaRef();
    if (javaRef == null) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(316, drop));
      return null; // can't deal with this
    }
    final IDecl decl = javaRef.getDeclaration();
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
    }
    /*
     * Compilation Unit
     */
    ScanTimeElementCu cu = null;
    String cuName = drop.getJavaRef().getSimpleFileName();
    for (ScanTimeElement e : pkg.getChildrenAsListReference()) {
      if (e instanceof ScanTimeElementCu) {
        final ScanTimeElementCu item = (ScanTimeElementCu) e;
        if (item.getLabel().equals(cuName)) {
          cu = item;
          break; // found
        }
      }
    }
    if (cu == null) { // need to create this compilation unit in the package
      cu = new ScanTimeElementCu(pkg, cuName);
    }
    // Are we done?
    if (javaRef.getDeclaration().getKind() == IDecl.Kind.PACKAGE) {
      /*
       * If the declaration is a package then we are dealing with timing
       * information reported about an analysis at the compilation unit level.
       * Hence there is no lower structure. We return the compilation unit as
       * the parent.
       */
      ScanTimeElementAnalysis result = new ScanTimeElementAnalysis(cu, analysisName);
      result.setDurationNs(durationNs, analysisName);
      return result;
    }
    /*
     * Verifying Analysis
     */
    ScanTimeElementAnalysis va = null;
    for (ScanTimeElement e : cu.getChildrenAsListReference()) {
      if (e instanceof ScanTimeElementAnalysis) {
        final ScanTimeElementAnalysis item = (ScanTimeElementAnalysis) e;
        if (analysisName.equals(item.getLabel())) {
          va = item;
          break; // found
        }
      }
    }
    if (va == null) { // need to create this analysis in the cu
      va = new ScanTimeElementAnalysis(cu, analysisName);
    }

    final class MatchFolder extends DeclVisitor {

      MatchFolder(ScanTimeElementAnalysis va) {
        f_at = va;
      }

      @NonNull
      private ScanTimeElement f_at;

      @NonNull
      ScanTimeElementJavaDecl getResult() {
        if (f_at instanceof ScanTimeElementJavaDecl)
          return (ScanTimeElementJavaDecl) f_at;
        else
          throw new IllegalStateException(I18N.err(317, javaRef, f_at));
      }

      private void visitNodeHelper(IDecl node) {
        for (ScanTimeElement element : f_at.getChildrenAsListReference()) {
          if (element instanceof ScanTimeElementJavaDecl) {
            final ScanTimeElementJavaDecl ejd = (ScanTimeElementJavaDecl) element;
            if (ejd.getDeclaration().equals(node)) {
              f_at = ejd;
              return; // found
            }
          }
        }
        // need to create
        final ScanTimeElementJavaDecl element = new ScanTimeElementJavaDecl(f_at, node);
        f_at = element;
        return;
      }

      @Override
      public void visitPackage(IDeclPackage node) {
        // visitNodeHelper(node);
      }

      @Override
      public boolean visitClass(IDeclType node) {
        visitNodeHelper(node);
        return false;
      }

      @Override
      public boolean visitInterface(IDeclType node) {
        visitNodeHelper(node);
        return false;
      }

      @Override
      public void visitAnnotation(IDeclType node) {
        visitNodeHelper(node);
      }

      @Override
      public void visitEnum(IDeclType node) {
        visitNodeHelper(node);
      }

      @Override
      public void visitField(IDeclField node) {
        visitNodeHelper(node);
      }

      @Override
      public void visitInitializer(IDecl node) {
        visitNodeHelper(node);
      }

      @Override
      public boolean visitMethod(IDeclFunction node) {
        visitNodeHelper(node);
        return false;
      }

      @Override
      public boolean visitConstructor(IDeclFunction node) {
        visitNodeHelper(node);
        return false;
      }

      @Override
      public void visitParameter(IDeclParameter node, boolean partOfDecl) {
        if (partOfDecl)
          visitNodeHelper(node);
      }

      @Override
      public void visitTypeParameter(IDeclTypeParameter node, boolean partOfDecl) {
        if (partOfDecl)
          visitNodeHelper(node);
      }
    }

    final MatchFolder matcher = new MatchFolder(va);
    decl.acceptRootToThis(matcher);
    final ScanTimeElementJavaDecl result = matcher.getResult();
    result.setDurationNs(durationNs, analysisName);
    return result;
  }
}
