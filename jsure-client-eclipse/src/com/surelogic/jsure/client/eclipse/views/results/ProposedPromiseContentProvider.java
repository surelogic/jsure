package com.surelogic.jsure.client.eclipse.views.results;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.refactor.Field;
import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.common.refactor.Method;
import com.surelogic.common.refactor.MethodParameter;
import com.surelogic.common.refactor.TypeContext;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.IJSureTreeContentProvider;
import com.surelogic.jsure.client.eclipse.views.IResultsTableContentProvider;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;

public final class ProposedPromiseContentProvider extends
		AbstractResultsTableContentProvider<IProposedPromiseDropInfo> implements
		IResultsTableContentProvider, IJSureTreeContentProvider {
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
		final boolean filter = 
			EclipseUtility.getBooleanPreference(JSurePreferencesUtility.PROPOSED_PROMISES_SHOW_ABDUCTIVE_ONLY);
		if (filter) {
			final Iterator<IProposedPromiseDropInfo> it = proposedPromiseDrops.iterator();
			while (it.hasNext()) {
				IProposedPromiseDropInfo p = it.next();
				if (p.getOrigin() != Origin.PROMISE) {
					it.remove();
				}
			}
		}		
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
		if (proposedPromiseDrops.size() == 0) {
			packages = noPackages;
		} else {
			packages = Package.factory.organize(proposedPromiseDrops);
		}
		Arrays.sort(packages);
		Collections.sort(contents, sortByProposal);
		return info.getLabel();
	}

	private static final Comparator<IProposedPromiseDropInfo> sortByProposal = new Comparator<IProposedPromiseDropInfo>() {
		public int compare(IProposedPromiseDropInfo d1,
				IProposedPromiseDropInfo d2) {
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

	static abstract class Factory<K, T> {
		@SuppressWarnings("unchecked")
		T[] organize(Collection<IProposedPromiseDropInfo> drops) {
			MultiMap<K, IProposedPromiseDropInfo> map = new MultiHashMap<K, IProposedPromiseDropInfo>();
			for (IProposedPromiseDropInfo d : drops) {
				K key = getKey(d);
				if (key == null) {
					continue;
				}
				map.put(key, d);
			}
			List<T> things = new ArrayList<T>();
			for (Map.Entry<K, Collection<IProposedPromiseDropInfo>> e : map
					.entrySet()) {
				things.add(make(e.getKey(), e.getValue()));
			}
			if (things.size() == 0) {
				return null;
			}
			T first = things.get(0);
			return things.toArray((T[]) Array.newInstance(first.getClass(),
					things.size()));
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

		static final Factory<String, Package> factory = new Factory<String, Package>() {
			@Override
			String getKey(IProposedPromiseDropInfo d) {
				return d.getSrcRef() == null ? null : d.getSrcRef()
						.getPackage();
			}

			@Override
			Package make(String key, Collection<IProposedPromiseDropInfo> drops) {
				return new Package(key, drops);
			}
		};
	}

	/**
	 * A temporary structure to collect data
	 * 
	 * @author Edwin
	 */
	static class Decl implements Comparable<Decl> {
		final String id;
		final Map<String, Decl> types = new HashMap<String, Decl>();
		final Map<String, Decl> fields = new HashMap<String, Decl>();
		final Map<String, Decl> methods = new HashMap<String, Decl>();
		final List<Decl> params = new ArrayList<Decl>();
		final List<IProposedPromiseDropInfo> proposals = new ArrayList<IProposedPromiseDropInfo>();

		Decl(String id) {
			this.id = id;
		}

		@Override
		public int compareTo(Decl o) {
			return id.compareTo(o.id);
		}

		Decl getDecl(Map<String, Decl> map, String id) {
			Decl d = map.get(id);
			if (d == null) {
				d = new Decl(id);
				map.put(id, d);
			}
			return d;
		}

		Decl getParam(int index) {
			// Make it big enough to avoid an exception
			while (index >= params.size()) {
				params.add(null);
			}
			Decl d = params.get(index);
			if (d == null) {
				d = new Decl(Integer.toString(index));
				params.set(index, d);
			}
			return d;
		}

		IProposedPromiseDropInfo[] getProposals() {
			return proposals.toArray(new IProposedPromiseDropInfo[proposals
					.size()]);
		}

		Decl[] sort(Map<String, Decl> map) {
			Decl[] decls = map.values().toArray(new Decl[map.size()]);
			Arrays.sort(decls);
			return decls;
		}

		Type[] getTypes() {
			List<Type> members = new ArrayList<Type>();
			for (Decl t : sort(types)) {
				members.add(new Type(t.id, t.getProposals(), t.getMembers()));
			}
			return members.toArray(new Type[members.size()]);
		}
		
		Member<?>[] getMembers() {
			List<Member<?>> members = new ArrayList<Member<?>>();
			for (Decl f : sort(fields)) {
				members.add(new FieldMember(f.id, f.getProposals(), f.getTypes()));
			}
			for (Decl m : sort(methods)) {
				members.add(new MethodMember(m.id, m.getProposals(), m.getTypes()));
			}
			for (Decl t : sort(types)) {
				members.add(new Type(t.id, t.getProposals(), t.getMembers()));
			}
			return members.toArray(new Member[members.size()]);
		}
	}

	/**
	 * @return the Decl that the proposal should be added to
	 */
	static Decl findDecl(Decl here, IJavaDeclaration decl) {
		if (decl instanceof TypeContext) {
			TypeContext t = decl.getTypeContext();
			TypeContext p = t.getParent();
			Method m = t.getMethod();
			if (p != null) {
				return findDecl(here, p);
			} else if (m != null) {
				return findDecl(here, m);
			} else { // Top-level
				return here.getDecl(here.types, t.getName());
			}
		} else if (decl instanceof Method) {
			Method m = (Method) decl;
			Decl type = findDecl(here, m.getTypeContext());
			return type.getDecl(type.methods, m.getSignature());
		} else if (decl instanceof Field) {
			Field m = (Field) decl;
			Decl type = findDecl(here, m.getTypeContext());
			return type.getDecl(type.fields, m.getField());
		} else if (decl instanceof MethodParameter) {
			MethodParameter p = (MethodParameter) decl;
			Decl m = findDecl(here, p.getMethod());
			return m.getParam(p.getParam());
		} else {
			throw new IllegalStateException("Unexpected " + decl);
		}
	}

	static Decl organize(Collection<IProposedPromiseDropInfo> drops) {
		Decl root = new Decl("/root");
		for (IProposedPromiseDropInfo p : drops) {
			Decl d = findDecl(root, p.getTargetInfo());
			d.proposals.add(p);
		}
		return root;
	}

	static abstract class Member<T extends Member<?>> extends
			AbstractTreeable<IProposedPromiseDropInfo> {
		final T[] members;
		
		Member(String id, IProposedPromiseDropInfo[] c, T[] members, boolean sort) {
			super(id, c, sort);
			this.members = members;
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PUBLIC);
		}
		
		@Override
		public boolean hasChildren() {
			return (members != null && members.length > 0)
					|| super.hasChildren();
		}

		@Override
		public Object[] getChildren() {
			Object[] children = super.getChildren();
			if (children.length == 0) {
				return members;
			} else {
				List<Object> temp = new ArrayList<Object>();
				for (Object o : children) {
					temp.add(o);
				}
				for (T m : members) {
					temp.add(m);
				}
				return temp.toArray();
			}
		}
	}

	static class FieldMember extends Member<Type> {
		FieldMember(String id, IProposedPromiseDropInfo[] c, Type[] types) {
			super(id, c, types, false);
		}
	}

	static class MethodMember extends Member<Type> {
		MethodMember(String sig, IProposedPromiseDropInfo[] c, Type[] types) {
			super(sig, c, types, false);
		}
	}

	static class Type extends Member<Member<?>> {
		Type(String t, IProposedPromiseDropInfo[] drops, Member<?>[] members) {
			super(t, drops, members, true);
		}

		@Override
		public Image getImage() {
			return SLImages.getImage(CommonImages.IMG_CLASS);
		}

		static final Factory<String, Type> factory = new Factory<String, Type>() {
			@Override
			String getKey(IProposedPromiseDropInfo d) {
				return d.getSrcRef() == null ? null : d.getSrcRef().getCUName();
			}

			@Override
			Type make(String key, Collection<IProposedPromiseDropInfo> proposals) {
				Decl root = ProposedPromiseContentProvider.organize(proposals);
				Decl type = root.getDecl(root.types, key);
				return new Type(key, type.getProposals(), type.getMembers());
			}
		};
	}
}