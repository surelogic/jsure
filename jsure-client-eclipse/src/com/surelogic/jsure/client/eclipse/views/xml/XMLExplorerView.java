package com.surelogic.jsure.client.eclipse.views.xml;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.XUtil;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.editors.*;
import com.surelogic.jsure.client.eclipse.views.*;
import com.surelogic.jsure.client.eclipse.views.results.ResultsImageDescriptor;
import com.surelogic.jsure.core.xml.PromisesLibMerge;
import com.surelogic.xml.*;

import edu.cmu.cs.fluid.util.Pair;

public class XMLExplorerView extends AbstractJSureView {

	private final Provider f_content = new Provider();

	private TreeViewer f_viewer;

	private static final String USER_MODS_ONLY = "Show only user-added/modified library annotations";

	private final Action f_toggleShowDiffs = new Action(USER_MODS_ONLY,
			IAction.AS_CHECK_BOX) {
		@Override
		public void run() {
			f_content.toggleViewingType();
			f_viewer.refresh();
		}
	};

	@Override
	protected Control buildViewer(Composite parent) {
		f_viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);

		f_viewer.setContentProvider(f_content);
		f_viewer.setLabelProvider(f_content);
		f_content.build();
		f_viewer.setInput(f_content); // Needed to show something?
		return f_viewer.getControl();
	}

	@Override
	protected StructuredViewer getViewer() {
		return f_viewer;
	}

	@Override
	protected void makeActions() {
		f_toggleShowDiffs.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_ANNOTATION_DELTA));
		f_toggleShowDiffs.setToolTipText(USER_MODS_ONLY);
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		manager.add(f_toggleShowDiffs);
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(f_toggleShowDiffs);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		final Object o = s.getFirstElement();
		if (XUtil.useExperimental() && o instanceof Type) {
			final Type t = (Type) o;
			if (t.isLocal) {
				final boolean hasUpdate = t.hasUpdate();
				manager.add(new Action(hasUpdate ? "Update local XML"
						: "Merge changes to JSure") {
					@Override
					public void run() {
						PromisesLibMerge.merge(hasUpdate, t.getPath());
						/*
						 * if (!hasUpdate) { Pair<File,File> rv =
						 * PromisesXMLEditor.findPromisesXML(t.getPath()); if
						 * (rv.second().isFile()) { rv.second().delete(); } }
						 */
						PromisesXMLReader.clear(t.getPath());
						PromisesXMLReader.refreshAll();
					}
				});
			}
		}
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		final Object o = selection.getFirstElement();
		if (o instanceof Type) {
			Type t = (Type) o;
			PromisesXMLEditor.openInEditor(t.getPath(), false);
		} else if (o instanceof Package) {
			Package p = (Package) o;
			PromisesXMLEditor.openInEditor(getPackagePath(p.name), false);
		} else if (o instanceof IJavaElement) {
			IJavaElement e = (IJavaElement) o;
			while (e != null) {
				if (e instanceof PackageElement) {
					break;
				}
				e = e.getParent();
			}
			PackageElement p = (PackageElement) e;

			if (p.getClassElement() == null) {
				PromisesXMLEditor.openInEditor(getPackagePath(p.getName()),
						false);
			} else {
				final IEditorPart ep = PromisesXMLEditor.openInEditor(p
						.getName().replace('.', '/')
						+ '/'
						+ p.getClassElement().getName()
						+ TestXMLParserConstants.SUFFIX, false);
				if (ep instanceof PromisesXMLEditor) {
					final PromisesXMLEditor xe = (PromisesXMLEditor) ep;
					xe.focusOn((IJavaElement) o);
				}
			}
		}
	}

	private String getPackagePath(String qname) {
		return qname.replace('.', '/') + "/package-info"
				+ TestXMLParserConstants.SUFFIX;
	}

	private static final Package[] noPackages = new Package[0];

	enum Viewing {
		ALL, DIFFS() {
			@Override
			boolean matches(Filterable f) {
				return f.hasDiffs();
			}
			/*
			 * }, CONFLICTS() {
			 * 
			 * @Override boolean matches(Filterable f) { return
			 * f.hasConflicts(); }
			 */
		};
		boolean matches(Filterable f) {
			return true;
		}
	}

	static final Object[] noDiffs = new Object[] {
		"No changes have been made to the standard library annotations"
	};
	
	class Provider extends PromisesXMLContentProvider implements
			IJSureTreeContentProvider, PromisesXMLReader.Listener {
		Package[] pkgs = noPackages;
		Viewing type = Viewing.ALL;

		Provider() {
			super(true);
			PromisesXMLReader.listenForRefresh(this);
		}

		void toggleViewingType() {
			type = (type == Viewing.ALL) ? Viewing.DIFFS : Viewing.ALL;
		}

		void setViewingType(Viewing v) {
			if (v != null) {
				type = v;
			}
		}

		public void refresh(PackageElement e) {
			refreshAll();
		}

		public void refreshAll() {
			build();

			// This shouldn't be necessary, but Eclipse doesn't seem to 
			// realize that the viewer changed
			new SLUIJob() {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					f_viewer.refresh();
					return Status.OK_STATUS;
				}
			}.schedule();	
		}

		@Override
		public String build() {
			final List<Package> l = new ArrayList<Package>();
			final Map<String, Collection<String>> local = PromisesXMLEditor
					.findLocalPromisesXML();
			for (Map.Entry<String, Collection<String>> e : PromisesXMLEditor
					.findAllPromisesXML().entrySet()) {
				Package p = new Package(e, local.get(e.getKey()));
				l.add(p);
			}
			Collections.sort(l);
			pkgs = l.toArray(noPackages);
			return "Something";
		}

		@Override
		public Object[] getElements(Object inputElement) {
			switch (type) {
			// case CONFLICTS:
			case DIFFS:
				Object[] rv = filter(type, pkgs);
				if (rv.length == 0) {
					return noDiffs;
				}
				return rv;
			default:
				return pkgs;
			}
		}

		@Override
		public boolean hasChildren(Object element) {			
			if (element instanceof Package) {
				Package p = (Package) element;
				return p.types.length != 0;
			}
			if (element instanceof Type) {
				Type t = (Type) element;
				return t.hasChildren();
			}
			if (element instanceof String) {
				return false;
			}				
			return super.hasChildren(element);
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof Package) {
				Package p = (Package) parent;
				return filter(type, p.types);
			}
			if (parent instanceof Type) {
				Type t = (Type) parent;
				t.buildChildren();
				if (t.root != null) {
					return super.getChildren(t.root.getClassElement());
				}
			}
			if (parent instanceof String) {
				return noStrings;
			}
			// return noStrings;
			return super.getChildren(parent);
		}

		@Override
		public Object getParent(Object element) {			
			if (element instanceof Type) {
				Type t = (Type) element;
				return t.pkg;
			}
			if (element instanceof Package) {
				return null;
			}
			if (element instanceof String) {
				return null;
			}
			return super.getParent(element);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IJavaElement) {
				return super.getText(element);
			}
			if (element != null) {
				return element.toString();
			}
			return null;
		}

		/**
		 * Gets a cached image with an optional conflict (warning) decorator.
		 * 
		 * @param symbolicName
		 *            a name from {@link CommonImages}.
		 * @param conflict
		 *            {@code true} if a promise conflict exists, {@code false}
		 *            otherwise.
		 * @return an image that is carefully cached. The image should
		 *         <i>not</i> be disposed by the calling code.
		 */
		private final Image getCachedImage(String symbolicName, boolean conflict) {
			return getCachedImage(SLImages.getImageDescriptor(symbolicName),
					conflict);
		}

		/**
		 * Gets a cached image with an optional conflict (warning) decorator.
		 * 
		 * @param imageDescriptor
		 *            an image descriptor.
		 * @param conflict
		 *            {@code true} if a promise conflict exists, {@code false}
		 *            otherwise.
		 * @return an image that is carefully cached. The image should
		 *         <i>not</i> be disposed by the calling code.
		 */
		private final Image getCachedImage(ImageDescriptor imageDescriptor,
				boolean conflict) {
			final int flag = conflict ? CoE_Constants.INFO_WARNING
					: CoE_Constants.NONE;
			ResultsImageDescriptor rid = new ResultsImageDescriptor(
					imageDescriptor, flag, new Point(22, 16));
			return rid.getCachedImage();
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof Package) {
				Package p = (Package) element;
				return getCachedImage(CommonImages.IMG_PACKAGE,
						p.hasConflicts());
			}
			if (element instanceof Type) {
				Type t = (Type) element;
				return getCachedImage(CommonImages.IMG_CLASS, t.hasConflicts());
			}
			if (element instanceof String) {
				return null;
			}
			return getCachedImage(super.getImageDescriptor(element), false);
		}

		@Override
		public void dispose() {
			PromisesXMLReader.stopListening(this);
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Don't do anything on this event
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			// Don't do anything on this event
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// Don't do anything on this event
		}
	}

	interface Filterable {
		boolean hasDiffs();

		boolean hasConflicts();
	}

	static <T extends Filterable> Object[] filter(Viewing type, T[] elements) {
		if (elements.length == 0) {
			return elements;
		}
		List<T> l = new ArrayList<T>();
		for (T e : elements) {
			if (type.matches(e)) {
				l.add(e);
			}
		}
		return l.toArray();
	}

	static class Package implements Filterable, Comparable<Package> {
		final String name;
		final Type[] types;

		public Package(Entry<String, Collection<String>> e,
				Collection<String> local) {
			final boolean hasLocal = local != null;
			name = e.getKey();
			types = new Type[e.getValue().size()];
			int i = 0;
			for (String type : e.getValue()) {
				types[i] = new Type(this, type, hasLocal ? local.contains(type)
						: false);
				i++;
			}
			Arrays.sort(types);
		}

		@Override
		public String toString() {
			if (hasDiffs()) {
				return PromisesXMLContentProvider.DIRTY_PREFIX + name;
			}
			return name;
		}

		@Override
		public int compareTo(Package o) {
			return name.compareTo(o.name);
		}

		@Override
		public boolean hasConflicts() {
			if (hasDiffs()) {
				for (Type t : types) {
					if (t.hasUpdate()) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean hasDiffs() {
			for (Type t : types) {
				if (t.hasDiffs()) {
					return true;
				}
			}
			return false;
		}
	}

	static class Type implements Filterable, Comparable<Type> {
		final Package pkg;
		final String name;
		final boolean isLocal;
		PackageElement root;

		Type(Package pkg, String name, boolean isLocal) {
			this.pkg = pkg;
			this.name = name;
			this.isLocal = isLocal;
		}

		String getPath() {
			return pkg.name.replace('.', '/') + '/' + name
					+ TestXMLParserConstants.SUFFIX;
		}

		boolean hasChildren() {
			return true;
		}

		boolean hasUpdate() {
			return PromisesLibMerge.checkForUpdate(getPath());
		}

		@Override
		public String toString() {
			if (hasDiffs()) {
				/*
				 * Handled as a decorator
				 * 
				 * if (hasUpdate()) { return "<> " + name; }
				 */
				return PromisesXMLContentProvider.DIRTY_PREFIX + name;
			}
			return name;
		}

		@Override
		public int compareTo(Type o) {
			return name.compareTo(o.name);
		}

		void buildChildren() {
			buildChildren(true);
		}
		
		/**
		 * @return true if root exists after the call
		 */
		private boolean buildChildren(boolean force) {
			if (root != null) {
				return true;
			}
			final String path = getPath();
			if (force) {
				final Pair<File, File> rv = PromisesXMLEditor.findPromisesXML(path);
				try {
					root = PromisesXMLReader.load(path, rv.first(), rv.second());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			} else {
				root = PromisesXMLReader.get(path);
				return root != null;
			}
		}

		@Override
		public boolean hasConflicts() {
			if (hasDiffs()) {
				return hasUpdate();
			}
			return false;
		}

		@Override
		public boolean hasDiffs() {
			// Check if there are any changes within Eclipse
			if (buildChildren(false)) {
				return root.isModified();
			}
			return isLocal;
		}
	}
}
