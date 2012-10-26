package com.surelogic.jsure.client.eclipse.views.proposals;

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

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IDeclField;
import com.surelogic.common.ref.IDeclFunction;
import com.surelogic.common.ref.IDeclPackage;
import com.surelogic.common.ref.IDeclParameter;
import com.surelogic.common.ref.IDeclType;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeProposedPromiseDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.AbstractResultsTableContentProvider;
import com.surelogic.jsure.client.eclipse.views.IJSureTreeContentProvider;
import com.surelogic.jsure.client.eclipse.views.IResultsTableContentProvider;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

final class ProposedPromiseViewContentProvider extends AbstractResultsTableContentProvider<IProposedPromiseDrop> implements
    IResultsTableContentProvider, IJSureTreeContentProvider {
  private static final Package[] noPackages = new Package[0];
  private static final String[] nothingToShow = new String[] { "No proposals to show" };

  private boolean asTree;
  private Package[] packages = noPackages;

  ProposedPromiseViewContentProvider(boolean asTree) {
    super("Proposed Promise");
    setAsTree(asTree);
  }

  public void setAsTree(boolean asTree) {
    this.asTree = asTree;
  }

  public boolean showAsTree() {
    return asTree;
  }

  protected String getAndSortResults(List<IProposedPromiseDrop> mutableContents) {
    final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (info == null) {
      packages = noPackages;
      return null;
    }
    List<IProposedPromiseDrop> proposedPromiseDrops = filterOutDuplicates(info.getProposedPromiseDrops());
    final boolean showAbductiveOnly = EclipseUtility
        .getBooleanPreference(JSurePreferencesUtility.PROPOSED_PROMISES_SHOW_ABDUCTIVE_ONLY);

    final Iterator<IProposedPromiseDrop> it = proposedPromiseDrops.iterator();
    while (it.hasNext()) {
      IProposedPromiseDrop p = it.next();
      boolean remove = false;
      if (showAbductiveOnly) {
        if (!p.isAbductivelyInferred())
          it.remove();
      }
      /*
       * We filter results based upon the code location.
       */
      if (!UninterestingPackageFilterUtility.keep(p))
        remove = true;

      if (remove)
        it.remove();
    }
    for (IProposedPromiseDrop id : proposedPromiseDrops) {
      if (id != null && id.getJavaRef() != null) {
        mutableContents.add(id);
      }
    }
    packages = Package.organize(proposedPromiseDrops);
    if (packages != null) {
      Arrays.sort(packages);
    }
    Collections.sort(mutableContents, sortByProposal);
    return info.getLabel();
  }

  /**
   * Filters out duplicate proposals so that they are not listed.
   * <p>
   * This doesn't handle proposed promises in binary files too well.
   * 
   * @param proposals
   *          the list of proposed promises.
   * @return the filtered list of proposals.
   */
  private static List<IProposedPromiseDrop> filterOutDuplicates(Collection<IProposedPromiseDrop> proposals) {
    List<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
    // Hash results
    MultiMap<Long, IProposedPromiseDrop> hashed = new MultiHashMap<Long, IProposedPromiseDrop>();
    for (IProposedPromiseDrop info : proposals) {
      long hash = computeHashFor(info);
      hashed.put(hash, info);
    }
    // Filter each list the old way
    for (Map.Entry<Long, Collection<IProposedPromiseDrop>> e : hashed.entrySet()) {
      result.addAll(filterOutDuplicates_slow(e.getValue()));
    }
    return result;
  }

  private static long computeHashFor(@NonNull IProposedPromiseDrop ppd) {
    long hash = 0;
    final String anno = ppd.getAnnotation();
    if (anno != null) {
      hash += anno.hashCode();
    }
    final String contents = ppd.getContents();
    if (contents != null) {
      hash += contents.hashCode();
    }
    final String replaced = ppd.getReplacedContents();
    if (replaced != null) {
      hash += replaced.hashCode();
    }
    final IJavaRef ref = ppd.getJavaRef();
    if (ref != null) {
      hash += ref.hashCode();
    }
    return hash;
  }

  // n^2 comparisons
  private static List<IProposedPromiseDrop> filterOutDuplicates_slow(Collection<IProposedPromiseDrop> proposals) {
    List<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
    for (IProposedPromiseDrop h : proposals) {
      boolean addToResult = true;
      for (IProposedPromiseDrop i : result) {
        if (IRFreeProposedPromiseDrop.isSameProposalAs(h, i)) {
          addToResult = false;
          break;
        }
      }
      if (addToResult)
        result.add(h);
    }
    return result;
  }

  private static final Comparator<IProposedPromiseDrop> sortByProposal = new Comparator<IProposedPromiseDrop>() {
    public int compare(IProposedPromiseDrop d1, IProposedPromiseDrop d2) {
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
  protected String getMainColumnText(IProposedPromiseDrop d) {
    return d.getJavaAnnotation().substring(1);
  }

  @Override
  public String getText(Object element) {
    if (element instanceof IProposedPromiseDrop) {
      IProposedPromiseDrop d = (IProposedPromiseDrop) element;
      return getMainColumnText(d);
    }
    return element.toString();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof Treeable) {
      return ((Treeable) element).getImage();
    }
    if (element instanceof IProposedPromiseDrop) {
      return SLImages.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
    }
    return null;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    if (asTree) {
      if (packages == null || packages.length == 0) {
        return nothingToShow;
      }
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

  static class Package extends AbstractTreeable<Type> {
    Package(String p, Collection<IProposedPromiseDrop> drops) {
      super("".equals(p) ? SLUtility.JAVA_DEFAULT_PACKAGE : p, Type.organize(drops));
    }

    @Override
    public Image getImage() {
      return SLImages.getImage(CommonImages.IMG_PACKAGE);
    }

    static Package[] organize(Collection<IProposedPromiseDrop> drops) {
      MultiMap<String, IProposedPromiseDrop> map = new MultiHashMap<String, IProposedPromiseDrop>();
      for (IProposedPromiseDrop d : drops) {
        if (d.getJavaRef() == null) {
          continue;
        }
        String key = d.getJavaRef().getPackageName();
        map.put(key, d);
      }
      List<Package> things = new ArrayList<Package>();
      for (Map.Entry<String, Collection<IProposedPromiseDrop>> e : map.entrySet()) {
        things.add(new Package(e.getKey(), e.getValue()));
      }
      return things.toArray(noPackages);
    }
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
    final List<IProposedPromiseDrop> proposals = new ArrayList<IProposedPromiseDrop>();

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

    IProposedPromiseDrop[] getProposals() {
      return proposals.toArray(new IProposedPromiseDrop[proposals.size()]);
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
  static Decl findDecl(Decl here, IDecl decl) {
    if (decl instanceof IDeclPackage) {
      return here;
    } else if (decl instanceof IDeclType) {
      Decl parent = findDecl(here, decl.getParent());
      return parent.getDecl(parent.types, decl.getName());
    } else if (decl instanceof IDeclFunction) {
      IDeclFunction m = (IDeclFunction) decl;
      Decl type = findDecl(here, m.getParent());
      return type.getDecl(type.methods, DeclUtil.getSignature(m));
    } else if (decl instanceof IDeclField) {
      IDeclField m = (IDeclField) decl;
      Decl type = findDecl(here, m.getParent());
      return type.getDecl(type.fields, m.getName());
    } else if (decl instanceof IDeclParameter) {
      IDeclParameter p = (IDeclParameter) decl;
      Decl m = findDecl(here, p.getParent());
      return m.getParam(p.getPosition());
    } else {
      throw new IllegalStateException("Unexpected " + decl);
    }
  }

  static Decl organize(Collection<IProposedPromiseDrop> drops) {
    Decl root = new Decl("/root");
    for (IProposedPromiseDrop p : drops) {
      Decl d = findDecl(root, p.getJavaRef().getDeclaration());
      d.proposals.add(p);
    }
    return root;
  }

  static abstract class Member<T extends Member<?>> extends AbstractTreeable<IProposedPromiseDrop> {
    final T[] members;

    Member(String id, IProposedPromiseDrop[] c, T[] members, boolean sort) {
      super(id, c, sort);
      this.members = members;
    }

    @Override
    public Image getImage() {
      return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
    }

    @Override
    public boolean hasChildren() {
      return (members != null && members.length > 0) || super.hasChildren();
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
    FieldMember(String id, IProposedPromiseDrop[] c, Type[] types) {
      super(id, c, types, false);
    }
  }

  static class MethodMember extends Member<Type> {
    MethodMember(String sig, IProposedPromiseDrop[] c, Type[] types) {
      super(sig, c, types, false);
    }
  }

  static class Type extends Member<Member<?>> {
    Type(String t, IProposedPromiseDrop[] drops, Member<?>[] members) {
      super(t, drops, members, true);
    }

    @Override
    public Image getImage() {
      return SLImages.getImage(CommonImages.IMG_CLASS);
    }

    static Type[] organize(Collection<IProposedPromiseDrop> proposals) {
      Decl root = ProposedPromiseViewContentProvider.organize(proposals);
      return root.getTypes();
    }
  }
}