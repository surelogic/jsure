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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.eclipse.DemoProjectAction;
import com.surelogic.common.eclipse.SLImages;
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
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

public class ResultsView extends AbstractDoubleCheckerView {

	private static final Logger LOG = SLLogger.getLogger("ResultsView");

	private final IResultsViewContentProvider m_contentProvider = makeContentProvider();

	private final IResultsViewLabelProvider m_labelProvider = makeLabelProvider();

	private Action actionShowProblemsView;

	private Action actionShowInferences;

	private Action actionExpand;

	private Action actionCollapse;

	private Action actionCollapseAll;

	private Action actionExportZIPForStandAloneResultsViewer;

	private Action actionExportXMLForSierra;

	private Action actionShowUnderlyingDropType;

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
				boolean c1IsNonProof = (c1.f_isInfo || c1.f_isPromiseWarning);
				boolean c2IsNonProof = (c2.f_isInfo || c2.f_isPromiseWarning);
				if (c1IsNonProof && !c2IsNonProof) {
					result = 1;
				} else if (c2IsNonProof && !c1IsNonProof) {
					result = -1;
				} else {
					boolean c1isPromise = c1.f_referencedDrop instanceof PromiseDrop;
					boolean c2isPromise = c2.f_referencedDrop instanceof PromiseDrop;
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
								if (f1 instanceof IResource
										&& f2 instanceof IResource) {
									final IResource file1 = (IResource) f1;
									final IResource file2 = (IResource) f2;
									result = file1.getFullPath().toString()
											.compareTo(
													file2.getFullPath()
															.toString());
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
		manager.add(actionCollapseAll);
		manager.add(new Separator());
		manager.add(actionShowInferences);
		manager.add(actionShowProblemsView);
		manager.add(new Separator());
		manager.add(new DemoProjectAction("Create PlanetBaronJSure", getClass()
				.getResource("/lib/PlanetBaronJSure.zip")));
		manager.add(new DemoProjectAction("Create BoundedFIFO", getClass()
				.getResource("/lib/BoundedFIFO.zip")));
		manager.add(new Separator());
		if (XUtil.useExperimental()) {
			manager.add(actionExportZIPForStandAloneResultsViewer);
			manager.add(actionExportXMLForSierra);
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		manager.add(actionExpand);
		manager.add(actionCollapse);
		if (XUtil.useDeveloperMode()) {
			manager.add(new Separator());
			manager.add(actionShowUnderlyingDropType);
			if (!s.isEmpty()) {
				Content c = (Content) s.getFirstElement();
				Drop d = c.f_referencedDrop;
				if (d != null) {
					actionShowUnderlyingDropType.setText("Type: "
							+ d.getClass().getName());
				} else {
					actionShowUnderlyingDropType.setText("Type: n/a");
				}
			} else {
				actionShowUnderlyingDropType.setText("Type: Unknown");
			}
		}
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionCollapseAll);
		manager.add(new Separator());
		manager.add(actionShowInferences);
	}

	@Override
	protected void makeActions() {
		actionShowProblemsView = new Action() {
			@Override
			public void run() {
				ViewUtility.showView(ProblemsView.class.getName());
			}
		};
		actionShowProblemsView.setText("Show Modeling Problems");
		actionShowProblemsView.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_JSURE_MODEL_PROBLEMS));

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
		actionShowInferences.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_SUGGESTIONS_WARNINGS));

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
					.setImageDescriptor(SLImages
							.getImageDescriptor(CommonImages.IMG_EXPORT_WITH_SOURCE));

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
			actionExportXMLForSierra.setImageDescriptor(SLImages
					.getImageDescriptor(CommonImages.IMG_EXPORT_WITH_SOURCE));
		}

		actionExpand = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				if (selection == null || selection == StructuredSelection.EMPTY) {
					treeViewer.expandToLevel(50);
				} else {
					Object obj = ((IStructuredSelection) selection)
							.getFirstElement();
					if (obj instanceof Content) {
						treeViewer.expandToLevel(obj, 50);
					} else {
						treeViewer.expandToLevel(50);
					}
				}
			}
		};
		actionExpand.setText("Expand");
		actionExpand
				.setToolTipText("Expand the current selection or all if none");
		actionExpand.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

		actionCollapse = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				if (selection == null || selection == StructuredSelection.EMPTY) {
					treeViewer.collapseAll();
				} else {
					Object obj = ((IStructuredSelection) selection)
							.getFirstElement();
					if (obj instanceof Content) {
						treeViewer.collapseToLevel(obj, 1);
					} else {
						treeViewer.collapseAll();
					}
				}
			}
		};
		actionCollapse.setText("Collapse");
		actionCollapse
				.setToolTipText("Collapse the current selection or all if none");
		actionCollapse.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

		actionCollapseAll = new Action() {
			@Override
			public void run() {
				treeViewer.collapseAll();
			}
		};
		actionCollapseAll.setText("Collapse All");
		actionCollapseAll.setToolTipText("Collapse All");
		actionCollapseAll.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

		actionShowUnderlyingDropType = new Action() {
			@Override
			public void run() {
				// Does nothing right now
			}
		};
		actionShowUnderlyingDropType.setText("Unknown");
		setViewState();
	}

	@Override
	protected void setViewState() {
		actionShowInferences.setChecked(m_contentProvider.isShowInferences());
		actionShowInferences.setText("Show Information/Warning Results");
		actionShowInferences
				.setToolTipText("Show information and warning analysis results");
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