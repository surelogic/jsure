package edu.cmu.cs.fluid.dc;

/**
 * Implements a context menu action for IProject and IJavaProject that turns on 
 * automatic analysis
 */
public final class AnalyzeAutoAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(Object current) {
    Plugin.getDefault().buildManually = false;
    return true;
  }
}
