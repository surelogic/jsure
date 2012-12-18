package com.surelogic.jsure.client.eclipse.views.proposals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.DropSeaUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.model.java.ElementJavaDecl;
import com.surelogic.jsure.client.eclipse.model.java.IViewDiffState;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;

public class ProposedAnnotationViewContentProvider implements ITreeContentProvider, IViewDiffState {

  public void dispose() {
    // nothing to do
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // nothing to do
  }

  public Object[] getElements(Object inputElement) {
    final Element[] root = f_root;
    return root != null ? root : Element.EMPTY;
  }

  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof Element)
      return ((Element) parentElement).getChildren();
    else
      return Element.EMPTY;
  }

  public Object getParent(Object element) {
    if (element instanceof Element)
      return ((Element) element).getParent();
    else
      return null;
  }

  public boolean hasChildren(Object element) {
    if (element instanceof Element)
      return ((Element) element).hasChildren();
    else
      return false;
  }

  private Element[] f_root = null;

  void changeContentsToCurrentScan(@NonNull final JSureScanInfo scan, @Nullable final ScanDifferences diff,
      final boolean showOnlyDifferences, final boolean showOnlyFromSrc, final boolean showOnlyAbductive) {
    final ElementJavaDecl.Folderizer tree = new ElementJavaDecl.Folderizer(null);

    final ArrayList<IProposedPromiseDrop> drops = filterOutDuplicates(scan.getProposedPromiseDrops());
    for (IProposedPromiseDrop ppd : drops) {
      if (!showOnlyAbductive || ppd.isAbductivelyInferred()) {
        /*
         * We filter results based upon the code location.
         */
        if (UninterestingPackageFilterUtility.keep(ppd))
          ElementDrop.addToTree(tree, ppd, false);
      }
    }
    f_root = tree.getRootElements();
  }

  /**
   * Tries to find and return an {@link Element} instance that represents the
   * passed drop.
   * 
   * @param drop
   *          a drop.
   * @return an element that represents the drop or {@code null} if none can be
   *         found.
   */
  @Nullable
  Element findElementForDropOrNull(final IDrop drop) {
    if (drop == null)
      return null;
    final Element[] root = f_root;
    if (root == null)
      return null;
    /*
     * We do a breath-first search to look for the element because we do not
     * want to build up a the element tree any more than it is unless we
     * absolutely have too. Of course, if we got passed a drop that doesn't
     * exist the code below will expand out the entire element model tree to its
     * leaves.
     */
    final Queue<Element> queue = new LinkedList<Element>();
    queue.addAll(Arrays.asList(root));
    while (!queue.isEmpty()) {
      final Element e = queue.poll();
      if (e != null) {
        // is e what we are looking for?
        if (e instanceof ElementDrop) {
          if (((ElementDrop) e).getDrop().equals(drop))
            return e;
        }
        queue.addAll(Arrays.asList(e.getChildren()));
      }
    }
    return null;
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
  private static ArrayList<IProposedPromiseDrop> filterOutDuplicates(Collection<IProposedPromiseDrop> proposals) {
    ArrayList<IProposedPromiseDrop> result = new ArrayList<IProposedPromiseDrop>();
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
    final String contents = ppd.getValue();
    if (contents != null) {
      hash += contents.hashCode();
    }
    final String replaced = ppd.getReplacedValue();
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
        if (DropSeaUtility.isSameProposalAs(h, i)) {
          addToResult = false;
          break;
        }
      }
      if (addToResult)
        result.add(h);
    }
    return result;
  }

  @Nullable
  private ScanDifferences f_scanDifferences;

  @Nullable
  public ScanDifferences getScanDifferences() {
    return f_scanDifferences;
  }

  private boolean f_highlightDifferences;

  public boolean highlightDifferences() {
    return f_highlightDifferences;
  }

  void setHighlightDifferences(boolean value) {
    f_highlightDifferences = value;
  }
}
