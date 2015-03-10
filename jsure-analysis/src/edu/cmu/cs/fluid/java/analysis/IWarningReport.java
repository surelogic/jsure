package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IWarningReport {

  /**
	 * A callback to report a promise warning item to Tallyho.
	 * 
	 * @param description
	 *          Details on the specific issue being reported
	 * @param lineNo
	 *          The line number where the issue was found (or 1 if none)
	 */
  /*
  public void reportWarning(String description, int lineNo);
  
  public void reportProblem(String description, int lineNo);
  */
  public void reportWarning(String description, IRNode here);  
  public void reportProblem(String description, IRNode here);  
}
