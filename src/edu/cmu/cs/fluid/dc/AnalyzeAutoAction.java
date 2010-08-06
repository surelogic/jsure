package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;

/**
 * Implements a context menu action for IProject and IJavaProject that turns on 
 * automatic analysis
 */
public final class AnalyzeAutoAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(IProject current) {
    Plugin.getDefault().buildManually = false;
    return true;
  }
}
