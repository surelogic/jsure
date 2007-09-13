package test_requires_lock;

/**
 * Test detection of duplicate lock names in the requiresLock list.
 * 
 * @TestResult is INCONSISTENT
 * @RegionLock L is this protects Instance
 */
public class TestDuplicates {
  private int v;

  /**
   * Good.
   * @TestResult is CONSISTENT
   * @RequiresLock L
   */
  public void set(int a) { v = a; }
  
  /**
   * Good
   * @TestResult is CONSISTENT
   * @RequiresLock L, p:L
   */
  public void bad2(final TestDuplicates p) {}
  
  /**
   * BAD.
   * @TestResult is UNASSOCIATED
   * @RequiresLock L, L
   */
  public int get() { return v; }
  
  /**
   * BAD.
   * @TestResult is UNASSOCIATED
   * @RequiresLock L, this:L
   */
  public void bad() {}
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED
   * @RequiresLock p:L, p:L
   */
  public void bad(final TestDuplicates p) {
    // bad
    v = 1;
    // good
    p.v = 1;
  }
}
