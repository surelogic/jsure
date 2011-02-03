package edu.cmu.cs.fluid.eclipse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;

/**
 * Callback interface for notifications about adaption of an Eclipse
 * ICompilationUnit into IR Java representation.  An object that wishes
 * to be notified must implement this interface and register itself via
 * the registration method in the fluid.eclipse.Eclipse class.
 */
@Deprecated
public interface ISrcAdapterNotify {  
  /**
   * Callback to notify that an Eclipse ICompilationUnit
   * has been adapted into the IR.
   * @param top The JavaNode at the top of the IR representing this ICompilationUnit.
   * @param javaOSFileName The complete OS path and name for the Java file.
   * @param src The source code for this ICompilationUnit.
   */
  void run(CodeInfo info);
  
  void gotNewPackage(IRNode pkg, String name);
}