package edu.cmu.cs.fluid.dcf.views.coe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.AbstractAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
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

	private final IResultsViewContentProvider f_contentProvider = makeContentProvider();

	private final IResultsViewLabelProvider f_labelProvider = makeLabelProvider();

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
				final Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				if (obj instanceof Content) {
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
				final Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				if (obj instanceof Content) {
					treeViewer.collapseToLevel(obj, 1);
				} else {
					treeViewer.collapseAll();
				}
			}
		}
	};

	private final Action f_actionCollapseAll = new Action() {
		@Override
		public void run() {
			treeViewer.collapseAll();
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
		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			int result; // = super.compare(viewer, e1, e2);
			final boolean bothContent = e1 instanceof Content
					&& e2 instanceof Content;
			if (bothContent) {
				final Content c1 = (Content) e1;
				final Content c2 = (Content) e2;
				final boolean c1IsNonProof = c1.f_isInfo
						|| c1.f_isPromiseWarning;
				final boolean c2IsNonProof = c2.f_isInfo
						|| c2.f_isPromiseWarning;
				if (c1IsNonProof && !c2IsNonProof) {
					result = 1;
				} else if (c2IsNonProof && !c1IsNonProof) {
					result = -1;
				} else {
					final boolean c1isPromise = c1.f_referencedDrop instanceof PromiseDrop;
					final boolean c2isPromise = c2.f_referencedDrop instanceof PromiseDrop;
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
									result = line1 == line2 ? 0
											: line1 < line2 ? -1 : 1;
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
		viewer.setContentProvider(f_contentProvider);
		viewer.setLabelProvider(f_labelProvider);
		viewer.setSorter(createSorter());
		ColumnViewerToolTipSupport.enableFor(viewer);
		/*
		final int ops = DND.DROP_COPY;
		final Transfer[] transfers = new Transfer[] { TextTransfer.getInstance()};
		viewer.addDragSupport(ops, transfers, new DragSourceAdapter() {
			public void dragSetData(DragSourceEvent event){
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					Object o = event.data;
					event.data = f_labelProvider.getText(o);
				}
			}
		});
		*/
	}

	protected ViewerSorter createSorter() {
		return new ContentNameSorter();
	}

	String getSelectedText() {
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		StringBuilder sb = new StringBuilder();
		for(Object elt : selection.toList()) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(f_labelProvider.getText(elt));
		}	
		return sb.toString();
	}
	
	@Override
	protected void fillGlobalActionHandlers(IActionBars bars) {
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), new Action() {	
			public void run() {
				clipboard.setContents(
						new Object[] { getSelectedText() },
						new Transfer[] { TextTransfer.getInstance()});
			}
		});
	}
	
	@Override
	protected void fillLocalPullDown(final IMenuManager manager) {
		manager.add(f_actionCollapseAll);
		manager.add(new Separator());
		manager.add(f_actionShowInferences);
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
		manager.add(f_actionExpand);
		manager.add(f_actionCollapse);
		if (XUtil.useDeveloperMode()) {
			manager.add(new Separator());
			manager.add(f_actionShowUnderlyingDropType);
			if (!s.isEmpty()) {
				final Content c = (Content) s.getFirstElement();
				final Drop d = c.f_referencedDrop;
				if (d != null) {
					f_actionShowUnderlyingDropType.setText("Type: "
							+ d.getClass().getName());
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
		manager.add(f_actionShowInferences);
	}

	@Override
	protected void makeActions() {
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
			f_actionExportZIPForStandAloneResultsViewer
					.setImageDescriptor(SLImages
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
		if (obj instanceof Content) {
			// try to open an editor at the point this item references
			// in the code
			final Content c = (Content) obj;
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
			f_contentProvider.buildModelOfDropSea();
			final long buildEnd = System.currentTimeMillis();
			System.err.println("Time to build model  = " + (buildEnd - start) + " ms");
			if (IJavaFileLocator.testIRPaging) {
				final EclipseFileLocator loc = (EclipseFileLocator) IDE
						.getInstance().getJavaFileLocator();
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
		final String proj = ProjectDrop.getProject();
		File location;
		if (true) {
			final Shell shell = Activator.getDefault().getWorkbench()
					.getDisplay().getActiveShell();
			final FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
			fileChooser.setFilterExtensions(new String[] { "*.xml" });
			fileChooser
					.setText("Select XML output filename (for import into Sierra)");
			final String filename = fileChooser.open();
			location = new File(filename);
		} else {
			final File userDir = new File(System.getProperty("user.home"));
			location = new File(userDir, "Desktop/" + proj + ".sea.xml");
		}
		try {
			new SeaSnapshot(location).snapshot(proj, Sea.getDefault());
			new JSureXMLReader(new TestListener()).read(location);
		} catch (final Exception e) {
			SLLogger.getLogger().log(Level.SEVERE, "Problem exporting for Sierra", e);
		}
	}
}