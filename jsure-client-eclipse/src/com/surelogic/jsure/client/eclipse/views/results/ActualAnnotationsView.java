package com.surelogic.jsure.client.eclipse.views.results;

import java.util.*;

import org.apache.commons.collections15.*;
import org.apache.commons.collections15.multimap.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.*;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;
import com.surelogic.persistence.JavaIdentifier;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class ActualAnnotationsView extends AbstractScanTreeView<ActualAnnotationsView.ITypeElement> {		
	public ActualAnnotationsView() {
		super(SWT.NONE, ActualAnnotationsView.ITypeElement.class, new ActualAnnotationsContentProvider());
	}
	
	@Override
	protected void makeActions() {
		// TODO Auto-generated method stub
	}
	
	private static final Package[] NO_ROOTS = new Package[0];
	
	static class ActualAnnotationsContentProvider implements IJSureTreeContentProvider {
		private Package[] roots = NO_ROOTS;
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public String build() {						
			/*
			final Pattern p;
			if (fromSource) {
				p = Pattern.compile(focusTypeName);
			} else {
				p = Pattern.compile(focusTypeName+"(\\$.*)*\\.class");
			}	
			*/		
			// Organize by package
			final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
			final MultiMap<String,IDropInfo> pkgToDrop = new MultiHashMap<String, IDropInfo>();
			for(IDropInfo d : info.getDropsOfType(PromiseDrop.class)) {
				final ISrcRef sr = d.getSrcRef();
				if (sr == null) {
					continue;
				}
				pkgToDrop.put(sr.getPackage(), d);
			}
			// Organize by type
			roots = new Package[pkgToDrop.size()];
			int i=0;
			for(Map.Entry<String, Collection<IDropInfo>> e : pkgToDrop.entrySet()) {
				final MultiMap<String,IDropInfo> cuToDrop = new MultiHashMap<String, IDropInfo>();
				for(IDropInfo d : e.getValue()) {
					cuToDrop.put(d.getSrcRef().getCUName(), d);
				}
				roots[i] = new Package(e.getKey(), cuToDrop);
				i++;
			}
			Arrays.sort(roots);
			/*
			// Organize by id
			final MultiMap<String,IDropInfo> idToDrop = new MultiHashMap<String, IDropInfo>();
			for(IDropInfo d : info.getDropsOfType(PromiseDrop.class)) {
				final ISrcRef sr = d.getSrcRef();
				if (sr == null) {
					continue;
				}
				if (matched(p, sr)) {								
					String id = sr.getJavaId();
					idToDrop.put(id, d);
				}
			}
			List<ITypeElement> decls = new ArrayList<ITypeElement>();
			for(Map.Entry<String, Collection<IDropInfo>> e : idToDrop.entrySet()) {
				String label = JavaIdentifier.extractDecl(focusTypeName, e.getKey());
				decls.add(new Decl(label, e.getValue()));
			}
			roots[0] = new Type(focusPkgName+"."+focusTypeName, decls);
			*/
			return info.findProjectsLabel();
		}

		/*
		private boolean matched(final Pattern p, final ISrcRef sr) {
			if (!focusPkgName.equals(sr.getPackage())) {
				return false;
			}
			final String cu = sr.getCUName();
			Matcher m = p.matcher(cu);
			return m.matches();
		}
		*/

		@Override
		public Object[] getChildren(Object o) {
			return ((ITypeElement) o).getChildren();
		}

		@Override
		public Object getParent(Object o) {
			return ((ITypeElement) o).getParent();
		}

		@Override
		public boolean hasChildren(Object o) {
			return ((ITypeElement) o).getChildren().length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return roots;
		}

		@Override
		public Image getImage(Object o) {
			return ((ITypeElement) o).getImage();
		}

		@Override
		public String getText(Object o) {
			return ((ITypeElement) o).getLabel();
		}

		@Override
		public void dispose() {
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}		
	}
	
	interface ITypeElement extends Comparable<ITypeElement> {
		String getLabel();	
		Image getImage();
		ITypeElement getParent();
		ITypeElement[] getChildren();
	}
	
	private static final ITypeElement[] NO_CHILDREN = new ITypeElement[0];
	
	static abstract class AbstractElement implements ITypeElement {
		private final String label;
		private ITypeElement parent;
		private final ITypeElement[] children;
		
		AbstractElement(String l, int size) {
			label = l;
			children = size == 0 ? NO_CHILDREN : new ITypeElement[size];
		}
		
		@Override
		public final String getLabel() {
			return label;
		}

		@Override
		public final ITypeElement getParent() {
			return parent;
		}
		
		@Override
		public final ITypeElement[] getChildren() {
			return children;
		}
		
	    public int compareTo(ITypeElement o) {
	    	int rv = 0;
	    	if (this instanceof Anno) {
	    		rv--;
	    	}
	    	if (o instanceof Anno) {
	    		rv++;
	    	}
	    	if (rv == 0) {
	    		return getLabel().compareTo(o.getLabel());
	    	}
	    	return rv;
	    }
	}
	
	static class Package extends AbstractElement {
		Package(String qname, MultiMap<String,IDropInfo> cuToDrop) {
			super(qname, cuToDrop.size());

			// Init types
			int i=0;
			for(Map.Entry<String, Collection<IDropInfo>> e : cuToDrop.entrySet()) {
				final MultiMap<String,IDropInfo> idToDrop = new MultiHashMap<String, IDropInfo>();
				String name = computeTypeName(e.getKey());
				for(IDropInfo d : e.getValue()) {
					String label = JavaIdentifier.extractDecl(name, d.getSrcRef().getJavaId());
					idToDrop.put(label, d);
				}
				getChildren()[i] = new Type(name, idToDrop);
				i++;
			}	
			Arrays.sort(getChildren());
		}
		
		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_PACKAGE);
		}		
	}
	
	static String computeTypeName(String cuName) {
		if (cuName.endsWith(".class")) {
			return cuName.substring(0, cuName.length()-6).replace('$', '.');
		}
		return cuName;
	}
	
	static int computeTypeChildren(String name, MultiMap<String,IDropInfo> idToDrop) {
		Collection<IDropInfo> onType = idToDrop.get(name);
		if (onType == null) {
			return idToDrop.size();
		}
		return onType.size() + idToDrop.size() - 1;
	}
	
	static class Type extends AbstractElement {
		Type(String name, MultiMap<String,IDropInfo> idToDrop) {
			super(name, computeTypeChildren(name, idToDrop));
						
			// Init decls					
			int i=0;
			Collection<IDropInfo> onType = idToDrop.remove(name);
			if (onType != null) {
				for(IDropInfo d : onType) {
					getChildren()[i] = new Anno(d);
					i++;
				}
			}			
			for(Map.Entry<String, Collection<IDropInfo>> e : idToDrop.entrySet()) {
				getChildren()[i] = new Decl(e.getKey(), e.getValue());
				i++;
			}	
			Arrays.sort(getChildren());
		}
		
		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_CLASS);
		}		
	}
	
	static class Decl extends AbstractElement {				
		Decl(String id, Collection<IDropInfo> drops) {
			super(id, drops.size());

			// Sort by message
			int i=0;
			for(IDropInfo d : drops) {
				getChildren()[i] = new Anno(d);
				i++;
			}
		}
		
		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_GREEN_DOT);
		}		
	}
	
	static class Anno extends AbstractElement {
		private final IDropInfo drop;
		
		public Anno(IDropInfo d) {
			super(d.getMessage(), 0);
			drop = d;
		}

		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION);
		}
		
	}
}
