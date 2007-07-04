package duplicates_in_requiresLock;

/**
 * Test detection of duplicate lock names in the requiresLock list.
 * 
 * @lock L is this protects Instance
 */
public class TestDuplicates {
  private int v;

  /**
   * Good.
   * @requiresLock L
   */
  public void set(int a) { 
    // good
    v = a;
  }
  
  /**
   * Good
   * @requiresLock L, p.L
   */
  public void bad2(final TestDuplicates p) {}
  
  /**
   * BAD.
   * @requiresLock L, L
   */
  public int get() { 
    // good
    return v;
  }
  
  /**
   * BAD.
   * @requiresLock L, this.L
   */
  public void bad() {}
  
  /**
   * BAD
   * @requiresLock p.L, p.L
   */
  public void bad(final TestDuplicates p) {
    // bad
    v = 1;
    // good
    p.v = 1;
  }
}
