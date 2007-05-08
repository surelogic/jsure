package test_final_exprs;

/**
 * Tests the finalness or not of local variable uses.
 */
public class TestLocalVars {
  public void good_finalParameter(final Object p) {
    // FINAL: parameter is final
    synchronized (p) {
      // do stuff
    }
  }

  public void good_finalLocal() {
    final Object o = new Object();
    // FINAL: locla variable is final
    synchronized (o) {
      // do stuff
    }
  }
  
  
  
  public void good_nonfinalParameter_readOnly(Object p) {
    // FINAL: parameter is non-final, but readonly in the sync block
    synchronized (p) {
      // do stuff
      @SuppressWarnings("unused")
      Object local = p;
      // do more stuff
    }
  }

  public void good_nonfinalLocal_readOnly() {
    Object o = new Object();
    // FINAL: local variable is non-final, but readonly in the sync block
    synchronized (o) {
      // do stuff
      @SuppressWarnings("unused")
      Object local = o;
      // do more stuff
    }
  }
  
  
  
  public void bad_nonfinalParameter_writtenTo(Object p) {
    // NON-FINAL: parameter is non-final, but readonly in the sync block
    synchronized (p) {
      // do stuff
      p = new Object();
      // do more stuff
    }
  }

  public void bad_nonfinalLocal_writtenTo() {
    Object o = new Object();
    // NON-FINAL: local variable is non-final, but readonly in the sync block
    synchronized (o) {
      // do stuff
      o = new Object();
      // do more stuff
    }
  }
}
