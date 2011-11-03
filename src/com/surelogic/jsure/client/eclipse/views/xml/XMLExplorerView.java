package com.surelogic.jsure.client.eclipse.views.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.editors.*;
import com.surelogic.jsure.client.eclipse.views.*;
import com.surelogic.jsure.core.xml.PromisesLibMerge;
import com.surelogic.xml.IJavaElement;
import com.surelogic.xml.PackageElement;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.util.Pair;

public class XMLExplorerView extends AbstractJSureView {	
	final IJSureTreeContentProvider f_content = new Provider();
	TreeViewer f_viewer;
	
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
		// TODO Auto-generated method stub				
	}
	
	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		// TODO Auto-generated method stub		
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		final Object o = s.getFirstElement();
		if (o instanceof Type) {
			final Type t = (Type) o;
			if (t.isLocal) {
				final boolean hasUpdate = t.hasUpdate();			
				manager.add(new Action(hasUpdate ? "Update local XML" : "Merge changes to JSure") {
					@Override
					public void run() {
						PromisesLibMerge.merge(hasUpdate, t.getPath());
						f_viewer.refresh();
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
		}
		else if (o instanceof Package) {
			Package p = (Package) o;
			PromisesXMLEditor.openInEditor(getPackagePath(p.name), false);
		}
		else if (o instanceof IJavaElement) {
			IJavaElement e = (IJavaElement) o;
			while (e != null) {
				if (e instanceof PackageElement) {
					break;
				}
				e = e.getParent();
			}
			PackageElement p = (PackageElement) e;
			
			if (p.getClassElement() == null) {
				PromisesXMLEditor.openInEditor(getPackagePath(p.getName()), false);
			} else {
				final IEditorPart ep = 
					PromisesXMLEditor.openInEditor(p.getName().replace('.', '/')+'/'+p.getClassElement().getName()+TestXMLParserConstants.SUFFIX, false);
				if (ep instanceof PromisesXMLEditor) {
					final PromisesXMLEditor xe = (PromisesXMLEditor) ep;
					xe.focusOn((IJavaElement) o);
				}
			}
		}
	}

	private String getPackagePath(String qname) {
		return qname.replace('.', '/')+"/package-info"+TestXMLParserConstants.SUFFIX;
	}
	
	/**
	 * Meant to be called when the editor creates new files
	 */
	void refresh() {
		f_content.build();		
	}
	
	private static final Package[] noPackages = new Package[0];
	
	class Provider extends PromisesXMLContentProvider implements IJSureTreeContentProvider, PromisesXMLContentProvider.Listener {
		Package[] pkgs = noPackages;
		
		Provider() {
			super(true, true);
			listenForRefresh(this);
		}
		
		public void refresh(PackageElement e) {
			f_viewer.refresh();
		}
		
		public String build() {
			final List<Package> l = new ArrayList<Package>();
			final Map<String,Collection<String>> local = PromisesXMLEditor.findLocalPromisesXML();
			for(Map.Entry<String,Collection<String>> e : PromisesXMLEditor.findAllPromisesXML().entrySet()) {
				Package p = new Package(e, local.get(e.getKey()));
				l.add(p);				
			}
			Collections.sort(l);
			pkgs = l.toArray(noPackages);
			return "Something";
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return pkgs;
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
			return super.hasChildren(element);
		}
		
		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof Package) {
				Package p = (Package) parent;
				return p.types;
			}
			if (parent instanceof Type) {
				Type t = (Type) parent;
				t.buildChildren();
				if (t.root != null) {
					return super.getChildren(t.root.getClassElement());
				}
			}
			//return noStrings;
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
		
		@Override
		public Image getImage(Object element) {
			if (element instanceof Package) {
				return SLImages.getImage(CommonImages.IMG_PACKAGE);
			}
			if (element instanceof Type) {
				return SLImages.getImage(CommonImages.IMG_CLASS);
			}
			return super.getImage(element);
		}
		
		@Override
		public void dispose() {
			// TODO Auto-generated method stub
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		} 
	}
	
	static class Package implements Comparable<Package> {
		final String name;
		final Type[] types;
		final boolean hasLocal;
		
		public Package(Entry<String, Collection<String>> e, Collection<String> local) {
			hasLocal = local != null;
			name = e.getKey();
			types = new Type[e.getValue().size()];
			int i=0;
			for(String type : e.getValue()) {
				types[i] = new Type(this, type, hasLocal ? local.contains(type) : false);				
				i++;
			}
			Arrays.sort(types);
		}

		@Override
		public String toString() {
			if (hasLocal) {
				return "> "+name;
			}
			return name;
		}
		
		@Override
		public int compareTo(Package o) {
			return name.compareTo(o.name);
		}		
	}
	
	static class Type implements Comparable<Type> {
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
			return pkg.name.replace('.', '/')+'/'+name+TestXMLParserConstants.SUFFIX;
		}

		boolean hasChildren() {
			return true;
		}

		boolean hasUpdate() {
			return PromisesLibMerge.checkForUpdate(getPath());	
		}
		
		@Override
		public String toString() {
			if (isLocal) {
				if (hasUpdate()) {
					return "<> "+name;
				}
				return "> "+name;
			}
			return name;
		}
		
		@Override
		public int compareTo(Type o) {
			return name.compareTo(o.name);
		}	
		
		void buildChildren() {
			if (root != null) {
				return;
			}
			final String path = getPath();		
			final Pair<File,?> rv = PromisesXMLEditor.findPromisesXML(path);
			if (rv != null) {
				try {
					InputStream in = new FileInputStream(rv.first());
					root = PromisesXMLContentProvider.getXML(path, in);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
