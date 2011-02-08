package edu.cmu.cs.fluid.dcf.views.metrics;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;

import com.surelogic.jsure.client.eclipse.views.AbstractDoubleCheckerView;


public class MetricsView extends AbstractDoubleCheckerView {
  static class Sorter extends ViewerSorter { 
  }
  
  private final ContentProvider m_contentProvider = new ContentProvider(this);
  
  @Override
  protected void setupViewer() {
    viewer.setContentProvider(m_contentProvider);
    viewer.setLabelProvider(m_contentProvider);
    viewer.setSorter(new Sorter());
  }
  
  @Override
  protected void makeActions() {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void fillLocalPullDown(IMenuManager manager) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void fillLocalToolBar(IToolBarManager manager) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void handleDoubleClick(IStructuredSelection selection) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void setViewState() {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void updateView() {
    // TODO Auto-generated method stub
    
  }
}
