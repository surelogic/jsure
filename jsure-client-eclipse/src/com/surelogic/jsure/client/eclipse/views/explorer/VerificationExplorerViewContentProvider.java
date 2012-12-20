package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
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
import com.surelogic.jsure.client.eclipse.model.java.IViewDiffState;

public final class VerificationExplorerViewContentProvider implements ITreeContentProvider, IViewDiffState {

  /**
   * Represents input for this content provider.
   */
  static class Input {
    @NonNull
    final JSureScanInfo f_scan;
    @Nullable
    final JSureScanInfo f_oldScan;
    @Nullable
    final ScanDifferences f_diff;
    final boolean f_showOnlyDifferences;
    final boolean f_showObsoleteDrops;
    final boolean f_showOnlyDerivedFromSrc;
    final boolean f_showAnalysisResults;
    final boolean f_showHints;

    Input(@NonNull final JSureScanInfo scan, @Nullable JSureScanInfo oldScan, @Nullable final ScanDifferences diff,
        boolean showOnlyDifferences, boolean showObsoleteDrops, boolean showOnlyDerivedFromSrc, boolean showAnalysisResults,
        boolean showHints) {
      f_scan = scan;
      f_oldScan = oldScan;
      f_diff = diff;
      f_showOnlyDifferences = showOnlyDifferences;
      f_showObsoleteDrops = showObsoleteDrops;
      f_showOnlyDerivedFromSrc = showOnlyDerivedFromSrc;
      f_showAnalysisResults = showAnalysisResults;
      f_showHints = showHints;
    }
  }

  public void dispose() {
    // nothing to do
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput instanceof Input) {
      final Input in = (Input) newInput;
      f_scanDifferences = in.f_diff;
      final ElementJavaDecl.Folderizer tree = new ElementJavaDecl.Folderizer(this);

      boolean noDiffAndOnlyShowingDiff = in.f_diff == null && in.f_showOnlyDifferences;
      if (!noDiffAndOnlyShowingDiff) {
        final ArrayList<IDrop> drops = new ArrayList<IDrop>();
        drops.addAll(in.f_scan.getProofDrops());
        if (in.f_showHints)
          drops.addAll(in.f_scan.getHintDrops());
        if (in.f_showObsoleteDrops && in.f_diff != null)
          drops.addAll(in.f_diff.getDropsOnlyInOldScan());

        for (IDrop pd : drops) {
          if (in.f_showOnlyDifferences && in.f_diff != null && in.f_diff.isSameInBothScans(pd))
            continue;
          if (!(pd instanceof IProofDrop || pd instanceof IHintDrop))
            continue;
          if (in.f_showOnlyDerivedFromSrc && pd instanceof IProofDrop && !((IProofDrop) pd).derivedFromSrc())
            continue;
          if (!in.f_showAnalysisResults && pd instanceof IAnalysisResultDrop)
            continue;
          if (pd instanceof IResultFolderDrop)
            continue;
          ElementDrop.addToTree(tree, pd, in.f_diff == null ? false : in.f_diff.isNotInNewScan(pd));
        }
      }
      f_root = tree.getRootElements();
    } else if (newInput == null) {
      f_root = Element.EMPTY;
      f_scanDifferences = null;
    } else {
      SLLogger.getLogger().log(Level.SEVERE, I18N.err(301, this.getClass().getSimpleName(), newInput));
    }
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
