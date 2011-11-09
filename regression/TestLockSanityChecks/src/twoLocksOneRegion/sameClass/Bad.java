package twoLocksOneRegion.sameClass;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;

@Region("public R")
@RegionLocks({
  @RegionLock("L is superLock protects R" /* is CONSISTENT */),
  @RegionLock("M is this protects R" /* is UNASSOCIATED */)
})
public class Bad {
  public final Object superLock = new Object();
}
