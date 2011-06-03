package com.surelogic.jsure.views.debug.oracleDiff;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ATTR;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;

import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;
import edu.cmu.cs.fluid.sea.xml.SeaSummary.*;
import edu.cmu.cs.fluid.util.ArrayUtil;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.xml.Entity;
import com.surelogic.jsure.client.eclipse.views.*;
import com.surelogic.jsure.core.listeners.PersistentDropInfo;
import com.surelogic.fluid.javac.scans.*;

public class SnapshotDiffContentProvider implements IJSureTreeContentProvider {
	private static final Object[] noElements = ArrayUtil.empty;
	private static final Object[] nothingToShow = new Object[1];
	static {
		nothingToShow[0] = new Category(null, "No differences");
	}	
	private Diff diff;
	
	@Override
	public String build(ScanStatus s) {
		final JSureScanInfo scan = JSureScansHub.getInstance().getCurrentScanInfo();
		if (scan == null) {
			return null;
		}
		final Collection<? extends IDropInfo> info = scan.getRawInfo();
		if (!info.isEmpty()) {
			try {
				File file = null;
				for(String name : scan.findProjectsLabel().split(", ")) {			
					final IProject p = EclipseUtility.getProject(name);
					if (p == null || !p.exists()) {
						continue;
					}
					final File pFile = p.getLocation().toFile();
					file  = SeaSummary.findSummary(pFile.getAbsolutePath());	
				}
				diff = SeaSummary.diff(info, file);				
				return scan.getLabel();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
				System.out.println("No snapshot to diff against");
		}
		return null;
	}
	
	public Object[] getElements(Object input) {
		if (diff != null) {
			Object[] rv = diff.getCategories();
			if (rv.length == 0) {
				return nothingToShow;
			}
			return rv;
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
			Entity e = (Entity) element;
			StringBuilder sb = new StringBuilder();
			if (e.isNewer()) {
				sb.append("New: ");
			}
			else if (e.isOld()) {
				sb.append("Old: ");
			}
			sb.append(e.getAttribute(MESSAGE_ATTR));
			return sb.toString();
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
