package test_singleThreaded_constructor;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;

/**
 * Test checking of singleThreaded constructors using uniqueness 
 */
@RegionLock("L is this protects Instance")
public class TestBorrowed {
//  @SuppressWarnings("unused")
  private int v;
  
  /**
   * GOOD: receiver is not aliased.
   */
  @Borrowed("this")
  public TestBorrowed() {
    // Safe access
    v = 1;  
  }
  
  /**
   * BAD: Receiver is aliased.
   */
  @Borrowed("this")
  public TestBorrowed(Other o) {
    // Safe access
    v = 10;
    o.doStuff(this);
  }
}

class Other {
  public void doStuff(TestBorrowed tb) {}
}