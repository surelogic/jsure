package test_requires_lock;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;

@RegionLocks({
  @RegionLock("L is this protects Instance"),
  @RegionLock("S is class protects StaticRegion")
})
@Region("protected static StaticRegion")
public class Other1 {

}
