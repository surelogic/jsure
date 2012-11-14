package com.surelogic.jsure.client.eclipse.views.proposals;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
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
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class ProposedAnnotationView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

  private static final String VIEW_STATE = "ProposedAnnotationView_TreeViewerUIState";

  private final File f_viewStatePersistenceFile;

  private PageBook f_viewerbook = null;
  private Label f_noResultsToShowLabel = null;
  private TreeViewer f_treeViewer;
  @NonNull
  private final ProposedAnnotationViewContentProvider f_contentProvider = new ProposedAnnotationViewContentProvider();
  private TreeViewerColumn f_showDiffTableColumn = null;
  private boolean f_showAsTree;
  private boolean f_showOnlyAbductive;

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

  public ProposedAnnotationView() {
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

  protected final Action f_actionAnnotateCode = new ProposedPromisesRefactoringAction() {
    @Override
    protected List<IProposedPromiseDrop> getProposedDrops() {
      return getSelectedProposals();
    }

    @Override
    protected String getDialogTitle() {
      return I18N.msg("jsure.eclipse.proposed.promise.edit");
    }
  };

  private final Action f_actionShowAsTree = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowAsTree.isChecked();
      if (f_showAsTree != buttonChecked) {
        f_showAsTree = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNOTATIONS_SHOW_AS_TREE, f_showAsTree);
        currentScanChanged(null);
      }
    }
  };

  private final Action f_actionShowOnlyAbductive = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      final boolean buttonChecked = f_actionShowOnlyAbductive.isChecked();
      if (f_showOnlyAbductive != buttonChecked) {
        f_showOnlyAbductive = buttonChecked;
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.PROPOSED_ANNOTATIONS_SHOW_ABDUCTIVE_ONLY, f_showOnlyAbductive);
        // TODO CHANGE VIEW
        // f_contentProvider.setHighlightDifferences(f_highlightDifferences);
        f_treeViewer.refresh();
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

    f_actionAnnotateCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
    f_actionAnnotateCode.setToolTipText(I18N.msg("jsure.eclipse.proposed.promise.tip"));
    f_actionAnnotateCode.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));
    f_actionAnnotateCode.setEnabled(false); // wait until something is selected

    f_actionShowAsTree.setText(I18N.msg("jsure.eclipse.proposed.promises.showAsTree"));
    f_actionShowAsTree.setToolTipText(I18N.msg("jsure.eclipse.proposed.promises.showAsTree.tip"));
    f_actionShowAsTree.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JAVA_DECLS_TREE));

    f_actionShowOnlyAbductive.setText(I18N.msg("jsure.eclipse.proposed.promises.showAbductiveOnly"));
    f_actionShowOnlyAbductive.setToolTipText(I18N.msg("jsure.eclipse.proposed.promises.showAbductiveOnly.tip"));
    f_actionShowOnlyAbductive.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION_ABDUCTIVE));

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
            manager.add(f_actionAnnotateCode);
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
    pulldown.add(f_actionShowAsTree);
    pulldown.add(f_actionShowOnlyAbductive);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionAnnotateCode);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowAsTree);
    toolbar.add(f_actionShowOnlyAbductive);
  }

  private void showScanOrEmptyLabel() {
    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    final JSureScanInfo oldScan = JSureDataDirHub.getInstance().getLastMatchingScanInfo();
    if (scan != null) {
      if (f_showDiffTableColumn != null) {
        final String label = oldScan == null ? "No Prior Scan" : "Differences from scan of " + oldScan.getProjects().getLabel()
            + " at " + SLUtility.toStringHMS(oldScan.getProjects().getDate());
        f_showDiffTableColumn.getColumn().setText(label);
      }
      final ScanDifferences diff = JSureDataDirHub.getInstance().getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull();
      // f_contentProvider.changeContentsToCurrentScan(scan, oldScan, diff,
      // f_showOnlyDifferences, f_showObsoleteDrops,
      // f_showOnlyDerivedFromSrc, f_showAnalysisResults, f_showHints);
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

  private List<IProposedPromiseDrop> getSelectedProposals() {
    // TODO Auto-generated method stub
    return null;
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
}
