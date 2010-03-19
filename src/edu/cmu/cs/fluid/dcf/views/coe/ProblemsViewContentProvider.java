package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.eclipse.SLImages;

import edu.cmu.cs.fluid.eclipse.ui.ITableContentProvider;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.Sea;

public final class ProblemsViewContentProvider implements ITableContentProvider {

	public static final String[] COLUMN_LABELS = { "Description", "Resource",
			"Line" };

	private final List<PromiseWarningDrop> f_contents = new ArrayList<PromiseWarningDrop>();

	public void build() {
		f_contents.clear();

		Set<? extends PromiseWarningDrop> promiseWarningDrops = Sea
				.getDefault().getDropsOfType(PromiseWarningDrop.class);
		for (PromiseWarningDrop id : promiseWarningDrops) {
			// PromiseWarningDrop id = (PromiseWarningDrop) j.next();
			// only show info drops at the main level if they are not
			// attached
			// to a promise drop or a result drop
			f_contents.add(id);
		}
		Collections.sort(f_contents, new Comparator<PromiseWarningDrop>() {
			public int compare(PromiseWarningDrop d1, PromiseWarningDrop d2) {
				String res1 = getResource(d1);
				String res2 = getResource(d2);
				int rv = 0;
				// Make those with a real path go first
				if (res1.startsWith("/")) {
					rv = -1;
				}
				if (res2.startsWith("/")) {
					rv++;
				}
				if (rv == 0) {
					// Make those with no path go last
					if (res1.length() == 0) {
						rv = 1;
					}
					if (res2.length() == 0) {
						rv--;
					}
				}
				if (rv == 0) {
					rv = res1.compareTo(res2);
				}
				if (rv == 0) {
					rv = getLine(d1) - getLine(d2);
				}
				if (rv == 0) {
					rv = d1.getMessage().compareTo(d2.getMessage());
				}
				return rv;
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
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR);
		} else {
			return null;
		}
	}

	private String getResource(PromiseWarningDrop d) {
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
			if (f == null) {
				return "";
			}
			return f.getFullPath().toPortableString();
		} else if (o != null) {
			return o.toString();
		}
		return "";
	}

	private int getLine(PromiseWarningDrop d) {
		ISrcRef ref = d.getSrcRef();
		if (ref != null) {
			return ref.getLineNumber();
		}
		return Integer.MAX_VALUE;
	}

	public String getColumnText(Object element, int columnIndex) {
		PromiseWarningDrop d = (PromiseWarningDrop) element;
		switch (columnIndex) {
		case 0:
			return d.getMessage();
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