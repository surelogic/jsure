/* Created on Mar 3, 2005
 */
package test_lockingPlusAggregationPlusEffects.siblingRegions;

/**
 * @region public R 
 * @region public Q
 */
public class D2 {
  /** @mapInto R */
  private int f1;
  
  /** @mapInto Q */
  private int f2;
  
  /** @writes nothing
   * @borrowed this */
  public D2() {
    f1 = 0;
    f2 = 0;
  }
  
  /** @writes Q 
   * @borrowed this */
  public void writesQ() {
    f2 = 1;
  }
  
  /** @writes R
   * @borrowed this */
  public void writesR() {
    f1 = 2;
  }
}
