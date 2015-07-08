package com.surelogic.jsure.client.eclipse.views.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.UIElement;

import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.jsure.client.eclipse.editors.IJSureTreeContentProvider;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLContentProvider;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;
import com.surelogic.jsure.core.xml.PromisesLibMerge;
import com.surelogic.xml.*;
import com.surelogic.xml.MethodElement;
import com.surelogic.xml.PackageElement;
import com.surelogic.xml.PromisesXMLParser;
import com.surelogic.xml.PromisesXMLReader;
import com.surelogic.xml.TestXMLParserConstants;

public class XMLExplorerView extends AbstractSLView implements EclipseUIUtility.IContextMenuFiller {

  static abstract class AbstractHandlerElementUpdater extends AbstractHandler implements IElementUpdater {
  };

  final Provider f_content = new Provider();

  TreeViewer f_viewer;

  abstract class SingleElementAction extends Action {
    SingleElementAction(String label) {
      super(label);
    }

    @Override
    public void run() {
      final TreeViewer treeViewer = f_viewer;
      if (treeViewer != null) {
        IStructuredSelection selection = (ITreeSelection) treeViewer.getSelection();
        if (selection != null && !selection.isEmpty()) {
          run(selection.getFirstElement());
        }
      }
    }

    abstract void run(Object o);
  }

  private final Action f_openXmlEditor = new SingleElementAction(I18N.msg("jsure.eclipse.view.open_xml_editor")) {
    @Override
    public void run(Object o) {
      handleDoubleClick(o);
    }
  };

  private final Action f_openSource = new SingleElementAction(I18N.msg("jsure.eclipse.view.open_in_editor")) {
    @Override
    public void run(Object o) {
      handleOpenSource(o);
    }
  };

  private final Action f_copyAnnos = new SingleElementAction("Copy Annotations") {
    @Override
    public void run(Object o) {
      if (o instanceof IJavaElement) {
        getClipboard().setFocus((IJavaElement) o);
      } else if (o instanceof Type) {
        Type t = (Type) o;
        getClipboard().setFocus(t.getRoot().getClassElement());
      }
    }
  };

  private final Action f_actionExpand = new Action() {
    @Override
    public void run() {
      final TreeViewer treeViewer = f_viewer;
      if (treeViewer != null) {
        final ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
        if (selection == null || selection.isEmpty()) {
          treeViewer.expandToLevel(50);
        } else {
          for (Object obj : selection.toList()) {
            if (obj != null) {
              treeViewer.expandToLevel(obj, 50);
            } else {
              treeViewer.expandToLevel(50);
            }
          }
        }
      }
    }
  };

