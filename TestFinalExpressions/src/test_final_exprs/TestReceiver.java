package test_final_exprs;

/**
 * Tests that the receiver "this" is a final expression.
 * Tests that a qualified receiver "C.this" is a final expression.
 * There are no negative examples for this usage.
 */
public class TestReceiver {
  public void good_testReceiver() {
    // final
    synchronized (this) {
      // Do stuff
    }
  }

  
  
  public class C {
    public void good_testQualifiedReceiver() {
      // final
      synchronized (C.this) {
        // do stuff
      }
    }
  }
}
