/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/TestTreePackage.java,v 1.3 2007/04/02 19:30:16 chance Exp $*/
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.test.*;
import edu.cmu.cs.fluid.version.TestVersionedTree;
import junit.framework.TestCase;

public class TestTreePackage extends TestCase {
  /*
  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  */
  
  String[] noVerboseOutput = new String[0];
  
  public void testDigraph() {
    new TestDigraph().setReporter(JUnitReporter.prototype).test(noVerboseOutput);
  }
  
  public void testEdgeDigraph() {
    new TestEdgeDigraph(JUnitReporter.prototype).test(noVerboseOutput);
  }
  
  public void testSymmetricEdgeDigraph() {
    new TestSymmetricEdgeDigraph(JUnitReporter.prototype).test(noVerboseOutput);
  }
  /* TODO add this back in when boyland fixes the code
   * 
  public void testSymmetricDigraph() {
    new TestSymmetricDigraph().setReporter(JUnitReporter.prototype).test(noVerboseOutput);
  }
  */
  public void testTree() {
    new TestTree().setReporter(JUnitReporter.prototype).test(noVerboseOutput);
  }
  
  public void testVersionedTree() {
    new TestVersionedTree().setReporter(JUnitReporter.prototype).test(noVerboseOutput);
  }
}
