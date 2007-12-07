package com.surelogic.jsure.client.eclipse;

import com.surelogic.analysis.IAnalysisReporter;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;

/**
 * Reports analysis events to the database
 * (via the promise matcher)
 * 
 * @author Edwin.Chan
 */
public class Reporter implements IAnalysisReporter {
  public void reportInfo(IRNode n, String msg) {
    System.out.println("On "+JavaNames.getFullName(n)+": "+msg);
  }
}
