/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TestVersionedStructures.java,v 1.5 2007/07/10 22:16:33 aarong Exp $*/
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.test.IReporter;
import edu.cmu.cs.fluid.tree.TestDigraph;
import edu.cmu.cs.fluid.tree.TestEdgeDigraph;


/**
 * Test that the regression tests for graphs still work for versioned slots.
 * @author boyland
 */
public class TestVersionedStructures implements IReporter {
  @Override
  public void reportError(String msg) {
    System.out.println("!!! "+msg);
  }
  public static void main(String[] args) {
    new TestVersionedStructures().test(args);
  }
  /**
   * @param args
   */
  void test(String[] args) {
    {
     TestDigraph td = new TestDigraph(this);
     td.test(args);
     if (td.getVerbose()) System.out.println("starting versioned tests");
     td.test("versioned",VersionedSlotFactory.prototype);
     if (td.getVerbose()) System.out.println("starting versioned dependent tests");
     td.test("dependent",VersionedSlotFactory.dependent);
     if (td.getVerbose()) System.out.println("starting versioned derived tests");
     td.test("bidirectional",VersionedSlotFactory.bidirectional(Version.getVersion()));
    }
    {
      TestEdgeDigraph td = new TestEdgeDigraph(this);
      td.test(args);
      if (td.getVerbose()) System.out.println("starting versioned tests");
      td.test("versioned",VersionedSlotFactory.prototype);
      if (td.getVerbose()) System.out.println("starting versioned dependent tests");
      td.test("dependent",VersionedSlotFactory.dependent);
      if (td.getVerbose()) System.out.println("starting versioned derived tests");
      td.test("bidirectional",VersionedSlotFactory.bidirectional(Version.getVersion()));
     }
    /*
    {
    TestSymmetricDigraph tsd = new TestSymmetricDigraph();
    tsd.setReporter(this);
    tsd.test(args);
    if (tsd.getVerbose()) System.out.println("starting versioned tests");
    tsd.test(VersionedSlotFactory.prototype);
    if (tsd.getVerbose()) System.out.println("starting versioned dependent tests");
    tsd.test(VersionedSlotFactory.dependent);
    if (tsd.getVerbose()) System.out.println("starting versioned derived tests");
    tsd.test(VersionedSlotFactory.bidirectional(Version.getVersion()));
    }{
    TestSymmetricEdgeDigraph tsd = new TestSymmetricEdgeDigraph(this);
    tsd.test(args);
    if (tsd.getVerbose()) System.out.println("starting versioned tests");
    tsd.test(VersionedSlotFactory.prototype);
    if (tsd.getVerbose()) System.out.println("starting versioned dependent tests");
    tsd.test(VersionedSlotFactory.dependent);
    if (tsd.getVerbose()) System.out.println("starting versioned derived tests");
    tsd.test(VersionedSlotFactory.bidirectional(Version.getVersion()));
    }
    */
  }
}
