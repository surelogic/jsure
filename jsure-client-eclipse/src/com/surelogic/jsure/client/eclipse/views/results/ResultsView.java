package com.surelogic.jsure.client.eclipse.views.results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TreeViewerUIState;
import com.surelogic.common.ui.dialogs.ImageDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;
import com.surelogic.jsure.client.eclipse.views.AbstractJSureResultsView;
import com.surelogic.jsure.core.driver.ConsistencyListener;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.ModelingProblemFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.AbstractJavaBinder;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.IProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

public final class ResultsView extends AbstractJSureResultsView implements JSureDataDirHub.CurrentScanChangeListener {

  private static final String VIEW_STATE = "ResultsView_TreeViewerUIState";

  final File f_viewStatePersistenceFile;

  public ResultsView() {
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
    super.createPartControl(parent);

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

  private final ResultsViewContentProvider f_contentProvider = new ResultsViewContentProvider();

  private final ResultsViewLabelProvider f_labelProvider = new ResultsViewLabelProvider();

  private final Action f_actionShowInferences = new Action() {
    @Override
    public void run() {
      final boolean toggle = !f_contentProvider.isShowInferences();
      f_contentProvider.setShowInferences(toggle);
      f_labelProvider.setShowInferences(toggle);
      setViewState();
      treeViewer.refresh();
    }
  };

  private final Action f_actionExpand = new Action() {
    @Override
    public void run() {
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
  };

  private final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
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
  };

  private final Action f_actionLinkToOriginal = new Action() {
    @Override
    public void run() {
      final ISelection selection = treeViewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        treeViewer.collapseAll();
      } else {
        final Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj instanceof ResultsViewContent) {
          final ResultsViewContent c = (ResultsViewContent) obj;
          if (c.cloneOf != null) {

            treeViewer.reveal(c.cloneOf);
            treeViewer.setSelection(new StructuredSelection(c.cloneOf), true);
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
      /*
       * There are two cases: (1) a single proposed promise drop in the tree is
       * selected and (2) a container folder for multiple proposed promise drops
       * is selected.
       */
      final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        return Collections.emptyList();
      }
      final List<IProposedPromiseDrop> proposals = new ArrayList<IProposedPromiseDrop>();
      for (final Object element : selection.toList()) {
        if (element instanceof ResultsViewContent) {
          final ResultsViewContent c = (ResultsViewContent) element;
          /*
           * Deal with the case where a single proposed promise drop is
           * selected.
           */
          if (c.getDropInfo().instanceOf(ProposedPromiseDrop.class)) {
            final IProposedPromiseDrop pp = (IProposedPromiseDrop) c.getDropInfo();
            if (pp != null) {
              proposals.add(pp);
            }
          } else {
            /*
             * In the case that the user selected a container for multiple
             * proposed promise drops we want add them all.
             */
            if (c.getMessage().equals(I18N.msg("jsure.eclipse.proposed.promise.content.folder"))) {
              for (ResultsViewContent content : c.getChildrenAsCollection()) {
                if (content.getDropInfo().instanceOf(ProposedPromiseDrop.class)) {
                  final IProposedPromiseDrop pp = (IProposedPromiseDrop) c.getDropInfo();
                  if (pp != null) {
                    proposals.add(pp);
                  }
                }
              }
            }
          }
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

  /*
   * Experimental actions
   */
  private Action f_actionShowUnderlyingDropType;

  /**
   * Changed to not depend on the Viewer
   */
  public static class ContentNameSorter extends ViewerSorter {
    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
      int result = 0; // = super.compare(viewer, e1, e2);
      final boolean bothContent = e1 instanceof ResultsViewContent && e2 instanceof ResultsViewContent;
      if (bothContent) {
        final ResultsViewContent c1 = (ResultsViewContent) e1;
        final ResultsViewContent c2 = (ResultsViewContent) e2;
        final boolean c1IsNonProof = c1.f_isInfo || c1.f_isPromiseWarning;
        final boolean c2IsNonProof = c2.f_isInfo || c2.f_isPromiseWarning;
        // Separating proof drops from info/warning drops
        if (c1IsNonProof && !c2IsNonProof) {
          result = 1;
        } else if (c2IsNonProof && !c1IsNonProof) {
          result = -1;
        } else {
          final boolean c1isPromise = c1.getDropInfo() != null && c1.getDropInfo().instanceOf(PromiseDrop.class);
          final boolean c2isPromise = c2.getDropInfo() != null && c2.getDropInfo().instanceOf(PromiseDrop.class);
          // Separating promise drops from other proof drops
          if (c1isPromise && !c2isPromise) {
            result = 1;
          } else if (c2isPromise && !c1isPromise) {
            result = -1;
          } else {
            if (c1isPromise && c2isPromise) {
              result = c1.getMessage().compareTo(c2.getMessage());
            }
            if (result == 0) {
              final ISrcRef ref1 = c1.getSrcRef();
              final ISrcRef ref2 = c2.getSrcRef();
              if (ref1 != null && ref2 != null) {
                final Object f1 = ref1.getEnclosingFile();
                final Object f2 = ref2.getEnclosingFile();
                if (f1 instanceof IResource && f2 instanceof IResource) {
                  final IResource file1 = (IResource) f1;
                  final IResource file2 = (IResource) f2;
                  result = file1.getFullPath().toString().compareTo(file2.getFullPath().toString());
                } else {
                  final String file1 = (String) f1;
                  final String file2 = (String) f2;
                  result = file1.compareTo(file2);
                }
                if (result == 0) {
                  final int line1 = ref1.getLineNumber();
                  final int line2 = ref2.getLineNumber();
                  result = line1 == line2 ? 0 : line1 < line2 ? -1 : 1;
                  if (result == 0) {
                    result = ref1.getOffset() - ref2.getOffset();
                  }
                }
              } else {
                result = c1.getMessage().compareTo(c2.getMessage());
              }
            }
          }
        }
      } else {
        SLLogger.getLogger().warning(
            "e1 and e2 are not ResultsViewContent objects: e1 = \"" + e1.toString() + "\"; e2 = \"" + e2.toString() + "\"");
        return -1;
      }

      return result;
    }
  }

  @Override
  protected void setupViewer() {
    treeViewer.setContentProvider(f_contentProvider);
    treeViewer.setLabelProvider(f_labelProvider);
    treeViewer.setSorter(createSorter());
    ColumnViewerToolTipSupport.enableFor(treeViewer);
  }

  private ViewerSorter createSorter() {
    return new ContentNameSorter();
  }

  String getSelectedText() {
    final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
    final StringBuilder sb = new StringBuilder();
    for (final Object elt : selection.toList()) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(f_labelProvider.getText(elt));
    }
    return sb.toString();
  }

  @Override
  protected void fillLocalPullDown(final IMenuManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_showQuickRef);
    manager.add(f_actionShowInferences);

    final IActionBars bars = getViewSite().getActionBars();
    bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_copy);
  }

  @Override
  protected void fillContextMenu(final IMenuManager manager, final IStructuredSelection s) {
    if (!s.isEmpty()) {
      final Object first = s.getFirstElement();
      if (first instanceof ResultsViewContent) {
        final ResultsViewContent c = (ResultsViewContent) first;
        final IDrop dropInfo = c.getDropInfo();
        if (dropInfo != null) {
          if (dropInfo.instanceOf(ProposedPromiseDrop.class)) {
            manager.add(f_addPromiseToCode);
            f_addPromiseToCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
            manager.add(new Separator());
          } else {
            if (c.getMessage().equals(I18N.msg("jsure.eclipse.proposed.promise.content.folder"))) {
              manager.add(f_addPromiseToCode);
              f_addPromiseToCode.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
              manager.add(new Separator());
            }
          }
        }
      }
    }
    manager.add(f_actionExpand);
    manager.add(f_actionCollapse);
    if (!s.isEmpty()) {
      final ResultsViewContent c = (ResultsViewContent) s.getFirstElement();
      if (c.cloneOf != null) {
        manager.add(f_actionLinkToOriginal);
      }
      manager.add(new Separator());
      manager.add(f_copy);
    }
    if (XUtil.useDeveloperMode()) {
      manager.add(new Separator());
      manager.add(f_actionShowUnderlyingDropType);
      if (!s.isEmpty()) {
        final ResultsViewContent c = (ResultsViewContent) s.getFirstElement();
        final IDrop d = c.getDropInfo();
        if (d != null) {
          f_actionShowUnderlyingDropType.setText("Type: " + d.getTypeName());
        } else {
          f_actionShowUnderlyingDropType.setText("Type: n/a");
        }
      } else {
        f_actionShowUnderlyingDropType.setText("Type: Unknown");
      }
    }
  }

  @Override
  protected void fillLocalToolBar(final IToolBarManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_showQuickRef);
    manager.add(f_actionShowInferences);
    manager.add(f_modelProblemsIndicator);
  }

