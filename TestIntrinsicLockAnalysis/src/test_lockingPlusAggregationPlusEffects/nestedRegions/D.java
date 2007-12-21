/* Created on Mar 3, 2005
 */
package test_lockingPlusAggregationPlusEffects.nestedRegions;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public R"),
  @Region("public Q extends R")
})
public class D {
  @InRegion("R")
  private int f1;
  
  @InRegion("Q")
  private int f2;
  
  @RegionEffects("none")
  @Borrowed("this")
  public D() {
    f1 = 0;
    f2 = 0;
  }
  
  @RegionEffects("writes Q")
  @Borrowed("this")
  public void writesQ() {
    f2 = 1;
  }
  
  @RegionEffects("writes R")
  @Borrowed("this")
  public void writesR() {
    f1 = 2;
  }
}
