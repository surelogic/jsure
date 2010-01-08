package com.surelogic.jsure.views.debug.oracleDiff;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;

import edu.cmu.cs.fluid.sea.xml.SeaSummary.*;
import com.surelogic.jsure.xml.Entity;

public class SnapshotDiffContentProvider implements ITreeContentProvider, ILabelProvider {
	private static final Object[] noElements = new Object[0];
	private static final Object[] nothingToShow = new Object[1];
	static {
		nothingToShow[0] = new Category(null, "No differences");
	}	
	private Diff diff;
	
	public void setDiff(Diff d) {
		diff = d;
	}
	
	public Object[] getElements(Object input) {
		if (diff != null) {
			Object[] rv = diff.getCategories();
			if (rv.length == 0) {
				return nothingToShow;
			}
		}
		return noElements;
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof Category) {
			Category c = (Category) parent;
			return c.getChildren();
		}
		return noElements;
	}

	public Object getParent(Object element) {
		throw new UnsupportedOperationException();
	}

	public boolean hasChildren(Object element) {
		if (element instanceof Category) {
			Category c = (Category) element;
			return c.hasChildren();
		}
		return false;
	}

	public String getText(Object element) {
		if (element instanceof Category) {
			Category c = (Category) element;
			if (c.file == null) {
				return c.name;
			}
			return c.name+"  in  "+c.file;
		}
		else if (element instanceof Entity) {
			return Category.toString((Entity) element);
		}
		return null;
	}	
	
	public Image getImage(Object element) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub		
	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub	
		return false;
	}
	
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub		
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub		
	}

	public void dispose() {
		// TODO Auto-generated method stub
	}
}
