package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.eclipse.SLImages;

import edu.cmu.cs.fluid.eclipse.ui.ITableContentProvider;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;

public final class ProposedPromiseContentProvider implements
		ITableContentProvider {

	public static final String[] COLUMN_LABELS = { "Proposed Promise",
			"Resource", "Line" };

	private final List<ProposedPromiseDrop> f_contents = new ArrayList<ProposedPromiseDrop>();

	public void build() {
		f_contents.clear();

		List<ProposedPromiseDrop> proposedPromiseDrops = ProposedPromiseDrop
				.filterOutDuplicates(Sea.getDefault().getDropsOfType(
						ProposedPromiseDrop.class));
		for (ProposedPromiseDrop id : proposedPromiseDrops) {
			if (id != null)
				f_contents.add(id);
		}
		Collections.sort(f_contents, new Comparator<ProposedPromiseDrop>() {
			public int compare(ProposedPromiseDrop d1, ProposedPromiseDrop d2) {
				return d1.toString().compareTo(d2.toString());
			}
		});
	}

	public Object[] getElements(Object inputElement) {
		return f_contents.toArray();
	}

	public int numColumns() {
		return COLUMN_LABELS.length;
	}

	public String getColumnTitle(int column) {
		return COLUMN_LABELS[column];
	}

	public int getColumnWeight(int column) {
		switch (column) {
		case 0:
			return 70;
		case 1:
			return 20;
		case 2:
			return 10;
		}
		return 10;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
		} else {
			return null;
		}
	}

	private String getResource(ProposedPromiseDrop d) {
		ISrcRef ref = d.getSrcRef();
		if (ref == null) {
			return "";
		}
		Object o = ref.getEnclosingFile();
		if (o instanceof IFile) {
			IFile f = (IFile) o;
			return f.getFullPath().toPortableString();
		} else if (o instanceof String) {
			String name = (String) o;
			if (name.indexOf('/') < 0) {
				// probably not a file
				return name;
			}
			IFile f = EclipseUtility.resolveIFile(name);
			return f.getFullPath().toPortableString();
		} else if (o != null) {
			return o.toString();
		}
		return "";
	}

	private int getLine(ProposedPromiseDrop d) {
		ISrcRef ref = d.getSrcRef();
		if (ref != null) {
			return ref.getLineNumber();
		}
		return Integer.MAX_VALUE;
	}

	public String getColumnText(Object element, int columnIndex) {
		ProposedPromiseDrop d = (ProposedPromiseDrop) element;
		switch (columnIndex) {
		case 0:
			return d.getJavaAnnotationNoAtSign();
		case 1:
			return getResource(d);
		case 2:
			int line = getLine(d);
			if (line > 0 && line < Integer.MAX_VALUE)
				return Integer.toString(line);
			else
				return "(binary)";
		}
		return "";
	}

	public void dispose() {
		f_contents.clear();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// throw new UnsupportedOperationException();
	}

	public boolean isLabelProperty(Object element, String property) {
		throw new UnsupportedOperationException();
	}

	public void addListener(ILabelProviderListener listener) {
		// throw new UnsupportedOperationException();
	}

	public void removeListener(ILabelProviderListener listener) {
		// throw new UnsupportedOperationException();
	}
}