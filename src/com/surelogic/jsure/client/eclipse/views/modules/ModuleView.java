/*
 * Created on Dec 8, 2005
 *
 */
package com.surelogic.jsure.client.eclipse.views.modules;

import java.util.Set;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;

import com.surelogic.jsure.client.eclipse.views.AbstractDoubleCheckerView;

import edu.cmu.cs.fluid.sea.drops.promises.ModuleModel;

/**
 * Designed to show the various modules and sub-modules at the top
 * of the tree, as well as their contents (packages, classes)
 * 
 * Note that the default module will be left empty?
 */
public class ModuleView extends AbstractDoubleCheckerView {
  final Provider provider = new Provider();
  
  @Override
  protected void setupViewer() {    
    viewer.setContentProvider(provider);
    viewer.setLabelProvider(provider);
    // viewer.setSorter();
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
  protected void setViewState() {
    // TODO Auto-generated method stub
  }

  @Override
  protected void updateView() {
    // TODO Auto-generated method stub
  }
  
  private static ModuleModel[] ofModuleModel = new ModuleModel[0];
  
  private static String[] modulesNotActive = new String[1];
  private static ModuleModel[] onlyTheWorld = new ModuleModel[1];
  static {
    modulesNotActive[0] = "Please turn on the Module Analyzer";
    onlyTheWorld[0] = ModuleModel.getTheWorld();
  }
  
  @Override
  protected void handleDoubleClick(IStructuredSelection selection) {
    // TODO Auto-generated method stub
  }
  
  private static class Provider implements ITreeContentProvider, ILabelProvider {
    private ModuleModel model(Object o) {
      if (o instanceof ModuleModel) {
        return (ModuleModel) o;
      }
      return null;
    }
    
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
    
    public Object[] getChildren(Object o) {
      ModuleModel m = model(o);
      if (m == null || m == ModuleModel.getTheWorld()) {
        return ofModuleModel; // empty array
      }
      return m.getChildren().toArray(ofModuleModel);
    }

    public Object getParent(Object o) {
      ModuleModel m = model(o);
      if (m == null) {
        return null;
      }
      return m.getParent();
    }

    public boolean hasChildren(Object o) {
      ModuleModel m = model(o);
      if (m == null || m == ModuleModel.getTheWorld()) {
        return false;
      }
      return !m.getChildren().isEmpty();
    }

    public Object[] getElements(Object inputElement) {
      boolean isModuleAnalysisActive = false;
      if (!isModuleAnalysisActive) {
        return modulesNotActive;
      }      
      Set<ModuleModel> modules = ModuleModel.getChildrenOfWorld();
      if (modules.isEmpty()) {
        return onlyTheWorld;
      }
      ModuleModel[] mods = new ModuleModel[modules.size()+1];
      ModuleModel[] mods2 = modules.toArray(mods);
      if (mods != mods2) {
        LOG.severe("Newly created array didn't fit all the top-level modules");
      }
      mods[mods.length-1] = ModuleModel.getTheWorld();
      return mods;
    }

    public void dispose() {
      // TODO Auto-generated method stub
    }

    public Image getImage(Object element) {
      // TODO Auto-generated method stub
      return null;
    }

    public String getText(Object o) {
      ModuleModel m = model(o);
      if (m == null) {
        return o.toString();
      }
      return m.getMessage();
    }

    public boolean isLabelProperty(Object element, String property) {
      // TODO Auto-generated method stub
      return false;
    }

    public void addListener(ILabelProviderListener listener) {
      // TODO Auto-generated method stub  
    }
    
    public void removeListener(ILabelProviderListener listener) {
      // TODO Auto-generated method stub  
    }
  }
}
