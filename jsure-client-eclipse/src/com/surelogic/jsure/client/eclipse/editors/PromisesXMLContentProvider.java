package com.surelogic.jsure.client.eclipse.editors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;

import com.surelogic.common.FileUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor.FileStatus;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.*;

import edu.cmu.cs.fluid.util.ArrayUtil;
import edu.cmu.cs.fluid.util.Pair;

public class PromisesXMLContentProvider extends AbstractContentProvider implements ITreeContentProvider {	
	private static final boolean saveDiff = true;
	
	private Color colorForModified;
	private Color colorForBadSyntax;
	
	URI location;
	FileStatus status = null;
	PackageElement pkg;
	Object[] roots;
	final boolean hideEmpty;
	String fluidXML = "";
	URI localXML = null;	
	
	protected PromisesXMLContentProvider(boolean hideEmpty) {
		this.hideEmpty = hideEmpty;
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
		if (!pkg.isDirty()) {
			return;
		}
		try {
			final File root = JSurePreferencesUtility.getJSureXMLDirectory();
			File f = new File(root, location.toASCIIString());
			File dir = f.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			PromisesXMLWriter w = new PromisesXMLWriter(f);
			if (saveDiff) {
				PackageElement p = PromisesXMLMerge.diff(pkg);
				w.write(p);
			} else {
				w.write(pkg);
			}
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
		if (colorForModified == null && viewer != null) {
			colorForModified = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_BLUE);
			colorForBadSyntax = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_RED);
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
				roots = new Object[1];
				try {
					URL url = location.toURL();
					InputStream in = url.openStream();
					status = FileStatus.READ_ONLY;
					roots[0] = pkg = PromisesXMLReader.loadRaw(in);
					fluidXML = FileUtility.getStreamContentsAsString(location.toString(), url.openStream());
					localXML = null;
				} catch(IllegalArgumentException e) {
					final String path = location.toASCIIString();
					Pair<File,File> rv = PromisesXMLEditor.findPromisesXML(path);
					roots[0] = pkg = PromisesXMLReader.load(path, rv.first(), rv.second());			

					if (pkg == null) {
						// No XML at all, so we have to create something
						final int lastSlash = path.lastIndexOf('/');
						if (lastSlash >= 0) {
							String p = path.substring(0, lastSlash).replace('/', '.');
							String name = path.substring(lastSlash+1, path.length() - TestXMLParserConstants.SUFFIX.length());
							System.out.println("Making AST for "+p+'.'+name);
							roots[0] = pkg = PromisesXMLBuilder.makeModel(p, name);
						}
					} else {				
						if (PromisesXMLBuilder.updateElements(pkg)) {
							System.out.println("Added elements to "+location);
						}
					}
					if (rv.first() != null) {
						status = rv.second() == null ? FileStatus.FLUID : FileStatus.LOCAL;
						fluidXML = FileUtility.getFileContentsAsStringOrDefaultValue(rv.first(), "");
					} else {
						status = FileStatus.LOCAL;
						fluidXML = "";
					}
					if (rv.second() != null) {
						localXML = rv.second().toURI();
					} else {
						localXML = null;
					}
				}
			} catch (Exception e) {
				pkg = null;
				roots = ArrayUtil.empty;
				fluidXML = "";
				localXML = null;
			}
		}
	}
	
	public IDocument getFluidDocument() {
		return new Document(fluidXML);
	}
	
	public IEditorInput getLocalInput() {
		if (localXML == null) {
			return null;
		}
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(localXML);
		return new FileStoreEditorInput(fileStore); 
 	}
	
	@Override
	public Object[] getChildren(Object element) {
		Object[] children = ((IJavaElement) element).getChildren();
		if (hideEmpty) {
			final List<IJavaElement> nonLeaf = new ArrayList<IJavaElement>();
			for(Object o : children) {
				IJavaElement c = (IJavaElement) o;
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
		String key = e.getImageKey();
		if (key != null) {
			Image i = SLImages.getImage(key);
			if (i != null) {
				return i;
			}
		}
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
	}

	@Override
	public Color getForeground(Object element) {
		if (element instanceof IJavaElement) {
			IJavaElement e = (IJavaElement) element;
			if (e.isBad()) {
				return colorForBadSyntax;
			}
			if (e.isModified()) {
				return colorForModified;
			}			
		}
		return null;
	}
}