package com.surelogic.jsure.client.eclipse.views.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IScopedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.javac.persistence.JSureScanInfo;

public final class VerificationStatusViewContentProvider implements ITreeContentProvider {

  static class Input {
    @NonNull
    final JSureScanInfo f_scan;
    @Nullable
    final ScanDifferences f_diff;
    final boolean f_showHints;
    final List<Element> f_root = new ArrayList<Element>();
    
    Element[] getRootAsArray() {
      return f_root.toArray(new Element[f_root.size()]);
    }

    /**
     * Should never be invoked from the UI thread!
     * 
     * @param scan
     *          new scan.
     * @param diff
     *          differences if any.
     * @param showHints
     *          {@code true} if hints should be displayed, {@code false} if not.
     */
    Input(@NonNull JSureScanInfo scan, @Nullable ScanDifferences diff, boolean showHints) {
      f_scan = scan;
      f_diff = diff;
      f_showHints = showHints;

      /*
       * Go ahead a calculate the model for the view
       */
      Element.f_showHints = f_showHints;
      Element.f_diff = f_diff;
      final ElementCategory.Categorizer pc = new ElementCategory.Categorizer(null);
      for (IPromiseDrop promise : f_scan.getPromiseDrops()) {
        if (promise.isFromSrc() || promise.derivedFromSrc()) {
          if (showAtTopLevel(promise)) {
            pc.add(promise);
          }
        }
      }
      f_root.addAll(pc.getAllElements());

      if (f_showHints) {
        /*
         * If the hint is uncategorized we don't show it in this section (it
         * shows up under the drop it is attached to).
         */
        final ElementCategory.Categorizer hc = new ElementCategory.Categorizer(null);
        for (IHintDrop hint : f_scan.getHintDrops()) {
          if (hint.getCategorizingMessage() != null)
            hc.add(hint);
        }
        if (!hc.isEmpty()) {
          final ElementCategory.Builder sw = new ElementCategory.Builder(null);
          sw.setLabel(ElementCategory.SPECIAL_HINT_FOLDER_NAME);
          sw.setImageName(CommonImages.IMG_INFO);
          sw.addCategories(hc.getBuilders());
          f_root.add(sw.build());
        }
      }
      for (Element e : f_root)
        Element.updateFlagsDeepHelper(e);
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
      f_root = in.getRootAsArray();
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

  void setHighlightDifferences(boolean value) {
    Element.f_highlightDifferences = value;
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
  static boolean showAtTopLevel(IPromiseDrop promise) {
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
