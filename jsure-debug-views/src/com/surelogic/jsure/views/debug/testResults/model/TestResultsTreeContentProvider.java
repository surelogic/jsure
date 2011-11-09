package com.surelogic.jsure.views.debug.testResults.model;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.cmu.cs.fluid.util.ArrayUtil;

public final class TestResultsTreeContentProvider implements ITreeContentProvider {
  private static final Object[] EMPTY_ARRAY = ArrayUtil.empty;
  private Root root = null;
  
  
  
  public TestResultsTreeContentProvider(final Root r) {
    root = r;
  }
  
  
  
  public Object[] getChildren(final Object parentElement) {
    if (parentElement instanceof AbstractTestResult) {
      return EMPTY_ARRAY;
    } else { // Heading
      return root.getChildren((Heading) parentElement);
    }
  }

  public Object getParent(final Object element) {
    if (element instanceof Heading) {
      return null;
    } else { // AbstractTestResult
      return root.computeParent((AbstractTestResult) element);
    }
  }

  public boolean hasChildren(final Object element) {
    if (element instanceof AbstractTestResult) {
      return false;
    } else { // Heading
      return true;
    }
  }

  public Object[] getElements(final Object inputElement) {
    return root.getHeadings();
  }

  public void dispose() {
    // do nothing
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    root = (Root) newInput;
  }
}
