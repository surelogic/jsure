package com.surelogic.jsure.client.eclipse.views.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.editors.*;
import com.surelogic.jsure.client.eclipse.views.*;
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
	protected void handleDoubleClick(IStructuredSelection selection) {
		final Object o = selection.getFirstElement();
		if (o instanceof Type) {
			Type t = (Type) o;
			PromisesXMLEditor.openInEditor(t.getPath(), false);
		}
		else if (o instanceof Package) {
			Package p = (Package) o;
			PromisesXMLEditor.openInEditor(p.name.replace('.', '/')+"/package-info"+TestXMLParserConstants.SUFFIX, false);
		}
	}
	
	/**
	 * Meant to be called when the editor creates new files
	 */
	void refresh() {
		f_content.build();		
	}
	
	private static final Package[] noPackages = new Package[0];
	
	class Provider extends PromisesXMLContentProvider implements IJSureTreeContentProvider {
		Package[] pkgs = noPackages;
		
		Provider() {
			super(true);
		}
		
		public String build() {
			final List<Package> l = new ArrayList<Package>();
			for(Map.Entry<String,Collection<String>> e : PromisesXMLEditor.findAllPromisesXML().entrySet()) {
				l.add(new Package(e));
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
		
		public Package(Entry<String, Collection<String>> e) {
			name = e.getKey();
			types = new Type[e.getValue().size()];
			int i=0;
			for(String s : e.getValue()) {
				types[i] = new Type(this, s);
				i++;
			}
			Arrays.sort(types);
		}

		@Override
		public String toString() {
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
		PackageElement root;
		
		Type(Package pkg, String name) {
			this.pkg = pkg;
			this.name = name;
		}

		String getPath() {
			return pkg.name.replace('.', '/')+'/'+name+TestXMLParserConstants.SUFFIX;
		}

		boolean hasChildren() {
			return true;
		}

		@Override
		public String toString() {
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
					root = PromisesXMLContentProvider.getXML(in);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
