package com.surelogic.jsure.client.eclipse.views.problems;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.AbstractResultsTableContentProvider;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

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
    if (element instanceof IModelingProblemDrop) {
      final IModelingProblemDrop mpd = (IModelingProblemDrop) element;

      if (columnIndex == 0) {
        if (!mpd.getProposals().isEmpty())
          return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR_PROPOSED);
        else
          return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR);
      } else if (columnIndex == 1) {
        if (mpd.isFromSrc())
          return SLImages.getImage(CommonImages.IMG_JAVA_COMP_UNIT);
        else
          return SLImages.getImage(CommonImages.IMG_LIBRARY);
      }
    }
    return null;
  }
}