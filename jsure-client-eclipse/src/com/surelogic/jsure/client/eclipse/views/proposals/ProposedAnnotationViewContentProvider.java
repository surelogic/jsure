package com.surelogic.jsure.client.eclipse.views.proposals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.DropSeaUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.model.java.ElementJavaDecl;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;

public class ProposedAnnotationViewContentProvider implements ITreeContentProvider {

  /**
   * Represents input for this content provider.
   */
  static class Input {
    @NonNull
    final ElementJavaDecl.Folderizer f_tree;

    Input(@NonNull JSureScanInfo scan, @Nullable ScanDifferences diff, boolean showOnlyFromSrc, boolean showOnlyAbductive) {
      f_tree = new ElementJavaDecl.Folderizer(diff);

      final ArrayList<IProposedPromiseDrop> drops = filterOutDuplicates(scan.getProposedPromiseDrops());
      for (IProposedPromiseDrop ppd : drops) {
        if (showOnlyAbductive && !ppd.isAbductivelyInferred())
          continue;
        if (showOnlyFromSrc && !ppd.isFromSrc())
          continue;

        /*
         * We filter results based upon the code location.
         */
        if (UninterestingPackageFilterUtility.keep(ppd))
          ElementDrop.addToTree(f_tree, ppd, false);
      }
      f_tree.updateFlagsDeep();
    }
  }

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof Input) {
      final Input in = (Input) newInput;
      f_root = in.f_tree.getRootElements();
    } else if (newInput == null) {
      f_root = Element.EMPTY;
    } else {
      SLLogger.getLogger().log(Level.SEVERE, I18N.err(301, this.getClass().getSimpleName(), newInput));
    }
  }

  @Override
  public Object[] getElements(Object inputElement) {
    final Element[] root = f_root;
    return root != null ? root : Element.EMPTY;
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof Element)
      return ((Element) parentElement).getChildren();
    else
      return Element.EMPTY;
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof Element)
      return ((Element) element).getParent();
    else
      return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof Element)
      return ((Element) element).hasChildren();
    else
      return false;
  }

  private Element[] f_root = null;

  public boolean isEmpty() {
    return f_root == null || f_root.length == 0;
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
  static ArrayList<IProposedPromiseDrop> filterOutDuplicates(Collection<IProposedPromiseDrop> proposals) {
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

  void setHighlightDifferences(boolean value) {
    Element.f_highlightDifferences = value;
  }
}
