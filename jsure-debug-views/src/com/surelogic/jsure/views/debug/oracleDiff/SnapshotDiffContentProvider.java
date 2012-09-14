package com.surelogic.jsure.views.debug.oracleDiff;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ATTR;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.xml.Entity;
import com.surelogic.jsure.client.eclipse.views.IJSureTreeContentProvider;
import com.surelogic.jsure.core.preferences.ModelingProblemFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;
import edu.cmu.cs.fluid.sea.xml.SeaSummary.*;
import edu.cmu.cs.fluid.util.ArrayUtil;

public class SnapshotDiffContentProvider implements IJSureTreeContentProvider {
	private static final Object[] noElements = ArrayUtil.empty;
	private static final Object[] nothingToDiff = new Object[1];
	private static final Object[] nothingToShow = new Object[1];
	static {
		nothingToDiff[0] = new Category(null, "Nothing to diff");
		nothingToShow[0] = new Category(null, "No differences");
	}
	private Diff diff;

	@Override
	public String build() {
		final JSureScanInfo scan = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		if (scan == null) {
			diff = null;
			return null;
		}
		final Collection<IDrop> info = scan.getDropInfo();
		if (!info.isEmpty()) {
			try {
				File file = null;
				for (String name : scan.findProjectsLabel().split(", ")) {
					final IProject p = EclipseUtility.getProject(name);
					if (p == null || !p.exists()) {
						continue;
					}
					final File pFile = p.getLocation().toFile();
					file = SeaSummary.findSummary(pFile.getAbsolutePath());
				}
				if (file != null) {
					diff = SeaSummary.diff(info, file, ModelingProblemFilterUtility.defaultFilter);
					if (diff != null) {
						return scan.getLabel();
					}
				} else {
					diff = null;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			System.out.println("No snapshot to diff against");
			diff = null;
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
		return nothingToDiff;
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof IViewable) {
			IViewable c = (IViewable) parent;
			return c.getChildren();
		}
		return noElements;
	}

	public Object getParent(Object element) {
		throw new UnsupportedOperationException();
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IViewable) {
			IViewable c = (IViewable) element;
			return c.hasChildren();
		}
		return false;
	}

	public String getText(Object element) {
		if (element instanceof IViewable) {
			IViewable c = (IViewable) element;
			return c.getText();
		} else if (element instanceof Entity) {
			Entity e = (Entity) element;
			StringBuilder sb = new StringBuilder();
			if (e.isNewer()) {
				sb.append("New: ");
			} else if (e.isOld()) {
				sb.append("Old: ");
			}
			sb.append(e.getAttribute(MESSAGE_ATTR));
			return sb.toString();
		}
		return null;
	}

	public Image getImage(Object element) {
		if (element instanceof Entity) {
			Entity e = (Entity) element;
			if (e.isNewer()) {
				return SLImages.getImage(CommonImages.IMG_EDIT_ADD);
			} else if (e.isOld()) {
				return SLImages.getImage(CommonImages.IMG_EDIT_DELETE); 
			}
		}
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
