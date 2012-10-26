package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IScopedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.javac.persistence.JSureScanInfo;

public final class VerificationStatusViewContentProvider implements ITreeContentProvider {

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
    Element.f_highlightDifferences = value;
  }

  void changeContentsToCurrentScan(@NonNull final JSureScanInfo scan, @Nullable final JSureScanInfo oldScan,
      @Nullable final ScanDifferences diff, final boolean showHints) {
    final List<Element> root = new ArrayList<Element>();
    Element.f_showHints = showHints;
    Element.f_diff = diff;
    final ElementCategory.Categorizer pc = new ElementCategory.Categorizer(null);
    for (IPromiseDrop promise : scan.getPromiseDrops()) {
      if (promise.isFromSrc() || promise.derivedFromSrc()) {
        if (showAtTopLevel(promise)) {
          pc.add(promise);
        }
      }
    }
    root.addAll(pc.getAllElements());

    if (showHints) {
      /*
       * If the hint is uncategorized we don't show it in this section (it shows
       * up under the drop it is attached to).
       */
      final ElementCategory.Categorizer hc = new ElementCategory.Categorizer(null);
      for (IHintDrop hint : scan.getAnalysisHintDrops()) {
        if (hint.getCategorizingMessage() != null)
          hc.add(hint);
      }
      if (!hc.isEmpty()) {
        final ElementCategory.Builder sw = new ElementCategory.Builder(null);
        sw.setLabel(ElementCategory.SPECIAL_HINT_FOLDER_NAME);
        sw.setImageName(CommonImages.IMG_INFO);
        sw.addCategories(hc.getBuilders());
        root.add(sw.build());
      }
    }
    f_root = root.toArray(new Element[root.size()]);
  }

  /**
   * Determines if a particular promise should be shown at the root level of the
   * viewer. A promise is shown at the root level if any of the following
   * predicates is true:
   * <ul>
   * <li>The class that the promise drop is an instance of (in the IR drop-sea)
   * implements {@link UiShowAtTopLevel}.</li>
   * <li>The promise drop has no "higher" (deponent) promise drops.</li>
   * <li>The promise drop has only "higher" (deponent) promise drops that are
   * scoped promises (they implement {@link IScopedPromiseDrop}).</li>
   * </ul>
   * 
   * @param promise
   *          a promise.
   * @return {@code true} if the promise should appear at the root level,
   *         {@code false} otherwise.
   */
  private static boolean showAtTopLevel(IPromiseDrop promise) {
    if (promise == null)
      return false;
    if (promise.instanceOfIRDropSea(UiShowAtTopLevel.class))
      return true;
    /*
     * If we have a deponent promise that is not a scoped promise we do not want
     * to show at the top level.
     */
    for (IPromiseDrop pd : promise.getDeponentPromises()) {
      if (!(pd instanceof IScopedPromiseDrop))
        return false;
    }
    return true;
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
