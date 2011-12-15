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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;

import com.surelogic.common.AnnotationConstants;
import com.surelogic.common.FileUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.views.AbstractContentProvider;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor.FileStatus;
import com.surelogic.jsure.client.eclipse.views.AbstractJSureView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesXMLBuilder;
import com.surelogic.xml.*;

import edu.cmu.cs.fluid.util.ArrayUtil;
import edu.cmu.cs.fluid.util.Pair;

public class PromisesXMLContentProvider extends AbstractContentProvider implements ITreeContentProvider, ITableLabelProvider {	
	public static final String DIRTY_PREFIX = "> "; 
	static final boolean saveDiff = true;
	
	//private Color colorForModified;
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
		try {
			final File root = JSurePreferencesUtility.getJSureXMLDirectory();
			File f = new File(root, location.toASCIIString());
			/* Maybe saving because the editor is "dirty"
			 * so we can't do the below anymore
			if (!pkg.isDirty()) {
				// Try to delete any diffs
				if (f.exists()) {
					f.delete();
				}
				return;
			}
			*/
			File dir = f.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}
			PromisesXMLWriter w = new PromisesXMLWriter(f);
			if (saveDiff) {
				PackageElement p = PromisesXMLMerge.diff(pkg);
				if (p != null) {
					w.write(p);
				} else {
					// No diffs, so make sure there's no empty file
					f.delete();
				}
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
		if (colorForBadSyntax == null && viewer != null) {
			//colorForModified = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_BLUE);
			colorForBadSyntax = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_RED);
		}
		
		if (newInput != location) {
			if (newInput instanceof URI) {
				location = (URI) newInput;				
				//System.out.println("Editor got "+location);
				build();
			}
		} else {
			//System.out.println("Ignoring duplicate input");
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

	public String build() {
		return build(false);
	}
	
	String build(boolean ignoreDiffs) { 
		if (location != null) {
			try {
				roots = new Object[1];
				try {
					URL url = location.toURL();
					InputStream in = url.openStream();
					status = FileStatus.READ_ONLY;
					roots[0] = pkg = PromisesXMLReader.loadRaw(in);
					fluidXML = FileUtility.getStreamContentsAsString(location.toString(), url.openStream());
					
					final File dummy = File.createTempFile("dummy", TestXMLParserConstants.SUFFIX);
					dummy.deleteOnExit();
					localXML = dummy.toURI();
				} catch(IllegalArgumentException e) {
					final String path = location.toASCIIString();
					Pair<File,File> rv = PromisesXMLParser.findPromisesXML(path);
					roots[0] = pkg = PromisesXMLReader.load(path, rv.first(), ignoreDiffs ? null : rv.second());			

					if (pkg == null) {
						// No XML at all, so we have to create something
						final int lastSlash = path.lastIndexOf('/');
						if (lastSlash >= 0) {
							String p = path.substring(0, lastSlash).replace('/', '.');
							String name = path.substring(lastSlash+1, path.length() - TestXMLParserConstants.SUFFIX.length());
							if (AnnotationConstants.PACKAGE_INFO.equals(name)) {
								//System.out.println("Making AST for "+p);
								roots[0] = pkg = PromisesXMLBuilder.makePackageModel(p);
							} else {
								//System.out.println("Making AST for "+p+'.'+name);
								roots[0] = pkg = PromisesXMLBuilder.makeModel(p, name);
							}
						}
					} else {				
						if (PromisesXMLBuilder.updateElements(pkg)) {
							//System.out.println("Added elements to "+location);
						}
					}
					if (rv.first().isFile()) {
						status = rv.second().isFile() ? FileStatus.LOCAL : FileStatus.FLUID;
						fluidXML = FileUtility.getFileContentsAsStringOrDefaultValue(rv.first(), "");
					} else {
						status = FileStatus.LOCAL;
						fluidXML = "";
					}
					//if (rv.second() != null) {
						localXML = rv.second().toURI();
					/*
				    } else {
						localXML = null;
					}
					*/
				}
			} catch (Exception e) {
				pkg = null;
				roots = ArrayUtil.empty;
				fluidXML = "";
				localXML = null;
			}
		}
		return null;
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
				if (c instanceof IMergeableElement || hasChildren(c)) {
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
		boolean rv = ((IJavaElement) element).hasChildren();
		if (hideEmpty && rv) {
			Object[] children = getChildren(element);
			return children.length > 0;
		}
		return rv;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return roots;
	}
	
	@Override
	public String getText(Object element) {
		IJavaElement e = (IJavaElement) element;
		if (e.isModified()) {
			return DIRTY_PREFIX+e.getLabel();
		}
		return e.getLabel();
	}

	@Override
	public Image getImage(Object element) {
		ImageDescriptor desc = getImageDescriptor(element);
		boolean isBad = false;
		if (element instanceof AnnotationElement) {
			IJavaElement e = (IJavaElement) element;
			isBad = e.isBad();
		}
		return AbstractJSureView.getCachedImage(desc, isBad);
	}
	
	public ImageDescriptor getImageDescriptor(Object element) {
		IJavaElement e = (IJavaElement) element;
		String key = e.getImageKey();
		if (key != null) {
			ImageDescriptor i = SLImages.getImageDescriptor(key);
			if (i != null) {
				return i;
			}
		}
		return JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_PUBLIC);
	}

	@Override
	public Image getColumnImage(Object element, int i) {
		if (i == 0) {
			return getImage(element);
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int i) {
		if (i == 0) {
			return getText(element);
		}
		return null;
	}
	
	@Override
	public Color getForeground(Object element) {
		if (element instanceof AnnotationElement) {
			IJavaElement e = (IJavaElement) element;
			if (e.isBad()) {
				return colorForBadSyntax;
			}
			/*
			if (e.isModified()) {
				return colorForModified;
			}			
			*/
		}
		return null;
	}

	void deleteAllChanges() {
		/*
    	File local = new File(localXML);
    	local.delete();
    	*/
    	deleteUnsavedChanges(false);
		build(true);
		PromisesXMLReader.refreshAll();
	}

	void deleteUnsavedChanges() {
		deleteUnsavedChanges(true);
	}
	
	private void deleteUnsavedChanges(boolean refreshAll) {
		final String path = location.toASCIIString();
		PromisesXMLReader.clear(path);
		if (refreshAll) {
			PromisesXMLReader.refreshAll();
		}
	}
}
