package test_requires_lock;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

/**
 * Test detection of duplicate lock names in the requiresLock list.
 */
@RegionLocks({
  @RegionLock("L is this protects Instance" /* is INCONSISTENT */),
  @RegionLock("S is class protects StaticRegion")
})
@Region("protected static StaticRegion")
public class TestDuplicates {
  private int v;

  /**
   * Good.
   */
  @RequiresLock("L" /* is CONSISTENT */)
  public void set1(int a) { v = a; }

  /**
   * Good.
   */
  @RequiresLock("this:L" /* is CONSISTENT */)
  public void set2(int a) { v = a; }
  
  /**
   * BAD.
   */
  @RequiresLock("L, this:L" /* is UNASSOCIATED */)
  public void bad() {}
  
  /**
   * Good
   */
  @RequiresLock("S" /* is CONSISTENT */)
  public static void goodStatic1() {}
  
  /**
   * Good
   */
  @RequiresLock("test_requires_lock.TestDuplicates:S" /* is CONSISTENT */)
  public static void goodStatic2() {}
  
  /**
   * BAD
   */
  @RequiresLock("S, test_requires_lock.TestDuplicates:S" /* is UNASSOCIATED */)
  public static void badStatic1() {}
  
  @RequiresLock("S, test_requires_lock.Other1:S" /* is CONSISTENT */)
  public static void goodStatic10() {}
  
  @RequiresLock("test_requires_lock.TestDuplicates:S, test_requires_lock.Other1:S" /* is CONSISTENT */)
  public static void goodStatic11() {}
  
  @RequiresLock("test_requires_lock.TestDuplicates:S, test_requires_lock.TestDuplicates:S" /* is UNASSOCIATED */)
  public static void goodStatic12() {}

  /**
   * Good
   */
  @RequiresLock("L, p:L" /* is CONSISTENT */)
  public void good100(final TestDuplicates p) {}
  
  @RequiresLock("p1:L, p2:L" /* is CONSISTENT */)
  public void good101(final TestDuplicates p1, final Other1 p2) {}
  
  /**
   * BAD.
   */
  @RequiresLock("L, L" /* is UNASSOCIATED */)
  public int get() { return v; }
  
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
