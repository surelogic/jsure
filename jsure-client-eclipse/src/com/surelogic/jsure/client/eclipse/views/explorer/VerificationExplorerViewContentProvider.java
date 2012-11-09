package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.model.java.ElementJavaDecl;

public final class VerificationExplorerViewContentProvider implements ITreeContentProvider {

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

  void setHighlightDifferences(boolean value) {
    Element.setHighlightDifferences(value);
  }

  void changeContentsToCurrentScan(@NonNull final JSureScanInfo scan, @Nullable final JSureScanInfo oldScan,
      @Nullable final ScanDifferences diff, final boolean showOnlyDifferences, final boolean showObsoleteDrops,
      final boolean showOnlyDerivedFromSrc, final boolean showAnalysisResults, final boolean showHints) {
    Element.setScanDifferences(diff);
    final ElementJavaDecl.Folderizer tree = new ElementJavaDecl.Folderizer();

    boolean noDiffAndOnlyShowingDiff = diff == null && showOnlyDifferences;
    if (!noDiffAndOnlyShowingDiff) {
      final ArrayList<IDrop> drops = new ArrayList<IDrop>();
      drops.addAll(scan.getProofDrops());
      if (showHints)
        drops.addAll(scan.getHintDrops());
      if (showObsoleteDrops && diff != null)
        drops.addAll(diff.getDropsOnlyInOldScan());

      for (IDrop pd : drops) {
        if (showOnlyDifferences && diff != null && diff.isSameInBothScans(pd))
          continue;
        if (!(pd instanceof IProofDrop || pd instanceof IHintDrop))
          continue;
        if (showOnlyDerivedFromSrc && pd instanceof IProofDrop && !((IProofDrop) pd).derivedFromSrc())
          continue;
        if (!showAnalysisResults && pd instanceof IAnalysisResultDrop)
          continue;
        if (pd instanceof IResultFolderDrop)
          continue;
        ElementDrop.addToTree(tree, pd, diff == null ? false : diff.isNotInNewScan(pd));
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
}
