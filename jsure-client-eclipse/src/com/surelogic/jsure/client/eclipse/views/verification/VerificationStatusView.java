package com.surelogic.jsure.client.eclipse.views.verification;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.common.ui.dialogs.ImageDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;
import com.surelogic.jsure.client.eclipse.views.DropInfoUtility;
import com.surelogic.jsure.client.eclipse.views.problems.ProblemsView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.ModelingProblemFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.java.ISrcRef;

public final class VerificationStatusView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

  /**
   * Utility class used to persist column widths based upon the use's
   * preference.
   */
  static class ColumnResizeListener extends ControlAdapter {

    final String f_prefKey;

    public ColumnResizeListener(String prefKey) {
      f_prefKey = prefKey;
    }

    @Override
    public void controlResized(ControlEvent e) {
      if (e.widget instanceof TreeColumn) {
        int width = ((TreeColumn) e.widget).getWidth();
        EclipseUtility.setIntPreference(f_prefKey, width);
      }
    }
  }

  private static final String VIEW_STATE = "ResultsView_TreeViewerUIState";

  final File f_viewStatePersistenceFile;

  final public static Point ICONSIZE = new Point(22, 16);

  private PageBook f_viewerbook = null;

  private Label f_noResultsToShowLabel = null;
  private TreeViewer treeViewer;

  private Action doubleClickAction;

  public VerificationStatusView() {
    File viewState = null;
    try {
      final File jsureData = JSurePreferencesUtility.getJSureDataDirectory();
      if (jsureData != null) {
        viewState = new File(jsureData, VIEW_STATE + ".xml");
      } else {
        viewState = File.createTempFile(VIEW_STATE, ".xml");
      }
      // System.out.println("Using location: "+location);
    } catch (IOException e) {
      // Nothing to do
    }
    f_viewStatePersistenceFile = viewState;
  }

  @Override
  public void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
    treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL);
    treeViewer.setContentProvider(f_contentProvider);
    treeViewer.setSorter(new ViewerSorter() {

      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof Element && e2 instanceof Element) {
          return Element.JAVA.compare((Element) e1, (Element) e2);
        }
        return super.compare(viewer, e1, e2);
      }
    });
    ColumnViewerToolTipSupport.enableFor(treeViewer);

    treeViewer.getTree().setHeaderVisible(true);
    treeViewer.getTree().setLinesVisible(true);

    final TreeViewerColumn column1 = new TreeViewerColumn(treeViewer, SWT.LEFT);
    column1.setLabelProvider(ColumnLabelProviderUtility.TREE);
    column1.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL1_WIDTH));
    column1.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL1_WIDTH));
    TreeViewerColumn column2 = new TreeViewerColumn(treeViewer, SWT.LEFT);
    column2.setLabelProvider(ColumnLabelProviderUtility.PROJECT);
    column2.getColumn().setText("Project");
    column2.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL2_WIDTH));
    column2.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL2_WIDTH));
    TreeViewerColumn column3 = new TreeViewerColumn(treeViewer, SWT.LEFT);
    column3.setLabelProvider(ColumnLabelProviderUtility.PACKAGE);
    column3.getColumn().setText("Package");
    column3.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL3_WIDTH));
    column3.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL3_WIDTH));
    TreeViewerColumn column4 = new TreeViewerColumn(treeViewer, SWT.LEFT);
    column4.setLabelProvider(ColumnLabelProviderUtility.TYPE);
    column4.getColumn().setText("Type");
    column4.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL4_WIDTH));
    column4.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL4_WIDTH));
    TreeViewerColumn column5 = new TreeViewerColumn(treeViewer, SWT.RIGHT);
    column5.setLabelProvider(ColumnLabelProviderUtility.LINE);
    column5.getColumn().setText("Line");
    column5.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL5_WIDTH));
    column5.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL5_WIDTH));

    treeViewer.setInput(getViewSite());
    makeActions_private();
    hookContextMenu();
    treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        doubleClickAction.run();
      }
    });
    contributeToActionBars();
    // start empty until the initial build is done
    setViewerVisibility(false);

    finishCreatePartControl();

    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
  }

  @Override
  public void dispose() {
    try {
      JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
    } finally {
      super.dispose();
    }
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (treeViewer != null) {
          final TreeViewerUIState state = new TreeViewerUIState(treeViewer);
          finishCreatePartControl();
          state.restoreViewState(treeViewer);
        } else {
          SLLogger.getLogger().log(Level.WARNING, "treeViewer is null when the current scan is being changed", new Exception());
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private final VerificationStatusViewContentProvider f_contentProvider = new VerificationStatusViewContentProvider();

  private final Action f_actionShowInferences = new Action() {
    @Override
    public void run() {
      final boolean toggle = !f_contentProvider.showHints();
      f_contentProvider.setShowHints(toggle);
      // f_labelProvider.setShowInferences(toggle);
      setViewState();
      treeViewer.refresh();
    }
  };

  private final Action f_actionExpand = new Action() {
    @Override
    public void run() {
      final ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
      if (selection == null || selection.isEmpty()) {
        treeViewer.expandToLevel(10);
      } else {
        for (Object obj : selection.toList()) {
          if (obj != null) {
            treeViewer.expandToLevel(obj, 10);
          } else {
            treeViewer.expandToLevel(10);
          }
        }
      }
    }
  };

  private final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
      final ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();
      if (selection == null || selection.isEmpty()) {
        treeViewer.collapseAll();
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
  };

  private final Action f_selectIdenticalAncestor = new Action() {
    @Override
    public void run() {
      final ISelection selection = treeViewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        treeViewer.collapseAll();
      } else {
        final Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj instanceof ElementDrop) {
          ElementDrop e = ((ElementDrop) obj).getAncestorWithSameDropOrNull();
          if (e != null) {
            treeViewer.reveal(e);
            treeViewer.setSelection(new StructuredSelection(e), true);
          }
        }
      }
    }
  };

  private final Action f_copy = new Action() {
    @Override
    public void run() {
      final Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
      try {
        clipboard.setContents(new Object[] { getSelectedText() }, new Transfer[] { TextTransfer.getInstance() });
      } finally {
        clipboard.dispose();
      }
    }
  };

  private final Action f_addPromiseToCode = new ProposedPromisesRefactoringAction() {

    @Override
    protected List<IProposedPromiseDrop> getProposedDrops() {
      final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        return Collections.emptyList();
      }
      final List<IProposedPromiseDrop> proposals = new ArrayList<IProposedPromiseDrop>();
      for (final Object element : selection.toList()) {
        if (element instanceof ElementProposedPromiseDrop) {
          proposals.add(((ElementProposedPromiseDrop) element).getDrop());
        }
      }
      return proposals;
    }

    @Override
    protected String getDialogTitle() {
      return I18N.msg("jsure.eclipse.proposed.promise.edit");
    }
  };

  private final Action f_actionCollapseAll = new Action() {
    @Override
    public void run() {
      treeViewer.collapseAll();
    }
  };

  private final Action f_showQuickRef = new Action() {
    @Override
    public void run() {
      final Image quickRefImage = SLImages.getImage(CommonImages.IMG_JSURE_QUICK_REF);
      final Image icon = SLImages.getImage(CommonImages.IMG_JSURE_QUICK_REF_ICON);
      final ImageDialog dialog = new ImageDialog(EclipseUIUtility.getShell(), quickRefImage, icon, "Iconography Quick Reference");
      dialog.open();
    }
  };

  private final Action f_modelProblemsIndicator = new Action() {
    @Override
    public void run() {
      /*
       * When pressed open the JSure perspective
       */
      EclipseUIUtility.showView(ProblemsView.class.getName());
    }
  };

  /**
   * Toggles between the empty viewer page and the Fluid results
   */
  private void setViewerVisibility(boolean showResults) {
    if (f_viewerbook.isDisposed())
      return;
    if (showResults) {
      treeViewer.setInput(getViewSite());
      f_viewerbook.showPage(treeViewer.getControl());
    } else {
      f_viewerbook.showPage(f_noResultsToShowLabel);
    }
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        IStructuredSelection s = (IStructuredSelection) treeViewer.getSelection();
        VerificationStatusView.this.fillContextMenu_private(manager, s);
      }
    });
    Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
    treeViewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, treeViewer);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    fillLocalPullDown(bars.getMenuManager());
    fillLocalToolBar(bars.getToolBarManager());
  }

  private void fillContextMenu_private(IMenuManager manager, IStructuredSelection s) {
    fillContextMenu(manager, s);
    manager.add(new Separator());
    // Other plug-ins can contribute there actions here
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void makeActions_private() {
    doubleClickAction = new Action() {
      @Override
      public void run() {
        ISelection selection = treeViewer.getSelection();
        handleDoubleClick((IStructuredSelection) selection);
      }
    };

    makeActions();
    setViewState();
  }

  private String getSelectedText() {
    final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
    final StringBuilder sb = new StringBuilder();
    for (final Object elt : selection.toList()) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(ColumnLabelProviderUtility.TREE.getText(elt));
    }
    return sb.toString();
  }

  private void fillLocalPullDown(final IMenuManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_showQuickRef);
    manager.add(f_actionShowInferences);

    final IActionBars bars = getViewSite().getActionBars();
    bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_copy);
  }

  private void fillContextMenu(final IMenuManager manager, final IStructuredSelection s) {
    if (!s.isEmpty()) {
      final Object first = s.getFirstElement();
      if (first instanceof ElementProposedPromiseDrop) {
        manager.add(f_addPromiseToCode);
        manager.add(new Separator());
      }
    }
    if (!s.isEmpty()) {
      Object o = s.getFirstElement();
      if (o instanceof ElementDrop) {
        if (((ElementDrop) o).getAncestorWithSameDropOrNull() != null) {
          manager.add(f_selectIdenticalAncestor);
          manager.add(new Separator());
        }
      }
      manager.add(f_actionExpand);
      manager.add(f_actionCollapse);
      manager.add(new Separator());
      manager.add(f_copy);
    }
  }

  private void fillLocalToolBar(final IToolBarManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_showQuickRef);
    manager.add(f_actionShowInferences);
    manager.add(f_modelProblemsIndicator);
  }

  private void makeActions() {
    f_actionShowInferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_SUGGESTIONS_WARNINGS));

    f_actionExpand.setText("Expand");
    f_actionExpand.setToolTipText("Expand the current selection or all if none");
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText("Collapse");
    f_actionCollapse.setToolTipText("Collapse the current selection or all if none");
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionCollapseAll.setText("Collapse All");
    f_actionCollapseAll.setToolTipText("Collapse All");
    f_actionCollapseAll.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_selectIdenticalAncestor.setText("Select Identical Ancestor");
    f_selectIdenticalAncestor.setToolTipText("Select to the node that this element is identical to");
    f_selectIdenticalAncestor.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_UP));

    f_copy.setText("Copy");
    f_copy.setToolTipText("Copy the selected verification result to the clipboard");

    f_addPromiseToCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
    f_addPromiseToCode.setToolTipText(I18N.msg("jsure.eclipse.proposed.promise.tip"));
    f_addPromiseToCode.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));

    f_showQuickRef.setText("Show Iconography Quick Reference Card");
    f_showQuickRef.setToolTipText("Show the iconography quick reference card");
    f_showQuickRef.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_QUICK_REF_ICON));

    f_modelProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));
    f_modelProblemsIndicator.setEnabled(false);

    setViewState();
  }

  /**
   * Ensure that any relevant view state is set, based on the internal state
   */
  private void setViewState() {
    f_actionShowInferences.setChecked(f_contentProvider.showHints());
    f_actionShowInferences.setText("Show Information/Warning Results");
    f_actionShowInferences.setToolTipText("Show information and warning analysis results");
  }

  private void handleDoubleClick(final IStructuredSelection selection) {
    final Object obj = selection.getFirstElement();
    if (obj instanceof ElementDrop) {
      /*
       * Try to open an editor at the point this item references in the code
       */
      final ISrcRef srcRef = ((ElementDrop) obj).getDrop().getSrcRef();
      if (srcRef != null) {
        EditorUtil.highlightLineInJavaEditor(srcRef);
      }
    }
    // open up the tree one more level
    if (!treeViewer.getExpandedState(obj)) {
      treeViewer.expandToLevel(obj, 1);
    }
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  @Override
  public void setFocus() {
    setViewState();
    treeViewer.getControl().setFocus();
  }

  /*
   * For use by view contribution actions in other plug-ins so that they can get
   * a pointer to the TreeViewer
   */
  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
    if (adapter == TreeViewer.class) {
      return treeViewer;
    } else {
      return super.getAdapter(adapter);
    }
  }

  public void showDrop(IDrop d) {
    // Find the corresponding Content
    Object c = findContent(d);
    if (c == null) {
      findContent(d);
      return;
    }
    treeViewer.reveal(c);
    treeViewer.setSelection(new StructuredSelection(c), true);
  }

  private Object findContent(IDrop d) {
    // for (Object o : f_contentProvider.getElements(null)) {
    // Object rv = findContent((ResultsViewContent) o, d);
    // if (rv != null) {
    // return rv;
    // }
    // }
    return null;
  }

  // private Object findContent(ResultsViewContent c, IDrop d) {
  // if (c == null) {
  // return null;
  // }
  // if (d == c.getDropInfo()) {
  // return c;
  // }
  // for (Object o : f_contentProvider.getChildren(c)) {
  // Object rv = findContent((ResultsViewContent) o, d);
  // if (rv != null) {
  // return rv;
  // }
  // }
  // return null;
  // }

  private void finishCreatePartControl() {
    final JSureScanInfo scanInfo = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (scanInfo != null) {
      final long start = System.currentTimeMillis();
      f_contentProvider.buildModelOfDropSea_internal();
      final int modelProblemCount = getModelProblemCount(scanInfo);
      setModelProblemIndicatorState(modelProblemCount);
      final long end = System.currentTimeMillis();
      setViewerVisibility(true);
      System.out.println("Loaded snapshot for " + this + ": " + (end - start) + " ms");

      // Running too early?
      if (f_viewStatePersistenceFile != null && f_viewStatePersistenceFile.exists()) {
        f_viewerbook.getDisplay().asyncExec(new Runnable() {
          public void run() {
            final TreeViewerUIState state = TreeViewerUIState.loadFromFile(f_viewStatePersistenceFile);
            state.restoreViewState(treeViewer);
          }
        });
      }
    } else {
      // Show no results
      f_viewerbook.getDisplay().asyncExec(new Runnable() {
        public void run() {
          setViewerVisibility(false);
        }
      });
    }
  }

  @Override
  public void saveState(IMemento memento) {
    try {
      final TreeViewerUIState state = new TreeViewerUIState(treeViewer);
      state.saveToFile(f_viewStatePersistenceFile);
    } catch (IOException e) {
      SLLogger.getLogger().log(Level.WARNING,
          "Trouble when saving ResultsView UI state to " + f_viewStatePersistenceFile.getAbsolutePath(), e);
    }
  }

  private void setModelProblemIndicatorState(int problemCount) {
    final boolean problemsExist = problemCount > 0;
    final String id = problemsExist ? CommonImages.IMG_JSURE_MODEL_PROBLEMS_EXIST : CommonImages.IMG_JSURE_MODEL_PROBLEMS;
    f_modelProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(id));
    f_modelProblemsIndicator.setEnabled(problemsExist);
    final String tooltip;
    final String suffix = " in this scan...press to show the Modeling Problems view";
    if (problemCount < 1) {
      tooltip = "No modeling problems";
    } else if (problemCount == 1) {
      tooltip = "1 modeling problem" + suffix;
    } else {
      tooltip = problemCount + " modeling problems" + suffix;
    }
    f_modelProblemsIndicator.setToolTipText(tooltip);

  }

  private int getModelProblemCount(final JSureScanInfo info) {
    int result = 0;
    if (info != null) {
      for (IModelingProblemDrop id : info.getModelingProblemDrops()) {
        final String resource = DropInfoUtility.getResource(id);
        /*
         * We filter results based upon the resource.
         */
        if (ModelingProblemFilterUtility.showResource(resource))
          result++;
      }
    }
    return result;
  }
}