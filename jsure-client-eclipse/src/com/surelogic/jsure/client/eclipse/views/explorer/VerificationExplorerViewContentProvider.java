package com.surelogic.jsure.client.eclipse.views.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
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

public final class VerificationExplorerViewContentProvider implements ITreeContentProvider {

  /**
   * Represents input for this content provider.
   */
  static class Input {
    @NonNull
    final ElementJavaDecl.Folderizer f_tree;

    Input(@NonNull final JSureScanInfo scan, @Nullable JSureScanInfo oldScan, @Nullable final ScanDifferences diff,
        boolean showObsoleteDrops, boolean showOnlyDerivedFromSrc, boolean showAnalysisResults, boolean showHints) {
      f_tree = new ElementJavaDecl.Folderizer(diff);

      final ArrayList<IDrop> drops = new ArrayList<IDrop>();
      drops.addAll(scan.getProofDrops());
      if (showHints)
        drops.addAll(scan.getHintDrops());
      if (showObsoleteDrops && diff != null)
        drops.addAll(diff.getDropsOnlyInOldScan(oldScan));

      final Set<IDrop> oldDrops = oldScan == null ? null : new HashSet<IDrop>(oldScan.getDropInfo());
      for (IDrop pd : drops) {
        if (!(pd instanceof IProofDrop || pd instanceof IHintDrop))
          continue;
        if (showOnlyDerivedFromSrc && pd instanceof IProofDrop && !((IProofDrop) pd).derivedFromSrc())
          continue;
        if (!showAnalysisResults && pd instanceof IAnalysisResultDrop)
          continue;
        if (pd instanceof IResultFolderDrop)
          continue;
        ElementDrop.addToTree(f_tree, pd, oldDrops == null ? false : oldDrops.contains(pd));
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

  void setHighlightDifferences(boolean value) {
    Element.f_highlightDifferences = value;
  }
}
