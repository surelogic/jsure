package edu.cmu.cs.fluid.dc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.common.XUtil;

/**
 * Java preference page to set preferences for double-checking. It reads and
 * updates the list of excluded analysis module extension points for
 * double-checking.
 */
public final class PreferencePage extends
		org.eclipse.jface.preference.PreferencePage implements
		IWorkbenchPreferencePage {
  public static final boolean showPrivate = XUtil.useExperimental();
//  public static final boolean showPrivate = QuickProperties.getInstance()
//  .getProperties().getProperty("dc.show.private", "false").equals(
//      "true");

	CheckboxTreeViewer checktree;

	AnalysisModuleContentProvider analysisModuleContentProvider;

	final public static Image ANALMOD_IMG = Plugin.getDefault()
			.getImageDescriptor("analmod.gif").createImage();

	final public static Image ANALMOD_CO_IMG = Plugin.getDefault()
			.getImageDescriptor("analmod_co.gif").createImage();

	final public static Image PREQ_IMG = Plugin.getDefault()
			.getImageDescriptor("preq.gif").createImage();

	final public static Image PREQ_CO_IMG = Plugin.getDefault()
			.getImageDescriptor("preq_co.gif").createImage();

	public void init(IWorkbench workbench) {
		// do nothing
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		Label label1 = new Label(composite, SWT.NONE);
		label1.setText("Analysis modules invoked on double-checked projects:");
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		label1.setLayoutData(data);
		checktree = new CheckboxTreeViewer(composite, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL);
		analysisModuleContentProvider = new AnalysisModuleContentProvider();
		checktree.setContentProvider(analysisModuleContentProvider);
		checktree.setLabelProvider(analysisModuleContentProvider);
		checktree.addCheckStateListener(analysisModuleContentProvider);
		checktree.addTreeListener(analysisModuleContentProvider);
		checktree.setInput(Plugin.getWorkspace());
		analysisModuleContentProvider.setState();
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessVerticalSpace = true;
		checktree.getControl().setLayoutData(data);
		Composite buttonHolder = new Composite(composite, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.type = SWT.VERTICAL;
		rowLayout.pack = false;
		buttonHolder.setLayout(rowLayout);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		buttonHolder.setLayoutData(data);
		Button buttonSelectAll = new Button(buttonHolder, SWT.PUSH);
		buttonSelectAll.setText("&Select All");
		buttonSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				analysisModuleContentProvider.setAll(true);
			}
		});
		Button buttonDeselectAll = new Button(buttonHolder, SWT.PUSH);
		buttonDeselectAll.setText("D&eselect All");
		buttonDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				analysisModuleContentProvider.setAll(false);
			}
		});
		return composite;
	}

	/**
	 * A simple class to hold information to provide to the
	 * {@link CheckboxTreeViewer}. Only the first level are "real" nodes that
	 * reflect analysis modules the rest are "mirrors" of the "real" nodes
	 * intended to allow the user to visualize the dependencies that are
	 * specified for each analysis module.
	 */
	private static class PreferenceTreeNode {

		boolean isAMirror; // if "false" then "real" otherwise a "mirror"

		Set<PreferenceTreeNode> prerequesites;

		PreferenceTreeNode original; // link to "real" node (or "this" if
		// "real")

		// only valid in "real" nodes
		IExtension am = null; // analysis module extension point information

		boolean isOn = false; // boolean isGrey = false;

		boolean isVisible = false;
	}

	private static final PreferenceTreeNode[] EMPTY = new PreferenceTreeNode[0];

	/**
	 * The content provider for the tree with check boxes that allows the user
	 * to turn on and off analysis module extension points.
	 */
	final class AnalysisModuleContentProvider extends LabelProvider implements
			ICheckStateListener, ITreeContentProvider, ITreeViewerListener {

		/**
		 * The contents for the viewer.
		 */
		private Set<PreferenceTreeNode> m_checktreeContents;

		/**
		 * A map used to link the {@link IExtension}list from the double-checker
		 * plugin to the root level of the {@link PreferenceTreeNode}.
		 */
		private Map<IExtension, PreferenceTreeNode> m_originalMap;

		/**
		 * Provides the list of analysis module identifiers that have been
		 * included by the user in the preference dialog.
		 * 
		 * @return array of ids for each analysis module included by the user in
		 *         the dialog (all are interned)
		 */
		Set<String> getOnIds() {
			Set<String> result = new HashSet<String>();
			for (PreferenceTreeNode node : m_checktreeContents) {
				if (node.isOn) {
					result.add(node.am.getUniqueIdentifier().intern());
				}
			}
			return result;
		}

		/**
		 * Constructs the content provider including reading the valid analysis
		 * modules and which have been excluded from the double-checker plugin.
		 */
		AnalysisModuleContentProvider() {
			// read information from the double-checker plugin
			IExtension[] ams = Plugin.getDefault().allAnalysisExtensions;
			m_checktreeContents = new HashSet<PreferenceTreeNode>();
			m_originalMap = new HashMap<IExtension, PreferenceTreeNode>();
			// Create core contents (ones we care about)
			for (IExtension am : ams) {
				PreferenceTreeNode node = new PreferenceTreeNode();
				node.am = am;
				node.original = node;
				node.isAMirror = false;
				for (String id : Plugin.getDefault().m_includedExtensions) {
					if (node.am.getUniqueIdentifier().equals(id)) {
						node.isOn = true;
					}
				}
				node.isVisible = computeVisibility(am);
				m_checktreeContents.add(node);
				m_originalMap.put(am, node);
			}
			// Create prerequisite mirror tree
			for (PreferenceTreeNode node : m_checktreeContents) {
				node.prerequesites = addPrereqs(node.original.am);
			}
		}

		/**
		 * Called by the constructor to build the prerequisite mirror tree.
		 * 
		 * @param extension
		 *            the analysis module extension point to construct the
		 *            prerequisites for
		 * @return The set of {@link PreferenceTreeNode}that reflects the
		 *         specified prerequisites for the analysis module
		 *         <code>extension</code>
		 */
		private Set<PreferenceTreeNode> addPrereqs(IExtension extension) {
			Set<PreferenceTreeNode> result = new HashSet<PreferenceTreeNode>();
			Set<IExtension> prereqs = Plugin.getDefault()
					.getPrerequisiteAnalysisExtensionPoints(extension);
			for (IExtension prereq : prereqs) {
				PreferenceTreeNode node = new PreferenceTreeNode();
				node.am = null;
				node.original = m_originalMap.get(prereq);
				node.isAMirror = true;
				node.prerequesites = addPrereqs(prereq);
				node.isVisible = node.original.isVisible;
				result.add(node);
			}
			return result;
		}

		/**
		 * @return false if not production, or if category=required
		 */
		private boolean computeVisibility(IExtension am) {
			if (showPrivate) {
				return true;
			}
			IConfigurationElement[] cfgs = am.getConfigurationElements();
			for (int i = 0; i < cfgs.length; i++) {
				if (cfgs[i].getName().equalsIgnoreCase("run")) {
					final String production = cfgs[i]
							.getAttribute("production");
					if (production != null && production.equals("false")) {
						return false;
					}
					final String category = cfgs[i].getAttribute("category");
					if (category != null && category.equals("required")) {
						return false;
					}
				}
			}
			return true;
		}

		private PreferenceTreeNode[] filterNodes(Set<PreferenceTreeNode> nodes) {
			int numVisible = 0;
			if (showPrivate) {
				numVisible = nodes.size();
			} else {
				for (PreferenceTreeNode n : nodes) {
					if (n.isVisible) {
						numVisible++;
					}
				}
			}
			if (numVisible == 0) {
				return EMPTY;
			}
			final PreferenceTreeNode[] result = new PreferenceTreeNode[numVisible];
			if (numVisible == nodes.size()) {
				return nodes.toArray(result);
			}
			int i = 0;
			for (PreferenceTreeNode n : nodes) {
				if (n.isVisible) {
					result[i] = n;
					i++;
				}
			}
			return result;
		}

		/**
		 * Checks if the current dialog state for a specific node is valid.
		 * 
		 * @param node
		 *            the node to check if its state is valid
		 * @return <code>true</code> if all prerequisite analysis modules are
		 *         currently turned on, <code>false</code> otherwise
		 */
		boolean checkState(PreferenceTreeNode node) {
			for (PreferenceTreeNode pnode : node.prerequesites) {
				if (!pnode.original.isOn) {
					return false;
				}
				if (!checkState(pnode)) {
					return false;
				}
			}
			return true;
		}

		void turnOnAllPrerequesites(PreferenceTreeNode node) {
			for (PreferenceTreeNode pnode : node.prerequesites) {
				pnode.original.isOn = true;
				turnOnAllPrerequesites(pnode);
			}
		}

		/**
		 * Updates the viewer to reflect the state of the analysis module
		 * preferences.
		 */
		void setState() {
			// check that prerequisites are enabled for every analysis module
			for (PreferenceTreeNode node : m_checktreeContents) {
				if (node.isOn) {
					node.isOn = checkState(node);
				}
			}
			// update viewer as needed
			for (PreferenceTreeNode node : m_checktreeContents) {
				checktree.setChecked(node, node.isOn);
				setState(node.prerequesites);
				checktree.update(node, null);
			}
		}

		/**
		 * Updates the viewer to reflect the state of a given set of
		 * prerequisites.
		 * 
		 * @param prerequisites
		 *            nodes to be updated
		 */
		private void setState(Set<PreferenceTreeNode> prerequisites) {
			for (PreferenceTreeNode node : prerequisites) {
				checktree.setChecked(node, node.original.isOn);
				checktree.update(node, null);
				setState(node.prerequesites);
			}
		}

		/**
		 * Used by the "Restore Defaults" buttons to reset the dialog to include
		 * only production analyses.
		 */
		private void restoreDefaults() {
			for (PreferenceTreeNode node : m_checktreeContents) {
				IExtension e = (node.isAMirror ? node.original.am : node.am);
				if (e == null) {
					throw new IllegalStateException(
							"analysis module should not be null");
				}
				node.isOn = true; // assume it is production
				for (IExtension nonProductionExtension : Plugin.getDefault().m_nonProductionAnalysisExtensions) {
					if (nonProductionExtension == e) {
						node.isOn = false; // not production
					}
				}
			}
			setState();
		}

		/**
		 * Used by the "Select All" and "Deselect All" buttons to turn on or off
		 * all analysis modules.
		 * 
		 * @param isOn
		 *            <code>true</code> turns all analysis modules on
		 *            <code>false</code> turns them all off
		 */
		private void setAll(boolean isOn) {
			for (PreferenceTreeNode node : m_checktreeContents) {
				node.isOn = isOn;
			}
			setState();
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			PreferenceTreeNode node = (PreferenceTreeNode) parentElement;
			PreferenceTreeNode[] result = filterNodes(node.prerequesites);
			Arrays.sort(result, new Comparator<PreferenceTreeNode>() {
				public int compare(PreferenceTreeNode o1, PreferenceTreeNode o2) {
					PreferenceTreeNode p1 = o1;
					PreferenceTreeNode p2 = o2;
					return p1.original.am.getLabel().compareToIgnoreCase(
							p2.original.am.getLabel());
				}
			});
			return result;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			return (this.getChildren(element).length > 0);
		}

		/**
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			PreferenceTreeNode[] result = filterNodes(m_checktreeContents);
			Arrays.sort(result, new Comparator<PreferenceTreeNode>() {
				public int compare(PreferenceTreeNode o1, PreferenceTreeNode o2) {
					PreferenceTreeNode p1 = o1;
					PreferenceTreeNode p2 = o2;
					return p1.original.am.getLabel().compareToIgnoreCase(
							p2.original.am.getLabel());
				}
			});
			return result;
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
		 *      java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing to do
		}

		/**
		 * @see org.eclipse.jface.viewers.ICheckStateListener#checkStateChanged(org.eclipse.jface.viewers.CheckStateChangedEvent)
		 */
		public void checkStateChanged(CheckStateChangedEvent event) {
			PreferenceTreeNode node = (PreferenceTreeNode) event.getElement();
			node.original.isOn = event.getChecked();
			if (node.original.isOn) {
				// turn on all prerequisites
				turnOnAllPrerequesites(node);
			}
			setState();
		}

		/**
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			PreferenceTreeNode node = (PreferenceTreeNode) element;
			IExtension am = node.original.am;
			IConfigurationElement[] cfgs = am.getConfigurationElements();
			for (int i = 0; i < cfgs.length; i++) {
				if (cfgs[i].getName().equalsIgnoreCase("run")) {
					final String production = cfgs[i]
							.getAttribute("production");
					final String category = cfgs[i].getAttribute("category");
					String label;
					if (production != null && production.equals("false")) {
						label = "experimental";
					} else {
						label = null;
					}
					if (category != null) {
						if (label == null) {
							label = category;
						} else {
							label = label + ", " + category;
						}
					}
					return (label == null) ? am.getLabel() : am.getLabel()
							+ " (" + label + ")";
				}
			}
			return am.getLabel();
		}

		/**
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		@Override
		public Image getImage(Object element) {
			PreferenceTreeNode node = (PreferenceTreeNode) element;
			if (node.isAMirror) {
				if (checktree.getChecked(node)) {
					return PREQ_IMG;
				}
				return PREQ_CO_IMG;
			}
			if (checktree.getChecked(node)) {
				return ANALMOD_IMG;
			}
			return ANALMOD_CO_IMG;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeViewerListener#treeCollapsed(org.eclipse.jface.viewers.TreeExpansionEvent)
		 */
		public void treeCollapsed(TreeExpansionEvent event) {
			setState();
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeViewerListener#treeExpanded(org.eclipse.jface.viewers.TreeExpansionEvent)
		 */
		public void treeExpanded(TreeExpansionEvent event) {
			setState();
		}
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		super.performDefaults();
		analysisModuleContentProvider.restoreDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		final Set<String> onIds = analysisModuleContentProvider.getOnIds();
		if (Plugin.getDefault().isIncludedExtensionsChanged(onIds)) {
			// save the excluded list from the dialog into the main plugin
			MessageDialog
					.openInformation(
							(Shell) null,
							"Analysis Set Change Warning",
							"For production work, it is recommended that you restart Eclipse"
									+ " when changing the set of JSure analyses.  Especially if you"
									+ " turned something off.  If you ignore this warning (i.e.,"
									+ " for testing), please perform \"Project | Clean...\" for "
									+ "all open projects");
			Plugin.getDefault().updateIncludedExtensions(onIds);
		}
		return true;
	}
}