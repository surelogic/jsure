/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/TestJavaAnalysisPackage.java,v 1.3 2007/07/10 22:16:29 aarong Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import junit.framework.TestCase;

public class TestJavaAnalysisPackage extends TestCase {
  final String[] noArgs = new String[0];

  public void testNothing() { 
	  // Nothing to do
  }
  
  /**
   * Still outputs to the console
   */
//  public void testUniqueAnalysis() {    
//    TestUniqueAnalysis t = new TestUniqueAnalysis() {
//      @Override public void reportError(String msg) {
//        fail(msg);
//      }
//    };
//    t.test(new String[] { "bad-field-init" });
//  }
}
