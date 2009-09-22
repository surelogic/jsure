package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.XUtil;
import com.surelogic.common.eclipse.DemoProjectAction;
import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.TestListener;
import com.surelogic.jsure.xml.JSureXMLReader;

import edu.cmu.cs.fluid.analysis.util.ConsistencyListener;
import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.eclipse.EclipseFileLocator;
import edu.cmu.cs.fluid.eclipse.adapter.SrcRef;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.AbstractJavaBinder;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

public class ResultsView extends AbstractDoubleCheckerView {

	private static final Logger LOG = SLLogger.getLogger("ResultsView");

	final public static String CHOICE_NAME = "choice.gif";

	final public static ImageDescriptor CHOICE_DESC = getImageDescriptor(CHOICE_NAME);

	final public static Image CHOICE_IMG = new ResultsImageDescriptor(
			CHOICE_DESC, 0, ICONSIZE).createImage();

	final public static String CHOICE_ITEM_NAME = "choice_item.gif";

	final public static ImageDescriptor CHOICE_ITEM_DESC = getImageDescriptor(CHOICE_ITEM_NAME);

	final public static Image CHOICE_ITEM_IMG = new ResultsImageDescriptor(
			CHOICE_ITEM_DESC, 0, ICONSIZE).createImage();

	final public static String PLUS_NAME = "plus.gif";

	final public static ImageDescriptor PLUS_DESC = getImageDescriptor(PLUS_NAME);

	final public static Image PLUS_IMG = new ResultsImageDescriptor(PLUS_DESC,
			0, ICONSIZE).createImage();

	final public static String REDX_NAME = "redx.gif";

	final public static ImageDescriptor REDX_DESC = getImageDescriptor(REDX_NAME);

	final public static Image REDX_IMG = new ResultsImageDescriptor(REDX_DESC,
			0, ICONSIZE).createImage();

	final public static ImageDescriptor UNKNOWN_DESC = getImageDescriptor("unknown.gif");

	final public static Image UNKNOWN_IMG = new ResultsImageDescriptor(
			UNKNOWN_DESC, 0, ICONSIZE).createImage();

	final public static String TALLYHO_NAME = "tallyho.gif";

	final public static ImageDescriptor TALLYHO_DESC = getImageDescriptor(TALLYHO_NAME);

	final public static Image TALLYHO_IMG = new ResultsImageDescriptor(
			TALLYHO_DESC, 0, ICONSIZE).createImage();

	final public static ImageDescriptor REFRESH_DESC = getImageDescriptor("refresh.gif");

	final public static Image REFRESH_IMG = new ResultsImageDescriptor(
			REFRESH_DESC, 0, ICONSIZE).createImage();

	final public static ImageDescriptor EXPANDALL_DESC = getImageDescriptor("expandall.gif");

	final public static Image EXPANDALL_IMG = new ResultsImageDescriptor(
			EXPANDALL_DESC, 0, ICONSIZE).createImage();

	final public static ImageDescriptor COLLAPSEALL_DESC = getImageDescriptor("collapseall.gif");

	final public static Image COLLAPSEALL_IMG = new ResultsImageDescriptor(
			COLLAPSEALL_DESC, 0, ICONSIZE).createImage();

	final public static ImageDescriptor EXPORT_DESC = getImageDescriptor("export.gif");

	final public static Image EXPORT_IMG = new ResultsImageDescriptor(
			EXPORT_DESC, 0, ICONSIZE).createImage();

	final public static ImageDescriptor EXPORT_WITH_SOURCE_DESC = getImageDescriptor("export_with_source.gif");

	final public static Image EXPORT_WITH_SOURCE_IMG = new ResultsImageDescriptor(
			EXPORT_WITH_SOURCE_DESC, 0, ICONSIZE).createImage();

	private final IResultsViewContentProvider m_contentProvider = makeContentProvider();

	private final IResultsViewLabelProvider m_labelProvider = makeLabelProvider();

	private Action actionShowProblemsView;
	
	private Action actionShowInferences;

	private Action actionExpand;

	private Action actionCollapse;

	private Action actionExportZIPForStandAloneResultsViewer;

	private Action actionExportXMLForSierra;

