package com.surelogic.jsure.client.eclipse.views.explorer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
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
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.JSureClientUtility;
import com.surelogic.jsure.client.eclipse.views.problems.ProblemsView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class VerificationExplorerView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

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

  private static final String VIEW_STATE = "VerificationExplorerView_TreeViewerUIState";

  private final File f_viewStatePersistenceFile;

  private PageBook f_viewerbook = null;
  private Label f_noResultsToShowLabel = null;
  private TreeViewer f_treeViewer;
  private TreeViewerColumn f_showDiffTableColumn = null;
  private boolean f_showHints;
  private boolean f_highlightDifferences;

  @Override
  public void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
    f_treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);

    final TreeViewerColumn columnDiff = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnDiff.setLabelProvider(ColumnLabelProviderUtility.DIFF);
    columnDiff.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.VEXPLORER_COL_DIFF_WIDTH));
    columnDiff.getColumn().addControlListener(
        new JSureClientUtility.ColumnResizeListener(JSurePreferencesUtility.VEXPLORER_COL_DIFF_WIDTH));
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
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.VSTATUS_HIGHLIGHT_DIFFERENCES, f_highlightDifferences);
        // f_contentProvider.setHighlightDifferences(f_highlightDifferences);
        f_treeViewer.refresh();
      }
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

  private void makeActions() {
    f_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          final Object first = s.getFirstElement();
          // if (first instanceof ElementDrop) {
          // /*
          // * Try to open an editor at the point this item references in the
          // * code
          // */
          // final IJavaRef ref = ((ElementDrop) first).getDrop().getJavaRef();
          // if (ref != null)
          // Activator.highlightLineInJavaEditor(ref);
          // }
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
    f_actionHighlightDifferences.setText("Highlight differences from the last scan");
    f_actionHighlightDifferences.setToolTipText(f_actionHighlightDifferences.getText());
    f_highlightDifferences = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.VEXPLORER_HIGHLIGHT_DIFFERENCES);
    f_actionHighlightDifferences.setChecked(f_highlightDifferences);
    // f_contentProvider.setHighlightDifferences(f_highlightDifferences);
  }

  private void hookContextMenu() {
    // TODO Auto-generated method stub
  }

  private void contributeToActionBars() {
    final IActionBars bars = getViewSite().getActionBars();

    final IMenuManager pulldown = bars.getMenuManager();
    pulldown.add(f_actionCollapseAll);
    pulldown.add(new Separator());
    pulldown.add(f_actionShowQuickRef);
    pulldown.add(new Separator());
    pulldown.add(f_actionHighlightDifferences);
    pulldown.add(f_actionShowHints);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionShowQuickRef);
    toolbar.add(new Separator());
    toolbar.add(f_actionProblemsIndicator);
    toolbar.add(new Separator());
    toolbar.add(f_actionHighlightDifferences);
    toolbar.add(f_actionShowHints);
  }

  private void showScanOrEmptyLabel() {
    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    final JSureScanInfo oldScan = JSureDataDirHub.getInstance().getLastMatchingScanInfo();
    if (scan != null) {
      final ScanDifferences diff = JSureDataDirHub.getInstance().getDifferencesBetweenCurrentScanAndLastCompatibleScanOrNull();
      // TODO create and show model

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

  @Override
  public void setFocus() {
    f_treeViewer.getControl().setFocus();
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
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
