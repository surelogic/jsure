package com.surelogic.jsure.core.driver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.surelogic.analysis.IAnalysisMonitor;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Configured to always do a full build over all the resources
 * 
 * @author chance
 */
public abstract class AbstractWholeIRAnalysisModule extends
    AbstractIRAnalysisModule {
  /**
   * Used to note whether a full build is being done by Majordomo
   */
  protected boolean fullBuildInProgress = false;

  /**
   * Used to note whether a full project pass is in progress
   */
  protected boolean doingFullProjectPass = false;

  protected AbstractWholeIRAnalysisModule(ParserNeed need) {
    super(need);
  }
  
  protected AbstractWholeIRAnalysisModule() {
    super(ParserNeed.OLD);
  }
  
  /**
   * @see com.surelogic.jsure.core.driver.IAnalysis#resetForAFullBuild(org.eclipse.core.resources.IProject)
   */
  @Override
  public final void resetForAFullBuild(IProject project) {
    fullBuildInProgress = true;
  }

  /**
   * @see com.surelogic.jsure.core.driver.IAnalysis#analyzeBegin(org.eclipse.core.resources.IProject)
   */
  @Override
  public void analyzeBegin(IProject project) {
    super.analyzeBegin(project);
    doingFullProjectPass = fullBuildInProgress;
  }

  /**
   * @see com.surelogic.jsure.core.driver.IAnalysis#analyzeResource(org.eclipse.core.resources.IResource,
   *      int)
   */
  @Override
  public boolean analyzeResource(IResource resource, int kind) {
    // Only analyze the compilation unit if we're doing a full pass, not a
    // partial one
    if (doingFullProjectPass) {
      return false; // please call my analyzeCompilationUnit()
    } 
    return true; // done
  }

  /**
   * @see com.surelogic.jsure.core.driver.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
   */
  @Override
  public Iterable<IRNode> finishAnalysis(IProject project, IAnalysisMonitor monitor) {
    if (IDE.getInstance().isCancelled()) {
      return NONE_TO_ANALYZE;
    }
    if (doingFullProjectPass) {
      // Already doing a full build, so no need to do it again
      doingFullProjectPass = fullBuildInProgress = false;
      return NONE_TO_ANALYZE;
    } 
    // Only doing a partial build, so analyze the whole thing
    doingFullProjectPass = true;
    return null; // the entire project needs to be analyzed again
  }
}
