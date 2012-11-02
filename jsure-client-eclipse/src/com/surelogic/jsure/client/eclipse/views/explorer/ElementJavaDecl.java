package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.DeclVisitor;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IDeclField;
import com.surelogic.common.ref.IDeclFunction;
import com.surelogic.common.ref.IDeclPackage;
import com.surelogic.common.ref.IDeclParameter;
import com.surelogic.common.ref.IDeclType;
import com.surelogic.common.ref.IDeclTypeParameter;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;

public final class ElementJavaDecl extends ElementWithChildren {

  static final class Folderizer {

    private final List<ElementProject> f_projects = new ArrayList<ElementProject>();

    @NonNull
    ElementProject[] getRootElements() {
      return f_projects.toArray(new ElementProject[f_projects.size()]);
    }

    @Nullable
    ElementJavaDecl getParentOf(final IDrop drop) {
      if (drop == null)
        return null;
      final IJavaRef javaRef = drop.getJavaRef();
      if (javaRef == null)
        return null;
      final IDecl decl = javaRef.getDeclaration();
      final String projectName = javaRef.getEclipseProjectName();
      ElementProject project = null;
      for (ElementProject ep : f_projects) {
        if (projectName.equals(ep.getLabel())) {
          project = ep;
          break; // found
        }
      }
      if (project == null) { // need to create
        project = new ElementProject(projectName);
        f_projects.add(project);
      }
      final MatchFolder matcher = new MatchFolder(project, javaRef);
      decl.acceptRootToThis(matcher);
      return matcher.getResult();
    }

    final class MatchFolder extends DeclVisitor {

      MatchFolder(ElementProject project, IJavaRef javaRef) {
        f_at = project;
        f_javaRefForReportingOnly = javaRef;
      }

      private final IJavaRef f_javaRefForReportingOnly;
      private ElementWithChildren f_at;

      @NonNull
      ElementJavaDecl getResult() {
        if (f_at instanceof ElementJavaDecl)
          return (ElementJavaDecl) f_at;
        else
          throw new IllegalStateException(I18N.err(267, f_javaRefForReportingOnly, f_at));
      }

      private void visitNodeHelper(IDecl node) {
        for (Element element : f_at.getChildrenAsListReference()) {
          if (element instanceof ElementJavaDecl) {
            final ElementJavaDecl ejd = (ElementJavaDecl) element;
            if (ejd.getDeclaration().equals(node)) {
              f_at = ejd;
              return; // found
            }
          }
        }
        // need to create
        final ElementJavaDecl element = new ElementJavaDecl(f_at, node);
        f_at = element;
        return;
      }

      @Override
      public void visitPackage(IDeclPackage node) {
        visitNodeHelper(node);
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
  }

  protected ElementJavaDecl(Element parent, IDecl javaDecl) {
    super(parent);
    f_javaDecl = javaDecl;
    if (parent != null)
      parent.addChild(this);
  }

  @NonNull
  private final IDecl f_javaDecl;

  @NonNull
  public IDecl getDeclaration() {
    return f_javaDecl;
  }

  @Override
  String getLabel() {
    return DeclUtil.getEclipseJavaOutlineLikeLabel(f_javaDecl);
  }

  @Override
  @Nullable
  Image getElementImage() {
    return SLImages.getImageFor(f_javaDecl);
  }
}
