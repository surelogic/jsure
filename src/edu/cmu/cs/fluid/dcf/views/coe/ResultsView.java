package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.eclipse.dialogs.ImageDialog;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;

import edu.cmu.cs.fluid.analysis.util.ConsistencyListener;
import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.eclipse.EclipseFileLocator;
import edu.cmu.cs.fluid.eclipse.adapter.SrcRef;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.AbstractJavaBinder;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.ProjectsDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

public class ResultsView extends AbstractDoubleCheckerView {

  private static final Logger LOG = SLLogger.getLogger("ResultsView");

  protected final IResultsViewContentProvider f_contentProvider = makeContentProvider();

  private final IResultsViewLabelProvider f_labelProvider = makeLabelProvider();

  private final Action f_actionShowProposedPromiseView = new Action() {
    @Override
    public void run() {
      ViewUtility.showView(ProposedPromiseView.class.getName());
    }
  };

  private final Action f_actionShowProblemsView = new Action() {
    @Override
    public void run() {
      ViewUtility.showView(ProblemsView.class.getName());
    }
  };

  private final Action f_actionShowInferences = new Action() {
    @Override
    public void run() {
      final boolean toggle = !f_contentProvider.isShowInferences();
      f_contentProvider.setShowInferences(toggle);
      f_labelProvider.setShowInferences(toggle);
      setViewState();
      viewer.refresh();
    }
  };
  
