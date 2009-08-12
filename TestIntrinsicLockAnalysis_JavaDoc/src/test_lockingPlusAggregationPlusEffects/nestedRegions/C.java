package test_lockingPlusAggregationPlusEffects.nestedRegions;
/* Created on Mar 3, 2005
 */

/**
 * @Region public X
 * @Region public Y extends X 
 * 
 * @RegionLock L is this protects Y
 */
public class C {
  /**
   * @Unique
   * @Aggregate Instance into Instance, R into X, Q into Y
   */
  private final D f;
  
  public C() {
    this.f = new D();
  }
  
  public void good1() {
    synchronized (this) {
      /* Writes <this.f>.Q --> this.Y, so needs lock for region Y.
       * Has lock, so should assure.
       */
      this.f.writesQ();
    }
  }
  
  public void bad1() {
    /* Writes <this.f>.Q --> this.Y, so needs lock for region Y.
     * Does not have lock, so should fail to assure.
     */
    this.f.writesQ();
  }
  
  public void bad2() {
    /* We need lock for region Y, even though the effect is on Region X because Q
     * is a subregion of R, and thus the method may change region Q, which is
     * mapped into region Y.
     */
    this.f.writesR();
  }
}
