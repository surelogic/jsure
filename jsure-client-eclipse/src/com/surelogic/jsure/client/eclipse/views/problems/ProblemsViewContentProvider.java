package com.surelogic.jsure.client.eclipse.views.problems;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.jsure.client.eclipse.views.AbstractResultsTableContentProvider;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

final class ProblemsViewContentProvider extends AbstractResultsTableContentProvider<IModelingProblemDrop> {

  ProblemsViewContentProvider() {
    super("Description");
  }

  protected final String getAndSortResults(List<IModelingProblemDrop> mutableContents) {
    final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (info == null) {
      return null;
    }
    for (IModelingProblemDrop problem : info.getModelingProblemDrops()) {
      /*
       * We filter results based upon the code location.
       */
      if (UninterestingPackageFilterUtility.keep(problem))
        mutableContents.add(problem);
    }
    Collections.sort(mutableContents, sortByLocation);
    return info.getLabel();
  }

  public Image getColumnImage(Object element, int columnIndex) {
    if (columnIndex == 0) {
      if (element instanceof IDrop) {
        IDrop id = (IDrop) element;
        if (!id.getProposals().isEmpty()) {
          return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR_PROPOSED);
        }
      }
      return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR);
    } else {
      return null;
    }
  }
}