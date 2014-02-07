package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

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
   * @param stateWrtMetricDrop
   *          the drop to add to the tree being built by this folderizer.
   * @return the new element, or {@code null} if something went wrong.
   */
  @Nullable
  public StateWrtElement addToTree(@NonNull IMetricDrop stateWrtMetricDrop) {
    if (stateWrtMetricDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "stateWrtMetricDrop"));

    int immutableFieldCount = stateWrtMetricDrop.getMetricInfoAsInt(IMetricDrop.CONCURR_IMMUTABLE_COUNT, 0);
    int threadSafeFieldCount = stateWrtMetricDrop.getMetricInfoAsInt(IMetricDrop.CONCURR_THREADSAFE_COUNT, 0);
    int lockProtectedFieldCount = stateWrtMetricDrop.getMetricInfoAsInt(IMetricDrop.CONCURR_LOCK_PROTECTED_COUNT, 0);
    int threadConfinedFieldCount = stateWrtMetricDrop.getMetricInfoAsInt(IMetricDrop.CONCURR_THREAD_CONFINED_COUNT, 0);
    int otherFieldCount = stateWrtMetricDrop.getMetricInfoAsInt(IMetricDrop.CONCURR_OTHER_COUNT, 0);
    final StateWrtElement node = createNodeFor(stateWrtMetricDrop);
    if (node != null) {
      node.f_immutableFieldCount = immutableFieldCount;
      node.f_threadSafeFieldCount = threadSafeFieldCount;
      node.f_lockProtectedFieldCount = lockProtectedFieldCount;
      node.f_threadConfinedFieldCount = threadConfinedFieldCount;
      node.f_otherFieldCount = otherFieldCount;
    }
    return node;
  }

  private final StateWrtElementScan f_scan;

  public Folderizer(String scanLabel) {
    f_scan = new StateWrtElementScan(scanLabel);
  }

  /**
   * Gets the set of root elements constructed by this, so far.
   * 
   * @return a set of root elements.
   */
  @NonNull
  public StateWrtElement[] getRootElements() {
    return new StateWrtElement[] { f_scan };
  }

  /**
   * Creates an element for a drop.
   * 
   * @param drop
   *          the metric drop to create an element representing its enclosing
   *          Java declaration.
   * @return a parent element representing the enclosing Java declaration for
   *         the passed drop.
   */
  @Nullable
  StateWrtElement createNodeFor(@NonNull IMetricDrop drop) {
    final IJavaRef javaRef = drop.getJavaRef();
    if (javaRef == null) {
      SLLogger.getLogger().log(Level.WARNING, I18N.err(313, drop));
      return null; // can't deal with this
    }
    final IDecl decl = javaRef.getDeclaration();
    /*
     * Project
     */
    final String projectName = javaRef.getEclipseProjectName();
    StateWrtElementProject project = null;
    for (StateWrtElement e : f_scan.getChildrenAsListReference()) {
      if (e instanceof StateWrtElementProject) {
        final StateWrtElementProject ep = (StateWrtElementProject) e;
        if (projectName.equals(ep.getLabel())) {
          project = ep;
          break; // found
        }
      }
    }
    if (project == null) { // need to create this project
      project = new StateWrtElementProject(f_scan, projectName);
    }
    /*
     * Package
     */
    StateWrtElementPackage pkg = null;
    final String pkgName = drop.getJavaRef().getPackageName();
    for (StateWrtElement e : project.getChildrenAsListReference()) {
      if (e instanceof StateWrtElementPackage) {
        final StateWrtElementPackage ep = (StateWrtElementPackage) e;
        if (pkgName.equals(ep.getLabel())) {
          pkg = ep;
          break; // found
        }
      }
    }
    if (pkg == null) { // need to create this package in the project
      pkg = new StateWrtElementPackage(project, pkgName);
    }
    /*
     * Compilation Unit
     */
    StateWrtElementCu cu = null;
    String cuName = drop.getJavaRef().getSimpleFileName();
    for (StateWrtElement e : pkg.getChildrenAsListReference()) {
      if (e instanceof StateWrtElementCu) {
        final StateWrtElementCu item = (StateWrtElementCu) e;
        if (item.getLabel().equals(cuName)) {
          cu = item;
          break; // found
        }
      }
    }
    if (cu == null) { // need to create this compilation unit in the package
      cu = new StateWrtElementCu(pkg, cuName);
    }

    final class MatchFolder extends DeclVisitor {

      MatchFolder(@NonNull StateWrtElementCu cu) {
        f_at = cu;
      }

      @NonNull
      private StateWrtElement f_at;

      @NonNull
      StateWrtElementJavaDecl getResult() {
        if (f_at instanceof StateWrtElementJavaDecl)
          return (StateWrtElementJavaDecl) f_at;
        else
          throw new IllegalStateException(I18N.err(317, javaRef, f_at));
      }

      private void visitNodeHelper(IDecl node) {
        for (StateWrtElement element : f_at.getChildrenAsListReference()) {
          if (element instanceof StateWrtElementJavaDecl) {
            final StateWrtElementJavaDecl ejd = (StateWrtElementJavaDecl) element;
            if (ejd.getDeclaration().equals(node)) {
              f_at = ejd;
              return; // found
            }
          }
        }
        // need to create
        final StateWrtElementJavaDecl element = new StateWrtElementJavaDecl(f_at, node);
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

    final MatchFolder matcher = new MatchFolder(cu);
    decl.acceptRootToThis(matcher);
    final StateWrtElementJavaDecl result = matcher.getResult();
    return result;
  }
}
