/* Created on Mar 3, 2005
 */
package test_lockingPlusAggregationPlusEffects.siblingRegions;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Writes;

@Regions({
  @Region("public R"),
  @Region("public Q")
})
public class D2 {
  @InRegion("R")
  private int f1;
  
  @InRegion("Q")
  private int f2;

  @Writes("nothing")
  @Borrowed("this")
  public D2() {
    f1 = 0;
    f2 = 0;
  }
  
  @Writes("Q")
  @Borrowed("this")
  public void writesQ() {
    f2 = 1;
  }
  
  @Writes("R")
  @Borrowed("this")
  public void writesR() {
    f1 = 2;
  }
}
