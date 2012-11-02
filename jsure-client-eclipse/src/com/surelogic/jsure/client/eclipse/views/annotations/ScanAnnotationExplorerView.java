package com.surelogic.jsure.client.eclipse.views.annotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTreeView;
import com.surelogic.jsure.client.eclipse.views.IJSureTreeContentProvider;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class ScanAnnotationExplorerView extends AbstractScanTreeView<ScanAnnotationExplorerView.ITypeElement> implements
    EclipseUIUtility.IContextMenuFiller {

  private final Action f_openSource = new Action() {
    @Override
    public void run() {
      final TreeViewer treeViewer = getViewer();
      if (treeViewer != null) {
        IStructuredSelection selection = (ITreeSelection) treeViewer.getSelection();
        if (selection != null) {
          handleOpenSource(selection);
        }
      }
    }
  };

  private final Action f_findType = new Action() {
    @Override
    public void run() {
      try {
        findType();
      } catch (JavaModelException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  };

  private final Action f_actionCollapseAll = new Action() {
    @Override
    public void run() {
      TreeViewer viewer = getViewer();
      if (viewer != null)
        viewer.collapseAll();
    }
  };

  public ScanAnnotationExplorerView() {
    super(SWT.NONE, ScanAnnotationExplorerView.ITypeElement.class, new ActualAnnotationsContentProvider());
  }

  @Override
  protected void makeActions() {
    f_openSource.setText("Open source");
    f_findType.setText("Find Type...");
    f_findType.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_OPEN_XML_TYPE));
    f_actionCollapseAll.setText("Collapse All");
    f_actionCollapseAll.setToolTipText("Collapse All");
    f_actionCollapseAll.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));
  }

  @Override
  protected void setupViewer(StructuredViewer viewer) {
    super.setupViewer(viewer);

    EclipseUIUtility.hookContextMenu(this, viewer, this);
  }

  public void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
    manager.add(f_openSource);
  }

  @Override
  protected void fillLocalPullDown(IMenuManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_findType);
  }

  @Override
  protected void fillLocalToolBar(IToolBarManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_findType);
  }

  void handleOpenSource(IStructuredSelection s) {
    final ITypeElement e = (ITypeElement) s.getFirstElement();
    if (e instanceof Decl) {
      Decl d = (Decl) e;
      Type t = (Type) d.getParent();
      Package p = (Package) t.getParent();
      int paren = d.getLabel().indexOf('(');
      if (paren < 0) {
        // Field?
        JDTUIUtility.tryToOpenInEditorUsingFieldName(p.getLabel(), t.getLabel(), d.getLabel());
      } else {
        JDTUIUtility.tryToOpenInEditorUsingMethodName(p.getLabel(), t.getLabel(), d.getLabel().substring(0, paren));
      }
    } else if (e instanceof Type) {
      Type t = (Type) e;
      Package p = (Package) t.getParent();
      JDTUIUtility.tryToOpenInEditor(p.getLabel(), t.getLabel());
    } else if (e instanceof Anno) {
      Anno a = (Anno) e;
      JDTUIUtility.tryToOpenInEditor(a.getJavaRef());
    }
  }

  private static final Package[] NO_ROOTS = new Package[0];

  static class ActualAnnotationsContentProvider implements IJSureTreeContentProvider {
    private Package[] roots = NO_ROOTS;

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // TODO Auto-generated method stub
    }

    @Override
    public String build() {
      /*
       * final Pattern p; if (fromSource) { p = Pattern.compile(focusTypeName);
       * } else { p = Pattern.compile(focusTypeName+"(\\$.*)*\\.class"); }
       */
      // Organize by package
      final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
      if (info == null) {
        roots = NO_ROOTS;
        return null;
      }
      final MultiMap<String, IDrop> pkgToDrop = new MultiHashMap<String, IDrop>();
      for (IPromiseDrop d : info.getPromiseDrops()) {
        final IJavaRef ref = d.getJavaRef();
        if (ref != null)
          pkgToDrop.put(ref.getPackageName(), d);
      }
      // Organize by type
      roots = new Package[pkgToDrop.size()];
      int i = 0;
      for (Map.Entry<String, Collection<IDrop>> e : pkgToDrop.entrySet()) {
        final MultiMap<String, IDrop> cuToDrop = new MultiHashMap<String, IDrop>();
        for (IDrop d : e.getValue()) {
          final IJavaRef ref = d.getJavaRef();
          if (ref != null) {
            String typeName = ref.getTypeNameOrNull();
            if (typeName != null)
              cuToDrop.put(typeName, d);
          }
        }
        roots[i] = new Package(e.getKey(), cuToDrop);
        i++;
      }
      Arrays.sort(roots);
      return info.findProjectsLabel();
    }

    @Override
    public Object[] getChildren(Object o) {
      return ((ITypeElement) o).getChildren();
    }

    @Override
    public Object getParent(Object o) {
      return ((ITypeElement) o).getParent();
    }

    @Override
    public boolean hasChildren(Object o) {
      return ((ITypeElement) o).getChildren().length > 0;
    }

    @Override
    public Object[] getElements(Object inputElement) {
      return roots;
    }

    @Override
    public Image getImage(Object o) {
      return ((ITypeElement) o).getImage();
    }

    @Override
    public String getText(Object o) {
      return ((ITypeElement) o).getLabel();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    ITypeElement findType(final IType t) {
      final String pkg = t.getPackageFragment().getElementName();
      for (Package p : roots) {
        if (pkg.equals(p.getLabel())) {
          String type = t.getElementName();
          for (ITypeElement te : p.getChildren()) {
            if (type.equals(te.getLabel())) {
              return te;
            }
          }
        }
      }
      return null;
    }
  }

  interface ITypeElement extends Comparable<ITypeElement> {
    String getLabel();

    Image getImage();

    ITypeElement getParent();

    ITypeElement[] getChildren();
  }

  private static final ITypeElement[] NO_CHILDREN = new ITypeElement[0];

  static abstract class AbstractElement implements ITypeElement {
    private final String label;
    private final ITypeElement parent;
    private final ITypeElement[] children;

    AbstractElement(ITypeElement p, String l, int size) {
      parent = p;
      label = l;
      children = size == 0 ? NO_CHILDREN : new ITypeElement[size];
    }

    @Override
    public final String getLabel() {
      return label;
    }

    @Override
    public final ITypeElement getParent() {
      return parent;
    }

    @Override
    public final ITypeElement[] getChildren() {
      return children;
    }

    public int compareTo(ITypeElement o) {
      int rv = 0;
      boolean isAnno = false;
      if (this instanceof Anno) {
        rv--;
        isAnno = true;
      }
      if (o instanceof Anno) {
        rv++;
        isAnno = true;
      }
      if (isAnno && rv == 0) {
        Anno a0 = (Anno) this;
        Anno a2 = (Anno) o;
        IJavaRef jr0 = a0.drop.getJavaRef();
        IJavaRef jr2 = a2.drop.getJavaRef();
        if (jr0 != null && jr2 != null)
          rv = jr0.getOffset() - jr2.getOffset();
      }
      if (rv == 0) {
        return getLabel().compareTo(o.getLabel());
      }
      return rv;
    }
  }

  static class Package extends AbstractElement {
    Package(String qname, MultiMap<String, IDrop> cuToDrop) {
      super(null, qname, cuToDrop.size());

      // Init types
      int i = 0;
      for (Map.Entry<String, Collection<IDrop>> e : cuToDrop.entrySet()) {
        final MultiMap<String, IDrop> idToDrop = new MultiHashMap<String, IDrop>();
        String name = e.getKey();
        for (IDrop d : e.getValue()) {
          final IJavaRef ref = d.getJavaRef();
          if (ref == null)
            continue;
          IDecl id = ref.getDeclaration();
          if (id == null)
            continue;
          idToDrop.put(DeclUtil.getEclipseJavaOutlineLikeLabel(id), d);
        }
        getChildren()[i] = new Type(this, name, idToDrop);
        i++;
      }
      Arrays.sort(getChildren());
    }

    @Override
    public Image getImage() {
      return SLImages.getImage(CommonImages.IMG_PACKAGE);
    }
  }

  static int computeTypeChildren(String name, MultiMap<String, IDrop> idToDrop) {
    Collection<IDrop> onType = idToDrop.get(name);
    if (onType == null) {
      return idToDrop.size();
    }
    return onType.size() + idToDrop.size() - 1;
  }

  static class Type extends AbstractElement {
    Type(Package p, String name, MultiMap<String, IDrop> idToDrop) {
      super(p, name, computeTypeChildren(name, idToDrop));

      // Init decls
      int i = 0;
      Collection<IDrop> onType = idToDrop.remove(name);
      if (onType != null) {
        for (IDrop d : onType) {
          getChildren()[i] = new Anno(this, d);
          i++;
        }
      }
      for (Map.Entry<String, Collection<IDrop>> e : idToDrop.entrySet()) {
        getChildren()[i] = new Decl(this, e.getKey(), e.getValue());
        i++;
      }
      Arrays.sort(getChildren());
    }

    @Override
    public Image getImage() {
      return SLImages.getImage(CommonImages.IMG_CLASS);
    }
  }

  static class Decl extends AbstractElement {
    Decl(Type t, String id, Collection<IDrop> drops) {
      super(t, id, drops.size());

      // Sort by message
      int i = 0;
      for (IDrop d : drops) {
        getChildren()[i] = new Anno(this, d);
        i++;
      }
      Arrays.sort(getChildren());
    }

    @Override
    public Image getImage() {
      return SLImages.getImage(CommonImages.IMG_GREEN_DOT);
    }
  }

  static class Anno extends AbstractElement {
    private final IDrop drop;

    public Anno(ITypeElement e, IDrop d) {
      super(e, d.getMessage(), 0);
      drop = d;
    }

    public static Point ICONSIZE = new Point(16, 16);
    public static Image consistentPromise = JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION,
        EnumSet.of(Flag.CONSISTENT), ICONSIZE);
    public static Image inconsistentPromise = JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION,
        EnumSet.of(Flag.INCONSISTENT), ICONSIZE);

    @Override
    public Image getImage() {
      if (drop instanceof IPromiseDrop) {
        IPromiseDrop p = (IPromiseDrop) drop;
        if (p.provedConsistent())
          return consistentPromise;
        else
          return inconsistentPromise;
      } else if (drop instanceof IResultDrop) {
        IResultDrop r = (IResultDrop) drop;
        if (r.provedConsistent()) {
          return SLImages.getImage(CommonImages.IMG_PLUS);
        }
        return SLImages.getImage(CommonImages.IMG_RED_X);
      } else if (drop instanceof IHintDrop) {
        IHintDrop h = (IHintDrop) drop;
        switch (h.getHintType()) {
        case INFORMATION:
          return SLImages.getImage(CommonImages.IMG_INFO);
        case WARNING:
          return SLImages.getImage(CommonImages.IMG_WARNING);
        }
      } else if (drop instanceof IProposedPromiseDrop) {
        return SLImages.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
      }
      return null;
    }

    @Nullable
    public IJavaRef getJavaRef() {
      // Find type
      ITypeElement e = getParent();
      while (!(e instanceof Type)) {
        e = e.getParent();
      }
      final Type t = (Type) e;
      final IJavaRef r = drop.getJavaRef();
      return r;
      // TODO WHAT IS THIS DOING?
      // if (r != null && t != null)
      // return new JavaRef.Builder(r).setTypeName(t.getLabel()).build();
      // else
      // return null;
    }
  }

  void findType() throws JavaModelException {
    final SelectionDialog dialog = JavaUI.createTypeDialog(EclipseUIUtility.getShell(), EclipseUIUtility.getIWorkbenchWindow(),
        SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false, "");
    dialog.setTitle("Find Annotations on a Type");

    int result = dialog.open();
    if (result != IDialogConstants.OK_ID) {
      return;
    }
    Object[] types = dialog.getResult();
    if (types == null || types.length == 0) {
      return;
    }
    // Focus on the corresponding type
    IType t = (IType) types[0];
    ActualAnnotationsContentProvider cp = (ActualAnnotationsContentProvider) getViewer().getContentProvider();
    ITypeElement focus = cp.findType(t);
    if (focus != null) {
      getViewer().reveal(focus);
      getViewer().expandToLevel(focus, 1);
    }
  }
}