  private final Action f_actionExpand = new Action() {
	@Override
    public void run() {
      final ISelection selection = viewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        treeViewer.expandToLevel(50);
      } else {
        final Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj instanceof AbstractContent) {
          treeViewer.expandToLevel(obj, 50);
        } else {
          treeViewer.expandToLevel(50);
        }
      }
    }
  };

  private final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
      final ISelection selection = viewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        treeViewer.collapseAll();
      } else {
        final Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj instanceof AbstractContent) {
          treeViewer.collapseToLevel(obj, 1);
        } else {
          treeViewer.collapseAll();
        }
      }
    }
  };

  private final Action f_actionLinkToOriginal = new Action() {
    @SuppressWarnings("unchecked")
	@Override
    public void run() {
      final ISelection selection = viewer.getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        treeViewer.collapseAll();
      } else {    	  
        final Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj instanceof AbstractContent) {
          final AbstractContent c = (AbstractContent) obj;
          if (c.cloneOf != null) {
        	
            treeViewer.reveal(c.cloneOf);
            treeViewer.setSelection(new StructuredSelection(c.cloneOf), true);
            /*
            {
              public boolean isEmpty() {
                return false;
              }
              public List toList() {
                return Collections.singletonList(c.cloneOf);
              }
              public Object[] toArray() {
                final Object[] rv = new Object[1];
                rv[0] = c.cloneOf;
                return rv;
              }
              public int size() {
                return 1;
              }
              public Iterator iterator() {
                return new SingletonIterator(c.cloneOf);
              }
              public Object getFirstElement() {
                return c.cloneOf;
              }
            }, true);
            */
          }
        }
      }
    }
  };

  private final Action f_copy = new Action() {
    @Override
    public void run() {
      clipboard.setContents(new Object[] { getSelectedText() },
          new Transfer[] { TextTransfer.getInstance() });
    }
  };

  private final Action f_addPromiseToCode = new ProposedPromisesRefactoringAction() {

    @Override
    protected List<ProposedPromiseDrop> getProposedDrops() {
      /*
       * There are two cases: (1) a single proposed promise drop in the tree is
       * selected and (2) a container folder for multiple proposed promise drops
       * is selected.
       */
      final IStructuredSelection selection = (IStructuredSelection) viewer
          .getSelection();
      if (selection == null || selection == StructuredSelection.EMPTY) {
        return Collections.emptyList();
      }
      final List<ProposedPromiseDrop> proposals = new ArrayList<ProposedPromiseDrop>();
      for (final Object element : selection.toList()) {
        if (element instanceof AbstractContent) {
          final AbstractContent<IDropInfo,?> c = (AbstractContent) element;
          /*
           * Deal with the case where a single proposed promise drop is
           * selected.
           */
          if (c.getDropInfo().isInstance(ProposedPromiseDrop.class)) {
            final ProposedPromiseDrop pp = c.getDropInfo().getAdapter(ProposedPromiseDrop.class);
            proposals.add(pp);
          } else {
            /*
             * In the case that the user selected a container for multiple
             * proposed promise drops we want add them all.
             */
            if (c.getMessage().equals(
                I18N.msg("jsure.eclipse.proposed.promise.content.folder"))) {
              for (AbstractContent content : c.getChildrenAsCollection()) {
                if (content.getDropInfo().isInstance(ProposedPromiseDrop.class)) {
                  final ProposedPromiseDrop pp = c.getDropInfo().getAdapter(ProposedPromiseDrop.class);
                  proposals.add(pp);
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
      final Image quickRefImage = SLImages
          .getImage(CommonImages.IMG_JSURE_QUICK_REF);
      final Image icon = SLImages
          .getImage(CommonImages.IMG_JSURE_QUICK_REF_ICON);
      final ImageDialog dialog = new ImageDialog(SWTUtility.getShell(),
          quickRefImage, icon, "Iconography Quick Reference");
      dialog.open();
    }
  };

  /*
   * Experimental actions
   */
  private Action f_actionExportZIPForStandAloneResultsViewer;
  private Action f_actionExportXMLForSierra;
  private Action f_actionShowUnderlyingDropType;

  /**
   * Changed to not depend on the Viewer
   */
  public static class ContentNameSorter extends ViewerSorter {
    @SuppressWarnings("unchecked")
	@Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
      int result = 0; // = super.compare(viewer, e1, e2);
      final boolean bothContent = e1 instanceof AbstractContent && e2 instanceof AbstractContent;
      if (bothContent) {
        final AbstractContent c1 = (AbstractContent) e1;
        final AbstractContent c2 = (AbstractContent) e2;
        final boolean c1IsNonProof = c1.f_isInfo || c1.f_isPromiseWarning;
        final boolean c2IsNonProof = c2.f_isInfo || c2.f_isPromiseWarning;
        // Separating proof drops from info/warning drops
        if (c1IsNonProof && !c2IsNonProof) {
          result = 1;
        } else if (c2IsNonProof && !c1IsNonProof) {
          result = -1;
        } else {
          final boolean c1isPromise = c1.getDropInfo() != null && c1.getDropInfo().isInstance(PromiseDrop.class);
          final boolean c2isPromise = c2.getDropInfo() != null && c2.getDropInfo().isInstance(PromiseDrop.class);
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
        				  result = file1.getFullPath().toString().compareTo(
        						  file2.getFullPath().toString());
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
        LOG.warning("e1 and e2 are not AbstractContent objects: e1 = \""
            + e1.toString() + "\"; e2 = \"" + e2.toString() + "\"");
        return -1;
      }

      return result;
    }
  }

  protected IResultsViewContentProvider makeContentProvider() {
    return new ResultsViewContentProvider();
  }

  protected IResultsViewLabelProvider makeLabelProvider() {
    return new ResultsViewLabelProvider();
  }

  @Override
  protected void setupViewer() {
    viewer.setContentProvider(f_contentProvider);
    viewer.setLabelProvider(f_labelProvider);
    viewer.setSorter(createSorter());
    ColumnViewerToolTipSupport.enableFor(viewer);
  }

  protected ViewerSorter createSorter() {
    return new ContentNameSorter();
  }

  String getSelectedText() {
    final IStructuredSelection selection = (IStructuredSelection) viewer
        .getSelection();
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
  protected void fillGlobalActionHandlers(final IActionBars bars) {
    bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_copy);
  }

  @Override
  protected void fillLocalPullDown(final IMenuManager manager) {
    manager.add(f_actionCollapseAll);
    manager.add(new Separator());
    manager.add(f_showQuickRef);
    manager.add(f_actionShowInferences);
    manager.add(f_actionShowProposedPromiseView);
    manager.add(f_actionShowProblemsView);
    if (XUtil.useExperimental()) {
      manager.add(new Separator());
      manager.add(f_actionExportZIPForStandAloneResultsViewer);
      manager.add(f_actionExportXMLForSierra);
    }
  }

  @Override
  protected void fillContextMenu(final IMenuManager manager,
      final IStructuredSelection s) {
    if (!s.isEmpty()) {
      final AbstractContent c = (AbstractContent) s.getFirstElement();
      if (c.getDropInfo().isInstance(ProposedPromiseDrop.class)) {
        manager.add(f_addPromiseToCode);
        f_addPromiseToCode.setText(I18N
            .msg("jsure.eclipse.proposed.promise.edit"));
        manager.add(new Separator());
      } else {
        if (c.getMessage().equals(
            I18N.msg("jsure.eclipse.proposed.promise.content.folder"))) {
          manager.add(f_addPromiseToCode);
          f_addPromiseToCode.setText(I18N
              .msg("jsure.eclipse.proposed.promises.edit"));
          manager.add(new Separator());
        }
      }
    }
    manager.add(f_actionExpand);
    manager.add(f_actionCollapse);
    if (!s.isEmpty()) {
      final AbstractContent c = (AbstractContent) s.getFirstElement();
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
        final AbstractContent c = (AbstractContent) s.getFirstElement();
        final IDropInfo d = c.getDropInfo();
        if (d != null) {
          f_actionShowUnderlyingDropType.setText("Type: "
              + d.getType());
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
  }

  @Override
  protected void makeActions() {
    f_actionShowProposedPromiseView.setText("Show Proposed Promises");
    f_actionShowProposedPromiseView.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_ANNOTATION));

    f_actionShowProblemsView.setText("Show Modeling Problems");
    f_actionShowProblemsView.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));

    f_actionShowInferences.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_SUGGESTIONS_WARNINGS));

    f_actionExpand.setText("Expand");
    f_actionExpand
        .setToolTipText("Expand the current selection or all if none");
    f_actionExpand.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText("Collapse");
    f_actionCollapse
        .setToolTipText("Collapse the current selection or all if none");
    f_actionCollapse.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionCollapseAll.setText("Collapse All");
    f_actionCollapseAll.setToolTipText("Collapse All");
    f_actionCollapseAll.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionLinkToOriginal.setText("Link to Original");
    f_actionLinkToOriginal
        .setToolTipText("Link to the node that this backedge would reference");

    f_copy.setText("Copy");
    f_copy
        .setToolTipText("Copy the selected verification result to the clipboard");

    f_addPromiseToCode.setToolTipText(I18N
        .msg("jsure.eclipse.proposed.promise.tip"));

    f_showQuickRef.setText("Show Iconography Quick Reference Card");
    f_showQuickRef.setToolTipText("Show the iconography quick reference card");
    f_showQuickRef.setImageDescriptor(SLImages
        .getImageDescriptor(CommonImages.IMG_JSURE_QUICK_REF_ICON));

    if (XUtil.useExperimental()) {
      f_actionExportZIPForStandAloneResultsViewer = new Action() {
        @Override
        public void run() {
          exportZIPForStandAloneResultsViewer();
        }
      };
      f_actionExportZIPForStandAloneResultsViewer
          .setText("Export Results && Source (for Stand-Alone Results Viewer)");
      f_actionExportZIPForStandAloneResultsViewer
          .setToolTipText("Creates a ZIP file containing source and analysis results for the Stand-Alone Results Viewer");
      f_actionExportZIPForStandAloneResultsViewer.setImageDescriptor(SLImages
          .getImageDescriptor(CommonImages.IMG_EXPORT_WITH_SOURCE));

      f_actionExportXMLForSierra = new Action() {
        @Override
        public void run() {
          exportXMLForSierra();
        }
      };
      f_actionExportXMLForSierra
          .setText("Export Results (XML for Sierra Viewer)");
      f_actionExportXMLForSierra
          .setToolTipText("Creates a XML file containing analysis results that can be imported into Sierra");
      f_actionExportXMLForSierra.setImageDescriptor(SLImages
          .getImageDescriptor(CommonImages.IMG_EXPORT_WITH_SOURCE));

      f_actionShowUnderlyingDropType = new Action() {
        @Override
        public void run() {
          // Does nothing right now
        }
      };
      f_actionShowUnderlyingDropType.setText("Unknown");
    }

    setViewState();
  }

  @Override
  protected void setViewState() {
    f_actionShowInferences.setChecked(f_contentProvider.isShowInferences());
    f_actionShowInferences.setText("Show Information/Warning Results");
    f_actionShowInferences
        .setToolTipText("Show information and warning analysis results");
  }

  @Override
  protected void handleDoubleClick(final IStructuredSelection selection) {
    final Object obj = selection.getFirstElement();
    if (obj instanceof AbstractContent) {
      // try to open an editor at the point this item references
      // in the code
      final AbstractContent c = (AbstractContent) obj;
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
      SrcRef.dumpStats();
      AbstractJavaBinder.printStats();
      RegionModel.purgeUnusedRegions();
      LockModel.purgeUnusedLocks();

      // update the whole-program proof
      ConsistencyListener.prototype.analysisCompleted();

      final long start = System.currentTimeMillis();
      synchronized (Sea.getDefault()) {
    	  f_contentProvider.buildModelOfDropSea();
      }
      final long buildEnd = System.currentTimeMillis();
      System.err
          .println("Time to build model  = " + (buildEnd - start) + " ms");
      JavacDriver.getInstance().recordViewUpdate();
      
      if (IJavaFileLocator.testIRPaging) {
        final EclipseFileLocator loc = (EclipseFileLocator) IDE.getInstance()
            .getJavaFileLocator();
        loc.testUnload(false, false);
        SlotInfo.compactAll();
      }
      setViewState();
    } catch (final Throwable t) {
      SLLogger.getLogger().log(Level.SEVERE, "Problem updating COE view", t);
    }
  }

  /**
   * Prompts the user for a filename, then creates a ZIP file with an XML
   * representation of the Fluid Verification Status view along with all Java
   * source files in the workspace.
   */
  private void exportZIPForStandAloneResultsViewer() {
    final Shell shell = Activator.getDefault().getWorkbench().getDisplay()
        .getActiveShell();
    final FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
    fileChooser.setFilterExtensions(new String[] { "*.zip" });
    fileChooser
        .setText("Select ZIP output filename (for import into the Stand-Alone Results Viewer)");
    String filename = fileChooser.open();
    if (filename != null) {
      if (!filename.endsWith(".zip")) {
        filename = filename.concat(".zip");
      }
      FileOutputStream zipFile;
      try {
        zipFile = new FileOutputStream(filename);
      } catch (final FileNotFoundException e) {
        LOG.log(Level.SEVERE, "Unable to create ZIP file ", e);
        MessageDialog.openError(shell, "Error exporting results",
            "Unable to create ZIP results file");
        exportZIPForStandAloneResultsViewer(); // try again
        return; // bail out of previous attempt
      }

      XMLReport.exportResultsWithSource(zipFile, treeViewer);
    }
  }

  private void exportXMLForSierra() {
    final String proj = ProjectsDrop.getProjects().getLabel();
    File location;
    
    if (true) {
      final Shell shell = Activator.getDefault().getWorkbench().getDisplay()
          .getActiveShell();
      final FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
      fileChooser.setFilterExtensions(new String[] { "*.xml" });
      fileChooser
          .setText("Select XML output filename (for import into Sierra)");
      final String filename = fileChooser.open();
      location = new File(filename);
  	/*
    } else {
      final File userDir = new File(System.getProperty("user.home"));
      location = new File(userDir, "Desktop/" + proj + ".sea.xml");
      */
    }
    try {
      new SeaSnapshot(location).snapshot(proj, Sea.getDefault());
      //new JSureXMLReader(new TestListener()).read(location);
      SeaSnapshot.loadSnapshot(location);
    } catch (final Exception e) {
      SLLogger.getLogger().log(Level.SEVERE, "Problem exporting for Sierra", e);
    }
  }
}