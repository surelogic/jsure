package test_final_exprs;

/**
 * Tests that class expressions, e.g., Foo.class, are treated as final 
 * expressions.  (There are only positive examples.  There should be no 
 * way to make a class expression not be final.)
 */
public class TestClassExprs {
  public void good() {
    // Should get unidentifiable lock warning
    synchronized (Object.class) {
      // do stuff
    }
  }
}
