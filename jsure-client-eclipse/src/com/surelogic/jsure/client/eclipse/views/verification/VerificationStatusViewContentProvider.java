package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.common.CommonImages;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IScopedPromiseDrop;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

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

  private boolean f_showHints = true;

  public boolean showHints() {
    return f_showHints;
  }

  public void setShowHints(boolean value) {
    f_showHints = value;
  }

  private Element[] f_root = null;

  public void buildModelOfDropSea_internal() {
    final List<Element> root = new ArrayList<Element>();
    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (scan != null) {
      final ElementCategory.Categorizer pc = new ElementCategory.Categorizer(null);
      for (IPromiseDrop promise : scan.getPromiseDrops()) {
        if (promise.isFromSrc() || promise.derivedFromSrc()) {
          if (showAtTopLevel(promise)) {
            pc.add(promise);
          }
        }
      }
      /*
       * If the hint is uncategorized we don't show it in this section (it shows
       * up under the drop it is attached to).
       */
      final ElementCategory.Categorizer hc = new ElementCategory.Categorizer(null);
      for (IHintDrop hint : scan.getAnalysisHintDrops()) {
        if (hint.getCategory() != null)
          hc.add(hint);
      }
      root.addAll(pc.getAllElements());
      if (!hc.isEmpty()) {
        final ElementCategory.Builder sw = new ElementCategory.Builder(null);
        sw.setLabel("Suggestions and warnings");
        sw.setImageName(CommonImages.IMG_INFO);
        sw.addCategories(hc.getBuilders());
        root.add(sw.build());
      }
      f_root = root.toArray(new Element[root.size()]);
    }
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
}
