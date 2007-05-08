package test_lockingPlusAggregationPlusEffects.siblingRegions;
/* Created on Mar 3, 2005
 */

/**
 * @region public X
 * @region public Y
 * 
 * @lock L2 is this protects Y
 */
public class C2 {
  /**
   * @unshared
   * @aggregate Instance into Instance, R into X, Q into Y
   */
  private D2 f;
  
  public C2() {
    this.f = new D2();
  }
  
  public void good1() {
    synchronized (this) {
      /* WRites <this.f>.Q --> this.Y, so needs lock for Y.
       * Has lock, so assures.
       */
      this.f.writesQ();
    }
  }
  
  public void bad1() {
    /* WRites <this.f>.Q --> this.Y, so needs lock for Y.
     * Doesn't have lock, should fail to assure.
     */
    this.f.writesQ();
  }
  
  public void good2() {
    /* Writes <this.f>.R --> this.X, which is independent from Y,
     * so no lock is needed.
     */
    this.f.writesR();
  }
}
