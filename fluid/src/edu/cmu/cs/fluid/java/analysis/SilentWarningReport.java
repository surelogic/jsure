/*
 * Created on Oct 3, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;


public class SilentWarningReport implements IWarningReport {
  public static final IWarningReport prototype = new SilentWarningReport();

  public void reportWarning(String description, IRNode here) {  
    // do nothing
  }

  public void reportProblem(String description, IRNode here) {
    // do nothing
  }
}
