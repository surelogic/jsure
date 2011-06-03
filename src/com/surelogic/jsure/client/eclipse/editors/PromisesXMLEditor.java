package com.surelogic.jsure.client.eclipse.editors;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.EditorPart;

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
    		   provider.inputChanged(null, null, f.getURI());    		   
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
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	class Provider implements ITreeContentProvider, ILabelProvider {
		URI location;
		Object[] roots;
		
		URI getInput() {
			return location;
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
		}

		private void build() {
			if (location != null) {
				try {
					InputStream in = location.toURL().openStream();
				} catch (MalformedURLException e) {
					roots = ArrayUtil.empty;
				} catch (IOException e) {
					roots = ArrayUtil.empty;
				}
			}
		}
		
		@Override
		public Object[] getChildren(Object parentElement) {
			System.out.println("Trying to get children");
			return ArrayUtil.empty;
		}

		@Override
		public Object getParent(Object element) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return roots;
		}
		
		@Override
		public String getText(Object element) {
			// TODO Auto-generated method stub
			return null;
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
