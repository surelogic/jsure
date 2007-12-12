/*
 * Created on Mar 2, 2005
 */
package test_aggregated_arrays;

/**
 * Test locking + aggregation with arrays.
 * 
 * @RegionLock L is this protects Instance
 */
public class Test {
  /**
   * @Unique
   * @Aggregate Instance into Instance
   */
  private Object[] good;
  private Object[] bad;
  
  public synchronized void goodMethod(Object o) {
    // GOOD: Access to this.good is protected
    // GOOD: Access to this.good[0] is protected
    this.good[0] = o;
    // GOOD: Access to this.bad is protected
    // WARNING: Possibly shared access to this.bad[0]
    this.bad[0] = o;
  }
  
  public Object badMethod1() {
    // BAD: Unprotected access to this.good
    // BAD: unprotected access to this.good[1]
    return this.good[1];
  }

  public Object badMethod2() {
    // BAD: Unprotected access to this.bad
    // WARNING: Possibly shared access to this.bad[1]
    return this.bad[1];
  }
}
