package com.surelogic.jsure.client.eclipse.preferences;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.analysis.IAnalysisInfo;
import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.core.driver.DoubleChecker;
import com.surelogic.jsure.core.driver.IAnalysisContainer;

import edu.cmu.cs.fluid.java.CommonStrings;

/**
 * Preference page to set preferences for the verifying analyses that are run by
 * the JSure tool. It reads and updates the list of excluded analysis module
 * extension points for double-checking.
 */
public final class AnalysisSelectionPreferencePage extends
		org.eclipse.jface.preference.PreferencePage implements
		IWorkbenchPreferencePage {

	private CheckboxTreeViewer checktree;

	private AnalysisModuleContentProvider analysisModuleContentProvider;

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
		label1.setText(I18N
				.msg("jsure.eclipse.preference.page.analysis.selection.title.msg"));
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
		checktree.setInput(DoubleChecker.getWorkspace());
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
		data.verticalAlignment = SWT.TOP;
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
		IAnalysisInfo am = null; // analysis module extension point information

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
		 * A map used to link the {@link IAnalysisInfo}list from the
		 * double-checker plugin to the root level of the
		 * {@link PreferenceTreeNode}.
		 */
		private Map<IAnalysisInfo, PreferenceTreeNode> m_originalMap;

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
					result.add(CommonStrings.intern(node.am
							.getUniqueIdentifier()));
				}
			}
			return result;
		}

		/**
		 * Constructs the content provider including reading the valid analysis
		 * modules and which have been excluded from the double-checker plugin.
		 */
		AnalysisModuleContentProvider() {
			// read information from the double-checker plug-in
			m_checktreeContents = new HashSet<PreferenceTreeNode>();
			m_originalMap = new HashMap<IAnalysisInfo, PreferenceTreeNode>();
			// Create core contents (ones we care about)
			for (IAnalysisInfo am : DoubleChecker.getDefault()
					.getAllAnalysisInfo()) {
				PreferenceTreeNode node = new PreferenceTreeNode();
				node.am = am;
				node.original = node;
				node.isAMirror = false;
				node.isOn = am.isIncluded();
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
		private Set<PreferenceTreeNode> addPrereqs(IAnalysisInfo extension) {
			Set<PreferenceTreeNode> result = new HashSet<PreferenceTreeNode>();
			Set<IAnalysisInfo> prereqs = DoubleChecker.getDefault()
					.getPrerequisiteAnalysisExtensionPoints(extension);
			for (IAnalysisInfo prereq : prereqs) {
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
		private boolean computeVisibility(IAnalysisInfo am) {
			if (XUtil.useExperimental) {
				return true;
			}
			return am.isProduction() && !isRequired(am);
		}

		private boolean isRequired(IAnalysisInfo am) {
			return "required".equals(am.getCategory());
		}

		private PreferenceTreeNode[] filterNodes(Set<PreferenceTreeNode> nodes) {
			int numVisible = 0;
			if (XUtil.useExperimental) {
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
				IAnalysisInfo e = (node.isAMirror ? node.original.am : node.am);
				if (e == null) {
					throw new IllegalStateException(
							"analysis module should not be null");
				}
				node.isOn = e.isProduction();
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
				if (isOn && (node.isVisible || isRequired(node.am))) {
					node.isOn = true;
					if (isOn) {
						turnOnAllPrerequesites(node);
					}
				} else {
					node.isOn = false;
				}
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

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return (this.getChildren(element).length > 0);
		}

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

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// nothing to do
		}

		public void checkStateChanged(CheckStateChangedEvent event) {
			PreferenceTreeNode node = (PreferenceTreeNode) event.getElement();
			node.original.isOn = event.getChecked();
			if (node.original.isOn) {
				// turn on all prerequisites
				turnOnAllPrerequesites(node);
			}
			setState();
		}

		@Override
		public String getText(Object element) {
			PreferenceTreeNode node = (PreferenceTreeNode) element;
			IAnalysisInfo am = node.original.am;
			String label;
			if (!am.isProduction()) {
				label = "experimental";
			} else {
				label = null;
			}
			if (am.getCategory() != null) {
				if (label == null) {
					label = am.getCategory();
				} else {
					label = label + ", " + am.getCategory();
				}
			}
			return (label == null) ? am.getLabel() : am.getLabel() + " ("
					+ label + ")";
		}

		@Override
		public Image getImage(Object element) {
			PreferenceTreeNode node = (PreferenceTreeNode) element;
			if (node.isAMirror) {
				if (checktree.getChecked(node)) {
					return SLImages.getImage(CommonImages.IMG_PREREQUISITE);
				}
				return SLImages.getImage(CommonImages.IMG_PREREQUISITE_GRAY);
			}
			if (checktree.getChecked(node)) {
				return SLImages.getImage(CommonImages.IMG_GREEN_DOT);
			}
			return SLImages.getImage(CommonImages.IMG_EMPTY_DOT);
		}

		public void treeCollapsed(TreeExpansionEvent event) {
			setState();
		}

		public void treeExpanded(TreeExpansionEvent event) {
			setState();
		}
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		analysisModuleContentProvider.restoreDefaults();
	}

	@Override
	public boolean performOk() {
		final Set<String> onIds = analysisModuleContentProvider.getOnIds();
		final IAnalysisContainer container = DoubleChecker.getDefault();
		if (container.isIncludedExtensionsChanged(onIds)) {
			container.updateIncludedExtensions(onIds);
		}
		return true;
	}
}