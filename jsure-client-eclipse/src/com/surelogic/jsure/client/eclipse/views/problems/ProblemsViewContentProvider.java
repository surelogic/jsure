package com.surelogic.jsure.client.eclipse.views.problems;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.jsure.client.eclipse.views.AbstractResultsTableContentProvider;
import com.surelogic.jsure.client.eclipse.views.DropInfoUtility;
import com.surelogic.jsure.core.preferences.ModelingProblemFilterUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;


final class ProblemsViewContentProvider extends AbstractResultsTableContentProvider<IDrop> {

  ProblemsViewContentProvider() {
    super("Description");
  }

  protected final String getAndSortResults(List<IDrop> contents) {
    final JSureScanInfo info = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (info == null) {
      return null;
    }
    for (IModelingProblemDrop id : info.getModelingProblemDrops()) {
      final String resource = DropInfoUtility.getResource(id);
      /*
       * We filter results based upon the resource.
       */
      if (ModelingProblemFilterUtility.showResource(resource))
        contents.add(id);
    }
    Collections.sort(contents, sortByLocation);
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