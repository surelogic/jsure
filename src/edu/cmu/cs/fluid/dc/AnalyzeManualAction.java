package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;

/**
 * Implements a context menu action for IProject and IJavaProject that turns off
 * automatic analysis
 */
public final class AnalyzeManualAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(IProject current) {
    Plugin.getDefault().buildManually = true;
    return true;
  }
}
