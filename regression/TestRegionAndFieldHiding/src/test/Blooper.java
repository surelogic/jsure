package test;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("public R")
@RegionLock("LLL is blooperLock protects R")
public class Blooper extends Duper {
  public final Object blooperLock = new Object();
  
  @InRegion("R")
  public int field;
  
  
  public void worse() {
    /* If region/field hiding weren't handled correctly, then lock analysis
     * would say that all three field accesses require the same lock, which
     * is not true.
     */
    ((Super) this).field = 2; // Needs L
    ((Duper) this).field = 3; // Needs LL
    field = 1; // Needs LLL
  }
}
