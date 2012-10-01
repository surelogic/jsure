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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import com.surelogic.common.IJavaRef;
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
import com.surelogic.jsure.client.eclipse.views.problems.ProblemsView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

public final class VerificationStatusView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

  /**
   * Utility class used to persist column widths based upon the use's
   * preference.
   */
  private static class ColumnResizeListener extends ControlAdapter {

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

  private static final String VIEW_STATE = "VerificationStatusView_TreeViewerUIState";

  private final File f_viewStatePersistenceFile;

  public static final Point ICONSIZE = new Point(22, 16);

  private PageBook f_viewerbook = null;
  private Label f_noResultsToShowLabel = null;
  private TreeViewer f_treeViewer;
  private final VerificationStatusViewContentProvider f_contentProvider = new VerificationStatusViewContentProvider();
  private boolean f_showHints;
  private final ViewerSorter f_alphaSorter = new ViewerSorter() {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof Element && e2 instanceof Element) {
        return Element.ALPHA.compare((Element) e1, (Element) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };
  private final ViewerSorter f_javaSorter = new ViewerSorter() {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof Element && e2 instanceof Element) {
        return Element.JAVA.compare((Element) e1, (Element) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  public VerificationStatusView() {
    File viewState = null;
    try {
      final File jsureData = JSurePreferencesUtility.getJSureDataDirectory();
      if (jsureData != null) {
        viewState = new File(jsureData, VIEW_STATE + ".xml");
      } else {
        viewState = File.createTempFile(VIEW_STATE, ".xml");
      }
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
    f_treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL);
    f_treeViewer.setContentProvider(f_contentProvider);
    f_treeViewer.setSorter(new ViewerSorter() {

      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof Element && e2 instanceof Element) {
          return Element.JAVA.compare((Element) e1, (Element) e2);
        }
        return super.compare(viewer, e1, e2);
      }
    });
    ColumnViewerToolTipSupport.enableFor(f_treeViewer);

    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);

    final TreeViewerColumn column1 = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    column1.setLabelProvider(ColumnLabelProviderUtility.TREE);
    column1.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL1_WIDTH));
    column1.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL1_WIDTH));
    TreeViewerColumn column2 = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    column2.setLabelProvider(ColumnLabelProviderUtility.PROJECT);
    column2.getColumn().setText("Project");
    column2.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL2_WIDTH));
    column2.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL2_WIDTH));
    TreeViewerColumn column3 = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    column3.setLabelProvider(ColumnLabelProviderUtility.PACKAGE);
    column3.getColumn().setText("Package");
    column3.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL3_WIDTH));
    column3.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL3_WIDTH));
    TreeViewerColumn column4 = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    column4.setLabelProvider(ColumnLabelProviderUtility.TYPE);
    column4.getColumn().setText("Type");
    column4.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL4_WIDTH));
    column4.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL4_WIDTH));
    TreeViewerColumn column5 = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    column5.setLabelProvider(ColumnLabelProviderUtility.LINE);
    column5.getColumn().setText("Line");
    column5.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL5_WIDTH));
    column5.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL5_WIDTH));

    f_treeViewer.setInput(getViewSite());

    makeActions();
    hookContextMenu();
    contributeToActionBars();

    // start empty until the initial build is done
    setViewerVisibility(false);

    showScanOrEmptyLabel(f_showHints);

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
  public void currentScanChanged(JSureScan doNotUseInThisMethod) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (f_treeViewer != null) {
          final TreeViewerUIState state = new TreeViewerUIState(f_treeViewer);
          showScanOrEmptyLabel(f_showHints);
          state.restoreViewState(f_treeViewer);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private final Action f_actionAlphaSort = new Action("", IAction.AS_RADIO_BUTTON) {
    @Override
    public void run() {
      final boolean alphabetical = f_actionAlphaSort.isChecked();
      setHowViewIsSorted(alphabetical);
    }
  };

  private final Action f_actionJavaSort = new Action("", IAction.AS_RADIO_BUTTON) {
    @Override
    public void run() {
      final boolean java = f_actionJavaSort.isChecked();
      setHowViewIsSorted(!java);
    }
  };

  private final Action f_actionShowHints = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowHints.isChecked();
      if (f_showHints != buttonChecked) {
        f_showHints = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VSTATUS_SHOW_HINTS, f_showHints);
        currentScanChanged(null);
      }
    }
  };

  private final Action f_actionExpand = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      if (!s.isEmpty()) {
        for (Object element : s.toList()) {
          if (element != null) {
            f_treeViewer.expandToLevel(element, 5);
          } else {
            f_treeViewer.expandToLevel(5);
          }
        }
      } else {
        f_treeViewer.expandToLevel(5);
      }
    }
  };

  private final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      if (!s.isEmpty()) {
        for (Object element : s.toList()) {
          if (element != null) {
            f_treeViewer.collapseToLevel(element, 1);
          } else {
            f_treeViewer.collapseAll();
          }
        }
      } else {
        f_treeViewer.collapseAll();
      }
    }
  };

  private final Action f_actionCollapseAll = new Action() {
    @Override
    public void run() {
      f_treeViewer.collapseAll();
    }
  };

  private final Action f_selectIdenticalAncestor = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      if (!s.isEmpty()) {
        final Object first = s.getFirstElement();
        if (first instanceof ElementDrop) {
          ElementDrop e = ((ElementDrop) first).getAncestorWithSameDropOrNull();
          if (e != null) {
            f_treeViewer.reveal(e);
            f_treeViewer.setSelection(new StructuredSelection(e), true);
          }
        }
      }
    }
  };

  private final Action f_actionCopy = new Action() {
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

  private final Action f_actionAddPromiseToCode = new ProposedPromisesRefactoringAction() {

    @Override
    protected List<IProposedPromiseDrop> getProposedDrops() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      if (!s.isEmpty()) {
        final List<IProposedPromiseDrop> proposals = new ArrayList<IProposedPromiseDrop>();
        for (final Object element : s.toList()) {
          if (element instanceof ElementProposedPromiseDrop) {
            proposals.add(((ElementProposedPromiseDrop) element).getDrop());
          }
        }
        return proposals;
      } else {
        return Collections.emptyList();
      }
    }

    @Override
    protected String getDialogTitle() {
      return I18N.msg("jsure.eclipse.proposed.promise.edit");
    }
  };

  private final Action f_actionShowQuickRef = new Action() {
    @Override
    public void run() {
      final Image quickRefImage = SLImages.getImage(CommonImages.IMG_JSURE_QUICK_REF);
      final Image icon = SLImages.getImage(CommonImages.IMG_JSURE_QUICK_REF_ICON);
      final ImageDialog dialog = new ImageDialog(EclipseUIUtility.getShell(), quickRefImage, icon, "Iconography Quick Reference");
      dialog.open();
    }
  };

  private final Action f_actionProblemsIndicator = new Action() {
    @Override
    public void run() {
      /*
       * When pressed open the JSure perspective
       */
      EclipseUIUtility.showView(ProblemsView.class.getName());
    }
  };

  private void makeActions() {
    f_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          final Object first = s.getFirstElement();
          if (first instanceof ElementDrop) {
            /*
             * Try to open an editor at the point this item references in the
             * code
             */
            // final ISrcRef srcRef = ((ElementDrop)
            // first).getDrop().getSrcRef();
            // if (srcRef != null) {
            // EditorUtil.highlightLineInJavaEditor(srcRef);
            // }
            final IJavaRef ref = ((ElementDrop) first).getDrop().getJavaRef();
            if (ref != null)
              EditorUtil.highlightLineInJavaEditor(ref);
          }
          // open up the tree one more level
          if (!f_treeViewer.getExpandedState(first)) {
            f_treeViewer.expandToLevel(first, 1);
          }
        }
      }
    });

    f_actionAlphaSort.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ALPHA_SORT));
    f_actionAlphaSort.setText("Sort contents alphabetically");
    f_actionAlphaSort.setToolTipText("Sort contents alphabetically");
    f_actionJavaSort.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JAVA_SORT));
    f_actionJavaSort.setText("Sort contents by Java location");
    f_actionJavaSort.setToolTipText("Sort contents by Java location");
    setHowViewIsSorted(EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VSTATUS_ALPHA_SORT));

    f_actionShowHints.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_SUGGESTIONS_WARNINGS));
    f_actionShowHints.setText("Show Information/Warning Hints");
    f_actionShowHints.setToolTipText("Show information and warning hints about the code");
    f_showHints = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VSTATUS_SHOW_HINTS);
    f_actionShowHints.setChecked(f_showHints);

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

    f_actionCopy.setText("Copy");
    f_actionCopy.setToolTipText("Copy the selected verification result to the clipboard");

    f_actionAddPromiseToCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
    f_actionAddPromiseToCode.setToolTipText(I18N.msg("jsure.eclipse.proposed.promise.tip"));
    f_actionAddPromiseToCode.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));

    f_actionShowQuickRef.setText("Show Iconography Quick Reference Card");
    f_actionShowQuickRef.setToolTipText("Show the iconography quick reference card");
    f_actionShowQuickRef.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_QUICK_REF_ICON));

    f_actionProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));
    f_actionProblemsIndicator.setEnabled(false);
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(final IMenuManager manager) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          final Object first = s.getFirstElement();
          /*
           * Proposed promise?
           */
          if (first instanceof ElementProposedPromiseDrop) {
            manager.add(f_actionAddPromiseToCode);
            manager.add(new Separator());
          }
          /*
           * Link to identical ancestor?
           */
          if (first instanceof ElementDrop) {
            if (((ElementDrop) first).getAncestorWithSameDropOrNull() != null) {
              manager.add(f_selectIdenticalAncestor);
              manager.add(new Separator());
            }
          }
          manager.add(f_actionExpand);
          manager.add(f_actionCollapse);
          manager.add(new Separator());
          manager.add(f_actionCopy);
        }
        manager.add(new Separator());
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });
    Menu menu = menuMgr.createContextMenu(f_treeViewer.getControl());
    f_treeViewer.getControl().setMenu(menu);
    getSite().registerContextMenu(menuMgr, f_treeViewer);
  }

  private void contributeToActionBars() {
    final IActionBars bars = getViewSite().getActionBars();

    bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_actionCopy);

    final IMenuManager pulldown = bars.getMenuManager();
    pulldown.add(f_actionCollapseAll);
    pulldown.add(new Separator());
    pulldown.add(f_actionJavaSort);
    pulldown.add(f_actionAlphaSort);
    pulldown.add(new Separator());
    pulldown.add(f_actionShowQuickRef);
    pulldown.add(f_actionShowHints);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionProblemsIndicator);
    toolbar.add(new Separator());
    toolbar.add(f_actionJavaSort);
    toolbar.add(f_actionAlphaSort);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowQuickRef);
    toolbar.add(f_actionShowHints);
  }

  /**
   * Gets the text selected&mdash;used by the {@link #f_actionCopy} action.
   */
  private String getSelectedText() {
    final IStructuredSelection selection = (IStructuredSelection) f_treeViewer.getSelection();
    final StringBuilder sb = new StringBuilder();
    for (final Object elt : selection.toList()) {
      if (elt instanceof Element) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(((Element) elt).getLabel());
      }
    }
    return sb.toString();
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  @Override
  public void setFocus() {
    f_treeViewer.getControl().setFocus();
  }

  /*
   * For use by view contribution actions in other plug-ins so that they can get
   * a pointer to the TreeViewer
   */
  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
    if (adapter == TreeViewer.class) {
      return f_treeViewer;
    } else {
      return super.getAdapter(adapter);
    }
  }

  /**
   * Attempts to show and select the passed drop in the viewer. Does nothing if
   * the drop is {@code null} or can't be found in the scan being displayed by
   * this view.
   * 
   * @param drop
   *          a drop.
   */
  public void attemptToShowAndSelectDropInViewer(final IDrop drop) {
    if (f_contentProvider == null || drop == null)
      return;

    Element c = f_contentProvider.findElementForDropOrNull(drop);
    if (c == null)
      return;

    f_treeViewer.reveal(c);
    f_treeViewer.setSelection(new StructuredSelection(c), true);
  }

  private void showScanOrEmptyLabel(boolean showHints) {
    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (scan != null) {
      // show the scan results
      f_contentProvider.changeContentsToCurrentScan(scan, showHints);
      final int modelProblemCount = getModelProblemCount(scan);
      setModelProblemIndicatorState(modelProblemCount);
      setViewerVisibility(true);

      // Running too early?
      if (f_viewStatePersistenceFile != null && f_viewStatePersistenceFile.exists()) {
        f_viewerbook.getDisplay().asyncExec(new Runnable() {
          public void run() {
            final TreeViewerUIState state = TreeViewerUIState.loadFromFile(f_viewStatePersistenceFile);
            state.restoreViewState(f_treeViewer);
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

  /**
   * Sets how the view is sorted: alphabetical or by Java location.
   * 
   * @param alphabetical
   *          {@code true} for alphabetical sorting of the view, {@code false}
   *          for Java location sorting.
   */
  private void setHowViewIsSorted(boolean alphabetical) {
    f_actionAlphaSort.setChecked(alphabetical);
    f_actionJavaSort.setChecked(!alphabetical);
    f_treeViewer.setSorter(alphabetical ? f_alphaSorter : f_javaSorter);
    EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VSTATUS_ALPHA_SORT, alphabetical);
  }

  /**
   * Toggles between the empty viewer page and the Fluid results
   */
  private void setViewerVisibility(boolean showResults) {
    if (f_viewerbook.isDisposed())
      return;
    if (showResults) {
      f_treeViewer.setInput(getViewSite());
      f_viewerbook.showPage(f_treeViewer.getControl());
    } else {
      f_viewerbook.showPage(f_noResultsToShowLabel);
    }
  }

  @Override
  public void saveState(IMemento memento) {
    try {
      final TreeViewerUIState state = new TreeViewerUIState(f_treeViewer);
      state.saveToFile(f_viewStatePersistenceFile);
    } catch (IOException e) {
      SLLogger.getLogger().log(Level.WARNING,
          "Trouble when saving ResultsView UI state to " + f_viewStatePersistenceFile.getAbsolutePath(), e);
    }
  }

  private void setModelProblemIndicatorState(int problemCount) {
    final boolean problemsExist = problemCount > 0;
    final String id = problemsExist ? CommonImages.IMG_JSURE_MODEL_PROBLEMS_EXIST : CommonImages.IMG_JSURE_MODEL_PROBLEMS;
    f_actionProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(id));
    f_actionProblemsIndicator.setEnabled(problemsExist);
    final String tooltip;
    final String suffix = " in this scan...press to show the Modeling Problems view";
    if (problemCount < 1) {
      tooltip = "No modeling problems";
    } else if (problemCount == 1) {
      tooltip = "1 modeling problem" + suffix;
    } else {
      tooltip = problemCount + " modeling problems" + suffix;
    }
    f_actionProblemsIndicator.setToolTipText(tooltip);

  }

  private int getModelProblemCount(final JSureScanInfo info) {
    int result = 0;
    if (info != null) {
      for (IModelingProblemDrop problem : info.getModelingProblemDrops()) {
        /*
         * We filter results based upon the resource.
         */
        if (UninterestingPackageFilterUtility.keep(problem))
          result++;
      }
    }
    return result;
  }
}