	/**
	 * Changed to not depend on the Viewer
	 */
	public static class ContentNameSorter extends ViewerSorter {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result; // = super.compare(viewer, e1, e2);
			boolean bothContent = (e1 instanceof Content)
					&& (e2 instanceof Content);
			if (bothContent) {
				Content c1 = (Content) e1;
				Content c2 = (Content) e2;
				boolean c1IsNonProof = (c1.isInfo || c1.isPromiseWarning);
				boolean c2IsNonProof = (c2.isInfo || c2.isPromiseWarning);
				if (c1IsNonProof && !c2IsNonProof) {
					result = 1;
				} else if (c2IsNonProof && !c1IsNonProof) {
					result = -1;
				} else {
					boolean c1isPromise = c1.referencedDrop instanceof PromiseDrop;
					boolean c2isPromise = c2.referencedDrop instanceof PromiseDrop;
					if (c1isPromise && !c2isPromise) {
						result = 1;
					} else if (c2isPromise && !c1isPromise) {
						result = -1;
					} else {
						result = c1.getMessage().compareTo(c2.getMessage());
						if (result == 0) {
							final ISrcRef ref1 = c1.getSrcRef();
							final ISrcRef ref2 = c2.getSrcRef();
							if (ref1 != null && ref2 != null) {
								final Object f1 = ref1.getEnclosingFile();
								final Object f2 = ref2.getEnclosingFile();
								if (f1 instanceof IResource && f2 instanceof IResource) {
									final IResource file1 = (IResource) f1;
									final IResource file2 = (IResource) f2;
									result = file1.getFullPath().toString()
									.compareTo(
											file2.getFullPath().toString());
								} else {
									final String file1 = (String) f1;
									final String file2 = (String) f2;
									result = file1.compareTo(file2);
								}
								if (result == 0) {
									final int line1 = ref1.getLineNumber();
									final int line2 = ref2.getLineNumber();
									result = (line1 == line2) ? 0
											: ((line1 < line2) ? -1 : 1);
								}
							}
						}
					}
				}
			} else {
				LOG.warning("e1 and e2 are not Content objects: e1 = \""
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
		viewer.setContentProvider(m_contentProvider);
		viewer.setLabelProvider(m_labelProvider);
		viewer.setSorter(createSorter());
	}
	
	protected ViewerSorter createSorter() {
	  return new ContentNameSorter();
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		manager.add(actionShowInferences);
		manager.add(new Separator());
		manager.add(new DemoProjectAction("Create PlanetBaronJSure", 
				    getClass().getResource("/lib/PlanetBaronJSure.zip")));
		manager.add(new DemoProjectAction("Create BoundedFIFO", 
				    getClass().getResource("/lib/BoundedFIFO.zip")));
		manager.add(new Separator());
		if (XUtil.useExperimental()) {
		  manager.add(actionExportZIPForStandAloneResultsViewer);
		  manager.add(actionExportXMLForSierra);
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(actionExpand);
		manager.add(actionCollapse);
		manager.add(new Separator());
		manager.add(actionShowInferences);
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionShowProblemsView);
		//manager.add(new Separator());
		manager.add(actionShowInferences);
		manager.add(new Separator());
		manager.add(actionExpand);
		manager.add(actionCollapse);
		manager.add(new Separator());
	}
	
	@Override
	protected void makeActions() {
		actionShowProblemsView = new Action() {
			@Override
			public void run() {
				ViewUtility.showView(ProblemsView.class.getName());
			}
		};

		
		actionShowInferences = new Action() {
			@Override
			public void run() {
				boolean toggle = !m_contentProvider.isShowInferences();
				m_contentProvider.setShowInferences(toggle);
				m_labelProvider.setShowInferences(toggle);
				setViewState();
				viewer.refresh();
			}
		};
		actionShowInferences.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_OBJS_INFO_TSK));

		if (XUtil.useExperimental()) {
  		actionExportZIPForStandAloneResultsViewer = new Action() {
  			@Override
  			public void run() {
  				exportZIPForStandAloneResultsViewer();
  			}
  		};
  		actionExportZIPForStandAloneResultsViewer
  				.setText("Export Results && Source (for Stand-Alone Results Viewer)");
  		actionExportZIPForStandAloneResultsViewer
  				.setToolTipText("Creates a ZIP file containing source and analysis results for the Stand-Alone Results Viewer");
  		actionExportZIPForStandAloneResultsViewer
  				.setImageDescriptor(EXPORT_WITH_SOURCE_DESC);

  		actionExportXMLForSierra = new Action() {
  			@Override
  			public void run() {
  				exportXMLForSierra();
  			}
  		};
  		actionExportXMLForSierra
  				.setText("Export Results (XML for Sierra Viewer)");
  		actionExportXMLForSierra
  				.setToolTipText("Creates a XML file containing analysis results that can be imported into Sierra");
  		actionExportXMLForSierra.setImageDescriptor(EXPORT_WITH_SOURCE_DESC);
		}
		
