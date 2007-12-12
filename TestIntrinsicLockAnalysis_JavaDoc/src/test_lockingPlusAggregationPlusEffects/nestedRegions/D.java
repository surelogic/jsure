/* Created on Mar 3, 2005
 */
package test_lockingPlusAggregationPlusEffects.nestedRegions;

/**
 * @Region public R 
 * @Region public Q extends R
 */
public class D {
  /** @InRegion R */
  private int f1;
  
  /** @InRegion Q */
  private int f2;
  
  /** @Writes nothing
   * @Borrowed this */
  public D() {
    f1 = 0;
    f2 = 0;
  }
  
  /** @Writes Q 
   * @Borrowed this */
  public void writesQ() {
    f2 = 1;
  }
  
  /** @Writes R
   * @Borrowed this */
  public void writesR() {
    f1 = 2;
  }
}
