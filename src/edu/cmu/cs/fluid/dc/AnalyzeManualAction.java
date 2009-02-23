package edu.cmu.cs.fluid.dc;

/**
 * Implements a context menu action for IProject and IJavaProject that turns off
 * automatic analysis
 */
public final class AnalyzeManualAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(Object current) {
    Plugin.getDefault().buildManually = true;
    return true;
  }
}