		actionExpand = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				if (obj instanceof Content) {
					treeViewer.expandToLevel(obj, 50);
				} else {
					treeViewer.expandToLevel(50);
				}
			}
		};
		actionExpand.setText("Expand Tree");
		actionExpand.setToolTipText("Expand Tree");
		actionExpand.setImageDescriptor(EXPANDALL_DESC);

		actionCollapse = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				if (obj instanceof Content) {
					treeViewer.collapseToLevel(obj, 1);
				} else {
					treeViewer.collapseAll();
				}
			}
		};
		actionCollapse.setText("Collapse Tree");
		actionCollapse.setToolTipText("Collapse Tree");
		actionCollapse.setImageDescriptor(COLLAPSEALL_DESC);

		setViewState();
	}

	@Override
	protected void setViewState() {
		actionShowInferences.setChecked(m_contentProvider.isShowInferences());
		if (m_contentProvider.isShowInferences()) {
			actionShowInferences.setText("Hide Information && Warning Results");
			actionShowInferences
					.setToolTipText("Hide analysis information && warning results...show only verification results");
		} else {
			actionShowInferences.setText("Show Information && Warning Results");
			actionShowInferences
					.setToolTipText("Show analysis information && warning results within verification results");
		}
		showProblemsView(m_contentProvider.getProblemsViewMessage());
	}

	private void showProblemsView(String msg) {
		if (msg != null) {
			actionShowProblemsView.setText(msg);
			actionShowProblemsView.setImageDescriptor(PlatformUI.getWorkbench()
					.getSharedImages().getImageDescriptor(
							ISharedImages.IMG_OBJS_WARN_TSK));
			actionShowProblemsView.setEnabled(true);
		} else {
			actionShowProblemsView.setText("");
			actionShowProblemsView.setImageDescriptor(null); 
			actionShowProblemsView.setEnabled(false);
		}
	}
	
	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		Object obj = selection.getFirstElement();
		if (obj instanceof Content) {
			// try to open an editor at the point this item references
			// in the code
			Content c = (Content) obj;
			ISrcRef sr = c.getSrcRef();
			if (sr != null) {
				highlightLineInJavaEditor(sr);
			}
			// open up the tree one more level
			if (!treeViewer.getExpandedState(obj))
				treeViewer.expandToLevel(obj, 1);
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

			long start = System.currentTimeMillis();
			m_contentProvider.buildModelOfDropSea();
			long buildEnd = System.currentTimeMillis();
			SLLogger.getLogger().log(Level.INFO,
					"Time to build model  = " + (buildEnd - start) + " ms");
			if (IJavaFileLocator.testIRPaging) {
				EclipseFileLocator loc = (EclipseFileLocator) IDE.getInstance()
						.getJavaFileLocator();
				// loc.printSummary(new PrintWriter(System.out));
				loc.testUnload(false, false);
				SlotInfo.compactAll();
				/*
				 * Enumeration<SlotInfo> e = JJNode.getBundle().attributes();
				 * while (e.hasMoreElements()) { SlotInfo si = e.nextElement();
				 * si.dumpState(); }
				 */
			}
			setViewState();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Prompts the user for a filename, then creates a ZIP file with an XML
	 * representation of the Fluid Verification Status view along with all Java
	 * source files in the workspace.
	 */
	private void exportZIPForStandAloneResultsViewer() {
		Shell shell = Activator.getDefault().getWorkbench().getDisplay()
				.getActiveShell();
		FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
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
			} catch (FileNotFoundException e) {
				LOG.severe("Unable to create ZIP file ");
				e.printStackTrace();
				MessageDialog.openError(shell, "Error exporting results",
						"Unable to create ZIP results file");
				exportZIPForStandAloneResultsViewer(); // try again
				return; // bail out of previous attempt
			}

			XMLReport.exportResultsWithSource(zipFile, treeViewer);
		}
	}

	private void exportXMLForSierra() {
		final String proj = ProjectDrop.getProject();
		File location;
		if (true) {
			Shell shell = Activator.getDefault().getWorkbench().getDisplay()
					.getActiveShell();
			FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
			fileChooser.setFilterExtensions(new String[] { "*.xml" });
			fileChooser
					.setText("Select XML output filename (for import into Sierra)");
			String filename = fileChooser.open();
			location = new File(filename);
		} else {
			File userDir = new File(System.getProperty("user.home"));
			location = new File(userDir, "Desktop/" + proj + ".sea.xml");
		}
		try {
			new SeaSnapshot().snapshot(proj, Sea.getDefault(), location);
			JSureXMLReader.readSnapshot(location, new TestListener());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}