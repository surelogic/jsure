package com.surelogic.jsure.client.eclipse.views.status;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.java.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;
import com.surelogic.jsure.client.eclipse.views.problems.ProblemsView;
import com.surelogic.jsure.core.JSureUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class VerificationStatusView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

  private static final String VIEW_STATE_FILENAME = SLUtility.VIEW_PERSISTENCE_PREFIX
      + VerificationStatusView.class.getSimpleName() + SLUtility.DOT_XML;

  @NonNull
  final File f_viewStateFile;

  PageBook f_viewerbook = null;
  Label f_noResultsToShowLabel = null;
  TreeViewer f_treeViewer;
  @NonNull
  final VerificationStatusViewContentProvider f_contentProvider = new VerificationStatusViewContentProvider();
  TreeViewerColumn f_showDiffTableColumn = null;
  boolean f_showHints;
  boolean f_highlightDifferences;
  boolean f_showOnlyDifferences;
  boolean f_scanHasDiff;

  final ViewerSorter f_alphaSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof Element && e2 instanceof Element) {
        return Element.ALPHA.compare((Element) e1, (Element) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  final ViewerSorter f_javaSorter = new ViewerSorter() {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof Element && e2 instanceof Element) {
        return Element.JAVA.compare((Element) e1, (Element) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  final ViewerFilter f_showOnlyDifferencesFilter = new ViewerFilter() {
    @Override
    public boolean select(Viewer viewer, Object parent, Object e) {
      if (f_scanHasDiff) {
        if (e instanceof Element) {
          final Element check = (Element) e;
          return check.descendantHasDifference();
        } else
          return false;
      } else
        return true;
    }
  };

  void updateShowOnlyDifferencesFilter() {
    if (f_showOnlyDifferences) {
      f_treeViewer.addFilter(f_showOnlyDifferencesFilter);
    } else {
      f_treeViewer.removeFilter(f_showOnlyDifferencesFilter);
    }
  }

  public VerificationStatusView() {
    final File jsureData = JSurePreferencesUtility.getJSureDataDirectory();
    f_viewStateFile = new File(jsureData, VIEW_STATE_FILENAME);
  }

  @Override
  public void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
    f_treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.VIRTUAL);
    f_treeViewer.setContentProvider(f_contentProvider);

    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(ColumnLabelProviderUtility.TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_TREE_WIDTH));
    final TreeViewerColumn columnProject = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnProject.setLabelProvider(ColumnLabelProviderUtility.PROJECT);
    columnProject.getColumn().setText("Project");
    columnProject.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_PROJECT_WIDTH));
    columnProject.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_PROJECT_WIDTH));
    final TreeViewerColumn columnPackage = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnPackage.setLabelProvider(ColumnLabelProviderUtility.PACKAGE);
    columnPackage.getColumn().setText("Package");
    columnPackage.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_PACKAGE_WIDTH));
    columnPackage.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_PACKAGE_WIDTH));
    final TreeViewerColumn columnType = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnType.setLabelProvider(ColumnLabelProviderUtility.TYPE);
    columnType.getColumn().setText("Type");
    columnType.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_TYPE_WIDTH));
    columnType.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_TYPE_WIDTH));
    final TreeViewerColumn columnLine = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnLine.setLabelProvider(ColumnLabelProviderUtility.LINE);
    columnLine.getColumn().setText("Line");
    columnLine.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_LINE_WIDTH));
    columnLine.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_LINE_WIDTH));
    final TreeViewerColumn columnDiff = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnDiff.setLabelProvider(ColumnLabelProviderUtility.DIFF);
    columnDiff.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VSTATUS_COL_DIFF_WIDTH));
    columnDiff.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.VSTATUS_COL_DIFF_WIDTH));
    f_showDiffTableColumn = columnDiff;

    makeActions();
    hookContextMenu();
    contributeToActionBars();

    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);

    // setup a job to "fake" a scan change.
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        currentScanChanged(null);
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  @Override
  public void dispose() {
    try {
      JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
    } finally {
      super.dispose();
    }
  }

  final Action f_actionAlphaSort = new Action("", IAction.AS_RADIO_BUTTON) {
    @Override
    public void run() {
      final boolean alphabetical = f_actionAlphaSort.isChecked();
      setHowViewIsSorted(alphabetical);
    }
  };

  final Action f_actionJavaSort = new Action("", IAction.AS_RADIO_BUTTON) {
    @Override
    public void run() {
      final boolean java = f_actionJavaSort.isChecked();
      setHowViewIsSorted(!java);
    }
  };

  final Action f_actionShowHints = new Action("", IAction.AS_CHECK_BOX) {
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

  final Action f_actionHighlightDifferences = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionHighlightDifferences.isChecked();
      if (f_highlightDifferences != buttonChecked) {
        f_highlightDifferences = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VSTATUS_HIGHLIGHT_DIFFERENCES, f_highlightDifferences);
        f_contentProvider.setHighlightDifferences(f_highlightDifferences);
        f_treeViewer.refresh();
      }
    }
  };

  final Action f_actionShowOnlyDifferences = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowOnlyDifferences.isChecked();
      if (f_showOnlyDifferences != buttonChecked) {
        f_showOnlyDifferences = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VSTATUS_SHOW_ONLY_DIFFERENCES, f_showOnlyDifferences);
        updateShowOnlyDifferencesFilter();
      }
    }
  };

  final Action f_actionExpand = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      final Object o = s.getFirstElement();
      if (o != null) {
        f_treeViewer.getTree().setRedraw(false);
        f_treeViewer.expandToLevel(o, 5);
        f_treeViewer.getTree().setRedraw(true);
      }
    }
  };

  final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      final Object o = s.getFirstElement();
      if (o != null) {
        f_treeViewer.getTree().setRedraw(false);
        f_treeViewer.collapseToLevel(o, 1);
        f_treeViewer.getTree().setRedraw(true);
      }
    }
  };

  final Action f_actionCollapseAll = new Action() {
    @Override
    public void run() {
      f_treeViewer.collapseAll();
    }
  };

  final Action f_selectIdenticalAncestor = new Action() {
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

  final Action f_actionCopy = new Action() {
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

  final Action f_actionAddPromiseToCode = new ProposedPromisesRefactoringAction() {

    @Override
    protected List<IProposedPromiseDrop> getProposedDrops() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      if (!s.isEmpty()) {
        final List<IProposedPromiseDrop> proposals = new ArrayList<>();
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

  final Action f_actionShowQuickRef = new Action() {
    @Override
    public void run() {
      final Image quickRefImage = SLImages.getImage(CommonImages.IMG_JSURE_QUICK_REF);
      final Image icon = SLImages.getImage(CommonImages.IMG_JSURE_QUICK_REF_ICON);
      final ImageDialog dialog = new ImageDialog(EclipseUIUtility.getShell(), quickRefImage, icon, "Iconography Quick Reference");
      dialog.open();
    }
  };

  final Action f_actionProblemsIndicator = new Action() {
    @Override
    public void run() {
      /*
       * When pressed open the JSure perspective
       */
      EclipseUIUtility.showView(ProblemsView.class.getName());
    }
  };

  void makeActions() {
    f_treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          final Object first = s.getFirstElement();
          if (first instanceof ElementDrop) {
            // Try to open an editor at the point this item references
            final IJavaRef ref = ((ElementDrop) first).getDrop().getJavaRef();
            if (ref != null)
              Activator.highlightLineInJavaEditor(ref);
          }
        }
      }
    });
    f_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          final Object first = s.getFirstElement();
          // open up the tree one more level
          if (!f_treeViewer.getExpandedState(first)) {
            f_treeViewer.expandToLevel(first, 1);
          }
        }
      }
    });

    f_actionAlphaSort.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ALPHA_SORT));
    f_actionAlphaSort.setText(I18N.msg("jsure.eclipse.status.sort_alphabetically"));
    f_actionAlphaSort.setToolTipText(I18N.msg("jsure.eclipse.status.sort_alphabetically.tip"));
    f_actionJavaSort.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JAVA_SORT));
    f_actionJavaSort.setText(I18N.msg("jsure.eclipse.status.sort_java"));
    f_actionJavaSort.setToolTipText(I18N.msg("jsure.eclipse.status.sort_java.tip"));
    setHowViewIsSorted(EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VSTATUS_ALPHA_SORT));

    f_actionHighlightDifferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG));
    f_actionHighlightDifferences.setText(I18N.msg("jsure.eclipse.view.highlight_diffs"));
    f_actionHighlightDifferences.setToolTipText(I18N.msg("jsure.eclipse.view.highlight_diffs.tip"));
    f_highlightDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VSTATUS_HIGHLIGHT_DIFFERENCES);
    f_actionHighlightDifferences.setChecked(f_highlightDifferences);
    f_contentProvider.setHighlightDifferences(f_highlightDifferences);

    f_actionShowHints.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_SUGGESTIONS_WARNINGS));
    f_actionShowHints.setText(I18N.msg("jsure.eclipse.view.show_hints"));
    f_actionShowHints.setToolTipText(I18N.msg("jsure.eclipse.view.show_hints.tip"));
    f_showHints = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VSTATUS_SHOW_HINTS);
    f_actionShowHints.setChecked(f_showHints);

    f_actionShowOnlyDifferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG_ONLY));
    f_actionShowOnlyDifferences.setText(I18N.msg("jsure.eclipse.view.show_only_diffs"));
    f_actionShowOnlyDifferences.setToolTipText(I18N.msg("jsure.eclipse.view.show_only_diffs.tip"));
    f_showOnlyDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VSTATUS_SHOW_ONLY_DIFFERENCES);
    f_actionShowOnlyDifferences.setChecked(f_showOnlyDifferences);
    updateShowOnlyDifferencesFilter();

    f_actionExpand.setText(I18N.msg("jsure.eclipse.view.expand"));
    f_actionExpand.setToolTipText(I18N.msg("jsure.eclipse.view.expand.tip"));
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText(I18N.msg("jsure.eclipse.view.collapse"));
    f_actionCollapse.setToolTipText(I18N.msg("jsure.eclipse.view.collapse.tip"));
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionCopy.setText(I18N.msg("jsure.eclipse.view.copy"));
    f_actionCopy.setToolTipText(I18N.msg("jsure.eclipse.view.copy.tip"));

    f_actionCollapseAll.setText(I18N.msg("jsure.eclipse.view.collapse_all"));
    f_actionCollapseAll.setToolTipText(I18N.msg("jsure.eclipse.view.collapse_all.tip"));
    f_actionCollapseAll.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_selectIdenticalAncestor.setText(I18N.msg("jsure.eclipse.status.select_ancestor"));
    f_selectIdenticalAncestor.setToolTipText(I18N.msg("jsure.eclipse.status.select_ancestor.tip"));
    f_selectIdenticalAncestor.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_UP));

    f_actionAddPromiseToCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
    f_actionAddPromiseToCode.setToolTipText(I18N.msg("jsure.eclipse.proposed.promise.tip"));
    f_actionAddPromiseToCode.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));

    f_actionShowQuickRef.setText(I18N.msg("jsure.eclipse.view.show_iconography"));
    f_actionShowQuickRef.setToolTipText(I18N.msg("jsure.eclipse.view.show_iconography.tip"));
    f_actionShowQuickRef.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_QUICK_REF_ICON));

    f_actionProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));
    f_actionProblemsIndicator.setEnabled(false);
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
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
    pulldown.add(f_actionShowQuickRef);
    pulldown.add(new Separator());
    pulldown.add(f_actionJavaSort);
    pulldown.add(f_actionAlphaSort);
    pulldown.add(new Separator());
    pulldown.add(f_actionHighlightDifferences);
    pulldown.add(f_actionShowOnlyDifferences);
    pulldown.add(new Separator());
    pulldown.add(f_actionShowHints);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowQuickRef);
    toolbar.add(new Separator());
    toolbar.add(f_actionProblemsIndicator);
    toolbar.add(new Separator());
    toolbar.add(f_actionJavaSort);
    toolbar.add(f_actionAlphaSort);
    toolbar.add(new Separator());
    toolbar.add(f_actionHighlightDifferences);
    toolbar.add(f_actionShowOnlyDifferences);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowHints);
  }

  /**
   * Gets the text selected&mdash;used by the {@link #f_actionCopy} action.
   */
  String getSelectedText() {
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

  @Override
  public void currentScanChanged(JSureScan doNotUseInThisMethod) {
    final Job bkJob = new Job("Updating Verification Status to display selected scan") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Setting up view contents", 3);
        final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
        monitor.worked(1);
        final JSureScanInfo oldScan = JSureDataDirHub.getInstance().getLastMatchingScanInfo();
        monitor.worked(1);
        final UIJob job;
        if (scan != null) {
          final ScanDifferences diff = JSureDataDirHub.getInstance().getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull();
          final VerificationStatusViewContentProvider.Input newInput = new VerificationStatusViewContentProvider.Input(scan, diff,
              f_showHints);
          monitor.worked(1);
          job = new SLUIJob() {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
              f_scanHasDiff = oldScan != null;
              // Show results in a tree table
              if (f_showDiffTableColumn != null) {
                final String label = oldScan == null ? "No Prior Scan" : "Differences from scan of "
                    + oldScan.getProjects().getLabel() + " at " + SLUtility.toStringDayHMS(oldScan.getProjects().getDate());
                f_showDiffTableColumn.getColumn().setText(label);
              }
              f_treeViewer.getTree().setRedraw(false);
              final boolean viewsSaveTreeState = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VIEWS_SAVE_TREE_STATE);
              TreeViewerUIState state = null;
              if (viewsSaveTreeState) {
                if (f_contentProvider.isEmpty()) {
                  if (f_viewStateFile.exists()) {
                    state = TreeViewerUIState.loadFromFile(f_viewStateFile);
                  }
                } else {
                  state = new TreeViewerUIState(f_treeViewer);
                }
              }
              f_treeViewer.setInput(newInput);
              setModelProblemIndicatorState(JSureUtility.getInterestingModelingProblemCount(scan));
              if (state != null) {
                state.restoreViewState(f_treeViewer);
              }
              f_treeViewer.getTree().setRedraw(true);
              f_viewerbook.showPage(f_treeViewer.getControl());
              return Status.OK_STATUS;
            }
          };
        } else {
          monitor.worked(1);
          job = new SLUIJob() {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
              // Show no results
              f_viewerbook.showPage(f_noResultsToShowLabel);
              f_scanHasDiff = false;
              return Status.OK_STATUS;
            }
          };
        }
        job.schedule();
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    bkJob.schedule();
  }

  /**
   * Sets how the view is sorted: alphabetical or by Java location.
   * 
   * @param alphabetical
   *          {@code true} for alphabetical sorting of the view, {@code false}
   *          for Java location sorting.
   */
  void setHowViewIsSorted(boolean alphabetical) {
    f_actionAlphaSort.setChecked(alphabetical);
    f_actionJavaSort.setChecked(!alphabetical);
    f_treeViewer.setSorter(alphabetical ? f_alphaSorter : f_javaSorter);
    EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VSTATUS_ALPHA_SORT, alphabetical);
  }

  @Override
  public void saveState(IMemento memento) {
    final boolean viewsSaveTreeState = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VIEWS_SAVE_TREE_STATE);
    boolean deleteOnExit = false;
    if (viewsSaveTreeState) {
      try {
        final TreeViewerUIState state = new TreeViewerUIState(f_treeViewer);
        if (state.isEmptyExceptForSelections())
          deleteOnExit = true;
        else
          state.saveToFile(f_viewStateFile);
      } catch (IOException e) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(300, this.getClass().getSimpleName(), f_viewStateFile.getAbsolutePath()),
            e);
      }
    } else
      deleteOnExit = true;

    if (deleteOnExit && f_viewStateFile.exists()) {
      f_viewStateFile.deleteOnExit();
    }

    // obsolete file cleanup 4.4 release TODO remove next release
    final File obsolete = new File(JSurePreferencesUtility.getJSureDataDirectory(), "VerificationStatusView_TreeViewerUIState.xml");
    if (obsolete.exists())
      obsolete.deleteOnExit();
  }

  void setModelProblemIndicatorState(int problemCount) {
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
}
