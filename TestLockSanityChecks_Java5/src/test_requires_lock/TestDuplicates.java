package test_requires_lock;

import com.surelogic.Lock;
import com.surelogic.RequiresLock;

/**
 * Test detection of duplicate lock names in the requiresLock list.
 */
@Lock("L is this protects Instance" /* is INCONSISTENT */)
public class TestDuplicates {
  private int v;

  /**
   * Good.
   */
  @RequiresLock("L" /* is CONSISTENT */)
  public void set(int a) { v = a; }
  
  /**
   * Good
   */
  @RequiresLock("L, p:L" /* is CONSISTENT */)
  public void bad2(final TestDuplicates p) {}
  
  /**
   * BAD.
   */
  @RequiresLock("L, L" /* is UNASSOCIATED */)
  public int get() { return v; }
  
  /**
   * BAD.
   */
  @RequiresLock("L, this:L" /* is UNASSOCIATED */)
  public void bad() {}
  
  /**
   * BAD
   */
  @RequiresLock("p:L, p:L" /* is UNASSOCIATED */)
  public void bad(final TestDuplicates p) {
    // bad
    v = 1;
    // good
    p.v = 1;
  }
}
