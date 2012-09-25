package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.common.CommonImages;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IScopedPromiseDrop;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

public final class XResultsViewContentProvider implements ITreeContentProvider {

  public void dispose() {
    // nothing to do
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // nothing to do
  }

  public Object[] getElements(Object inputElement) {
    System.out.println("getElements(" + inputElement);
    List<Object> result = categorize(f_rootPromises);
    if (!f_rootHints.isEmpty()) {
      ResultsViewCategoryContent.Builder builder = new ResultsViewCategoryContent.Builder();
      builder.setLabel("Suggestions and warnings");
      builder.setImageName(CommonImages.IMG_INFO);
    //  builder.addAll(categorize(f_rootHints));
    }
    return result.toArray();
  }

  public Object[] getChildren(Object parentElement) {
    System.out.println("getChildren(" + parentElement);
    return null;
  }

  public Object getParent(Object element) {
    System.out.println("getParent(" + element);
    return null;
  }

  public boolean hasChildren(Object element) {
    System.out.println("hasChildren(" + element);
    return false;
  }

  private boolean f_showHints = true;

  public boolean showHints() {
    return f_showHints;
  }

  public void setShowHints(boolean value) {
    f_showHints = value;
  }

  private final List<IPromiseDrop> f_rootPromises = new ArrayList<IPromiseDrop>();

  private final List<IHintDrop> f_rootHints = new ArrayList<IHintDrop>();

  public void buildModelOfDropSea_internal() {

    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    f_rootPromises.clear();
    f_rootHints.clear();

    if (scan != null) {
      for (IPromiseDrop promise : scan.getPromiseDrops()) {
        if (promise.isFromSrc() || promise.derivedFromSrc()) {
          if (showAtTopLevel(promise)) {
            f_rootPromises.add(promise);
          }
        }
      }
      /*
       * If the hint is uncategorized we don't show it in this section (it shows
       * up under the drop it is attached to).
       */
      for (IHintDrop hint : scan.getAnalysisHintDrops()) {
        if (hint.getCategory() != null)
          f_rootHints.add(hint);
      }
    }
  }

  private List<Object> categorize(List<? extends IDrop> drops) {
    final List<Object> result = new ArrayList<Object>();
    final Map<Category, ResultsViewCategoryContent.Builder> f_categoryToContent = new HashMap<Category, ResultsViewCategoryContent.Builder>();

    for (final IDrop drop : drops) {
      Category category = drop.getCategory();
      if (category == null) {
        result.add(drop);
      } else {
        ResultsViewCategoryContent.Builder builder = f_categoryToContent.get(category);
        if (builder == null) {
          builder = new ResultsViewCategoryContent.Builder();
          f_categoryToContent.put(category, builder);
          builder.setLabel(category.getMessage());
        }
        builder.add(drop);
      }
    }
    for (ResultsViewCategoryContent.Builder builder : f_categoryToContent.values()) {
      result.add(builder.build());
    }
    return result;
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
