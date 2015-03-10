package com.surelogic.analysis.batch;

import java.util.List;

import com.surelogic.ast.java.operator.ICompilationUnitNode;
import com.surelogic.ast.java.operator.IPackageDeclarationNode;

/**
 * Describes which files a given analysis wants to see.
 * For example:
 * -- Only changes to source files
 * -- All changes (to both source and binaries)
 * -- All source files
 * -- All files loaded
 * 
 * Assuming B depends on A, we'd see this sequence of calls:
 * 
 *    A.init()
 *    B.init()
 *    A.startAnalysis()
 *    ...
 *    A.analyzeFoo()
 *    ...
 *    A.endAnalysis()
 *    B.startAnalysis()
 *    ...
 *    B.analyzeFoo()
 *    ...
 *    B.endAnalysis()
 *    A.finish()
 *    B.finish()
 *    
 * If analysis is cancelled, the call sequence would end with:
 *
 *    A.cancel()
 *    B.cancel()
 *    
 * at whatever point the user cancelled analysis   
 * 
 * @author chance
 */
public interface IJavaAnalyzer {
  public enum AnalysisScope {
    NONE, 
    /**
     * Only look at added/changed/deleted files
     */
    CHANGED, 
    /**
     * Look at all such files
     */
    ALL
  }
  public enum FileStatus {
    UNCHANGED, ADDED, CHANGED, DELETED
  }
  
  /**
   * @return A unique identifier
   */
  String id();
  
  /**
   * @return The ids of the analyzers that it depends upon
   */
  List<String> dependsOn();
  
  /**
   * Gets the label for this analyzer
   */
  String getLabel();
  
  /**
   * Called before any analysis is done
   */
  void init(boolean clean);  
  
  /**
   * Called before any calls to analyzeFoo()
   */
  void startAnalysis();
  
  /**
   * Describes whether this analysis wants to see
   * project-info.java or package-info.java files
   *  
   * If CHANGED or ALL, analyzePackageInfo() is called
   * for each package-info.java file
   */
  AnalysisScope examineInfos();
  
  /**
   * Called for project-info.java 
   */
  void analyzeProjectInfo(String src, FileStatus s);
  
  /**
   * Called for each package-info.java file to be examined
   */
  void analyzePackageInfo(String pkg, IPackageDeclarationNode pd, FileStatus s);  
  
  /**
   * Describes whether this analysis wants to see
   * .java files 
   *  
   * If CHANGED or ALL, analyzeJavaFile() is called
   * for each .java file
   */
  AnalysisScope examineJavaFiles();    
  
  /**
   * Called for each .java file to be examined
   */
  void analyzeJavaFile(String filename, ICompilationUnitNode cu, String src, FileStatus s);
  
  /**
   * Describes whether this analysis wants to see
   * .class files 
   *  
   * If CHANGED or ALL, analyzeClassFile() is called
   * for each .class file
   */
  AnalysisScope examineClassFiles();
  
  /**
   * Called for each .class file to be examined
   */
  void analyzeClassFile(String qname, ICompilationUnitNode cu, FileStatus s);
  
  /**
   * Called for each analyzer if the analysis is 
   * cancelled by the user
   */
  void cancel();
  
  /**
   * Called after all calls to analyzeFoo()
   */
  void endAnalysis();
  
  /**
   * Called before all analysis is done
   */
  void finish();
}
