package test_singleThreaded_constructor;

/**
 * Test checking of singleThreaded constructors using uniqueness 
 * @author aarong
 *
 * @lock L is this protects Instance
 */
public class TestBorrowed {
//  @SuppressWarnings("unused")
  private int v;
  
  /**
   * GOOD: receiver is not aliased.
   * @singleThreaded
   * @borrowed this
   */
  public TestBorrowed() {
    // Safe access
    v = 1;  
  }
  
  /**
   * BAD: Receiver is aliased.
   * @singleThreaded
   * @borrowed this
   */
  public TestBorrowed(Other o) {
    // Safe access
    v = 10;
    o.doStuff(this);
  }
}

class Other {
  public void doStuff(TestBorrowed tb) {}
}