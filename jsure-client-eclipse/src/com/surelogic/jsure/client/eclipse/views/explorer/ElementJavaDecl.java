package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
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

public final class ElementJavaDecl extends Element {

  static final class Folderizer {

    private final List<ElementJavaDecl> f_packages = new ArrayList<ElementJavaDecl>();

    @NonNull
    ElementJavaDecl[] getRootElements() {
      return f_packages.toArray(new ElementJavaDecl[f_packages.size()]);
    }

    @Nullable
    ElementJavaDecl getParentOf(final IDrop drop) {
      if (drop == null)
        return null;
      final IJavaRef javaRef = drop.getJavaRef();
      if (javaRef == null)
        return null;
      final IDecl decl = javaRef.getDeclaration();
      final MatchFolder matcher = new MatchFolder();
      decl.acceptRootToThis(matcher);
      return matcher.getResult();
    }

    final class MatchFolder extends DeclVisitor {

      private ElementJavaDecl f_at;

      @NonNull
      ElementJavaDecl getResult() {
        if (f_at == null)
          throw new IllegalStateException("MatchFolder has null result...was it accepted on an IDecl?");
        return f_at;
      }

      @Override
      public void visitPackage(IDeclPackage node) {
        for (ElementJavaDecl element : f_packages) {
          if (element.getDeclaration().equals(node)) {
            f_at = element;
            return; // found
          }
        }
        // need to create
        final ElementJavaDecl element = new ElementJavaDecl(null, node);
        f_packages.add(element);
        f_at = element;
      }

      private void visitNodeHelper(IDecl node) {
        for (Element element : f_at.f_children) {
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

  protected ElementJavaDecl(ElementJavaDecl parent, IDecl javaDecl) {
    super(parent);
    f_javaDecl = javaDecl;
    if (parent != null)
      parent.addChild(this);
  }

  private final ArrayList<Element> f_children = new ArrayList<Element>();

  void addChild(Element child) {
    if (child == null)
      return;
    f_children.add(child);
  }

  @Override
  @NonNull
  Element[] getChildren() {
    return f_children.toArray(new Element[f_children.size()]);
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
