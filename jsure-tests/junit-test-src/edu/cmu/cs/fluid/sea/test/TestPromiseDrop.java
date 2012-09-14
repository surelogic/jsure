package edu.cmu.cs.fluid.sea.test;

import junit.framework.TestCase;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.INodeVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;

/**
 * JUnit testcase for the {@link PromiseDrop} class.
 */
public class TestPromiseDrop extends TestCase {

  static private final IRNode i = new PlainIRNode();

  static private final AASTRootNode aast = new AASTRootNode(-1) {

    @Override
    public IAASTNode cloneTree() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String unparse(boolean debug, int indent) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <T> T accept(INodeVisitor<T> visitor) {
      // TODO Auto-generated method stub
      return null;
    }

  };
  
  static {
    aast.setPromisedFor(i);
  }

  static class TestLockPromiseDrop extends PromiseDrop<IAASTRootNode> {
    // Nothing to add
    TestLockPromiseDrop() {
      super(aast);
    }
  }

  @SuppressWarnings("unused")
  private TestLockPromiseDrop d1, d2, d3, d4, d5, d6, d7;

  public void testPromiseDropBasics() {
    assertFalse("promise should be unchecked at this point", d1.isCheckedByAnalysis());
    assertEquals("associated fAST node wrong", i, d1.getNode());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    d1 = new TestLockPromiseDrop();
    // d1.setNode(i);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Sea.getDefault().invalidateAll();
  }
}