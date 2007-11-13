package test;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("public R")
@RegionLock("LL is duperLock protects R")
public class Duper extends Super {
  public final Object duperLock = new Object();
  
  @InRegion("R")
  public int field;
  
  
  public void worse() {
    /* If region/field hiding weren't handled correctly, then lock analysis
     * would say that both the field accesses require the same lock, which
     * is not true.
     */
    ((Super) this).field = 2; // Needs L
    field = 1; // Needs LL
  }
}
