package test_empty_RequiresLock;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

@Regions({
  @Region("public R1"),
  @Region("public R2")
})
@RegionLock("L1 is this protects R1")
public class C {
  @InRegion("R1")
  private int field1;
  
  @InRegion("R2")
  private int field2;

  
  
  @RequiresLock("L1" /* is INCONSISTENT: 1 good, 1 bad */)
  public void touchR1(int v) {
    field1 = v;
  }
  
  @RequiresLock("" /* is CONSISTENT: doesn't require anything */)
  public void touchR2(int v) {
    field2 = v;
  }
  
  
  
  public void stuff(int a, int b) {
    synchronized (this) {
      touchR1(a);
      touchR2(b);
    }
    
    touchR1(b);  // bad
    touchR2(a);
  }
}
