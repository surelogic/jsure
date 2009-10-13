package com.surelogic.jsure.views.debug.testResults.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;

import edu.cmu.cs.fluid.java.JavaNames;

public final class TestResultsLabelProvider extends LabelProvider {
  private static final ImageDescriptor FAIL_IMAGE_DESC =
    SLImages.getImageDescriptor(CommonImages.IMG_RED_X);
  private static final Image FAIL_IMAGE = FAIL_IMAGE_DESC.createImage();

  private static final ImageDescriptor SUCCESS_IMAGE_DESC =
    SLImages.getImageDescriptor(CommonImages.IMG_PLUS);
  private static final Image SUCCESS_IMAGE = SUCCESS_IMAGE_DESC.createImage();

  
  
  public TestResultsLabelProvider() {
    // do nothing
  }
  
  
  
  @Override
  public Image getImage(final Object element) {
    if (element instanceof Heading) {
      return ((Heading) element).isSuccessful() ? SUCCESS_IMAGE : FAIL_IMAGE;
    } else if (element instanceof SuccessfulTestResult) {
      return SUCCESS_IMAGE;
    } else {
      return FAIL_IMAGE;
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