  private final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
      final TreeViewer treeViewer = f_viewer;
      if (treeViewer != null) {
        final ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
        if (selection == null || selection.isEmpty()) {
          treeViewer.expandToLevel(50);
        } else {
          for (Object obj : selection.toList()) {
            if (obj != null) {
              treeViewer.collapseToLevel(obj, 1);
            } else {
              treeViewer.collapseAll();
            }
          }
        }
      }
    }
  };

  @Override
  protected Control buildViewer(Composite parent) {
    f_viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);

    /*
     * We want a double-click to also expand the tree if necessary. This will
     * take care of that functionality.
     */
    f_viewer.addDoubleClickListener(new IDoubleClickListener() {

      @Override
      public void doubleClick(DoubleClickEvent event) {
        final TreeViewer treeViewer = f_viewer;
        ITreeSelection sel = (ITreeSelection) treeViewer.getSelection();
        if (sel == null)
          return;
        Object obj = sel.getFirstElement();
        if (obj == null)
          return;
        // open up the tree one more level
        if (!treeViewer.getExpandedState(obj)) {
          treeViewer.expandToLevel(obj, 1);
        }
      }
    });

    f_viewer.setContentProvider(f_content);
    f_viewer.setLabelProvider(f_content);
    f_content.build();
    f_viewer.setInput(f_content); // Needed to show something?

    return f_viewer.getControl();
  }

  @Override
  protected void setupViewer(StructuredViewer viewer) {
    super.setupViewer(viewer);

    viewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        final ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          handleDoubleClick((IStructuredSelection) selection);
        }
      }
    });

    EclipseUIUtility.hookContextMenu(this, viewer, this);
  }

  @Override
  protected StructuredViewer getViewer() {
    return f_viewer;
  }

  @Override
  protected void hookHandlersToCommands(IHandlerService hs) {
    super.hookHandlersToCommands(hs);

    hs.activateHandler("com.surelogic.jsure.client.eclipse.command.XMLExplorerView.collapseAll", new AbstractHandler() {
      @Override
      public Object execute(ExecutionEvent event) throws ExecutionException {
        if (f_viewer != null)
          f_viewer.collapseAll();
        return null;
      }
    });
    final String toggleDiffsCmd = "com.surelogic.jsure.client.eclipse.command.XMLExplorerView.toggleShowDiffs";
    hs.activateHandler(toggleDiffsCmd, new AbstractHandlerElementUpdater() {
      @Override
      public Object execute(ExecutionEvent event) throws ExecutionException {
        final ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
        final Command command = service.getCommand(toggleDiffsCmd);
        final State state = command.getState(toggleDiffsCmd + ".state");
        f_content.toggleViewingType();

        boolean checked = f_content.getViewingType() == Viewing.DIFFS;
        state.setValue(checked);

        service.refreshElements(toggleDiffsCmd, null);
        f_viewer.refresh();
        return null;
      }

      @Override
      public void updateElement(UIElement element, Map parameters) {
        final ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
        final Command command = service.getCommand(toggleDiffsCmd);
        final State state = command.getState(toggleDiffsCmd + ".state");
        final boolean checked = Boolean.TRUE.equals(state.getValue());
        element.setChecked(checked);
      }
    });
  }

  @Override
  protected void makeActions() {
    f_actionExpand.setText(I18N.msg("jsure.eclipse.view.expand"));
    f_actionExpand.setToolTipText(I18N.msg("jsure.eclipse.view.expand.tip"));
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText(I18N.msg("jsure.eclipse.view.collapse"));
    f_actionCollapse.setToolTipText(I18N.msg("jsure.eclipse.view.collapse.tip"));
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));
  }

  @Override
  public void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
    manager.add(f_openSource);
    manager.add(new Separator());
    manager.add(f_openXmlEditor);
    manager.add(f_copyAnnos);
    manager.add(new Separator());
    manager.add(f_actionExpand);
    manager.add(f_actionCollapse);

    if (XUtil.useExperimental) {
      final Object o = s.getFirstElement();
      if (o instanceof Filterable) {
        final Filterable t = (Filterable) o;
        manager.add(new Separator());
        if (t.hasLocal()) {
          manager.add(new Action("Publish Changes to JSure For Release") {
            @Override
            public void run() {
              PromisesLibMerge.mergeLocalToJSure(t.getPath());
              PromisesXMLReader.clear(t.getPath());
              PromisesXMLReader.refreshAll();
            }
          });
        }
        manager.add(new Action("Rewrite JSure File (to update file format)") {
          @Override
          public void run() {
            PromisesLibMerge.rewriteJSure(t.getPath());
            PromisesXMLReader.clear(t.getPath());
            PromisesXMLReader.refreshAll();
          }
        });
      }
    }
  }

  void handleDoubleClick(IStructuredSelection selection) {
    final Object o = selection.getFirstElement();
    handleDoubleClick(o);
  }

  void handleDoubleClick(Object o) {
    if (o instanceof Type) {
      Type t = (Type) o;
      PromisesXMLEditor.openInEditor(t.getPath(), false);
    } else if (o instanceof Package) {
      Package p = (Package) o;
      PromisesXMLEditor.openInEditor(getPackagePath(p.name), false);
    } else if (o instanceof IJavaElement) {
      IJavaElement e = (IJavaElement) o;
      PackageElement p = findPackageElt(e);

      if (p.getClassElement() == null) {
        PromisesXMLEditor.openInEditor(getPackagePath(p.getName()), false);
      } else {
        final IEditorPart ep = PromisesXMLEditor.openInEditor(
            p.getName().replace('.', '/') + '/' + p.getClassElement().getName() + TestXMLParserConstants.SUFFIX, false);
        if (ep instanceof PromisesXMLEditor) {
          final PromisesXMLEditor xe = (PromisesXMLEditor) ep;
          xe.focusOn((IJavaElement) o);
        }
      }
    }
  }

  private PackageElement findPackageElt(IJavaElement e) {
    while (e != null) {
      if (e instanceof PackageElement) {
        break;
      }
      e = e.getParent();
    }
    return (PackageElement) e;
  }

  void handleOpenSource(Object o) {
    if (o instanceof Type) {
      Type t = (Type) o;
      JDTUIUtility.tryToOpenInEditor(t.pkg.name, t.name);
    } else if (o instanceof IJavaElement) {
      IJavaElement e = (IJavaElement) o;
      PackageElement p = findPackageElt(e);

      if (p.getClassElement() != null) {
        if (e instanceof MethodElement) {
          MethodElement m = (MethodElement) e;
          JDTUIUtility.tryToOpenInEditorUsingMethodName(p.getName(), p.getClassElement().getName(), m.getName());
        } else {
          JDTUIUtility.tryToOpenInEditor(p.getName(), p.getClassElement().getName());
        }
      }
    }
  }

  private String getPackagePath(String qname) {
    return qname.replace('.', '/') + '/' + TestXMLParserConstants.PACKAGE_PROMISES;
  }

  static final Package[] noPackages = new Package[0];

  enum Viewing {
    ALL, DIFFS() {
      @Override
      boolean matches(Filterable f) {
        return f.hasDiffs();
      }
      /*
       * }, CONFLICTS() {
       * 
       * @Override boolean matches(Filterable f) { return f.hasConflicts(); }
       */
    };
    boolean matches(Filterable f) {
      return true;
    }
  }

  static final Object[] noDiffs = new Object[] { "No changes have been made to the standard library annotations" };

  class Provider extends PromisesXMLContentProvider implements IJSureTreeContentProvider, PromisesXMLReader.Listener {
    Package[] pkgs = noPackages;
    Viewing type = Viewing.ALL;

    Provider() {
      super(true);
      PromisesXMLReader.listenForRefresh(this);
    }

    void toggleViewingType() {
      type = (type == Viewing.ALL) ? Viewing.DIFFS : Viewing.ALL;
    }

    void setViewingType(Viewing v) {
      if (v != null) {
        type = v;
      }
    }

    Viewing getViewingType() {
      return type;
    }

    @Override
    public void refresh(PackageElement e) {
      refreshAll();
    }

    @Override
    public void refreshAll() {
      new SLUIJob() {
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
          if (!f_viewer.getControl().isDisposed()) {
            // save the view state
            final TreeViewerUIState state = new TreeViewerUIState(f_viewer);
            build();
            new SLUIJob() {
              @Override
              public IStatus runInUIThread(IProgressMonitor monitor) {
                if (!f_viewer.getControl().isDisposed()) {
                  f_viewer.refresh();
                  // restore the view state
                  state.restoreViewState(f_viewer, true);
                  f_viewer.refresh();
                }
                return Status.OK_STATUS;
              }
            }.schedule();
          }
          return Status.OK_STATUS;
        }
      }.schedule();
    }

    @Override
    public String build() {
      final List<Package> l = new ArrayList<>();
      final Map<String, Collection<String>> local = PromisesXMLEditor.findLocalPromisesXML();
      for (Map.Entry<String, Collection<String>> e : PromisesXMLEditor.findAllPromisesXML().entrySet()) {
        Package p = new Package(e, local.get(e.getKey()));
        l.add(p);
      }
      Collections.sort(l);
      pkgs = l.toArray(noPackages);
      return "Something";
    }

    @Override
    public Object[] getElements(Object inputElement) {
      switch (type) {
      // case CONFLICTS:
      case DIFFS:
        Object[] rv = filter(type, pkgs);
        if (rv.length == 0) {
          return noDiffs;
        }
        return rv;
      default:
        return pkgs;
      }
    }

    @Override
    public boolean hasChildren(Object element) {
      if (element instanceof Package) {
        Package p = (Package) element;
        return p.types.length != 0;
      }
      if (element instanceof Type) {
        Type t = (Type) element;
        return t.hasChildren();
      }
      if (element instanceof String) {
        return false;
      }
      return super.hasChildren(element);
    }

    @Override
    public Object[] getChildren(Object parent) {
      if (parent instanceof Package) {
        Package p = (Package) parent;
        return filter(type, p.types);
      }
      if (parent instanceof Type) {
        Type t = (Type) parent;
        PackageElement root = t.getRoot();
        if (root != null) {
          return super.getChildren(root.getClassElement());
        }
      }
      if (parent instanceof String) {
        return SLUtility.EMPTY_STRING_ARRAY;
      }
      // return noStrings;
      return super.getChildren(parent);
    }

    @Override
    public Object getParent(Object element) {
      if (element instanceof Type) {
        Type t = (Type) element;
        return t.pkg;
      }
      if (element instanceof Package) {
        return null;
      }
      if (element instanceof String) {
        return null;
      }
      return super.getParent(element);
    }

    @Override
    public String getText(Object element) {
      if (element instanceof IJavaElement) {
        return super.getText(element);
      }
      if (element != null) {
        return element.toString();
      }
      return null;
    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof Package) {
        Package p = (Package) element;
        return JSureDecoratedImageUtility.getImage(CommonImages.IMG_PACKAGE,
            p.hasWarning() ? EnumSet.of(Flag.HINT_WARNING) : EnumSet.noneOf(Flag.class));
      }
      if (element instanceof Type) {
        final Type t = (Type) element;
        return JSureDecoratedImageUtility.getImage(CommonImages.IMG_CLASS,
            t.confirmed ? EnumSet.noneOf(Flag.class) : EnumSet.of(Flag.HINT_WARNING));
      }
      if (element instanceof String) {
        return null;
      }
      return JSureDecoratedImageUtility.getImage(super.getBaseImageHelper(element));
    }

    @Override
    public void dispose() {
      PromisesXMLReader.stopListening(this);
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // Don't do anything on this event
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
      // Don't do anything on this event
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
      // Don't do anything on this event
    }
  }

  interface Filterable {
    boolean hasLocal();

    String getPath();

    boolean hasDiffs();
  }

  static <T extends Filterable> Object[] filter(Viewing type, T[] elements) {
    if (elements.length == 0) {
      return elements;
    }
    List<T> l = new ArrayList<>();
    for (T e : elements) {
      if (type.matches(e)) {
        l.add(e);
      }
    }
    return l.toArray();
  }

  static class Package implements Filterable, Comparable<Package> {
    final String name;
    final Type[] types;
    final boolean isLocal;

    public Package(Entry<String, Collection<String>> e, Collection<String> local) {
      final boolean hasLocal = local != null;
      name = e.getKey();
      // Adjust for package-info XML
      types = new Type[e.getValue().size() - (e.getValue().contains(name) ? 1 : 0)];
      int i = 0;
      for (String type : e.getValue()) {
        if (name.equals(type)) {
          continue;
        }
        types[i] = new Type(this, type, hasLocal ? local.contains(type) : false);
        i++;
      }
      isLocal = hasLocal && local.contains(name);
      Arrays.sort(types);
    }

    boolean hasWarning() {
      for (Type t : types) {
        if (!t.confirmed) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      if (hasDiffs()) {
        return PromisesXMLContentProvider.DIRTY_PREFIX + (name.length() == 0 ? SLUtility.JAVA_DEFAULT_PACKAGE : name);
      }
      return name.length() == 0 ? SLUtility.JAVA_DEFAULT_PACKAGE : name;
    }

    @Override
    public int compareTo(Package o) {
      return name.compareTo(o.name);
    }

    @Override
    public boolean hasDiffs() {
      if (isLocal) {
        return true;
      }
      // TODO check for mods in the package
      for (Type t : types) {
        if (t.hasDiffs()) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String getPath() {
      return PackageAccessor.computeXMLPath(name);
    }

    @Override
    public boolean hasLocal() {
      return isLocal;
    }
  }

  static class Type implements Filterable, Comparable<Type> {
    final Package pkg;
    final String name;
    final boolean isLocal;
    final boolean confirmed;
    private PackageElement root;

    Type(Package pkg, String name, boolean isLocal) {
      this.pkg = pkg;
      this.name = name;
      this.isLocal = isLocal;

      confirmed = JDTUtility.findIType(null, pkg.name, name) != null;
    }

    @Override
    public String getPath() {
      if (pkg.name.length() == 0) {
        return name + TestXMLParserConstants.SUFFIX;
      }
      return pkg.name.replace('.', '/') + '/' + name + TestXMLParserConstants.SUFFIX;
    }

    boolean hasChildren() {
      return true;
    }

    @Override
    public boolean hasLocal() {
      return isLocal;
    }

    @Override
    public String toString() {
      if (hasDiffs()) {
        /*
         * Handled as a decorator
         * 
         * if (hasUpdate()) { return "<> " + name; }
         */
        return PromisesXMLContentProvider.DIRTY_PREFIX + name;
      }
      return name;
    }

    @Override
    public int compareTo(Type o) {
      return name.compareTo(o.name);
    }

    void buildChildren() {
      buildChildren(true);
    }

    /**
     * @return true if root exists after the call
     */
    private boolean buildChildren(boolean force) {
      if (root != null) {
        return true;
      }
      final String path = getPath();
      if (force) {
        // final Pair<File, File> rv =
        // PromisesXMLEditor.findPromisesXML(path);
        try {
          root = PromisesXMLParser.load(path);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return true;
      } else {
        root = PromisesXMLReader.get(path);
        return root != null;
      }
    }

    public PackageElement getRoot() {
      buildChildren();
      return root;
    }

    @Override
    public boolean hasDiffs() {
      // Check if there are any changes within Eclipse
      if (buildChildren(false)) {
        return root.isModified();
      }
      return isLocal;
    }
  }

  /**
   * Mainly for copying annotations
   */
  public static class Clipboard {
    private IJavaElement focus;

    public IJavaElement getFocus() {
      return focus;
    }

    public void setFocus(IJavaElement e) {
      focus = e;
    }
  }

  private static final Clipboard clipboard = new Clipboard();

  public static Clipboard getClipboard() {
    return clipboard;
  }
}
