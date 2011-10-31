package com.surelogic.jsure.client.eclipse.editors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor.FileStatus;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.CommentElement;
import com.surelogic.xml.IJavaElement;
import com.surelogic.xml.IMergeableElement;
import com.surelogic.xml.PackageElement;
import com.surelogic.xml.PromisesXMLReader;
import com.surelogic.xml.PromisesXMLWriter;

import edu.cmu.cs.fluid.util.ArrayUtil;
import edu.cmu.cs.fluid.util.Pair;

public class PromisesXMLContentProvider extends AbstractContentProvider implements ITreeContentProvider {	
	private Color colorRed;
	
	URI location;
	FileStatus status = null;
	PackageElement pkg;
	Object[] roots;
	final boolean hideEmpty;
	final boolean alsoHideComments;
	
	protected PromisesXMLContentProvider(boolean hideEmpty, boolean alsoHideComments) {
		this.hideEmpty = hideEmpty;
		this.alsoHideComments = alsoHideComments;
	}
	
	boolean isMutable() {
		return status == FileStatus.FLUID || status == FileStatus.LOCAL;
	}
	
	void markAsReadOnly() {
		status = FileStatus.READ_ONLY;
	}
	
	URI getInput() {
		return location;
	}
	
	void save(IProgressMonitor monitor) {
		if (status == null || status == FileStatus.READ_ONLY) {
			return;
		}
		try {
			final File root = JSurePreferencesUtility.getJSureXMLDirectory();
			File f = new File(root, location.toASCIIString());
			PromisesXMLWriter w = new PromisesXMLWriter(f);
			w.write(pkg);
			pkg.markAsClean();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	boolean isDirty() {
		if (pkg == null) {
			return false;
		}
		return pkg.isDirty();
	}

	@Override
	public void inputChanged(final Viewer viewer, Object oldInput, Object newInput) {
		if (colorRed == null && viewer != null) {
			colorRed = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_RED);
		}
		
		if (newInput != location) {
			if (newInput instanceof URI) {
				location = (URI) newInput;				
				System.out.println("Editor got "+location);
				build();
			}
		} else {
			System.out.println("Ignoring duplicate input");
			/*
			IType t = JDTUtility.findIType(null, pkg.getName(), pkg.getClassElement().getName());
			contents.setInput(t);
            */
		}
		if (viewer instanceof TreeViewer) {
			final TreeViewer tree = (TreeViewer) viewer;
			viewer.getControl().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					tree.expandAll();
				}					
			});

		}
	}

	private void build() {
		if (location != null) {
			try {
				InputStream in = null;					
				try {
					in = location.toURL().openStream();
					status = FileStatus.READ_ONLY;
				} catch(IllegalArgumentException e) {
					final String path = location.toASCIIString();
					Pair<File,FileStatus> rv = PromisesXMLEditor.findPromisesXML(path);
					if (rv != null) {
						in = new FileInputStream(rv.first());
						status = rv.second();
					} else {
						throw e;
					}
				}
				roots = new Object[1];
				roots[0] = pkg = getXML(in);
			} catch (Exception e) {
				pkg = null;
				roots = ArrayUtil.empty;
			}
		}
	}
	
	public static PackageElement getXML(InputStream in ) throws Exception {
		PromisesXMLReader r = new PromisesXMLReader();
		r.read(in);
		if (true) {
			if (PromisesXMLBuilder.updateElements(r.getPackage())) {
				System.out.println("Added elements");
			}
		}
		return r.getPackage();
	}
	
	@Override
	public Object[] getChildren(Object element) {
		Object[] children = ((IJavaElement) element).getChildren();
		if (hideEmpty) {
			final List<IJavaElement> nonLeaf = new ArrayList<IJavaElement>();
			for(Object o : children) {
				IJavaElement c = (IJavaElement) o;
				if (alsoHideComments && c instanceof CommentElement) {
					continue;
				}
				if (c.hasChildren() || c instanceof IMergeableElement) {
					nonLeaf.add(c);
				}
			}
			return nonLeaf.toArray();
		}
		return children;
	}

	@Override
	public Object getParent(Object element) {
		return ((IJavaElement) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return ((IJavaElement) element).hasChildren();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return roots;
	}
	
	@Override
	public String getText(Object element) {
		return ((IJavaElement) element).getLabel();
	}

	@Override
	public Image getImage(Object element) {
		IJavaElement e = (IJavaElement) element;
		return SLImages.getImage(e.getImageKey());
	}
	
	@Override
	public Color getForeground(Object element) {
		if (element instanceof IJavaElement) {
			IJavaElement e = (IJavaElement) element;
			if (e.isModified()) {
				return colorRed;
			} 
		}
		return null;
	}
}