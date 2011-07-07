package com.surelogic.jsure.client.eclipse.views.results;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.jsure.client.eclipse.views.IResultsTableContentProvider;
import com.surelogic.scans.ScanStatus;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

abstract class AbstractResultsTableContentProvider<T extends IDropInfo> implements IResultsTableContentProvider{
	private final List<T> f_contents = new ArrayList<T>();
	private final String[] f_labels;
	
	protected final Comparator<T> sortAsString = new Comparator<T>() {
		public int compare(T d1, T d2) {
			return d1.toString().compareTo(d2.toString());
		}
	};
	
	protected final Comparator<T> sortByLocation = new Comparator<T>() {
		public int compare(T d1, T d2) {
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
	};
	
	AbstractResultsTableContentProvider(String[] labels) {
		f_labels = labels;
	}
	
	AbstractResultsTableContentProvider(String mainLabel) {
		this(new String[] { mainLabel, "Resource", "Line" });
	}
	
	public final String[] getColumnLabels() {
		return f_labels;
	}
	
	public final int numColumns() {
		return f_labels.length;
	}

	public final String getColumnTitle(int column) {
		return f_labels[column];
	}
	
	public boolean isIntSortedColumn(int colIdx) {
		return colIdx == 2;
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
	
	public final Object[] getElements(Object inputElement) {
		return f_contents.toArray();
	}
	
	public String build(ScanStatus status) {
		f_contents.clear();
		return getAndSortResults(status, f_contents);
	}
	
	protected abstract String getAndSortResults(ScanStatus status, List<T> contents);
	
	protected String getMainColumnText(T d) {
		return d.getMessage();
	}
	
	@SuppressWarnings("unchecked")
	public String getColumnText(Object element, int columnIndex) {
		T d = (T) element;
		switch (columnIndex) {
		case 0:
			return getMainColumnText(d);
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
	
	protected final String getResource(T d) {
		ISrcRef ref = d.getSrcRef();
		if (ref == null) {
			return "";
		}
		if (ref.getEnclosingURI() != null) {
			String path = ref.getRelativePath();
			if (path != null) {
				return path;
			}
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
			if (name.endsWith(".class")) {
				return name;
			}
			final int bang = name.lastIndexOf('!');
			if (bang >= 0) {
				return name.substring(bang+1);
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
	
	protected final int getLine(T d) {
		ISrcRef ref = d.getSrcRef();
		if (ref != null) {
			return ref.getLineNumber();
		}
		return Integer.MAX_VALUE;
	}
	
	public final void dispose() {
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
