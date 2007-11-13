package test;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("public R")
@RegionLock("L is superLock protects R")
public class Super {
  public final Object superLock = new Object();
  
  @InRegion("R")
  public int field;
  
  public void bad() {
    field = 1; // Needs L
  }
}
