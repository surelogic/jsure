package com.surelogic.jsure.client.eclipse.editors;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ITypeHierarchyViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.surelogic.annotation.IAnnotationParseRule;
import com.surelogic.annotation.NullAnnotationParseRule;
import com.surelogic.annotation.Attribute;
import com.surelogic.annotation.rules.ScopedPromiseRules;
import com.surelogic.annotation.rules.ThreadEffectsRules;
import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.CommonImages;
import com.surelogic.common.Pair;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IDecl.Kind;
import com.surelogic.common.ref.IDeclParameter;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.common.ui.actions.LoggedSelectionAdapter;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.text.XMLLineStyler;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.client.eclipse.dialogs.LibraryAnnotationDialog;
import com.surelogic.jsure.client.eclipse.views.xml.XMLExplorerView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.AbstractFunctionElement;
import com.surelogic.xml.AbstractJavaElementVisitor;
import com.surelogic.xml.AnnotatedJavaElement;
import com.surelogic.xml.AnnotationElement;
import com.surelogic.xml.ClassElement;
import com.surelogic.xml.ConstructorElement;
import com.surelogic.xml.FunctionParameterElement;
import com.surelogic.xml.IJavaElement;
import com.surelogic.xml.IMergeableElement;
import com.surelogic.xml.IXmlProcessor;
import com.surelogic.xml.MethodElement;
import com.surelogic.xml.NestedClassElement;
import com.surelogic.xml.PackageAccessor;
import com.surelogic.xml.PackageElement;
import com.surelogic.xml.PromisesXMLMerge;
import com.surelogic.xml.PromisesXMLParser;
import com.surelogic.xml.PromisesXMLReader;
import com.surelogic.xml.PromisesXMLWriter;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class PromisesXMLEditor extends MultiPageEditorPart implements PromisesXMLReader.Listener {
  enum FileStatus {
    READ_ONLY,
    /** Mutable, but saves to a local file */
    FLUID,
    /** Mutable in the usual way */
    LOCAL
  }

  public static final boolean hideEmpty = false;

  private final PromisesXMLContentProvider provider = new PromisesXMLContentProvider(hideEmpty);
  private static final JavaElementProvider jProvider = new JavaElementProvider();
  // private final ParameterProvider paramProvider = new ParameterProvider();
  private static final AnnoProvider annoProvider = new AnnoProvider();
  private TreeViewer contents;

  /**
   * Really used to check if we deleted all the changes
   */
  private boolean isDirty = false;
  private TextViewer fluidXML;
  private TextEditor localXML;

  public PromisesXMLEditor() {
    PromisesXMLReader.listenForRefresh(this);
  }

  @Override
  protected void createPages() {
    createContentsPage(getContainer());
    int index = addPage(contents.getControl());
    setPageText(index, "Editor");

    createFluidXMLPage();
    createLocalXMLPage();
    updateTitle();
  }

  private void updateTitle() {
    IEditorInput input = getEditorInput();
    setPartName(input.getName());
    setTitleToolTip(input.getToolTipText());
  }

  private void createContentsPage(final Composite parent) {
    contents = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    contents.setContentProvider(provider);
    contents.setLabelProvider(provider);
    if (provider.getInput() != null) {
      contents.setInput(provider.getInput());
    }
    contents.getControl().addMenuDetectListener(new MenuDetectListener() {
      @Override
      public void menuDetected(final MenuDetectEvent e) {
        final Menu menu = new Menu(contents.getControl().getShell(), SWT.POP_UP);
        setupContextMenu(menu);
        contents.getTree().setMenu(menu);
      }
    });
    // http://bingjava.appspot.com/snippet.jsp?id=2208
    contents.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(final DoubleClickEvent event) {
        if (provider.isMutable()) {
          final IStructuredSelection s = (IStructuredSelection) event.getSelection();
          // System.out.println("Doubleclik on "+s.getFirstElement());
          // contents.editElement(s.getFirstElement(), 0);
          Object o = s.getFirstElement();
          if (o instanceof AnnotationElement) {
            AnnotationElement a = (AnnotationElement) o;
            if (a.isEditable()) {
              startAnnotationEditDialog(a);
            } else {
              LibraryAnnotationDialog.cannotEdit(a.getLabel());
            }
          }
        }
      }
    });

    /*
     * Save the tree state if we need to restore it later (due to a major
     * update)
     */
    TreeViewerUIState.registerListenersToSaveTreeViewerStateOnChange(contents);

    contents.setComparer(new Comparer());
  }

  private void createFluidXMLPage() {
    fluidXML = new TextViewer(getContainer(), SWT.V_SCROLL | SWT.H_SCROLL);
    fluidXML.setDocument(provider.getFluidDocument());
    fluidXML.getTextWidget().setFont(JFaceResources.getTextFont());
    fluidXML.getTextWidget().addLineStyleListener(new XMLLineStyler());
    fluidXML.setEditable(false);

    int index = addPage(fluidXML.getControl());
    setPageText(index, "Baseline");
  }

  private void createLocalXMLPage() {
    final IEditorInput input = provider.getLocalInput();
    if (input == null) {
      return;
    }
    try {
      localXML = new TextEditor() {
        @Override
        public void createPartControl(final Composite parent) {
          super.createPartControl(parent);
          getSourceViewer().getTextWidget().addLineStyleListener(new XMLLineStyler());
        }

        @Override
        public boolean isEditable() {
          return false;
        }
      };

      int index = addPage(localXML, input);
      setPageText(index, "Diffs");
    } catch (PartInitException e) {
      SLLogger.getLogger().log(Level.WARNING, "Error creating source page for " + input.getToolTipText(), e);
    }
  }

  @Override
  public void init(final IEditorSite site, final IEditorInput input) {
    setSite(site);
    setInput(input);
    if (input instanceof IURIEditorInput) {
      IURIEditorInput f = (IURIEditorInput) input;
      if (contents != null) {
        contents.setInput(f.getURI());
      } else {
        provider.inputChanged(contents, null, f.getURI());
        if (f instanceof Input) {
          Input i = (Input) f;
          if (i.readOnly) {
            provider.markAsReadOnly();
          }
        }
      }
    }
    setPartName(input.getName());
  }

  @Override
  public void setFocus() {
    // System.out.println("Focus on "+getActivePage());
    switch (getActivePage()) {
    case 0:
      if (contents != null) {
        contents.getControl().setFocus();
      }
      break;
    case 1:
      if (fluidXML != null) {
        fluidXML.getControl().setFocus();
      }
      break;
    case 2:
      if (localXML != null) {
        localXML.setFocus();
      }
      break;
    }
  }

  /*
   * TODO public void gotoMarker(IMarker marker) { }
   */

  @Override
  public void doSave(final IProgressMonitor monitor) {
    boolean wasDirty = isDirty();
    provider.save(monitor);
    localXML.doRevertToSaved();

    if (wasDirty) {
      isDirty = false;
      fireDirtyProperty();
      updateTitle(); // does this help refresh?
      PromisesXMLReader.refresh(provider.pkg);
    }
  }

  @Override
  public void doSaveAs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dispose() {
    PromisesXMLReader.stopListening(this);

    if (isDirty()) {
      // Nuke changes
      provider.deleteUnsavedChanges();
    }
    super.dispose();
  }

  @Override
  public boolean isDirty() {
    return isDirty || provider.isDirty();
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  private void fireDirtyProperty() {
    // This shouldn't be necessary, but Eclipse doesn't seem to
    // realize that the editor is dirty otherwise
    new SLUIJob() {
      @Override
      public IStatus runInUIThread(final IProgressMonitor monitor) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  private void markAsDirty() {
    fireDirtyProperty();

    // otherwise already dirty
    syncLocalXMLEditor();
    PromisesXMLReader.refresh(provider.pkg);
  }

  private void syncLocalXMLEditor() {
    final IURIEditorInput input = (IURIEditorInput) provider.getLocalInput();
    final IDocument doc = localXML.getDocumentProvider().getDocument(input);
    if (doc != null) {
      // System.out.println(doc.get());
      StringWriter sw = new StringWriter(doc.getLength());
      PromisesXMLWriter pw = new PromisesXMLWriter(new PrintWriter(sw));
      PackageElement p = provider.pkg.cloneMe(null);
      p = PromisesXMLMerge.generateDiff(p);
      pw.write(p);

      final String updated = sw.toString();
      doc.set(updated);
      // System.out.println(updated);
    }
  }

  private void setupContextMenu(final Menu menu) {
    final IStructuredSelection s = (IStructuredSelection) contents.getSelection();
    if (s.size() != 1) {
      return;
    }
    final IJavaElement o = (IJavaElement) s.getFirstElement();

    if (addNavigationActions(menu, o)) {
      new MenuItem(menu, SWT.SEPARATOR);
    }

    makeMenuItem(menu, new LoggedSelectionAdapter("Copy") {
      @Override
      protected void selected(SelectionEvent e) {
        XMLExplorerView.getClipboard().setFocus(o);
      }
    });

    if (!provider.isMutable()) {
      return;
    }

    if (o instanceof AnnotatedJavaElement) {
      final AnnotatedJavaElement j = (AnnotatedJavaElement) o;
      if (XMLExplorerView.getClipboard().getFocus() != null) {
        makeMenuItem(menu, new LoggedSelectionAdapter("Paste") {
          @Override
          protected void selected(SelectionEvent e) {
            boolean changed = pasteAnnotations(j, XMLExplorerView.getClipboard().getFocus());
            if (changed) {
              markAsDirty();
              contents.refresh();
              // Expand the selection
              contents.setExpandedState(((ITreeSelection) contents.getSelection()).getFirstElement(), true);
            }
          }
        });
      }

      new MenuItem(menu, SWT.SEPARATOR);

      makeMenuItem(menu, new AnnotationCreator("Add Annotation...", j));

      if (o instanceof ClassElement) {
        for (ScopedTargetType t : ScopedTargetType.values()) {
          makeMenuItem(menu, new AnnotationCreator("Add Scoped Promise For " + t.label + "...", j, t));
        }
      }
    }

    if (o instanceof IMergeableElement) {
      addActionsForAnnotations(menu, o);
    }
    new MenuItem(menu, SWT.SEPARATOR);

    final boolean markUnannotated = provider.markUnannotated();
    final MenuItem markDecls = makeMenuItem(menu, null, new LoggedSelectionAdapter("Mark Unannotated Methods") {
      @Override
      protected void selected(final SelectionEvent e) {
        provider.setMarkUnannotated(!markUnannotated);
        contents.refresh();
      }
    }, SWT.CHECK);
    markDecls.setSelection(markUnannotated);

    new MenuItem(menu, SWT.SEPARATOR);
    makeMenuItem(menu, new LoggedSelectionAdapter("Revert To Baseline") {
      @Override
      protected void selected(final SelectionEvent e) {
        final Shell s = contents.getTree().getShell();
        if (provider.pkg.isModified()) {
          if (MessageDialog.openQuestion(s, "Revert All Changes?", "Do you really want to revert all changes?")) {
            /*
             * Get the UI state before we do the revert to baseline.
             */
            final TreeViewerUIState state = TreeViewerUIState.getSavedTreeViewerStateIfPossible(contents);

            provider.deleteAllChanges();
            contents.refresh();

            if (state != null) {
              /*
               * Restore the UI state
               */
              (new SLUIJob() {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                  state.restoreViewState(contents, true);
                  return Status.OK_STATUS;
                }
              }).schedule();
            }
            if (provider.getLocalInput().exists()) {
              isDirty = true;
              markAsDirty();
            } else {
              // Otherwise, we're just reverted to the saved state
              // in fluid
              isDirty = false;
              markAsDirty(); // just updating the editor state
            }
          }
        } else {
          MessageDialog.openInformation(s, "No Changes", "There are no changes to revert");
        }
      }
    });
  }

  private void addActionsForAnnotations(final Menu menu, final IJavaElement o) {
    if (o instanceof AnnotationElement) {
      final AnnotationElement a = (AnnotationElement) o;
      MenuItem m = makeMenuItem(menu, SLImages.getImage(CommonImages.IMG_ANNOTATION), new LoggedSelectionAdapter(
          "Edit Annotation...") {
        @Override
        protected void selected(final SelectionEvent se) {
          startAnnotationEditDialog(a);
        }
      });
      m.setEnabled(a.isEditable());

      MenuItem m2 = makeMenuItem(menu, SLImages.getImage(CommonImages.IMG_ANNOTATION), new LoggedSelectionAdapter("Revert") {
        @Override
        protected void selected(final SelectionEvent se) {
          a.revert();
          isDirty = true;
          markAsDirty();
        }
      });
      m2.setEnabled(a.canRevert());
    }
    final IMergeableElement me = (IMergeableElement) o;
    makeMenuItem(menu, SLImages.getImage(CommonImages.IMG_RED_X), new LoggedSelectionAdapter("Delete") {
      @Override
      protected void selected(final SelectionEvent e) {
        final Shell s = contents.getTree().getShell();
        if (MessageDialog.openQuestion(s, "Delete Annotation?", "Do you really want to delete @" + me.getLabel() + "?")) {
          final boolean origDirty = provider.pkg.isDirty();
          final boolean deletedNew = me.delete();
          if (deletedNew && !origDirty) {
            // Probably saved after creation, so we need to
            // save it
            isDirty = true;
          }
          markAsDirty();
          contents.refresh();
        }
      }
    });
  }

  abstract class ITypeSelector<T extends IMember> extends LoggedSelectionAdapter {
    final ClassElement c;
    final List<T> members = new ArrayList<T>();
    final Comparator<T> comparator;

    ITypeSelector(String label, final ClassElement cls, final Comparator<T> compare) {
      super(label);
      c = cls;
      comparator = compare;
    }

    @Override
    protected final void selected(final SelectionEvent e) {
      final IType t = findIType(c, "");
      if (t == null) {
        return;
      }
      ListSelectionDialog d;
      try {
        preselect(t);
        Collections.sort(members, comparator);
        d = new ListSelectionDialog(contents.getTree().getShell(), members.toArray(), jProvider, jProvider,
            "Select one or more of these");
        if (d.open() == Window.OK) {
          boolean changed = false;
          for (Object o : d.getResult()) {
            @SuppressWarnings("unchecked")
            T m = (T) o;
            create(m);
            changed = true;
          }
          if (changed) {
            markAsDirty();
            contents.refresh();
          }
        }
      } catch (JavaModelException e1) {
        e1.printStackTrace();
      }
    }

    protected abstract void preselect(final IType t) throws JavaModelException;

    protected abstract void create(T member) throws JavaModelException;
  }

  /**
   * 
   * @param menu
   *          the context menu
   * @param o
   *          the target.
   * @return <tt>true</tt> if anything was added to the menu.
   */
  private boolean addNavigationActions(final Menu menu, final IJavaElement o) {
    boolean result = false;
    if (o instanceof ClassElement) {
      result = true;
      final ClassElement c = (ClassElement) o;
      makeMenuItem(menu, new LoggedSelectionAdapter("Open") {
        @Override
        protected void selected(final SelectionEvent e) {
          final IType t = findIType(c, "");
          if (t != null) {
            JDTUIUtility.tryToOpenInEditor(t.getPackageFragment().getElementName(), t.getTypeQualifiedName('.'));
          }
        }
      });
      makeMenuItem(menu, new LoggedSelectionAdapter("Open Type Hierarchy") {
        @Override
        protected void selected(final SelectionEvent e) {
          final IViewPart view = EclipseUIUtility.showView(JavaUI.ID_TYPE_HIERARCHY);
          if (view instanceof ITypeHierarchyViewPart) {
            final ITypeHierarchyViewPart v = (ITypeHierarchyViewPart) view;
            final IType t = findIType(c, "");
            if (t != null) {
              v.setInputElement(t);
            }
          }
        }
      });
    } else if (o instanceof MethodElement) {
      result = true;
      final MethodElement m = (MethodElement) o;
      makeMenuItem(menu, new LoggedSelectionAdapter("Open") {
        @Override
        protected void selected(final SelectionEvent e) {
          ClassElement c = (ClassElement) m.getParent();
          final IType t = findIType(c, "");
          if (t != null) {
            JDTUIUtility.tryToOpenInEditorUsingMethodName(t.getPackageFragment().getElementName(), t.getTypeQualifiedName('.'),
                m.getName());
          }
        }
      });
    }
    return result;
  }

  private class AnnotationCreator extends LoggedSelectionAdapter {
    final AnnotatedJavaElement j;
    final ScopedTargetType target;
    final boolean makeScopedPromise;

    AnnotationCreator(String label, final AnnotatedJavaElement aje, final ScopedTargetType t) {
      super(label);
      j = aje;
      target = t;
      makeScopedPromise = t != null;
    }

    AnnotationCreator(String label, final AnnotatedJavaElement aje) {
      this(label, aje, null);
    }

    @Override
    protected void selected(final SelectionEvent e) {
      final List<String> annos = computePossibleAnnos(j, target, makeScopedPromise);
      ListSelectionDialog d = new ListSelectionDialog(contents.getTree().getShell(), annos.toArray(), annoProvider, annoProvider,
          makeScopedPromise ? "Select scoped promise(s) to add for " + target.label.toLowerCase() : "Select annotation(s) to add") {

        @Override
        protected void configureShell(Shell shell) {
          super.configureShell(shell);
          shell.setImage(SLImages.getImage(CommonImages.IMG_ANNOTATION));
          shell.setText("Add Annotation");
        }
      };
      if (d.open() == Window.OK) {
        int num = 0;
        AnnotationElement first = null;
        for (Object o : d.getResult()) {
          final String tag = (String) o;
          final String contents = getDefaultContents(tag, makeScopedPromise);
          final AnnotationElement a;
          final Map<String, String> attrs = Collections.<String, String> emptyMap();
          if (makeScopedPromise) {
            a = new AnnotationElement(j, null, ScopedPromiseRules.PROMISE, "@" + tag + contents + " for " + target.target, attrs);
          } else {
            a = new AnnotationElement(j, null, tag, contents, attrs);
          }
          // System.out.println("Created elt: "+a);
          j.addPromise(a);
          a.markAsModified();
          num++;

          if (first == null) {
            first = a;
          }
        }
        if (num > 0) {
          contents.refresh();
          contents.setExpandedState(((ITreeSelection) contents.getSelection()).getFirstElement(), true);
          markAsDirty();
        }
      }
    }

    private String getDefaultContents(final String tag, final boolean isScopedPromise) {
      if (ThreadEffectsRules.STARTS.equals(tag)) {
        return isScopedPromise ? "(nothing)" : "nothing";
      }
      return "";
    }
  }

  List<String> computePossibleAnnos(AnnotatedJavaElement j, ScopedTargetType target, boolean makeScopedPromise) {
    final List<String> annos;
    if (!makeScopedPromise) {
      annos = findMissingAnnos(j);
    } else {
      annos = sortSet(remove(findApplicableAnnos(target.op), ScopedPromiseRules.PROMISE));
    }
    return annos;
  }

  private List<String> sortSet(final Set<String> s) {
    List<String> rv = new ArrayList<String>(s);
    Collections.sort(rv);
    return rv;
  }

  private Set<String> findApplicableAnnos(final IDecl.Kind kind) {
	final Operator op = kindMap.get(kind);
	if (op == null) {
	  return Collections.emptySet();
	}
	return findApplicableAnnos(op);
  }
	
  private Set<String> findApplicableAnnos(final Operator op) {
    final Set<String> annos = new HashSet<String>();
    // Get valid/applicable annos
    for (IAnnotationParseRule<?, ?> rule : PromiseFramework.getInstance().getParseDropRules()) {
      if (!(rule instanceof NullAnnotationParseRule) && rule.declaredOnValidOp(op) && AnnotationElement.isIdentifier(rule.name())) {
        annos.add(rule.name());
      }
    }
    // These should never appear in XML files
    annos.remove(ScopedPromiseRules.ASSUME);
    return annos;
  }

  private static Map<IDecl.Kind, Operator> kindMap = new HashMap<IDecl.Kind, Operator>();
  private static Declaration[] declOps = {
	  PackageDeclaration.prototype,
	  ClassDeclaration.prototype,
	  MethodDeclaration.prototype,
	  ConstructorDeclaration.prototype,
	  ParameterDeclaration.prototype,
  };
  static {
	for(Declaration op : declOps) {
	  kindMap.put(op.getKind(), op);
	}
	kindMap.put(IDecl.Kind.FIELD, FieldDeclaration.prototype);
	kindMap.put(IDecl.Kind.INITIALIZER, ClassInitDeclaration.prototype);
  }
  
  private Set<String> remove(final Set<String> s, final String elt) {
    s.remove(elt);
    return s;
  }

  private List<String> findMissingAnnos(final AnnotatedJavaElement j) {
    if (j == null) {
      return Collections.emptyList();
    }
    final Set<String> annos = findApplicableAnnos(j.getKind());

    // Remove clashes
    for (AnnotationElement a : j.getPromises()) {
      // This will remove it if there should only be one of that kind
      annos.remove(a.getUid());
    }
    return sortSet(annos);
  }

  static class NewParameter {
    final int index;
    final String type;

    NewParameter(final int i, final String t) {
      index = i;
      type = t;
    }
  }

  static IType findIType(final ClassElement c, final String nameSoFar) {
    String typeName = nameSoFar.isEmpty() ? c.getName() : c.getName() + '.' + nameSoFar;
    if (c instanceof NestedClassElement) {
      ClassElement parent = (ClassElement) c.getParent();
      return findIType(parent, typeName);
    } else { // top-level
      PackageElement pkg = (PackageElement) c.getParent();
      return JDTUtility.findIType(null, pkg.getName(), typeName);
    }
  }

  static MenuItem makeMenuItem(Menu menu, LoggedSelectionAdapter l) {
    return makeMenuItem(menu, null, l);
  }

  static MenuItem makeMenuItem(Menu menu, Image image, LoggedSelectionAdapter l) {
    return makeMenuItem(menu, image, l, SWT.PUSH);
  }

  static MenuItem makeMenuItem(final Menu menu, final Image image, final LoggedSelectionAdapter l, int flags) {
    MenuItem item1 = new MenuItem(menu, flags);
    item1.setText(l.getContext());
    item1.addSelectionListener(l);
    if (image != null) {
      item1.setImage(image);
    }
    return item1;
  }

  static class JavaElementProvider extends AbstractContentProvider {
    @Override
    public Object[] getElements(final Object inputElement) {
      return (Object[]) inputElement;
    }

    @Override
    public String getText(final Object element) {
      org.eclipse.jdt.core.IJavaElement e = (org.eclipse.jdt.core.IJavaElement) element;
      if (e instanceof IMethod) {
        IMethod m = (IMethod) e;
        try {
          if (m.isConstructor()) {
            return "new " + m.getElementName() + '(' + PromisesXMLBuilder.translateParameters(m) + ')';
          }
          return m.getElementName() + '(' + PromisesXMLBuilder.translateParameters(m) + ')';
        } catch (JavaModelException e1) {
          // ignore
        }
        return m.getElementName() + "(???)";
      }
      return e.getElementName();
    }
  }

  class ParameterProvider extends AbstractContentProvider {
    @Override
    public Object[] getElements(final Object inputElement) {
      return (Object[]) inputElement;
    }

    @Override
    public String getText(final Object element) {
      if (element instanceof NewParameter) {
        NewParameter p = (NewParameter) element;
        return FunctionParameterElement.PREFIX + (p.index + 1) + " : " + p.type;
      }
      FunctionParameterElement p = (FunctionParameterElement) element;
      return p.getLabel();
    }

    @Override
    public Color getForeground(final Object element) {
      if (element instanceof FunctionParameterElement) {
        return contents.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY);
      }
      return null;
    }
  }

  static class AnnoProvider extends AbstractContentProvider {
    @Override
    public Object[] getElements(final Object inputElement) {
      return (Object[]) inputElement;
    }

    @Override
    public String getText(final Object element) {
      return element.toString();
    }
  }

  public void focusOn(IDecl decl) {
	if (decl == null) {
		return;
	}
	switch (decl.getKind()) {
	case CLASS:
		focusOnNestedType(getRelativeTypeName(decl));
		break;
	case CONSTRUCTOR:
	case METHOD:
		focusOnMethod(decl.getParent().getName(), decl.getName(), formatParams(decl.getParameters()));
	default:
	}
  }
  
  private String getRelativeTypeName(IDecl decl) {	  
	StringBuilder sb = new StringBuilder();
	computeRelativeTypeName(sb, decl);
	return sb.toString();
  }

  private void computeRelativeTypeName(StringBuilder sb, IDecl decl) {
	if (decl == null) {
		return;
	}
	if (decl.getKind() == Kind.CLASS) {
		computeRelativeTypeName(sb, decl.getParent());
		if (sb.length() > 0) {
			sb.append('.');			
		}
		sb.append(decl.getName());
	}
  }

  private String formatParams(List<IDeclParameter> parameters) {
	if (parameters.isEmpty()) {
		return "";
	}
	StringBuilder sb = new StringBuilder();
	for(IDeclParameter p : parameters) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
		sb.append(p.getTypeOf().getCompact());
	}
	return sb.toString();
  }

  public void focusOnMethod(final String enclosingTypeName, final String name, final String params) {
    PackageElement p = provider.pkg;
    if (p != null) {
      final AbstractFunctionElement m = p.visit(new MethodFinder(enclosingTypeName, enclosingTypeName.equals(name) ? "new" : name, params));
      if (m != null) {
        focusOn(m);
      }
    }
  }

  public void focusOnNestedType(final String relativeName) {
    PackageElement p = provider.pkg;
    if (p != null) {
      final NestedClassElement m = p.visit(new TypeFinder(relativeName));
      if (m != null) {
        focusOn(m);
      }
    }
  }

  abstract static class ElementFinder<T> extends AbstractJavaElementVisitor<T> {
    ElementFinder() {
      super(null);
    }

    @Override
    protected T combine(final T old, final T result) {
      if (old != null) {
        return old;
      }
      return result;
    }
  }

  static class TypeFinder extends ElementFinder<NestedClassElement> {
    final String[] names;
    final Stack<String> types = new Stack<String>();

    TypeFinder(final String relativeName) {
      names = relativeName.split("\\.");
    }

    @Override
    public NestedClassElement visit(final NestedClassElement e) {
      types.clear();

      // Find out where this is
      NestedClassElement here = e;
      while (here != null) {
        types.push(here.getName());

        IJavaElement p = here.getParent();
        if (p instanceof NestedClassElement) {
          here = (NestedClassElement) p;
        } else {
          break;
        }
      }
      // Compare against names
      for (String name : names) {
        if (types.isEmpty()) {
          return null; // No match
        }
        String type = types.pop();
        if (!name.equals(type)) {
          return null; // No match
        }
      }
      if (!types.isEmpty()) {
        return null; // Still something to match
      }
      return e;
    }
  }

  static class MethodFinder extends ElementFinder<AbstractFunctionElement> {
	final String enclosingTypeName;
    final String name, params;

    MethodFinder(String type, final String name, final String params) {
      enclosingTypeName = type;
      this.name = name;
      this.params = params;
    }

    @Override
    public AbstractFunctionElement visit(final ConstructorElement c) {
        if (enclosingTypeName.equals(c.getParent().getName()) && name.equals(c.getName())) {
            if (params == null || params.equals(c.getParams())) {
            	return c;           
            }
        }
        return null;
    }
    
    @Override
    public AbstractFunctionElement visit(final MethodElement m) {
      if (enclosingTypeName.equals(m.getParent().getName()) && name.equals(m.getName())) {
        if (params == null || params.equals(m.getParams())) {
        	return m;           
        }
      }
      return null;
    }
  }

  static class XmlMap extends HashMap<String, Collection<String>> implements IXmlProcessor {
    private static final long serialVersionUID = 1L;
    private final boolean makeUnique;

    XmlMap(final boolean makeUnique) {
      this.makeUnique = makeUnique;
    }

    private Collection<String> getPkg(final String qname) {
      Collection<String> c = get(qname);
      if (c == null) {
        c = makeUnique ? new HashSet<String>(4) : new ArrayList<String>(2);
        put(qname, c);
      }
      return c;
    }

    @Override
    public void addPackage(final String qname) {
      Collection<String> c = getPkg(qname);
      c.add(qname);
    }

    @Override
    public void addType(final String pkg, final String name) {
      Collection<String> c = getPkg(pkg);
      c.add(name);
    }
  }

  /**
   * @return a map of packages to qualified names
   */
  public static Map<String, Collection<String>> findAllPromisesXML() {
    return findLocalPromisesXML(true);
  }

  public static Map<String, Collection<String>> findLocalPromisesXML() {
    return findLocalPromisesXML(false);
  }

  private static Map<String, Collection<String>> findLocalPromisesXML(final boolean includeFluid) {
    final XmlMap map = new XmlMap(true);
    if (includeFluid) {
      final File xml = PromisesXMLParser.getFluidXMLDir();
      PackageAccessor.findPromiseXMLsInDir(map, xml);
    }
    final File localXml = JSurePreferencesUtility.getJSureXMLDirectory();
    if (localXml != null) {
      PackageAccessor.findPromiseXMLsInDir(map, localXml);
    }
    return map;
  }

  public static IEditorPart openInXMLEditor(final IDecl decl) {
    if (decl == null)
      throw new IllegalArgumentException(I18N.err(44, "decl"));
    final String qname = DeclUtil.getTypeNameFullyQualifiedOutermostTypeNameOnly(decl);
    PromisesXMLEditor xe = (PromisesXMLEditor)
    		PromisesXMLEditor.openInEditor(qname.replace('.', '/') + TestXMLParserConstants.SUFFIX, false);
    xe.focusOn(decl);
    return xe;
  }

  public static IEditorPart openInXMLEditor(final IType t) {
	  return openInXMLEditor(t, false);
  }
  
  public static IEditorPart openInXMLEditor(final IType t, boolean readOnly) {
    String qname = t.getFullyQualifiedName();
    int firstDollar = qname.indexOf('$');
    if (firstDollar >= 0) {
      // Eliminate any refs to nested classes
      qname = qname.substring(0, firstDollar);
      // TODO find nested classes
    }
    return PromisesXMLEditor.openInEditor(qname.replace('.', '/') + TestXMLParserConstants.SUFFIX, readOnly);
  }

  public static IEditorPart openInEditor(final String path, final boolean readOnly) {
    final IEditorInput i = makeInput(path, readOnly);
    if (i == null) {// || !i.exists()) {
      return null;
    }
    return EclipseUIUtility.openInEditor(i, PromisesXMLEditor.class.getName());
  }

  public static IEditorInput makeInput(final String relativePath, final boolean readOnly) {
    try {
      if (!relativePath.endsWith(TestXMLParserConstants.SUFFIX)) {
        return null;
      }
      return new Input(relativePath, readOnly);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private static class Input implements IURIEditorInput {
    private final boolean readOnly;
    private final String path;
    private final String name;
    private final URI uri;

    Input(final String relativePath, final boolean ro) throws URISyntaxException {
      readOnly = ro;
      path = relativePath;
      uri = new URI(path);

      final int lastSlash = path.lastIndexOf('/');
      if (lastSlash < 0) {
        name = path;
      } else {
        name = path.substring(lastSlash + 1);
      }
    }

    @Override
    public boolean exists() {
      final Pair<File, File> f = PromisesXMLParser.findPromisesXML(path);
      if (f.first().isFile() || f.second().isFile()) {
        return true;
      }
      // Check if the type exists
      final int lastSlash = path.lastIndexOf('/');
      final String pkg;
      if (lastSlash < 0) {
        pkg = "";
      } else {
        pkg = path.substring(0, lastSlash).replace('/', '.');

      }
      final String type = name.substring(0, name.length() - TestXMLParserConstants.SUFFIX.length());
      return JDTUtility.findIType(null, pkg, type) != null;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
      return null; // TODO
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public IPersistableElement getPersistable() {
      return new IPersistableElement() {
        @Override
        public void saveState(final IMemento memento) {
          memento.putString(PromisesXMLFactory.PATH, path);
          memento.putBoolean(PromisesXMLFactory.READ_ONLY, readOnly);
        }

        @Override
        public String getFactoryId() {
          return PromisesXMLFactory.class.getName();
        }
      };
    }

    @Override
    public String getToolTipText() {
      return path;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(final Class adapter) {
      if (adapter == Object.class) {
        return this;
      }
      return null;
    }

    @Override
    public URI getURI() {
      return uri;
    }

    @Override
    public boolean equals(final Object o) {
      if (o instanceof IURIEditorInput) {
        IURIEditorInput i = (IURIEditorInput) o;
        return uri.equals(i.getURI());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }
  }

  public void focusOn(final IJavaElement e) {
    contents.setSelection(new StructuredSelection(e));
    contents.reveal(e);
  }
  
  static class Comparer implements IElementComparer {
    @Override
    public int hashCode(final Object element) {
      if (element instanceof AnnotationElement) {
        return System.identityHashCode(element);
      }
      return element.hashCode();
    }

    @Override
    public boolean equals(final Object a, final Object b) {
      return a == b;
    }
  }

  @Override
  public void refresh(final PackageElement e) {
    if (e == provider.pkg) {
      contents.refresh();
      // Doesn't change what's saved on disk
    }
  }

  @Override
  public void refreshAll() {
    provider.build();
    contents.refresh();
    fluidXML.refresh();
    localXML.doRevertToSaved();
  }

  void startAnnotationEditDialog(final AnnotationElement a) {
    // Collect initial attribute values
    Map<Attribute, String> initialAttrs = new TreeMap<Attribute, String>();
    for (Map.Entry<String, Attribute> e : a.getAttributeDefaults().entrySet()) {
      if (AnnotationConstants.VALUE_ATTR.equals(e.getKey())) {
        initialAttrs.put(e.getValue(), a.getContents());
      } else {
        // TODO push into AnnoElt?
        String value = a.getAttribute(e.getKey());
        if (value == null && e.getValue().getDefaultValueOrNull() != null) {
          value = e.getValue().getDefaultValueOrNull(); // the default
        }
        initialAttrs.put(e.getValue(), value);
      }
    }
    Map<Attribute, String> changedAttrs = LibraryAnnotationDialog.edit(a, initialAttrs);
    if (!changedAttrs.isEmpty()) {
      boolean modified = false;
      // edit contents and attrs
      for (Map.Entry<Attribute, String> e : changedAttrs.entrySet()) {
        if (AnnotationConstants.VALUE_ATTR.equals(e.getKey().getName())) {
          // modified |=
          // a.modify(a.getPromise()+'('+changedAttrs.get(AnnotationConstants.VALUE_ATTR)+')',
          // null);
          modified |= a.modifyContents(e.getValue());
        } else {
          a.setAttribute(e.getKey().getName(), e.getValue());
          modified = true;
        }
      }
      if (modified) {
        isDirty = true;
        markAsDirty();
      }
    }
    /*
     * // TODO Assumes all attributes are boolean ListSelectionDialog d = new
     * ListSelectionDialog(contents.getTree().getShell(),
     * a.getAttributeDefaults().keySet().toArray(), annoProvider, annoProvider,
     * ""); final Set<String> initiallySet = new TreeSet<String>();
     * 
     * d.setInitialSelections(initiallySet.toArray()); if (d.open() ==
     * Window.OK) { // Figure out which changed Set<String> nowUnset = new
     * TreeSet<String>(initiallySet); Set<String> nowSet = new
     * TreeSet<String>(); for(Object o : d.getResult()) { if
     * (!nowUnset.remove(o)) { nowSet.add(o.toString()); } } // Update the
     * attributes for(String s : nowUnset) { a.setAttribute(s, "false"); }
     * for(String s : nowSet) { a.setAttribute(s, "true"); } if
     * (!nowSet.isEmpty() || !nowUnset.isEmpty()) { markAsDirty(); } }
     */
  }

  boolean pasteAnnotations(AnnotatedJavaElement target, IJavaElement source) {
    if (source instanceof AnnotationElement) {
      AnnotationElement orig = (AnnotationElement) source;
      return pasteAnnotation(target, orig);
    } else if (source instanceof AnnotatedJavaElement) {
      AnnotatedJavaElement src = (AnnotatedJavaElement) source;
      boolean changed = pasteAttachedAnnotations(target, src);

      if (src instanceof AbstractFunctionElement && target instanceof AbstractFunctionElement) {
        AbstractFunctionElement sf = (AbstractFunctionElement) src;
        AbstractFunctionElement tf = (AbstractFunctionElement) target;
        return pasteExtraAnnotationsForMethod(tf, sf) || changed;
      } else if (src instanceof ClassElement && target instanceof ClassElement) {
        ClassElement sc = (ClassElement) src;
        ClassElement tc = (ClassElement) target;
        return pasteExtraAnnotationsForType(tc, sc) || changed;
      }
      return changed;
    }
    return false;
  }

  private boolean pasteExtraAnnotationsForType(ClassElement tc, ClassElement sc) {
    boolean changed = false;
    // Match up methods
    for (MethodElement sm : sc.getMethods()) {
      MethodElement tm = tc.findMethod(sm.getName(), sm.getParams());
      if (tm != null) {
        changed |= pasteAnnotations(tm, sm);
      }
    }
    // Match up constructors
    for (ConstructorElement s : sc.getConstructors()) {
      ConstructorElement t = tc.findConstructor(s.getParams());
      if (t != null) {
        changed |= pasteAnnotations(t, s);
      }
    }
    return changed;
  }

  private boolean pasteExtraAnnotationsForMethod(AbstractFunctionElement target, AbstractFunctionElement src) {
    boolean changed = false;
    int i = 0;
    for (FunctionParameterElement sp : src.getParameters()) {
      if (sp == null) {
        continue;
      }
      FunctionParameterElement tp = target.getParameter(i);
      if (tp == null) {
        continue;
      }
      changed |= pasteAnnotations(tp, sp);
      i++;
    }
    return changed;
  }

  private boolean pasteAttachedAnnotations(AnnotatedJavaElement target, AnnotatedJavaElement src) {
    final List<String> couldBeNewAnnos = computePastableAnnos(target);
    boolean changed = false;
    for (AnnotationElement orig : src.getPromises()) {
      changed |= pasteAnnotation(couldBeNewAnnos, target, orig);
    }
    return changed;
  }

  private static final List<String> ONLY_PROMISE = Collections.singletonList(ScopedPromiseRules.PROMISE);

  private List<String> computePastableAnnos(AnnotatedJavaElement target) {
    final List<String> couldBeNewAnnos = computePossibleAnnos(target, null, false);
    if (couldBeNewAnnos.isEmpty()) {
      return ONLY_PROMISE;
    } else {
      couldBeNewAnnos.add(ScopedPromiseRules.PROMISE);
    }
    return couldBeNewAnnos;
  }

  private boolean pasteAnnotation(AnnotatedJavaElement target, AnnotationElement orig) {
    final List<String> couldBeNewAnnos = computePastableAnnos(target);
    return pasteAnnotation(couldBeNewAnnos, target, orig);
  }

  private boolean pasteAnnotation(List<String> couldBeNewAnnos, AnnotatedJavaElement target, AnnotationElement orig) {
    if (!couldBeNewAnnos.contains(orig.getPromise())) {
      return false;
    }
    AnnotationElement a = orig.cloneAsNew(target);
    boolean added = target.addPromise(a, false) == a;
    if (added) {
      a.markAsModified();
    }
    return added;
  }
}
