package twoLocksOneRegion.inherited;

import com.surelogic.InRegion;
import com.surelogic.RegionLock;

@RegionLock("LL is duperLock protects R" /* is UNASSOCIATED */)
public class Duper extends Super {
  public final Object duperLock = new Object();
  
  @InRegion("R")
  public int field;
  
  
  public void worse() {
    field = 1; // should require L and not LL
  }
}
