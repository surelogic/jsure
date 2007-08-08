package test_lockingPlusAggregationPlusEffects.nestedRegions;
/* Created on Mar 3, 2005
 */

/**
 * @Region public X
 * @Region public Y extends X 
 * 
 * @Lock L is this protects Y
 */
public class C {
  /**
   * @Unique
   * @Aggregate Instance into Instance, R into X, Q into Y
   */
  private D f;
  
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
    /* writes <this.f>.R --> this.X, which contains region Y, 
     * so need lock for region 
     */
    this.f.writesR();
  }
}
