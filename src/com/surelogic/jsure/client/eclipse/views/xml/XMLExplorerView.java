package com.surelogic.jsure.client.eclipse.views.xml;

import java.util.*;
import java.util.Map.Entry;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;
import com.surelogic.jsure.client.eclipse.views.*;

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
	
	/**
	 * Meant to be called when the editor creates new files
	 */
	void refresh() {
		f_content.build();		
	}
	
	private static final Package[] noPackages = new Package[0];
	
	class Provider implements IJSureTreeContentProvider {
		Package[] pkgs = noPackages;
		
		@Override
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
			return false;
		}
		
		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof Package) {
				Package p = (Package) parent;
				return p.types;
			}
			return noStrings;
		}

		@Override
		public Object getParent(Object element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getText(Object element) {
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
			if (element instanceof String) {
				return SLImages.getImage(CommonImages.IMG_CLASS);
			}
			return null;
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
		final String[] types;
		
		public Package(Entry<String, Collection<String>> e) {
			name = e.getKey();
			types = e.getValue().toArray(noStrings);
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
}
