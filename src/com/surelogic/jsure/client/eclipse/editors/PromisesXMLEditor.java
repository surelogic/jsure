package com.surelogic.jsure.client.eclipse.editors;

import java.io.*;
import java.net.URI;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.EditorPart;

import com.surelogic.xml.IJavaElement;
import com.surelogic.xml.PackageElement;
import com.surelogic.xml.PromisesXMLReader;

import edu.cmu.cs.fluid.util.ArrayUtil;

public class PromisesXMLEditor extends EditorPart {
	private final Provider provider = new Provider();
    private TreeViewer contents;
    
    @Override
    public void createPartControl(Composite parent) {
       contents = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
       contents.setContentProvider(provider);
       contents.setLabelProvider(provider);
       if (provider.getInput() != null) {
    	   contents.setInput(provider.getInput());
       }
    }
    
    @Override
    public void init(IEditorSite site, IEditorInput input) {
       setSite(site);
       setInput(input);              
       if (input instanceof IURIEditorInput) {
    	   IURIEditorInput f = (IURIEditorInput) input;
    	   if (contents != null) {
    		   contents.setInput(f.getURI());
    	   } else {
    		   provider.inputChanged(contents, null, f.getURI());    		   
    	   }
       }
    }
    
    @Override
    public void setFocus() {
       if (contents != null) {
          contents.getControl().setFocus();
       }
    }	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		provider.save(monitor);
	}

	@Override
	public void doSaveAs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDirty() {
		return provider.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	class Provider implements ITreeContentProvider, ILabelProvider {
		URI location;
		PackageElement pkg;
		Object[] roots;
		
		URI getInput() {
			return location;
		}
		
		void save(IProgressMonitor monitor) {
			// TODO Auto-generated method stub
			
		}

		boolean isDirty() {
			return pkg.isDirty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != location) {
				location = (URI) newInput;				
				System.out.println("Editor got "+location);
				build();
			} else {
				System.out.println("Ignoring duplicate input");
			}
			if (viewer != null && viewer == contents) {
				contents.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						contents.expandAll();
					}					
				});

			}
		}

		private void build() {
			if (location != null) {
				try {
					/*
					File f = new File(location);
					System.out.println("location = "+f);
					*/
					InputStream in = location.toURL().openStream();
					PromisesXMLReader r = new PromisesXMLReader();
					r.read(in);
					roots = new Object[1];
					roots[0] = pkg = r.getPackage();					
				} catch (Exception e) {
					pkg = null;
					roots = ArrayUtil.empty;
				}
			}
		}
		
		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof String) {
				return ArrayUtil.empty;
			}
			return ((IJavaElement) element).getChildren();
		}

		@Override
		public Object getParent(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof String) {
				return false;
			}
			return ((IJavaElement) element).hasChildren();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return roots;
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof String) {
				return element.toString();
			}
			return ((IJavaElement) element).getLabel();
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {
			// Nothing to do
		}
		@Override
		public boolean isLabelProperty(Object element, String property) {
			// Nothing to do
			return false;
		}
		@Override
		public void removeListener(ILabelProviderListener listener) {
			// Nothing to do
		}
		@Override
		public void dispose() {
			// Nothing to do
		}
	}
}
