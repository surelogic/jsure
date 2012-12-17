package com.surelogic.jsure.client.eclipse.views.explorer;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.ColumnResizeListener;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.common.ui.dialogs.ImageDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.views.problems.ProblemsView;
import com.surelogic.jsure.client.eclipse.views.status.VerificationStatusView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class VerificationExplorerView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

  private static final String VIEW_STATE = "VerificationExplorerView_TreeViewerUIState";

  private final File f_viewStatePersistenceFile;

  private PageBook f_viewerbook = null;
  private Label f_noResultsToShowLabel = null;
  private TreeViewer f_treeViewer;
  @NonNull
  private final VerificationExplorerViewContentProvider f_contentProvider = new VerificationExplorerViewContentProvider();
  private TreeViewerColumn f_showDiffTableColumn = null;
  private boolean f_highlightDifferences;
  private boolean f_showOnlyDifferences;
  private boolean f_showObsoleteDrops;
  private boolean f_showOnlyDerivedFromSrc;
  private boolean f_showAnalysisResults;
  private boolean f_showHints;

  private final ViewerSorter f_alphaLineSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof Element && e2 instanceof Element) {

        final int e1LineNumber = ((Element) e1).getLineNumber();
        final int e2LineNumber = ((Element) e2).getLineNumber();

        if (e1LineNumber > -1 && e2LineNumber > -1) {
          if (e1LineNumber != e2LineNumber)
            return e1LineNumber - e2LineNumber;
          else {
            boolean e1IsPromise = e1 instanceof ElementDrop ? ((ElementDrop) e1).getDrop() instanceof IPromiseDrop : false;
            boolean e2IsPromise = e2 instanceof ElementDrop ? ((ElementDrop) e2).getDrop() instanceof IPromiseDrop : false;
            if (e1IsPromise && !e2IsPromise)
              return -1;
            else if (!e1IsPromise && e2IsPromise)
              return 1;
          }
        } else if (e1LineNumber > -1 && e2LineNumber == -1)
          return -1;
        else if (e1LineNumber == -1 && e2LineNumber > -1)
          return 1;
        return Element.ALPHA.compare((Element) e1, (Element) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  public VerificationExplorerView() {
    File viewState = null;
    try {
      final File jsureData = JSurePreferencesUtility.getJSureDataDirectory();
      if (jsureData != null) {
        viewState = new File(jsureData, VIEW_STATE + ".xml");
      } else {
        viewState = File.createTempFile(VIEW_STATE, ".xml");
      }
    } catch (IOException ignore) {
      // Nothing to do
    }
    f_viewStatePersistenceFile = viewState;
  }

  @Override
  public void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
    f_treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    f_treeViewer.setContentProvider(f_contentProvider);
    f_treeViewer.setSorter(f_alphaLineSorter);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(ColumnLabelProviderUtility.TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VEXPLORER_COL_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VEXPLORER_COL_TREE_WIDTH));
    if (XUtil.useExperimental) {
      final TreeViewerColumn columnPosition = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
      columnPosition.setLabelProvider(ColumnLabelProviderUtility.POSITION);
      columnPosition.getColumn().setText("Position");
      columnPosition.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VEXPLORER_COL_LINE_WIDTH));
      columnPosition.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VEXPLORER_COL_LINE_WIDTH));
    }
    final TreeViewerColumn columnLine = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnLine.setLabelProvider(ColumnLabelProviderUtility.LINE);
    columnLine.getColumn().setText("Line");
    columnLine.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VEXPLORER_COL_LINE_WIDTH));
    columnLine.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VEXPLORER_COL_LINE_WIDTH));
    final TreeViewerColumn columnDiff = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnDiff.setLabelProvider(ColumnLabelProviderUtility.DIFF);
    columnDiff.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VEXPLORER_COL_DIFF_WIDTH));
    columnDiff.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VEXPLORER_COL_DIFF_WIDTH));
    f_showDiffTableColumn = columnDiff;

    makeActions();
    hookContextMenu();
    contributeToActionBars();

    // start empty until the initial build is done
    setViewerVisibility(false);

    showScanOrEmptyLabel();

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

  private final Action f_openProofContext = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      if (!s.isEmpty()) {
        final Object o = s.getFirstElement();
        if (o instanceof ElementDrop) {
          final IDrop drop = ((ElementDrop) o).getDrop();
          final VerificationStatusView view = (VerificationStatusView) EclipseUIUtility.showView(VerificationStatusView.class
              .getName());
          if (view != null)
            view.attemptToShowAndSelectDropInViewer(drop);
        }
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

  private final Action f_actionHighlightDifferences = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionHighlightDifferences.isChecked();
      if (f_highlightDifferences != buttonChecked) {
        f_highlightDifferences = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VEXPLORER_HIGHLIGHT_DIFFERENCES, f_highlightDifferences);
        f_contentProvider.setHighlightDifferences(f_highlightDifferences);
        f_treeViewer.refresh();
      }
    }
  };

  private final Action f_actionShowOnlyDifferences = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowOnlyDifferences.isChecked();
      if (f_showOnlyDifferences != buttonChecked) {
        f_showOnlyDifferences = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_ONLY_DIFFERENCES, f_showOnlyDifferences);
        currentScanChanged(null);
      }
    }
  };

  private final Action f_actionShowObsoleteDrops = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowObsoleteDrops.isChecked();
      if (f_showObsoleteDrops != buttonChecked) {
        f_showObsoleteDrops = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_OBSOLETE_DROP_DIFFERENCES, f_showObsoleteDrops);
        currentScanChanged(null);
      }
    }
  };

  private final Action f_actionShowOnlyDerivedFromSrc = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowOnlyDerivedFromSrc.isChecked();
      if (f_showOnlyDerivedFromSrc != buttonChecked) {
        f_showOnlyDerivedFromSrc = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_ONLY_DERIVED_FROM_SRC, f_showOnlyDerivedFromSrc);
        currentScanChanged(null);
      }
    }
  };

  private final Action f_actionShowAnalysisResults = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowAnalysisResults.isChecked();
      if (f_showAnalysisResults != buttonChecked) {
        f_showAnalysisResults = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_ANALYSIS_RESULTS, f_showAnalysisResults);
        currentScanChanged(null);
      }
    }
  };

  private final Action f_actionShowHints = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowHints.isChecked();
      if (f_showHints != buttonChecked) {
        f_showHints = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_HINTS, f_showHints);
        currentScanChanged(null);
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
            final IJavaRef ref = ((ElementDrop) first).getDrop().getJavaRef();
            if (ref != null)
              Activator.highlightLineInJavaEditor(ref);
          }
          // open up the tree one more level
          if (!f_treeViewer.getExpandedState(first)) {
            f_treeViewer.expandToLevel(first, 1);
          }
        }
      }
    });

    f_actionCollapseAll.setText("Collapse All");
    f_actionCollapseAll.setToolTipText("Collapse All");
    f_actionCollapseAll.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionShowQuickRef.setText("Show Iconography Quick Reference Card");
    f_actionShowQuickRef.setToolTipText("Show the iconography quick reference card");
    f_actionShowQuickRef.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_QUICK_REF_ICON));

    f_actionProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));
    f_actionProblemsIndicator.setEnabled(false);

    f_actionHighlightDifferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG));
    f_actionHighlightDifferences.setText("Highlight Differences");
    f_actionHighlightDifferences.setToolTipText("Highlight differences from the last scan");
    f_highlightDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_HIGHLIGHT_DIFFERENCES);
    f_actionHighlightDifferences.setChecked(f_highlightDifferences);
    f_contentProvider.setHighlightDifferences(f_highlightDifferences);

    f_actionShowOnlyDifferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG_ONLY));
    f_actionShowOnlyDifferences.setText("Show Only Differences");
    f_actionShowOnlyDifferences.setToolTipText("Show only differences from the last scan");
    f_showOnlyDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_ONLY_DIFFERENCES);
    f_actionShowOnlyDifferences.setChecked(f_showOnlyDifferences);

    f_actionShowObsoleteDrops.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG_OLD_SCAN_ONLY));
    f_actionShowObsoleteDrops.setText("Show Obsolete Results");
    f_actionShowObsoleteDrops.setToolTipText("Show obsolete results from the last scan");
    f_showObsoleteDrops = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_OBSOLETE_DROP_DIFFERENCES);
    f_actionShowObsoleteDrops.setChecked(f_showObsoleteDrops);

    f_actionShowOnlyDerivedFromSrc.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JAVA_COMP_UNIT));
    f_actionShowOnlyDerivedFromSrc.setText("Show Only Results Derived From Source");
    f_actionShowOnlyDerivedFromSrc.setToolTipText("Show only results derived from Java source code (directly or indirectly)");
    f_showOnlyDerivedFromSrc = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_ONLY_DERIVED_FROM_SRC);
    f_actionShowOnlyDerivedFromSrc.setChecked(f_showOnlyDerivedFromSrc);

    f_actionShowAnalysisResults.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANALYSIS_RESULT));
    f_actionShowAnalysisResults.setText("Show Analysis Results");
    f_actionShowAnalysisResults.setToolTipText("Show analysis results about the code");
    f_showAnalysisResults = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_ANALYSIS_RESULTS);
    f_actionShowAnalysisResults.setChecked(f_showAnalysisResults);

    f_actionShowHints.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_SUGGESTIONS_WARNINGS));
    f_actionShowHints.setText("Show Information/Warning Hints");
    f_actionShowHints.setToolTipText("Show information and warning hints about the code");
    f_showHints = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_SHOW_HINTS);
    f_actionShowHints.setChecked(f_showHints);

    f_openProofContext.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_LOGO));
    f_openProofContext.setText("Open In Proof Context");
    f_openProofContext.setToolTipText("Open this result in the Verification Status view to show it within its proof context");

    f_actionExpand.setText("Expand");
    f_actionExpand.setToolTipText("Expand the current selection or all if none");
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText("Collapse");
    f_actionCollapse.setToolTipText("Collapse the current selection or all if none");
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionCopy.setText("Copy");
    f_actionCopy.setToolTipText("Copy the selected verification result to the clipboard");

  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(final IMenuManager manager) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          if (s.getFirstElement() instanceof ElementDrop) {
            manager.add(f_openProofContext);
            manager.add(new Separator());
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
    pulldown.add(f_actionShowQuickRef);
    pulldown.add(new Separator());
    pulldown.add(f_actionHighlightDifferences);
    pulldown.add(f_actionShowOnlyDifferences);
    pulldown.add(f_actionShowObsoleteDrops);
    pulldown.add(new Separator());
    pulldown.add(f_actionShowOnlyDerivedFromSrc);
    pulldown.add(f_actionShowAnalysisResults);
    pulldown.add(f_actionShowHints);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowQuickRef);
    toolbar.add(new Separator());
    toolbar.add(f_actionProblemsIndicator);
    toolbar.add(new Separator());
    toolbar.add(f_actionHighlightDifferences);
    toolbar.add(f_actionShowOnlyDifferences);
    toolbar.add(f_actionShowObsoleteDrops);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowOnlyDerivedFromSrc);
    toolbar.add(f_actionShowAnalysisResults);
    toolbar.add(f_actionShowHints);
  }

  private void showScanOrEmptyLabel() {
    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    final JSureScanInfo oldScan = JSureDataDirHub.getInstance().getLastMatchingScanInfo();
    if (scan != null) {
      if (f_showDiffTableColumn != null) {
        final String label = oldScan == null ? "No Prior Scan" : "Differences from scan of " + oldScan.getProjects().getLabel()
            + " at " + SLUtility.toStringDayHMS(oldScan.getProjects().getDate());
        f_showDiffTableColumn.getColumn().setText(label);
      }
      final ScanDifferences diff = JSureDataDirHub.getInstance().getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull();
      f_contentProvider.changeContentsToCurrentScan(scan, oldScan, diff, f_showOnlyDifferences, f_showObsoleteDrops,
          f_showOnlyDerivedFromSrc, f_showAnalysisResults, f_showHints);
      final int modelProblemCount = getModelProblemCount(scan);
      setModelProblemIndicatorState(modelProblemCount);
      setViewerVisibility(true);

      // Running too early?
      if (f_viewStatePersistenceFile != null && f_viewStatePersistenceFile.exists()) {
        EclipseUIUtility.asyncExec(new Runnable() {
          public void run() {
            final TreeViewerUIState state = TreeViewerUIState.loadFromFile(f_viewStatePersistenceFile);
            state.restoreViewState(f_treeViewer);
          }
        });
      }
    } else {
      // Show no results
      EclipseUIUtility.asyncExec(new Runnable() {
        public void run() {
          setViewerVisibility(false);
        }
      });
    }
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

  @Override
  public void setFocus() {
    f_treeViewer.getControl().setFocus();
  }

  @Override
  public void currentScanChanged(JSureScan doNotUseInThisMethod) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (f_treeViewer != null) {
          final TreeViewerUIState state = new TreeViewerUIState(f_treeViewer);
          showScanOrEmptyLabel();
          state.restoreViewState(f_treeViewer);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
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
