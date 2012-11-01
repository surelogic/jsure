package com.surelogic.jsure.views.debug.testResults.model;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.java.JavaNames;

public final class TestResultsLabelProvider extends LabelProvider {

  public TestResultsLabelProvider() {
    // do nothing
  }

  @Override
  public Image getImage(final Object element) {
    final Image success = SLImages.getImage(CommonImages.IMG_PLUS);
    final Image failure = SLImages.getImage(CommonImages.IMG_RED_X);
    if (element instanceof Heading) {
      return ((Heading) element).isSuccessful() ? success : failure;
    } else if (element instanceof SuccessfulTestResult) {
      return success;
    } else {
      return failure;
    }
  }

  @Override
  public String getText(final Object element) {
    if (element instanceof Heading) {
      return JavaNames.getFullName(((Heading) element).getNode());
    } else {
      return ((AbstractTestResult) element).getMessage();
    }
  }
}
