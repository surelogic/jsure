package edu.cmu.cs.fluid.sea.test;

import com.surelogic.aast.IAASTRootNode;

import junit.framework.TestCase;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;

/**
 * JUnit testcase for the {@link PromiseDrop} class.
 */
public class TestPromiseDrop extends TestCase {

  static private final IRNode i = new PlainIRNode();

  static class TestLockPromiseDrop extends PromiseDrop<IAASTRootNode> {
	  // Nothing to add
  }

  @SuppressWarnings("unused")
  private TestLockPromiseDrop d1, d2, d3, d4, d5, d6, d7;

  public void testPromiseDropBasics() {
    assertFalse("promise should be unchecked at this point", d1
        .isCheckedByAnalysis());
    assertEquals("associated fAST node wrong", i, d1.getNode());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    d1 = new TestLockPromiseDrop();
    d1.setNode(i);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Sea.getDefault().invalidateAll();
  }
}