  @Override
  protected void makeActions() {
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

    f_actionLinkToOriginal.setText("Link to Original");
    f_actionLinkToOriginal.setToolTipText("Link to the node that this backedge would reference");

    f_copy.setText("Copy");
    f_copy.setToolTipText("Copy the selected verification result to the clipboard");

    f_addPromiseToCode.setToolTipText(I18N.msg("jsure.eclipse.proposed.promise.tip"));

    f_showQuickRef.setText("Show Iconography Quick Reference Card");
    f_showQuickRef.setToolTipText("Show the iconography quick reference card");
    f_showQuickRef.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_QUICK_REF_ICON));

    f_modelProblemsIndicator.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));
    f_modelProblemsIndicator.setEnabled(false);

    setViewState();
  }

  @Override
  protected void setViewState() {
    f_actionShowInferences.setChecked(f_contentProvider.isShowInferences());
    f_actionShowInferences.setText("Show Information/Warning Results");
    f_actionShowInferences.setToolTipText("Show information and warning analysis results");
  }

  @Override
  protected void handleDoubleClick(final IStructuredSelection selection) {
    final Object obj = selection.getFirstElement();
    if (obj instanceof ResultsViewContent) {
      // try to open an editor at the point this item references
      // in the code
      final ResultsViewContent c = (ResultsViewContent) obj;
      if (c.cloneOf != null) {
        f_actionLinkToOriginal.run();
        return;
      }
      final ISrcRef sr = c.getSrcRef();
      if (sr != null) {
        highlightLineInJavaEditor(sr);
      }
      // open up the tree one more level
      if (!treeViewer.getExpandedState(obj)) {
        treeViewer.expandToLevel(obj, 1);
      }
    }
  }

  @Override
  protected void updateView() {
    try {
      AbstractJavaBinder.printStats();
      RegionModel.purgeUnusedRegions();
      LockModel.purgeUnusedLocks();

      // update the whole-program proof
      ConsistencyListener.prototype.analysisCompleted();

      final long start = System.currentTimeMillis();
      f_contentProvider.buildModelOfDropSea(treeViewer, f_viewStatePersistenceFile, f_viewerbook);
      final long buildEnd = System.currentTimeMillis();
      System.err.println("Time to build model  = " + (buildEnd - start) + " ms");

      /*
       * if (IJavaFileLocator.testIRPaging) { final EclipseFileLocator loc =
       * (EclipseFileLocator) IDE.getInstance() .getJavaFileLocator();
       * loc.testUnload(false, false); SlotInfo.compactAll(); }
       */
      setViewState();
    } catch (final Throwable t) {
      SLLogger.getLogger().log(Level.SEVERE, "Problem updating COE view", t);
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
    for (Object o : f_contentProvider.getElements(null)) {
      Object rv = findContent((ResultsViewContent) o, d);
      if (rv != null) {
        return rv;
      }
    }
    return null;
  }

  private Object findContent(ResultsViewContent c, IDrop d) {
    if (c == null) {
      return null;
    }
    if (d == c.getDropInfo()) {
      return c;
    }
    for (Object o : f_contentProvider.getChildren(c)) {
      Object rv = findContent((ResultsViewContent) o, d);
      if (rv != null) {
        return rv;
      }
    }
    return null;
  }

  @Override
  protected void finishCreatePartControl() {
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
      Set<? extends IDrop> promiseWarningDrops = info.getDropsOfType(PromiseWarningDrop.class);
      for (IDrop id : promiseWarningDrops) {
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