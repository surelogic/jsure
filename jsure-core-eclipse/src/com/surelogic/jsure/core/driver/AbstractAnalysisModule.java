package com.surelogic.jsure.core.driver;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.ConcurrentAnalysis;
import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.ICompUnitContext;

/**
 * A minimum analysis module, intended to be extended by other classes to create
 * new analysis modules. Analysis modules are singleton classes that implement
 * the {@link IAnalysis}interface.
 */
public class AbstractAnalysisModule<Q extends ICompUnitContext> extends ConcurrentAnalysis<Q> implements IAnalysis {
  public AbstractAnalysisModule() {
	  super(false, null);
  }
  public AbstractAnalysisModule(boolean inParallel, Class<Q> type) {
	  super(inParallel, type);
  }
	
  /**
   * The label from the plugin.xml that defined this analysis module. It is
   * passed in via {@link #setLabel(String)}by {@link Majordomo}and retrieved
   * by {@link #getLabel()}.
   */
  private String label = "unknown";

  /**
   * An empty array of {@link IResource}for use within
   * {@link #analyzeEnd(IProject)}.
   */
  static public final IResource[] NONE_FURTHER = new IResource[0];

  /**
   * @see IAnalysis#postBuild(IProject)
   */
  public void postBuild(IProject project) {
    // do nothing
  }

  /**
   * @see IAnalysis#resetForAFullBuild(IProject)
   */
  public void resetForAFullBuild(IProject project) {
    // do nothing
  }

  /**
   * @see IAnalysis#analyzeBegin(IProject)
   */
  public void analyzeBegin(IProject project) {
    // do nothing
  }

  /**
   * @see IAnalysis#analyzeResource(IResource, int)
   */
  public boolean analyzeResource(IResource resource, int kind) {
    return true; // do not call analyzeCompilationUnit()
  }

  /**
   * @see IAnalysis#needsAST()
   */
  public boolean needsAST() {
    return true;
  }

  /**
   * @see IAnalysis#analyzeCompilationUnit(ICompilationUnit, CompilationUnit)
   */
  public boolean analyzeCompilationUnit(ICompilationUnit file, CompilationUnit ast, 
          IAnalysisMonitor monitor) {
	  // do nothing
	  return false;
  }

  /**
   * @see IAnalysis#analyzeEnd(IProject)
   */
  public IResource[] analyzeEnd(IProject project, IAnalysisMonitor monitor) {
    return NONE_FURTHER;
  }

  /**
   * @see IAnalysis#preBuild(IProject)
   */
  public void preBuild(IProject project) {
    // do nothing
  }

  /**
   * @see com.surelogic.jsure.core.driver.IAnalysis#getLabel()
   */
  public String getLabel() {
    return label;
  }

  /**
   * @see IAnalysis#setLabel(String)
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * @see IAnalysis#setArguments(Map)
   */
  @SuppressWarnings("unchecked")
public void setArguments(Map args) {
    // do nothing
  }

  /**
   * @see IAnalysis#cancel()
   */
  public void cancel() {
    // do nothing
  }
}
