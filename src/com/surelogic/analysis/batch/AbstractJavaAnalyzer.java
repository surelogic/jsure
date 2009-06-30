/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/batch/AbstractJavaAnalyzer.java,v 1.1 2007/11/21 16:05:03 chance Exp $*/
package com.surelogic.analysis.batch;

import com.surelogic.ast.java.operator.ICompilationUnitNode;
import com.surelogic.ast.java.operator.IPackageDeclarationNode;

public abstract class AbstractJavaAnalyzer implements IJavaAnalyzer {
  public void init(boolean clean) {
  }

  public void startAnalysis() {
  }

  public void analyzeProjectInfo(FileStatus s) {
  }
  
  public AnalysisScope examineInfos() {
    return AnalysisScope.NONE;
  }
  
  public void analyzePackageInfo(String pkg, IPackageDeclarationNode pd, FileStatus s) {
  }

  public AnalysisScope examineJavaFiles() {
    return AnalysisScope.NONE;
  }
  
  public void analyzeJavaFile(String filename, ICompilationUnitNode cu, String src, FileStatus s) {
  }

  public AnalysisScope examineClassFiles() {
    return AnalysisScope.NONE;
  }
  
  public void analyzeClassFile(String qname, ICompilationUnitNode cu, FileStatus s) {
  }
 
  public void cancel() {  
  }

  public void endAnalysis() {
  }

  public void finish() {
  }
}
