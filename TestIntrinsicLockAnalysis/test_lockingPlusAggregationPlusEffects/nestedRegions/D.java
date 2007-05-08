/* Created on Mar 3, 2005
 */
package test_lockingPlusAggregationPlusEffects.nestedRegions;

/**
 * @region public R 
 * @region public Q extends R
 */
public class D {
  /** @mapInto R */
  private int f1;
  
  /** @mapInto Q */
  private int f2;
  
  /** @writes nothing
   * @borrowed this */
  public D() {
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
