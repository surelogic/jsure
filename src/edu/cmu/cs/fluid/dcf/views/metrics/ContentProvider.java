/**
 * 
 */
package edu.cmu.cs.fluid.dcf.views.metrics;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.cmu.cs.fluid.sea.reporting.Report;

class ContentProvider extends LabelProvider implements ITreeContentProvider {
  /**
   * 
   */
  private final MetricsView m_view;

  /**
   * @param view
   */
  ContentProvider(MetricsView view) {
    m_view = view;
  }

  public Object[] getChildren(Object parentElement) {
    // TODO Auto-generated method stub
    return null;
  }

  public Object getParent(Object element) {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean hasChildren(Object element) {
    // TODO Auto-generated method stub
    return false;
  }

  public Object[] getElements(Object inputElement) {
    final Map<String, Integer> droptypeToCount = Report.generateDropCounts();
    final List<String> counts = Report.interpretDropCounts(droptypeToCount);
    return counts.toArray();
  }

  @Override
  public void dispose() {
    // TODO Auto-generated method stub
    
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // TODO Auto-generated method stub
    
  }

  /****************************************
   * For ILabelProvider
   ****************************************/
  /*
  public Image getImage(Object element) {
    return null;
  }
  */

  @Override
  public String getText(Object element) {
    return element == null ? "" : element.toString();//$NON-NLS-1$
  }
}