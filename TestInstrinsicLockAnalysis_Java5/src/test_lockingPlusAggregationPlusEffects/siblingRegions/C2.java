package test_lockingPlusAggregationPlusEffects.siblingRegions;

import com.surelogic.Aggregate;
import com.surelogic.Lock;
import com.surelogic.Region;
import com.surelogic.Regions;
/* Created on Mar 3, 2005
 */
import com.surelogic.Unique;

@Regions({
  @Region("public X"),
  @Region("public Y")
})
@Lock("L2 is this protects Y")
public class C2 {
  @Unique
  @Aggregate("Instance into Instance, R into X, Q into Y")
  private D2 f;
  
  public C2() {
    this.f = new D2();
  }
  
  public void good1() {
    synchronized (this) {
      /* Writes <this.f>.Q --> this.Y, so needs lock for Y.
       * Has lock, so assures.
       */
      this.f.writesQ();
    }
  }
  
  public void bad1() {
    /* Writes <this.f>.Q --> this.Y, so needs lock for Y.
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
