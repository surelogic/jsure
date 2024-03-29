package com.surelogic.jsure.client.eclipse.model.java;

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
import com.surelogic.common.ref.IDeclLambda;
import com.surelogic.common.ref.IDeclPackage;
import com.surelogic.common.ref.IDeclParameter;
import com.surelogic.common.ref.IDeclType;
import com.surelogic.common.ref.IDeclTypeParameter;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ScanDifferences;

public final class ElementJavaDecl extends ElementWithChildren {

  public static final class Folderizer {

    /**
     * Constructs a new instance with a provider for difference information and
     * preferences.
     * 
     * @param diff
     *          difference information and preferences.
     */
    public Folderizer(@Nullable ScanDifferences diff, @NonNull HighlightDifferencesSource source) {
      f_diff = diff;
      if (source == null)
        throw new IllegalArgumentException(I18N.err(44, "source"));
      f_source = source;
    }

    @Nullable
    private final ScanDifferences f_diff;

    @NonNull
    private final HighlightDifferencesSource f_source;

    /**
     * Gets the scan differences for this folderizer.
     * 
     * @return the scan differences for this folderizer.
     */
    @Nullable
    public ScanDifferences getDiff() {
      return f_diff;
    }

    @NonNull
    private final List<ElementProject> f_projects = new ArrayList<>();

    /**
     * Gets the set of root elements constructed by this, so far.
     * <p>
     * Be sure to call {@link #updateFlagsDeep()} before using this method to
     * fill the viewer.
     * 
     * @return a set of root elements.
     */
    @NonNull
    public ElementProject[] getRootElements() {
      return f_projects.toArray(new ElementProject[f_projects.size()]);
    }

    /**
     * This method helps do a deep descent into the tree after construction to
     * set flags that are used by the viewer.
     * <p>
     * This method should be invoked prior to using the results obtained from
     * {@link #getRootElements()}.
     */
    public void updateFlagsDeep() {
      for (ElementProject p : f_projects) {
        Element.updateFlagsDeep(p);
      }
    }

    /**
     * Gets a parent element for a drop.
     * 
     * @param viewDiffState
     *          difference information and preferences.
     * @param drop
     *          the drop to get a parent element representing its enclosing Java
     *          declaration.
     * @param grayscale
     *          {@code true} if non-existing Java elements should be grayscale
     *          rather than color. Use this if the passed drop is only in the
     *          old scan.
     * @return a parent element representing the enclosing Java declaration for
     *         the passed drop.
     */
    @Nullable
    public ElementJavaDecl getParentOf(final IDrop drop, final boolean grayscale) {
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
        project = new ElementProject(f_source, projectName, grayscale);
        f_projects.add(project);
      }

      final class MatchFolder extends DeclVisitor {

        MatchFolder(ElementProject project) {
          f_at = project;
        }

        @NonNull
        private ElementWithChildren f_at;

        @NonNull
        ElementJavaDecl getResult() {
          if (f_at instanceof ElementJavaDecl)
            return (ElementJavaDecl) f_at;
          else
            throw new IllegalStateException(I18N.err(267, javaRef, f_at));
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
          final ElementJavaDecl element = new ElementJavaDecl(f_at, node, grayscale);
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
        public boolean visitLambda(IDeclLambda node) {
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

      final MatchFolder matcher = new MatchFolder(project);
      decl.acceptRootToThis(matcher);
      return matcher.getResult();
    }
  }

  protected ElementJavaDecl(@NonNull Element parent, IDecl javaDecl, boolean grayscale) {
    super(parent, parent.getSource());
    f_javaDecl = javaDecl;
    if (parent != null)
      parent.addChild(this);
    f_grayscale = grayscale;
  }

  @NonNull
  private final IDecl f_javaDecl;
  private final boolean f_grayscale;

  @NonNull
  public IDecl getDeclaration() {
    return f_javaDecl;
  }

  @Override
  public String getLabel() {
    return DeclUtil.getEclipseJavaOutlineLikeLabel(f_javaDecl);
  }

  @Override
  @Nullable
  public Image getElementImage() {
    final Image baseImage = SLImages.getImageFor(f_javaDecl);
    if (f_grayscale)
      return SLImages.getGrayscaleImage(baseImage);
    else
      return baseImage;
  }
}
