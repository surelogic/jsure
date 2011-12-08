package com.surelogic.jsure.client.eclipse.views.results;

import java.lang.reflect.Array;
import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.IJSureTableTreeContentProvider;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.*;

public final class ProposedPromiseContentProvider 
extends	AbstractResultsTableContentProvider<IProposedPromiseDropInfo> 
implements IJSureTableTreeContentProvider
{
	private static final Package[] noPackages = new Package[0];
	
	private boolean asTree;
	private Package[] packages = noPackages;
	
	ProposedPromiseContentProvider(boolean asTree) {
		super("Proposed Promise");
		setAsTree(asTree);
	}

	public void setAsTree(boolean asTree) {
		this.asTree = asTree;		
	}

	public boolean showAsTree() {
		return asTree;
	}
	
	protected String getAndSortResults(List<IProposedPromiseDropInfo> contents) {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		if (info == null) {
			packages = noPackages;
			return null;
		} 
		List<IProposedPromiseDropInfo> proposedPromiseDrops = ProposedPromiseDrop
				.filterOutDuplicates(info
						.<IProposedPromiseDropInfo, ProposedPromiseDrop> getDropsOfType(ProposedPromiseDrop.class));		
		for (IProposedPromiseDropInfo id : proposedPromiseDrops) {
			if (id != null && id.getSrcRef() != null) {
				// TODO omit annotations on implicitly created methods in enums?
				/*
				 * if (id.getSrcRef() == null) {
				 * System.out.println("Got proposal on "
				 * +DebugUnparser.toString(id.getNode())+" in "+
				 * JavaNames.getFullTypeName
				 * (VisitUtil.getEnclosingType(id.getNode()))); }
				 */
				contents.add(id);
			}
		}
		packages = Package.factory.organize(proposedPromiseDrops);
		Arrays.sort(packages);		
		Collections.sort(contents, sortByProposal);
		return info.getLabel();
	}

	private static final Comparator<IProposedPromiseDropInfo> sortByProposal = new Comparator<IProposedPromiseDropInfo>() {
		public int compare(IProposedPromiseDropInfo d1, IProposedPromiseDropInfo d2) {
			return d1.getJavaAnnotation().compareTo(d2.getJavaAnnotation());
		}
	};
	
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
		} else {
			return null;
		}
	}

	@Override
	protected String getMainColumnText(IProposedPromiseDropInfo d) {
		return d.getJavaAnnotation().substring(1);
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof IProposedPromiseDropInfo) {
			IProposedPromiseDropInfo d = (IProposedPromiseDropInfo) element;		
			return getMainColumnText(d);
		}
		return element.toString();
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof Treeable) {
			return ((Treeable) element).getImage();
		}
		if (element instanceof IProposedPromiseDropInfo) {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
		}
		return null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (asTree) {
			return packages;
		} else {
			return super.getElements(inputElement);
		}
	}
	
	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof Treeable) {
			return ((Treeable) element).getChildren();
		}
		return noPackages;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Treeable) {
			return ((Treeable) element).hasChildren();
		}
		return false;
	}
	
	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}
	
	interface Treeable extends Comparable<Treeable> {
		Image getImage();
		boolean hasChildren();
		Object[] getChildren();
	}
	
	static abstract class AbstractTreeable<T> implements Treeable {
		final String id;
		final T[] children;
		
		AbstractTreeable(String id, T[] c) {
			this(id, c, true);
		}
		
		AbstractTreeable(String id, T[] c, boolean sort) {
			this.id = id;
			children = c;
			if (sort) {
				Arrays.sort(children);
			}
		}

		@Override
		public final String toString() {
			return id;
		}
		
		@Override
		public int compareTo(Treeable o) {
			return toString().compareTo(o.toString());
		}
		
		public boolean hasChildren() {
			return children != null && children.length > 0;
		}
		
		public Object[] getChildren() {
			return children;
		}
	}
	
	static abstract class Factory<K,T> {
		@SuppressWarnings("unchecked")
		T[] organize(Collection<IProposedPromiseDropInfo> drops) {
			MultiMap<K,IProposedPromiseDropInfo> map = new MultiHashMap<K, IProposedPromiseDropInfo>();
			for(IProposedPromiseDropInfo d : drops) {
				K key = getKey(d);
				if (key == null) {
					continue;
				}
 				map.put(key, d);
			}		
			List<T> things = new ArrayList<T>();
			for(Map.Entry<K, Collection<IProposedPromiseDropInfo>> e : map.entrySet()) {
				things.add(make(e.getKey(), e.getValue()));
			}
			T first = things.get(0);			
			return things.toArray((T[]) Array.newInstance(first.getClass(), things.size()));
		}
		abstract K getKey(IProposedPromiseDropInfo d);
		abstract T make(K key, Collection<IProposedPromiseDropInfo> drops);
	}

	static class Package extends AbstractTreeable<Type> {		
		Package(String p, Collection<IProposedPromiseDropInfo> drops) {
			super(p, Type.factory.organize(drops));
		}
		
		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_PACKAGE);
		}
		
		static Factory<String,Package> factory = new Factory<String, Package>() {			
			@Override
			String getKey(IProposedPromiseDropInfo d) {
				return d.getSrcRef() == null ? null : d.getSrcRef().getPackage();
			}
			@Override
			Package make(String key, Collection<IProposedPromiseDropInfo> drops) {
				return new Package(key, drops);
			}			
		};
		/*
		static Package[] make(List<IProposedPromiseDropInfo> drops) {
			MultiMap<String,IProposedPromiseDropInfo> map = new MultiHashMap<String, IProposedPromiseDropInfo>();
			for(IProposedPromiseDropInfo d : drops) {
				if (d.getSrcRef() == null) {
					continue;
				}
				map.put(d.getSrcRef().getPackage(), d);
			}		
			List<Package> pkgs = new ArrayList<Package>();
			for(Map.Entry<String, Collection<IProposedPromiseDropInfo>> e : map.entrySet()) {
				pkgs.add(new Package(e.getKey(), e.getValue()));
			}
			return pkgs.toArray(noPackages);
		}
		*/
	}
	
	static abstract class Member extends AbstractTreeable<IProposedPromiseDropInfo> {
		Member(String id, IProposedPromiseDropInfo[] c, boolean sort) {
			super(id, c, sort);
		}
	}
	
	static class Type extends Member {
		Type(String t, Collection<IProposedPromiseDropInfo> drops) {
			super(t, /*TODO*/drops.toArray(new IProposedPromiseDropInfo[drops.size()]), false);
		}		
		
		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_CLASS);
		}
		
		/*
		static CU[] make(Collection<IProposedPromiseDropInfo> drops) {
			MultiMap<String,IProposedPromiseDropInfo> map = new MultiHashMap<String, IProposedPromiseDropInfo>();
			for(IProposedPromiseDropInfo d : drops) {
				map.put(d.getSrcRef().getCUName(), d);
			}		
			List<CU> cus = new ArrayList<CU>();
			for(Map.Entry<String, Collection<IProposedPromiseDropInfo>> e : map.entrySet()) {
				cus.add(new CU(e.getKey(), e.getValue()));
			}
			return cus.toArray(new CU[cus.size()]);
		}
		*/
		static Factory<String,Type> factory = new Factory<String, Type>() {			
			@Override
			String getKey(IProposedPromiseDropInfo d) {
				return d.getSrcRef() == null ? null : d.getSrcRef().getCUName();
			}
			@Override
			Type make(String key, Collection<IProposedPromiseDropInfo> drops) {
				return new Type(key, drops);
			}			
		};
	}
}