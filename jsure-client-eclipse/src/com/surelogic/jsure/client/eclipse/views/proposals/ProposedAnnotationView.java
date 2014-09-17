package com.surelogic.jsure.client.eclipse.views.proposals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;
import com.surelogic.jsure.client.eclipse.views.status.VerificationStatusView;
import com.surelogic.jsure.core.preferences.IUninterestingPackageFilterObserver;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class ProposedAnnotationView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener,
    IUninterestingPackageFilterObserver {

  private static final String VIEW_STATE_FILENAME = SLUtility.VIEW_PERSISTENCE_PREFIX
      + ProposedAnnotationView.class.getSimpleName() + SLUtility.DOT_XML;

  @NonNull
  final File f_viewStateFile;

  PageBook f_viewerbook = null;
  Label f_noResultsToShowLabel = null;
  TreeViewer f_treeViewer;
  @NonNull
  final ProposedAnnotationViewContentProvider f_contentProvider = new ProposedAnnotationViewContentProvider();
  boolean f_highlightDifferences;

  boolean f_showOnlyAbductive;

  public boolean getShowOnlyAbductive() {
    return f_showOnlyAbductive;
  }

  boolean f_showOnlyDifferences;

  public boolean getShowOnlyDifferences() {
    return f_showOnlyDifferences;
  }

  boolean f_showOnlyFromSrc;

  public boolean getShowOnlyFromSrc() {
    return f_showOnlyFromSrc;
  }

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

  final ViewerFilter f_showOnlyDifferencesFilter = new ViewerFilter() {
    @Override
    public boolean select(Viewer viewer, Object parent, Object e) {
      if (e instanceof Element) {
        final Element check = (Element) e;
        return check.descendantHasDifference();
      }
      return false;
    }
  };

  void updateShowOnlyDifferencesFilter() {
    if (f_showOnlyDifferences) {
      f_treeViewer.addFilter(f_showOnlyDifferencesFilter);
    } else {
      f_treeViewer.removeFilter(f_showOnlyDifferencesFilter);
    }
  }

  public ProposedAnnotationView() {
    final File jsureData = JSurePreferencesUtility.getJSureDataDirectory();
    f_viewStateFile = new File(jsureData, VIEW_STATE_FILENAME);
  }

  @Override
  public void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
    f_treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
    f_treeViewer.setContentProvider(f_contentProvider);
    f_treeViewer.setSorter(f_alphaLineSorter);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);

    final ISelectionChangedListener listener = new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        selectionChangedHelper();
      }
    };
    f_treeViewer.addSelectionChangedListener(listener);

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(ColumnLabelProviderUtility.TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.PROPOSED_ANNO_COL_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.PROPOSED_ANNO_COL_TREE_WIDTH));
    final TreeViewerColumn columnLine = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnLine.setLabelProvider(ColumnLabelProviderUtility.LINE);
    columnLine.getColumn().setText("Line");
    columnLine.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.PROPOSED_ANNO_COL_LINE_WIDTH));
    columnLine.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.PROPOSED_ANNO_COL_LINE_WIDTH));

    makeActions();
    hookContextMenu();
    contributeToActionBars();

    // start empty until the initial build is done
    // setViewerVisibility(false);

    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
    UninterestingPackageFilterUtility.registerObserver(this);

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
      UninterestingPackageFilterUtility.unregisterObserver(this);
    } finally {
      super.dispose();
    }
  }

  final Action f_actionHighlightDifferences = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionHighlightDifferences.isChecked();
      if (f_highlightDifferences != buttonChecked) {
        f_highlightDifferences = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNO_HIGHLIGHT_DIFFERENCES, f_highlightDifferences);
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
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNO_SHOW_ONLY_DIFFERENCES, f_showOnlyDifferences);
        updateShowOnlyDifferencesFilter();
      }
    }
  };

  final Action f_actionShowOnlyFromSrc = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowOnlyFromSrc.isChecked();
      if (f_showOnlyFromSrc != buttonChecked) {
        f_showOnlyFromSrc = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNO_SHOW_ONLY_FROM_SRC, f_showOnlyFromSrc);
        currentScanChanged(null);
      }
    }
  };

  final Action f_openProofContext = new Action() {
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

  final Action f_actionAnnotateCode = new ProposedPromisesRefactoringAction() {
    @Override
    protected List<IProposedPromiseDrop> getProposedDrops() {
      return getSelectedProposals();
    }

    @Override
    protected String getDialogTitle() {
      return I18N.msg("jsure.eclipse.proposed.promise.edit");
    }
  };

  final Action f_actionShowOnlyAbductive = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowOnlyAbductive.isChecked();
      if (f_showOnlyAbductive != buttonChecked) {
        f_showOnlyAbductive = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNOTATIONS_SHOW_ABDUCTIVE_ONLY, f_showOnlyAbductive);
        currentScanChanged(null);
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

  private void makeActions() {
    f_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
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

    f_openProofContext.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_LOGO));
    f_openProofContext.setText(I18N.msg("jsure.eclipse.proposed.promises.openInProofContext"));
    f_openProofContext.setToolTipText(I18N.msg("jsure.eclipse.proposed.promises.openInProofContext.tip"));

    f_actionCollapseAll.setText(I18N.msg("jsure.eclipse.view.collapse_all"));
    f_actionCollapseAll.setToolTipText(I18N.msg("jsure.eclipse.view.collapse_all.tip"));
    f_actionCollapseAll.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionAnnotateCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
    f_actionAnnotateCode.setToolTipText(I18N.msg("jsure.eclipse.proposed.promise.tip"));
    f_actionAnnotateCode.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));
    f_actionAnnotateCode.setEnabled(false); // wait until something is selected

    f_actionShowOnlyAbductive.setText(I18N.msg("jsure.eclipse.proposed.promises.showAbductiveOnly"));
    f_actionShowOnlyAbductive.setToolTipText(I18N.msg("jsure.eclipse.proposed.promises.showAbductiveOnly.tip"));
    f_actionShowOnlyAbductive.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_ABDUCTIVE));
    f_showOnlyAbductive = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNOTATIONS_SHOW_ABDUCTIVE_ONLY);
    f_actionShowOnlyAbductive.setChecked(f_showOnlyAbductive);

    f_actionExpand.setText(I18N.msg("jsure.eclipse.view.expand"));
    f_actionExpand.setToolTipText(I18N.msg("jsure.eclipse.view.expand.tip"));
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText(I18N.msg("jsure.eclipse.view.collapse"));
    f_actionCollapse.setToolTipText(I18N.msg("jsure.eclipse.view.collapse.tip"));
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionCopy.setText(I18N.msg("jsure.eclipse.view.copy"));
    f_actionCopy.setToolTipText(I18N.msg("jsure.eclipse.view.copy.tip"));
    f_actionHighlightDifferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG));
    f_actionHighlightDifferences.setText(I18N.msg("jsure.eclipse.view.highlight_diffs"));
    f_actionHighlightDifferences.setToolTipText(I18N.msg("jsure.eclipse.view.highlight_diffs.tip"));
    f_highlightDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNO_HIGHLIGHT_DIFFERENCES);
    f_actionHighlightDifferences.setChecked(f_highlightDifferences);
    f_contentProvider.setHighlightDifferences(f_highlightDifferences);

    f_actionShowOnlyDifferences.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_CHANGELOG_ONLY));
    f_actionShowOnlyDifferences.setText(I18N.msg("jsure.eclipse.view.show_only_diffs"));
    f_actionShowOnlyDifferences.setToolTipText(I18N.msg("jsure.eclipse.view.show_only_diffs.tip"));
    f_showOnlyDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNO_SHOW_ONLY_DIFFERENCES);
    f_actionShowOnlyDifferences.setChecked(f_showOnlyDifferences);
    updateShowOnlyDifferencesFilter();

    f_actionShowOnlyFromSrc.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JAVA_COMP_UNIT));
    f_actionShowOnlyFromSrc.setText(I18N.msg("jsure.eclipse.proposed.promises.showOnlyFromSrc"));
    f_actionShowOnlyFromSrc.setToolTipText(I18N.msg("jsure.eclipse.proposed.promises.showOnlyFromSrc.tip"));
    f_showOnlyFromSrc = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNO_SHOW_ONLY_FROM_SRC);
    f_actionShowOnlyFromSrc.setChecked(f_showOnlyFromSrc);
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(final IMenuManager manager) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          manager.add(f_actionAnnotateCode);
          manager.add(new Separator());
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
    pulldown.add(f_actionAnnotateCode);
    pulldown.add(new Separator());
    pulldown.add(f_actionHighlightDifferences);
    pulldown.add(f_actionShowOnlyDifferences);
    pulldown.add(new Separator());
    pulldown.add(f_actionShowOnlyFromSrc);
    pulldown.add(f_actionShowOnlyAbductive);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionAnnotateCode);
    toolbar.add(new Separator());
    toolbar.add(f_actionHighlightDifferences);
    toolbar.add(f_actionShowOnlyDifferences);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowOnlyFromSrc);
    toolbar.add(f_actionShowOnlyAbductive);
  }

  void selectionChangedHelper() {
    final boolean proposalsSelected = !getSelectedProposals().isEmpty();
    f_actionAnnotateCode.setEnabled(proposalsSelected);
  }

  List<IProposedPromiseDrop> getSelectedProposals() {
    final List<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
    final IStructuredSelection selection = (IStructuredSelection) f_treeViewer.getSelection();
    for (final Object elt : selection.toList()) {
      if (elt instanceof Element)
        getSelectedProposalsHelper((Element) elt, result);
    }
    return result;
  }

  private void getSelectedProposalsHelper(Element e, List<IProposedPromiseDrop> mutableResult) {
    if (e instanceof ElementDrop) {
      final IDrop drop = ((ElementDrop) e).getDrop();
      if (drop instanceof IProposedPromiseDrop) {
        mutableResult.add((IProposedPromiseDrop) drop);
      }
    } else {
      for (Element child : e.getChildren())
        getSelectedProposalsHelper(child, mutableResult);
    }
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

  @Override
  public void setFocus() {
    f_treeViewer.getControl().setFocus();
  }

  @Override
  public void currentScanChanged(JSureScan doNotUseInThisMethod) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
        if (scan != null) {
          final ScanDifferences diff = JSureDataDirHub.getInstance().getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull();
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
          f_treeViewer
              .setInput(new ProposedAnnotationViewContentProvider.Input(scan, diff, f_showOnlyFromSrc, f_showOnlyAbductive));
          if (state != null) {
            state.restoreViewState(f_treeViewer);
          } else {
            f_treeViewer.expandToLevel(3);
          }
          f_treeViewer.getTree().setRedraw(true);
          f_viewerbook.showPage(f_treeViewer.getControl());
        } else {
          // Show no results
          f_viewerbook.showPage(f_noResultsToShowLabel);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
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
  }

  @Override
  public void uninterestingPackageFilterChanged() {
    currentScanChanged(null);
  }
}
