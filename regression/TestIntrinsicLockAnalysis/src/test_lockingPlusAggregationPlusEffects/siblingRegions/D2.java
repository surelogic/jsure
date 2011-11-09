/* Created on Mar 3, 2005
 */
package test_lockingPlusAggregationPlusEffects.siblingRegions;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public R"),
  @Region("public Q")
})
public class D2 {
  @InRegion("R")
  private int f1;
  
  @InRegion("Q")
  private int f2;

  @RegionEffects("none")
  @Borrowed("this")
  public D2() {
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
