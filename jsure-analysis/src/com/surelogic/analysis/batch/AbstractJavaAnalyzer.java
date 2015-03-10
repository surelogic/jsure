/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/batch/AbstractJavaAnalyzer.java,v 1.1 2007/11/21 16:05:03 chance Exp $*/
package com.surelogic.analysis.batch;

import com.surelogic.ast.java.operator.ICompilationUnitNode;
import com.surelogic.ast.java.operator.IPackageDeclarationNode;

public abstract class AbstractJavaAnalyzer implements IJavaAnalyzer {
  @Override
  public void init(boolean clean) {
  }

  @Override
  public void startAnalysis() {
  }

  public void analyzeProjectInfo(FileStatus s) {
  }
  
  @Override
  public AnalysisScope examineInfos() {
    return AnalysisScope.NONE;
  }
  
  @Override
  public void analyzePackageInfo(String pkg, IPackageDeclarationNode pd, FileStatus s) {
  }

  @Override
  public AnalysisScope examineJavaFiles() {
    return AnalysisScope.NONE;
  }
  
  @Override
  public void analyzeJavaFile(String filename, ICompilationUnitNode cu, String src, FileStatus s) {
  }

  @Override
  public AnalysisScope examineClassFiles() {
    return AnalysisScope.NONE;
  }
  
  @Override
  public void analyzeClassFile(String qname, ICompilationUnitNode cu, FileStatus s) {
  }
 
  @Override
  public void cancel() {  
  }

  @Override
  public void endAnalysis() {
  }

  @Override
  public void finish() {
  }
}
