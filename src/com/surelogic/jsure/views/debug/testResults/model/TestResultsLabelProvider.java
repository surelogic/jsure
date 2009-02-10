package com.surelogic.jsure.views.debug.testResults.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.java.JavaNames;

public final class TestResultsLabelProvider extends LabelProvider {
  private static final String FAIL_IMAGE_NAME = "redx.gif";
  private static final ImageDescriptor FAIL_IMAGE_DESC =
    AbstractDoubleCheckerView.getImageDescriptor(FAIL_IMAGE_NAME);
  private static final Image FAIL_IMAGE = FAIL_IMAGE_DESC.createImage();

  private static final String SUCCESS_IMAGE_NAME = "plus.gif";
  private static final ImageDescriptor SUCCESS_IMAGE_DESC =
    AbstractDoubleCheckerView.getImageDescriptor(SUCCESS_IMAGE_NAME);
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
